---
name: fromagerie-operations-hardening-frontier
description: >
  The hardening roadmap for LaFromagerie, where "beyond state of the art" means bulletproof
  DAILY OPERATIONS of a one-shop cheese business — zero payment losses, zero delivery-day
  surprises, crash-free — polish over novelty. Load when planning what to improve next,
  triaging operational risk, or scoping a hardening project. Each open problem has: why the
  current state falls short, the project's specific leverage, the first three concrete
  file-level steps IN THIS REPO, and a falsifiable "you have a result when…" milestone.
  Items covered: payment-funnel observability gaps, hosted-checkout hardening (3 MAJOR
  TODOs, owner-confirmed 2026-07-07: missing safety net, single-shot verification,
  hardcoded URL/key-in-APK batch), backend versioning (RESOLVED 2026-07-07, README
  residual), no admin auth, no CI, the 2 failing AdminViewModelTest tests, AGP 10
  migration debt, delivery-day offline resilience, cart persistence / client notifications.
---

# LaFromagerie — Operations Hardening Frontier

The frontier for THIS app is not novel features — it is **making the daily operation of one
real cheese shop unbreakable**: a customer's payment never silently lost, a delivery day
never derailed by a dead phone or a stale route, the app never crashing at the till or the
door. Everything below is a **candidate / open** problem, ranked by risk to the shop's daily
operation. Verified against the repo as of **2026-07-06** (HEAD `b97eb83`).

Read each item as: **Why it falls short → Our leverage → First three steps (file-level) →
Result when…**.

## When NOT to use this skill

- The concrete payment-reliability work plan → **fromagerie-payment-reliability-campaign**
  (this skill only frames payment *observability* at the roadmap level).
- How payments actually work today → **fromagerie-payments-reference**.
- How a change gets gated/merged / the failing-test baseline rules → **fromagerie-change-control**.
- Module/DI/flavor design & invariants → **fromagerie-architecture-contract**.
- Firestore contract & schema-evolution → **fromagerie-firestore-data-model**.
- Past dead ends so you don't refight them → **fromagerie-failure-archaeology**.
- Build/toolchain/AGP mechanics → **fromagerie-build-and-env**.

**Non-negotiables that bound every item here:** prod Firestore is sacred; never complete a
real payment; all changes go via PR to main. See **fromagerie-change-control**.

---

## Risk ranking (highest daily-operation risk first)

| # | Problem | Class | Daily-op blast radius |
|---|---|---|---|
| 1 | Payment-funnel observability gaps | payment | **Money lost silently** — worst case |
| 1b | **Hosted-checkout hardening — 3 MAJOR TODOs** (§1b) | payment | Paid-but-PENDING orders on the new path |
| 2 | Backend not in version control — ✅ resolved `ecda756` (2026-07-07), README residual | infra | ~~Server logic unrecoverable~~ — closed |
| 3 | Delivery-day offline resilience | ops | Delivery day derailed by dead signal |
| 4 | No admin authentication | security | Anyone with the APK controls the shop |
| 5 | No CI | quality | Broken flavor/tests reach main unnoticed |
| 6 | 2 failing AdminViewModelTest tests | quality | Green baseline is a lie; masks regressions |
| 7 | AGP 10 migration debt | infra | Future toolchain bump blocked |
| 8 | Cart persistence / client notifications | product | UX gaps (product-decision-pending) |

---

## 1. Payment-funnel observability gaps  (frontier framing only)

- **Why it falls short:** the payment chain is durable (WorkManager `FinalizePaymentWorker`
  reconciles PAID/CANCELED), but there is **no aggregate view** of where real payments
  succeed, stall, or fail. A silently-lost payment is the single worst outcome for the shop,
  and today it would only surface as a customer complaint or a missing order.
- **Our leverage:** Crashlytics + Analytics are already wired in `home/data`; the payment
  chain already has discrete states (checkout create → SumUp PUT → 3DS → poll → order write
  → finalize). Instrumenting the existing state transitions is cheap.
- **First three steps (mechanics belong to the campaign, not here):**
  1. Read **fromagerie-payment-reliability-campaign** — that is the owning plan; this item
     only tracks the *observability* facet.
  2. Enumerate the payment state transitions in
     `checkout/presentation/.../viewmodel/CheckoutViewModel.kt` and
     `checkout/data/.../work/FinalizePaymentWorker.kt`.
  3. Propose a minimal analytics/log breadcrumb per transition (no PII, no card data — the
     SumUp Ktor client already sanitizes auth headers and only logs bodies in debug).
- **Result when…** you can answer, from data and not anecdote, "in the last N real
  payments, how many reached `PAID` vs stalled at 3DS/polling vs were finalized by the
  worker after an app kill" — falsifiable against Firestore order statuses.

## 1b. Hosted-checkout hardening — 3 MAJOR TODOs (owner-confirmed 2026-07-07)

The hosted-checkout path landed in `ecda756` as working WIP (see
**fromagerie-payments-reference** for the wiring). Tommy confirmed these three gaps as
**major TODOs** before the path can be considered finished. Execution detail lives in
**fromagerie-payment-reliability-campaign** ("LANDED as working WIP" block); this is the
roadmap entry so no session misses them.

- **MAJOR TODO 1 — No durable safety net on the hosted path.** Unlike the Google Pay
  path, no `FinalizePaymentWorker` marker is scheduled around the custom-tab flow
  (`CheckoutViewModel.getSumUpPaymentLink` / `verifySumUpWebCheckoutStatus`): a customer
  who pays in the tab and never returns to the app leaves a paid-but-`PENDING` order
  that nobody reconciles. Fix direction: schedule the FPW marker before launching the
  tab (webhook confirmation is the stronger long-term fix).
- **MAJOR TODO 2 — Single-shot verification on return.** `verifySumUpWebCheckoutStatus()`
  checks the checkout reference once; a payment still processing at that instant shows
  the "contact support" error while money may have moved. Fix direction: reuse the
  existing polling loop (`MAX_POLLING_ATTEMPTS`) instead of a single check.
- **MAJOR TODO 3 — Mechanical hardening batch.** (a) the function URL
  `https://createsumupcheckout-ocgett4nrq-uc.a.run.app` is hardcoded in
  `SumUpDataSource.kt` — promote to a BuildConfig axis; (b) hosted-path *verification*
  still uses the app-side `SUMUP_PRIVATE_KEY`, so the key has not actually left the APK;
  (c) stray artifact `la-fromagerie-backend/.agents/.../xcode_spm_setup/.gitignore`
  rode in with `ecda756` — confirm and delete.
- **Result when…** a hosted-path payment interrupted at ANY point (tab dismissed, app
  killed, network drop on return) still converges to the correct Firestore order status
  without human intervention — provable with the FPW unit-test patterns in
  **fromagerie-validation-and-qa**, never with a real payment.

## 2. Backend not in version control — ✅ RESOLVED 2026-07-07 (commit `ecda756`)

- **Resolution:** the backend entered the repo at `ecda756` — `la-fromagerie-backend/`
  (functions source, `firebase.json`, `tsconfig.json`, `package.json`) is git-tracked;
  `functions/.env` (holding `SUMUP_SECRET_KEY`, `SUMUP_MERCHANT_CODE`) is correctly
  covered by `la-fromagerie-backend/.gitignore:66` — verified with `git check-ignore`,
  zero secrets committed. The same commit wired the hosted-checkout path the function
  serves (see **fromagerie-payments-reference**).
- **Residual (small, still open):**
  1. No backend README documenting the deploy command
     (`npm --prefix functions run deploy` → `firebase deploy --only functions`) and the
     required `.env` key names.
  2. A stray artifact rode in: `la-fromagerie-backend/.agents/skills/xcode-project-setup/scripts/xcode_spm_setup/.gitignore`
     (an Xcode-related tool leftover in an Android repo) — confirm with Tommy and remove.
  3. Deployed-vs-tracked drift is now checkable — after any backend deploy, the deployed
     code must equal the tracked source at HEAD.
- **Result when…** a fresh clone can deploy the backend from documentation alone —
  only the README residual stands between here and that.

## 3. Delivery-day offline resilience

- **Current state (verified, honest):**
  - Delivery **paths** have offline support: `GetAllDeliveryPathsUseCase` fetches from the
    local `RoomDeliveryRepository` (its KDoc says "for faster access and offline support"),
    and `MapBoxComposable` has an offline fallback for bounds.
  - The admin **order list** and **preparation_status** are read **Firestore-direct** via
    `FirebaseAdminRepositoryImpl` → `FirestoreAdminDatasource` — **no Room cache layer** for
    admin orders. (Firestore's SDK has default local persistence, but there is no explicit
    app-level pre-fetch of the day's orders for offline use — **UNVERIFIED (as of
    2026-07-06)** whether Firestore offline persistence is enabled/relied upon here.)
  - **Route optimization requires network:** `GetOptimizedDeliveryUseCase` calls Google
    Routes (`GoogleRouteDataSource`) and OpenRouteService (`OpenRouteDataSource`). If the
    route was not computed before losing signal, it cannot be computed on the road.
  - The foreground `DeliveryTrackingService` (`app/src/admin/.../DeliveryTrackingService.kt`)
    tracks location and posts notifications; that part works offline.
- **Why it falls short:** a delivery day in a low-signal rural area could leave the admin
  without the order list or an optimized route if either wasn't fetched/computed while
  online.
- **First three steps:**
  1. Verify Firestore offline persistence: grep for `setFirestoreSettings` /
     `persistenceEnabled` / `PersistentCacheSettings` across the repo; if absent, the app
     relies on SDK defaults — document which.
  2. Confirm whether the day's orders + optimized route are computed **before** departure and
     survive going offline (trace `AdminViewModel` load order vs
     `DeliveryTrackingService.startForeground`).
  3. If a gap exists, scope a pre-departure "prepare delivery day" snapshot (orders + route
     to local storage) — a candidate, product-adjacent change to surface.
- **Result when…** you can put the admin device in airplane mode after "start delivery" and
  still see the full ordered stop list and per-stop addresses for the day. (Falsifiable on
  device — never tap pay; this is admin flavor, no payment.)

## 4. No admin authentication

- **Why it falls short:** the admin flavor has **no auth of any kind** (grep-verified; the
  `auth` module is an empty stub — see **fromagerie-architecture-contract §1.1, §6**).
  Firebase Auth is a dependency in `home/data` but is not used to gate anything. The admin
  app is protected **only** by controlled APK distribution. Anyone who obtains the admin APK
  can edit prices/products, read every customer's order and address, and run deliveries.
- **Our leverage:** Firebase Auth is already a project dependency and Firestore is already
  Firebase; a single-operator login (one owner) is a small surface. The empty `auth` module
  is a ready home.
- **First three steps:**
  1. Confirm the current no-auth state:
     `grep -rin 'signIn\|FirebaseAuth\|currentUser\|login' app/src/admin admin --include='*.kt' | grep -v /build/`
     (expect no gate as of 2026-07-06).
  2. Decide the mechanism with Tommy (Firebase Auth email/single-operator vs device
     attestation) — **security change, surface first** (see change-control autonomy table).
  3. Implement the gate in the empty `:auth` module + admin `MainActivity`, and pair it with
     **Firestore Security Rules** (currently the real backstop is absent too — inspect rules
     read-only in the Firebase console).
- **Result when…** launching the admin flavor requires authenticating, AND prod Firestore
  rules reject unauthenticated writes to `products`/`delivery_paths`/`orders` — falsifiable
  by attempting an unauthenticated write from a test harness (never against prod data).

## 5. No CI

- **Why it falls short:** there is **no in-repo CI** (no `.github/workflows`, no CI yaml; the
  `fastlane` `distribute` lane points at a dormant CI-agent path). Nothing automatically
  assembles both flavors or runs the unit tests before merge — the gate is entirely the
  human pre-merge checklist (see **fromagerie-change-control §1.2**). A broken **admin**
  flavor can slip in behind a green client build.
- **Our leverage:** the checklist commands already exist and are cheap; encoding them is
  mechanical. GitHub is already the merge host.
- **First three steps:**
  1. Codify the exact gate from change-control §1.2 (`assembleClientDebug`,
     `assembleAdminDebug`, `testClientDebugUnitTest`, `testAdminDebugUnitTest`).
  2. Add a minimal GitHub Actions workflow that runs it on PRs to `main` (JDK/toolchain per
     **fromagerie-build-and-env**; secrets absent in CI bake `"null"` sentinels — CI must
     tolerate that, or the payment BuildConfig fields will be `"null"`).
  3. Make the 2 known-failing tests (#6) not red-wall the pipeline until triaged (allow-list
     or fix-first) so CI signal stays meaningful.
- **Result when…** every PR to main shows an automated check that both flavors assemble and
  the unit suite runs, with the known-failing set explicitly accounted for.

## 6. The 2 failing AdminViewModelTest tests

- **Why it falls short:** the baseline is **not green**. As of 2026-07-06, `AdminViewModelTest`
  (`admin/presentation`) has 2 failures around the "aborts when local image upload fails"
  behaviour (candidates: `addNewProduct aborts when local image upload fails`,
  `updateProduct aborts when local image upload fails`; MockK verification failures). A
  non-green baseline hides new regressions and normalizes ignoring red.
- **Our leverage:** small, isolated, fully local (no prod, no device). Ideal first hardening
  win.
- **First three steps:**
  1. Reproduce and capture the exact failing set:
     `./gradlew testAdminDebugUnitTest --continue` and read the report.
  2. **Triage: is the code wrong or the test wrong?** Read `AdminViewModel`'s image-upload
     abort path against the test's MockK `verify` expectations — decide which reflects the
     intended behaviour before changing anything.
  3. Fix the wrong side (and note in the PR which it was); if the *code* is wrong, this is a
     real bug in admin product editing (related to the `ProductEditDialog.kt:52` picture+data
     TODO — see **fromagerie-failure-archaeology**).
- **Result when…** `testAdminDebugUnitTest` is fully green AND the fix is documented as
  code-fix vs test-fix.

## 7. AGP 10 migration debt

- **Why it falls short:** `gradle.properties` carries deliberate AGP-9 opt-outs —
  `android.builtInKotlin=false` and `android.newDsl=false` — explicitly temporary ("must be
  migrated before AGP 10"; the AGP-9 migration was itself painful, git `9d8d53e "Temporary
  shit"`). AGP is pinned at `9.0.1`. Until these are migrated, an AGP 10 bump is blocked.
- **Our leverage:** the debt is localized to two flags and whatever DSL/built-in-Kotlin usage
  they gate; it's a bounded, well-understood migration, not open-ended.
- **First three steps:**
  1. Confirm the flags: `grep -nE 'builtInKotlin\|newDsl' gradle.properties` and the AGP pin
     `grep -n '^agp' gradle/libs.versions.toml`.
  2. On a **dedicated dependency-bump branch** (never mixed with features — change-control
     class (d)), flip one flag at a time and fix the resulting build errors.
  3. Read **fromagerie-failure-archaeology** for the AGP-9 opt-out reasons before assuming a
     flag is safe to flip.
- **Result when…** both flavors assemble and the unit suite runs with
  `android.builtInKotlin` and `android.newDsl` at their AGP-10 defaults (flags removed).

## 8. Cart persistence / client notifications (product-decision-pending)

- **Why it falls short:** two flagged TODOs mark deliberately-deferred product decisions,
  not bugs:
  - **Client notifications not built:** `app/src/client/.../MainActivity.kt:227` ("notifications
    feature not built yet"). The client has a notification button placeholder but no feature.
  - **Preferred-cart persistence:** `cart/presentation/.../CartViewModel.kt:30` (a "save a
    preferred cart" feature request "needs product/UX decisions").
  - (Related deferred: `checkout/.../CheckoutScreen.kt:243` legal/CGV decision, `:254` feature
    blocked on a state rework of the screen.)
- **Why it's ranked last:** none of these can break a payment or a delivery day; they are UX
  enhancements. Both are explicitly **product-decision-pending** — do NOT implement them
  autonomously (adding/removing user-facing features requires owner sign-off; see the "+
  button" doctrine in **fromagerie-change-control §3**).
- **First three steps (only after Tommy decides scope):**
  1. Surface the decision to Tommy with the two TODOs quoted.
  2. For notifications: confirm the delivery-day/notification infra already built for admin
     (`DeliveryTrackingService`, git `f272679`/`83b4ff2`) and what, if anything, is reusable.
  3. For cart persistence: define the DataStore key and UI entry point the TODO asks for
     (`shared_settings` already holds the live cart — see **fromagerie-run-and-operate** /
     **fromagerie-config-and-secrets**).
- **Result when…** the product decision is recorded, then (if approved) the feature ships
  behind the normal gate. Until then, this stays labeled **open / product-decision-pending**.

---

## Provenance and maintenance

| Item | Re-verification command |
|---|---|
| 1b. hosted-path safety net still missing | `grep -n "schedulePaymentFinalization" checkout/presentation/src/main/java/com/mtdevelopment/checkout/presentation/viewmodel/CheckoutViewModel.kt` (if it appears in the hosted-path functions, TODO 1 is done — update §1b) |
| 2. backend tracked, secrets out | `git ls-files la-fromagerie-backend | head` (expect functions source + firebase.json) and `git check-ignore la-fromagerie-backend/functions/.env` (expect a match) |
| 3. path offline support | `grep -rn 'offline' delivery --include='*.kt' \| grep -v /build/` |
| 3. Firestore offline persistence | `grep -rin 'persistenceEnabled\|PersistentCacheSettings\|setFirestoreSettings' . \| grep -v /build/` (empty = SDK defaults, UNVERIFIED) |
| 3. admin orders are Firestore-direct | read `admin/data/.../repository/FirebaseAdminRepositoryImpl.kt` (no Room = still direct) |
| 4. no admin auth | `grep -rin 'signIn\|FirebaseAuth\|currentUser\|login' app/src/admin admin --include='*.kt' \| grep -v /build/` |
| 5. no CI | `ls .github/workflows 2>/dev/null; find . -maxdepth 2 -iname '*.yml' -o -iname '*.yaml' \| grep -v /build/ \| grep -vi google-services` |
| 6. failing tests | `./gradlew testAdminDebugUnitTest --continue` |
| 7. AGP opt-out flags | `grep -nE 'builtInKotlin\|newDsl' gradle.properties; grep -n '^agp' gradle/libs.versions.toml` |
| 8. flagged TODOs | `grep -rn 'TODO' . \| grep '\.kt:' \| grep -v /build/ \| grep -v src/test` |

If any output diverges from this skill, update the affected item and date-stamp it.
