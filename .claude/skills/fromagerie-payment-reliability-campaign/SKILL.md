---
name: fromagerie-payment-reliability-campaign
description: Executable, decision-gated campaign to make the LaFromagerie payment chain bulletproof (north star - zero payment losses). Load this when working on payment reliability, payment observability, duplicate-charge protection, WorkManager finalization guarantees, polling timeouts, 3DS robustness, or when asked to "harden payments", "make sure no payment is lost", or investigate a customer charged without an order. Prereq reading - fromagerie-payments-reference.
---

# Payment Reliability Campaign

**Mission:** zero payment losses. A "payment loss" is any of: (a) customer charged, no usable order for the admin; (b) customer told "not charged" when they were; (c) customer double-charged; (d) order marked PAID without money received. Success is measured by evidence (Crashlytics/Analytics counts, Firestore vs SumUp-dashboard reconciliation), **never judged by eye**.

Prerequisite: read **fromagerie-payments-reference** first — this skill assumes you know the flow (steps 1–7), the polling machine, and the WorkManager safety net. Jargon is defined there; only campaign-specific terms are defined here. For how payment hardening ranks against the project's OTHER open risks (untracked backend, no admin auth, no CI, …), see **fromagerie-operations-hardening-frontier** — this campaign is item-level execution, that skill is the portfolio view.

**Hard rules (non-negotiable, from change control):** never complete a real payment (stop before the Google Pay button — the environment is PRODUCTION); never write/delete production Firestore from dev sessions; every change lands via branch → PR to `main`.

## Phase 0 — Baseline (run before proposing anything)

### 0.1 Test baseline

Task-name trap: `checkout:domain` has **no product flavors**, so its unit-test task is `testDebugUnitTest`. `checkout:data` and `checkout:presentation` are flavored (`client`/`admin`).

```bash
./gradlew :checkout:data:testClientDebugUnitTest :checkout:domain:testDebugUnitTest :checkout:presentation:testClientDebugUnitTest
```

**GATE:** expect `BUILD SUCCESSFUL` (all green — verified 2026-07-06). If a checkout test fails → you are not at the documented baseline; bisect against `main` before continuing (`git log --oneline -10`). If Gradle says "task not found" → re-check flavor config: `grep -l flavorDimensions checkout/*/build.gradle.kts` (as of 2026-07-06 that lists `data` and `presentation` only).

Known-failing elsewhere (do not chase as part of this campaign): 2 `AdminViewModelTest` failures in `admin:presentation` (baseline as of 2026-07-06; see fromagerie-failure-archaeology §13).

### 0.2 Instrumentation inventory (what we can currently observe)

```bash
grep -rn "FirebaseCrashlytics\|recordException\|logEvent" --include="*.kt" checkout/ | grep -v "/build/"
```

**GATE:** as of 2026-07-06 expect exactly three Crashlytics `recordException` sites, all in `CheckoutViewModel` (lines ~202, ~213: Google Pay unavailability/check errors; ~316: payment-flow exceptions) — plus log tags in `SumUpDataSource`/`FinalizePaymentWorker` visible only via logcat. If you see more → someone advanced Phase 1; read their work before re-instrumenting. **Conclusion of record:** there is currently NO payment funnel telemetry — you cannot count how many customers reach the sheet, hit 3DS, or time out.

### 0.3 Failure-mode map (traced from code, 2026-07-06)

Every place a payment can fail and what state results. File references: `checkout/presentation/.../viewmodel/CheckoutViewModel.kt` (CVM), `checkout/data/.../remote/source/SumUpDataSource.kt` (SDS), `checkout/data/.../work/FinalizePaymentWorker.kt` (FPW), `checkout/data/.../remote/source/FirestoreOrderDataSource.kt` (FOD).

| # | Failure point | Customer charged? | Firestore order | Detected by | Gap? |
|---|---|---|---|---|---|
| F1 | `createOrder` fails (step 2) | No | none | callback `isSuccess(false)` | benign |
| F2 | SumUp checkout creation fails (step 3) | No | orphan `PENDING` | UI error | **G4** (orphan) |
| F3 | User abandons Google Pay sheet | No | orphan `PENDING` | nothing | **G4** |
| F4 | PUT rejected (4xx/5xx) | No (auth failed) | `PENDING` | UI error + Crashlytics | G4 |
| F5 | 202 → 3DS abandoned/failed in WebView | Usually no; bank-dependent | `PENDING` until poll sees FAILED → FPW marks `CANCELED` | polling | relies on FPW |
| F6 | Polling timeout (~2 min, `POLLING_TIMEOUT`) | **Unknown — maybe** | `PENDING`; marker still set | UI shows error | **G2: UI says "Vous n'avez pas été débité" — unproven claim** |
| F7 | App killed after PUT (incl. process death during 3DS) | Maybe/yes | `PENDING` → FPW reconciles | marker + WorkManager | guaranteed *if* FPW runs (G3) |
| F8 | FPW gives up (5 attempts) | Maybe/yes | stays `PENDING`; marker kept | next app launch re-enqueues; marker expires after 24 h | **G3: depends on user reopening app** |
| F9 | FPW: Firestore `update()` fails repeatedly | Yes (PAID seen) | stays `PENDING` | retry → G3 | G3 |
| F10 | Retry after F6: user pays again | **Possible double charge** | second order doc; first marker **overwritten** (single-slot DataStore marker) | nothing | **G2/G7** |
| F11 | `orderId == null` at `setPaymentData` (process death between steps 2 and 5) | Yes possible | order exists but **no marker scheduled; payment proceeds anyway** | nothing | **G1** (CVM: marker scheduling is guarded by `checkoutId != null && orderId != null`, but `processCheckout` runs unconditionally) |
| F12 | Order PAID but refund needed later | n/a | admin handles manually via SumUp dashboard | out of scope | — |

**3DS robustness notes (traced):** 3DS runs in a `WebView` inside `ThreeDSecureActivity` — there is NO custom tab and NO deep link (`lafromagerie://` appears in no manifest intent-filter), so "custom-tab dismissal" and "deep-link re-entry" cannot occur in the current design. The real 3DS risks are: user back-presses out of the WebView (activity finishes without RESULT_OK — harmless, because the ViewModel's polling, already running since the 202, still resolves the status) and process death mid-challenge (→ F7, FPW reconciles). The 3DS activity result is decorative; polling is the source of truth.

**Ordering guarantee (payment-success vs order-write, traced):** the order doc is written **before** money can move (step 2 precedes steps 3–6), and `FOD.createOrder` uses `.document(id).set()` — idempotent per id, no duplicates on retry with the same id. So money-taken-with-no-order-recorded requires reconciliation failure (F8/F9): money taken while the order stays `PENDING` forever. The reverse — order-recorded-without-money — is routine and by design (`PENDING` orphans, F2/F3). **VERIFIED 2026-07-07:** the admin fetches ALL orders with no status filter (`FirestoreAdminDatasource.getAllOrders` — plain `collection("orders").get()`), so `PENDING` orders ARE visible to the admin. Caveat: a paid-but-stuck-PENDING order is indistinguishable from an abandoned-cart orphan `PENDING` — the admin is a backstop only if a stale-PENDING alert (Phase 3.3) exists.

**Identifier fact for dedup work (G6):** `checkout_reference` (`"Name-With-Dashes_<millis>"`, built in `CVM.createCheckout`) ≠ `orderId` (`"name#<millis>"`, built in `CVM.createOrder`). Two identifiers, generated at different moments, never equal — cross-referencing the SumUp dashboard with Firestore requires the marker or timestamps.

### 0.4 On-device state inspection (read-only, safe)

```bash
adb shell dumpsys package com.mtdevelopment.lafromagerie | grep lastUpdateTime   # stale-APK guard first!
adb shell run-as com.mtdevelopment.lafromagerie sh -c 'cat files/datastore/checkout_settings.preferences_pb' | strings
```

**GATE:** expect keys like `sumup_token`, and after any (never-completed) checkout attempt, the pending-finalization marker fields. If `run-as` fails → not a debug build; reinstall with `./gradlew :app:assembleClientDebug` + `adb install -r`.

## Phase 1 — Observability of the payment funnel

Goal: every funnel stage emits a countable event, so reliability becomes measurable *before* it is changed.

1. Branch off `main`. Add Crashlytics custom keys / Analytics events at: checkout screen shown → order created → SumUp session created → sheet opened → PUT sent → 202/3DS shown → terminal PAID/FAILED → `POLLING_TIMEOUT` → FPW ran/reconciled/gave up. Include `orderId` and `checkoutId` as keys on every payment exception (today's `recordException` calls carry neither).
2. Unit-test the funnel hooks with MockK (pattern: existing `CheckoutViewModelTest` in `checkout/presentation/src/test/`, `PaymentFinalizationUseCasesTest` in `checkout/domain/src/test/`).

**GATE 1:** Phase 0.1 test command green AND an adb dry run to the checkout screen shows the new events in logcat (`adb logcat -s FirebaseCrashlytics` or your chosen tag). If events don't fire on device → run the stale-APK guard (0.4) before debugging code. **Stop before the Google Pay button.**

Promotion: PR per protocol (bottom). Only after this phase do failure *rates* exist; later phases cite them.

## Phase 2 — Ambiguous-timeout honesty and the single-marker problem (G2, G7, F10)

Facts (traced 2026-07-06): on `POLLING_TIMEOUT` the flow throws; `CheckoutViewModel`'s catch shows "Vous n'avez pas été débité, merci de réessayer" — an **unproven claim**, since timeout means *outcome unknown*, not "not charged". A retry then generates a fresh order + checkout, and `setPendingFinalization` **overwrites** the previous marker (single-slot DataStore preference in `checkout/data/.../local/CheckoutDatastorePreferenceImpl.kt`), so the first attempt loses its reconciler while its charge may still land → double charge plus an unreconciled first order.

1. Distinguish post-PUT ambiguity (`POLLING_TIMEOUT` and any error after the PUT was sent) from pre-PUT errors in the UI: post-PUT must say "payment status unknown — do not pay again; we are verifying" and block immediate retry while a marker is pending.
2. **Candidate** (stays candidate until tested): replace the single-slot marker with a small journal (list of pending finalizations) so overlapping attempts each get reconciled. Verification obligation: unit tests proving two interleaved markers both reconcile (Turbine on the DataStore flow); FPW iterates the journal.

**GATE 2:** unit tests green AND a manual code trace shows no path where a post-PUT failure leads to a retry-enabled UI while a marker is live. If the required state rework is too invasive → note that `checkout/.../CheckoutScreen.kt:254` already records a blocked state-rework TODO; scope the fix to the ViewModel error branch instead and record the deferral in fromagerie-operations-hardening-frontier.

## Phase 3 — Finalization guarantees (G1, G3, F8/F9)

What is **actually guaranteed** today (traced from `FinalizePaymentWorker` + `WorkManagerPaymentFinalizationScheduler`, 2026-07-06): the enqueued unique work (`"payment_finalization"`, policy REPLACE) is persisted by WorkManager, so it survives app kill **and device reboot**; it waits for `NetworkType.CONNECTED`, retries with linear 30 s backoff up to `MAX_RUN_ATTEMPTS = 5`; each attempt re-polls SumUp for up to ~2 min; on give-up the marker survives and is re-enqueued **only at the next app launch** (`CheeseApplication` → `ResumePendingPaymentFinalizationUseCase`); the marker expires after 24 h (`PendingPaymentFinalization.MAX_AGE_MILLIS`), after which the SumUp dashboard is the only source of truth. **Assumed, not guaranteed:** that the user reopens the app within 24 h after a give-up (F8), and that the order document exists for `update()` to succeed (F9 — `update` fails on a missing doc; it does not create).

1. Close **G1** first (small, testable): in `CheckoutViewModel.setPaymentData`, refuse to submit the payment when `orderId == null || checkoutId == null` — surface an error instead of proceeding without a safety net.
2. **Candidate:** on FPW give-up, self-schedule a delayed retry (`OneTimeWorkRequest` with initial delay) instead of waiting for an app launch, or extend attempts with exponential backoff. Verification obligation: unit tests on the retry decision + an on-device dry run where a *simulated* marker (written via a debug hook, never a real payment) shows the worker resuming after airplane-mode toggling.
3. **Candidate:** admin-side alert — surface `PENDING` orders older than N hours in the admin flavor. Blocked on resolving the 0.3 UNVERIFIED item (does the admin preparation screen show PENDING orders at all?).

**GATE 3:** extended `PaymentFinalizationUseCasesTest` green AND WorkManager diagnostics show the work enqueued after a simulated marker: `adb shell am broadcast -a "androidx.work.diagnostics.REQUEST_DIAGNOSTICS" -p com.mtdevelopment.lafromagerie` then `adb logcat -s WM-Diagnostics` (UNVERIFIED (as of 2026-07-06): exact logcat tag on this WorkManager version — if empty, fall back to `adb shell dumpsys jobscheduler | grep -i -A4 lafromagerie`). If neither shows the job → confirm debug build and the WorkManager version in `gradle/libs.versions.toml` before concluding the scheduler is broken.

## Phase 4 — Duplicate-payment protection (G6)

1. **Candidate:** unify identifiers — derive the SumUp `checkout_reference` from the Firestore `orderId`. Then "is there already a session for this order?" becomes one GET (`SumUpDataSource.getCheckoutsList(reference=…)` already supports filtering). Verification obligations: confirm SumUp accepts the `#` character in references (UNVERIFIED as of 2026-07-06 — test with a read-only GET filter first, or sanitize the id); unit tests for the derivation; `grep -rn "checkoutRef" checkout/` to confirm nothing depends on the current `Name_millis` format.
2. **Candidate:** before creating a new checkout (step 3), query `getCheckoutsList(reference)` for an existing PENDING session and resume it instead of creating a duplicate.

**GATE 4:** unit tests green AND a read-only GET against the production API (GETs are safe) filtered by a *historical* reference parses into `CheckoutResponse` correctly. If parsing fails → fix `CheckoutResponse` nullability first; it is also the polling workhorse, so a parse bug there is itself a reliability bug.

## Phase 5 — Server-side confirmation (candidate horizon)

**Candidate: webhook/server-side confirmation via a Cloud Function.** A function receiving SumUp status callbacks could write the order status server-side, removing the dependency on the customer's device entirely — the strongest possible fix for F7/F8, and now for the hosted path's missing safety net too. **Prerequisite CLEARED 2026-07-07:** the backend entered version control at commit `ecda756` (`la-fromagerie-backend/` tracked; `functions/.env` gitignored — verified). UNVERIFIED (as of 2026-07-06): SumUp webhook capabilities/payload format — verify against current SumUp API docs before designing anything.

**LANDED as working WIP (`ecda756`, 2026-07-07): the hosted-checkout path.** Owner-labeled "close to finish" — see fromagerie-payments-reference for the full wiring and the path-differences table. The hardening work this campaign now owns for it (each traced from the `ecda756` diff, unit-verified where possible before promoting):
1. **No durable safety net:** unlike the Google Pay path, no `FinalizePaymentWorker` marker is scheduled — a customer who pays in the custom tab and never returns to the app (tab dismissed, app killed) leaves a paid-but-PENDING order until a human notices. Extending the FPW marker to the hosted path is the natural fix; webhook confirmation (above) is the strong fix.
2. **Single-shot verification:** `verifySumUpWebCheckoutStatus()` checks the reference once on deep-link return, not with the polling loop — a checkout still mid-processing at that instant reads as "not validated" and shows the support-contact error while money may have moved (same class as F6/G2 honesty issues).
3. **Duplicate-protection is per-path now:** hosted `checkout_reference = orderId` (good — identifier unification arrived for free), but a customer who fails Google Pay then retries hosted (or taps the button twice) needs the two-path duplicate analysis redone before this leaves WIP.
4. **Mechanical hardening:** function URL is hardcoded in `SumUpDataSource.kt` (not a BuildConfig axis); amount crosses the boundary as `Double` (`toPriceDouble()` — keep the cent-Long invariant in sight); the ad-hoc CIO client has no logging; the app-side `SUMUP_PRIVATE_KEY` is still used for hosted-path verification, so the key has not left the APK yet.
5. **AMOUNT-INTEGRITY HOLE (found 2026-07-07 audit, MAJOR):** the Cloud Function is `invoker: "public"` with no auth/App Check and **trusts the client-supplied `amount` and `orderId` verbatim** (`la-fromagerie-backend/functions/src/index.ts`), and `verifySumUpWebCheckoutStatus()` accepts ANY checkout with status `PAID` for the reference **without comparing the paid amount to the order total**. A tampered client (or plain curl) can create a 0.01 € hosted checkout with `checkout_reference = <their orderId>`, pay 1 cent, return to the app → full order marked `PAID`. Fix direction: the function derives the amount server-side from the Firestore order doc (ignore the client amount), and/or verification compares `checkout.amount` to the stored order total before finalizing. Related fail-unsafe defaults in `CheckoutViewModel.getSumUpPaymentLink()`: `totalPrice ?: 0.0` and `orderId ?: ""` silently create a 0 €/blank-reference checkout when state is missing — should abort instead.
6. **Firestore rules not in the repo (found 2026-07-07 audit):** no `*.rules` file anywhere and `firebase.json` has no `firestore` section — rules live only in the console, unauditable from the repo. With no user auth in the app, if the console rules are permissive, `orders`/`products` are world-writable with the API key from the APK. Export the live rules into `la-fromagerie-backend/` and review them (read-only export is safe; changing rules needs Tommy).

## Solution menu, ranked

| Rank | Solution | Status | Verification obligation |
|---|---|---|---|
| 1 | Funnel telemetry (Phase 1) | ready to build | events visible in logcat dry run; counts appear in Crashlytics/Analytics console |
| 2 | G1 null-guard before submit (Phase 3.1) | ready to build | unit test: no PUT when orderId/checkoutId null |
| 3 | Honest ambiguous-outcome UX + retry block (Phase 2.1) | ready to build | unit tests + code trace at GATE 2 |
| 4 | Pending-finalization journal replacing single-slot marker (Phase 2.2) | **candidate** | interleaved-marker unit tests (Turbine) |
| 5 | FPW self-rescheduling after give-up (Phase 3.2) | **candidate** | retry-policy tests + simulated-marker device run |
| 6 | orderId-derived checkout_reference + session reuse (Phase 4) | **candidate** | SumUp `#` acceptance check; GET-filter parse test |
| 7 | Admin stale-PENDING alert (Phase 3.3) | **candidate** | blocked on admin-screen UNVERIFIED item |
| 8 | Webhook confirmation via Cloud Function (Phase 5) | **candidate** (unblocked 2026-07-07: backend in VCS since `ecda756`) | SumUp webhook capabilities still UNVERIFIED |
| 9 | Hosted-checkout path via `createSumUpCheckout` | **LANDED as working WIP** (`ecda756`) | harden before calling it done: FPW safety net, single-shot verification, two-path duplicate analysis, hardcoded URL — see the LANDED block above |

## Known wrong paths — fenced off

| Wrong path | Why it is fenced | Instead |
|---|---|---|
| SumUp **test merchant + Google Pay** | HTTP 409 `NON_EXTDEV_PAYMENT_METHOD`; fundamentally incompatible (settled — fromagerie-failure-archaeology §1) | MockK unit tests; code-path reading; GET-only API checks |
| **Completing a real payment** "just to see" | PRODUCTION Google Pay environment, real merchant, real money | adb-drive the UI up to but never past the Google Pay button |
| Trusting a **single poll result** as final | PENDING is a normal transient; only PAID/FAILED are terminal | Let `pollCheckoutStatus` / FPW own the loop |
| Writing to **prod Firestore** from dev sessions | Production data of a single real shop is sacred | Unit tests; read-only inspection; simulated markers in local DataStore only |
| Raising `MAX_POLLING_ATTEMPTS` alone to "fix" timeouts | Delays the ambiguity without resolving it; the app can be killed anyway | Phase 2 (honest UX) + Phase 3 (durable reconciliation) |
| Removing AGP opt-outs while touching checkout builds | Separate settled battle with open migration work | fromagerie-failure-archaeology §2 |

## Validation-and-promotion protocol (every phase)

1. Branch off `main` (never commit to `main` directly).
2. Relevant unit tests green: the Phase 0.1 command + any module you touched.
3. **Both flavors build:** `./gradlew :app:assembleClientDebug :app:assembleAdminDebug`.
4. On-device dry run (client debug; current device convention: Pixel 7 Pro): stale-APK guard → drive to checkout screen → verify logs/events → **stop before the pay button**.
5. PR to `main` via GitHub; merge only when 2–4 are evidenced in the PR description (full gating rules: fromagerie-change-control).
6. Post-merge success metrics — measurable, never by eye: per-stage funnel event counts (Phase 1) with no unexplained drop-offs; `POLLING_TIMEOUT` count trending to zero; FPW give-up count = 0; periodic manual reconciliation of the SumUp dashboard against the `orders` collection (read-only) showing zero unmatched charges.

## When NOT to use this skill

- You just need to know how payments work → **fromagerie-payments-reference**
- Why a payment decision was made historically → **fromagerie-failure-archaeology**
- A live payment incident is happening right now → **fromagerie-debugging-playbook** first (triage), then return here for the durable fix
- Broader roadmap beyond payments → **fromagerie-operations-hardening-frontier**
- Test-writing conventions and the evidence bar → **fromagerie-validation-and-qa**
- Secrets needed to build the checkout module → **fromagerie-config-and-secrets**

## Provenance and maintenance

Traced 2026-07-06 against HEAD `b97eb83` (worktree branch `claude/distracted-chaum-0986e4`). Re-verification one-liners for drift-prone claims:

- Test baseline: `./gradlew :checkout:data:testClientDebugUnitTest :checkout:domain:testDebugUnitTest :checkout:presentation:testClientDebugUnitTest` (expect green)
- Instrumentation still sparse: `grep -rn "recordException\|logEvent" --include="*.kt" checkout/ | grep -v /build/` (3 hits, all in CheckoutViewModel = Phase 1 not done)
- G1 guard still missing: `grep -n "checkoutId != null && orderId != null" checkout/presentation/src/main/java/com/mtdevelopment/checkout/presentation/viewmodel/CheckoutViewModel.kt` (a hit = unfixed)
- Single-slot marker: `grep -n "setPendingFinalization" checkout/data/src/main/java/com/mtdevelopment/checkout/data/local/CheckoutDatastorePreferenceImpl.kt`
- FPW policy: `grep -n "MAX_RUN_ATTEMPTS\|ExistingWorkPolicy\|MAX_AGE_MILLIS" checkout/data/src/main/java/com/mtdevelopment/checkout/data/work/*.kt checkout/domain/src/main/java/com/mtdevelopment/checkout/domain/model/PendingPaymentFinalization.kt`
- Timeout copy still ambiguous: `grep -n "débité" checkout/presentation/src/main/java/com/mtdevelopment/checkout/presentation/viewmodel/CheckoutViewModel.kt`
- Identifier mismatch: `grep -n "checkoutRef\|orderId = " checkout/presentation/src/main/java/com/mtdevelopment/checkout/presentation/viewmodel/CheckoutViewModel.kt`
- 3DS still WebView / no deep link: `grep -rn "lafromagerie://" --include="AndroidManifest.xml" app/src checkout` (expect empty)
- Backend still unversioned: from the main checkout, `git -C /Users/tommy/StudioProjects/LaFromagerie check-ignore la-fromagerie-backend/functions && echo STILL-IGNORED`
