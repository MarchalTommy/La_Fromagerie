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
  residual), admin auth (PIN gate landed 2026-07-08, Firestore rules still open), secret
  handling (18 secrets in a TRACKED gradle.properties and `"null"`-sentinel builds that pass
  silently, found 2026-07-27), admin code leaking into the client AAB / broken flavor
  isolation (found 2026-07-24: manifest patched, dependency wiring still to fix — also the
  Play Store background-location blocker), geocoding on a retired government host (found
  and migrated to the Géoplateforme 2026-07-30), delivery-day offline resilience, no CI, the 2 failing
  AdminViewModelTest tests, AGP 10 migration debt, cart persistence / client
  notifications.
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

| #  | Problem | Class | Daily-op blast radius |
|----|---|---|---|
| 1  | Payment-funnel observability gaps | payment | **Money lost silently** — worst case |
| 1b | **Hosted-checkout hardening — 3 MAJOR TODOs** (§1b) | payment | Paid-but-PENDING orders on the new path |
| 2  | Backend not in version control — ✅ resolved `ecda756` (2026-07-07), README residual | infra | ~~Server logic unrecoverable~~ — closed |
| 3  | Delivery-day offline resilience | ops | Delivery day derailed by dead signal |
| 3b | Geocoding on a retired government host — ✅ migrated to `data.geopf.fr` 2026-07-30, on-device pass residual (§3b) | infra | ~~All address matching dies at once, on someone else's schedule~~ — closed |
| 4  | Admin auth — PIN gate LANDED 2026-07-08; hardened Firestore rules still undeployed | security | UI gated; DB still world-writable until rules ship |
| 4b | **Admin code leaks into the client AAB** (§4b) — manifest patched 2026-07-24, wiring NOT fixed | security | Admin keys ship to customers; blocks Play releases |
| 4c | **Secrets in a tracked `gradle.properties` + `"null"` builds pass silently** (§4b) | security / quality | One `git add -A` leaks SumUp + keystore keys; one `git restore` loses them |
| 5  | No CI | quality | Broken flavor/tests reach main unnoticed |
| 6  | 2 failing AdminViewModelTest tests | quality | Green baseline is a lie; masks regressions |
| 7  | AGP 10 migration debt | infra | Future toolchain bump blocked |
| 8  | Cart persistence (client notifications SHIPPED 2026-07-07) | product | UX gap (product-decision-pending) |

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

- **MAJOR TODO 1 — hosted-path durable safety net — ✅ CLOSED 2026-07-08.**
  `CheckoutViewModel.getSumUpPaymentLink` now schedules the `FinalizePaymentWorker`
  marker BEFORE opening the custom tab. The Cloud Function never returns the SumUp
  session id, so the marker carries `checkoutId = null` + `expectedAmountCents`, and
  the worker reconciles by `checkout_reference` (= orderId) via
  `SumUpDataSource.pollHostedCheckoutStatus`, refusing any PAID session whose amount
  does not match the order total (unit-tested: `SumUpDataSourceTest`,
  `PaymentFinalizationUseCasesTest`, `CheckoutViewModelTest`). Webhook confirmation
  remains the stronger long-term fix (campaign Phase 5).
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

## 3b. Geocoding ran on a retired government host — RESOLVED 2026-07-30

- **Resolution (same day it was found):** all four call sites now target
  `data.geopf.fr/geocodage`. Each endpoint shape was curl-verified against the new host before
  the change and returns byte-identical payloads to the old one; the response DTOs did not move.
  The duplicated `AUTOCOMPLETE_*` constant pair in `core/data` was **deleted** rather than
  fixed — it was malformed (`host` containing a slash, which Ktor's `URLBuilder.host` cannot
  take) and it duplicated `ADDRESS_*`. One host constant per module remains, plus a shared
  `GEOCODAGE_PATH_PREFIX`, and `GeocodingHostTest` (`delivery/data`) fails if the two modules
  drift apart or if a host regains a path segment.
- **Residual:** the on-device pass of the client delivery form and the admin path editor was not
  done in that session (device locked); unit tests and both-flavor compiles were green. Worth one
  confirmation run on the next device session.
- **Original finding, kept for context (verified 2026-07-30 by curl and by Ktor logs on device):**
  every call to `api-adresse.data.gouv.fr` answered `200` **and** carried:
  ```
  x-api-deprecated: true
  sunset: Sat, 31 Jan 2026 22:59:59 GMT
  x-api-new-host: https://data.geopf.fr/geocodage/
  ```
  The sunset date had passed roughly six months earlier; the host answered only by grace.
- **What would have broken when it stopped — all at once, with no app change to trigger it:**
  - customer address → tournée matching (`AddressApiDataSource.getLngLatFromAddress`),
  - path city → lat/lng, which drops any path whose cities fail to geocode **from the picker
    entirely** (see `fromagerie-delivery-logistics-reference`, enrichment pipeline step 3),
  - city autocomplete on both the client form and the admin path editor,
  - street suggestions in the path editor.
  Errors are swallowed into `emptyList()` / `null` throughout, so the failure mode is not a
  crash — it is *"no path covers your address"* shown to paying customers.
- **The cruel detail that made it look half-fixed:** a `data.geopf.fr/geocodage` constant already
  existed and was already wired as the Ktor `DefaultRequest` host for the autocomplete client —
  so the migration *looked* started. It was inert twice over: `AutoCompleteApiDataSource` set
  `host = ADDRESS_API_BASE_URL_WITHOUT_HTTPS` per request and a per-request host wins, and the
  constant was itself unusable as a host because of the embedded slash. Two plausible-looking
  constants, neither doing anything.
- **Lesson worth carrying:** an "already migrated" constant proves nothing until you trace it to
  the request that actually goes out. Read the Ktor log, not the constant.
- **Result when…** `curl -s -D- -o /dev/null "<each URL the app builds>" | grep -i deprecated`
  returns nothing for every endpoint the app calls, and address matching still resolves a known
  Boujailles address to the right path on device. First half done 2026-07-30; the on-device half
  is the residual noted above.

## 4. Admin authentication — app-side gate LANDED 2026-07-08, rules half still open

- **What landed:** the admin flavor now has a **PIN gate** in the `:auth` module
  (branch `claude/admin-pin-auth`; details in **fromagerie-architecture-contract §1.1**).
  A 6-digit PIN is the password of one fixed Firebase account, so the admin app now
  holds a real `request.auth != null` identity, and the admin `MainActivity` blocks the
  whole UI until sign-in. Session persists (one prompt per install). Unit-tested
  (`:auth:testDebugUnitTest`, 10 tests).
- **What still falls short (the open half):** the gate only *keeps people out of the
  admin UI*. It does NOT yet protect the database, because the live Firestore rules are
  still `allow write: if true` (`la-fromagerie-backend/firestore.rules`). Anyone with
  ANY APK's API key can still `curl` prod. Deploying the hardened rules
  (`firestore.rules.hardened-proposal`, which gates on `request.auth != null`) is the
  remaining step — **needs Tommy** (rules deploy + it depends on the console account
  existing). Also low PIN entropy (10^6), mitigated by Firebase throttling + hand
  distribution, not bank-grade.
- **Remaining steps:**
  1. **Console setup** (owner, one-time): enable Email/Password, create the single
     operator account — `la-fromagerie-backend/ADMIN_AUTH_SETUP.md`. Until done, every
     PIN returns an error (provider/user absent).
  2. **Deploy hardened rules** and flip the `TODO(admin-auth)` blocks to
     `request.auth != null` once the account exists — `FIRESTORE_RULES_AUDIT.md`.
  3. On-device verify the gate (admin flavor, no payment): PIN screen → wrong/right code
     → session persists across relaunch.
- **Result when…** launching the admin flavor requires the PIN, AND prod Firestore rules
  reject unauthenticated writes to `products`/`delivery_paths`/`orders` — falsifiable by
  attempting an unauthenticated write from a test harness (never against prod data).
  App-side milestone is met; rules milestone is not yet.
- **Re-verify the gate exists:**
  `grep -rn 'PinLockScreen\|authViewModel' app/src/admin --include='*.kt' | grep -v /build/`

## 4b. Admin code leaks into the client AAB — flavor isolation is broken (found 2026-07-24)

- **Why it falls short:** `app/build.gradle.kts:111-114` wires the admin modules with
  `adminImplementation`, which *looks* like the client flavor excludes them. It does not.
  Four **shared** modules (compiled into both flavors) depend on `:admin:presentation` with
  a plain `implementation`, dragging the whole module — manifest *and* classes — into the
  client build:
  - `home/presentation/build.gradle.kts:54`
  - `details/presentation/build.gradle.kts:50`
  - `checkout/presentation/build.gradle.kts:54`
  - `delivery/presentation/build.gradle.kts:59`

  This **defeats the stated goal of commit `c436c68`** ("admin API keys will NOT be built
  into clients version of the app, protecting them against reverse-engineering"): admin
  code ships to every customer. It first surfaced as a **Play Store release blocker** —
  `ACCESS_BACKGROUND_LOCATION` appeared in the signed client AAB with no client-facing
  feature to justify it (blame: *"ADDED from [:admin:presentation]"* in
  `app/build/outputs/logs/manifest-merger-client-release-report.txt`).
- **Symptom treated, cause NOT fixed (2026-07-24):** the *manifest* half is patched —
  `app/src/client/AndroidManifest.xml` strips `ACCESS_BACKGROUND_LOCATION` and
  `FOREGROUND_SERVICE_LOCATION` via `tools:node="remove"`, and the admin-only
  `DeliveryTrackingService` + `NotificationBroadcastReceiver` declarations moved from
  `app/src/main/AndroidManifest.xml` to a new `app/src/admin/AndroidManifest.xml` (their
  classes were always admin-only in `app/src/admin/java`, so the client AAB was declaring a
  location foreground service it could not even run). **The classes still ship to clients.**
  This is a band-aid with a documented removal condition, not a fix.
- **Our leverage:** the correct wiring already exists and is proven — `adminImplementation`
  in `app/build.gradle.kts`, plus the `auth` module precedent. The change is four lines.
- **First three steps:**
  1. Flip the four `implementation(project(":admin:presentation"))` above to
     `"adminImplementation"(project(":admin:presentation"))`.
  2. Compile the **client** flavor and triage every unresolved reference: shared code in
     those modules that touches admin classes is the real coupling. Push it behind an
     interface owned by the shared module (admin supplies the impl via Koin), or move it
     into the `admin` source set. Do **not** re-add the dependency to make it compile.
  3. Once client compiles, delete the two `tools:node="remove"` lines from
     `app/src/client/AndroidManifest.xml` and re-verify (below). Both flavors must assemble.
- **Result when…** the client release manifest contains **no** `ACCESS_BACKGROUND_LOCATION`
  *without* any `tools:node="remove"` crutch, AND `:admin:presentation` no longer appears in
  the client dependency graph:
  ```
  ./gradlew :app:dependencies --configuration clientReleaseRuntimeClasspath | grep -i "admin"
  ```
  (empty = fixed), cross-checked against the merge report blame:
  ```
  grep -A2 BACKGROUND_LOCATION app/build/outputs/logs/manifest-merger-client-release-report.txt
  ```
- **Priority:** treat as the top non-payment item — it is a security regression (admin keys
  in customer hands) *and* it gates Play Store releases. Own branch, own PR.

## 4c. Secret handling — 18 secrets live in a TRACKED file, and `"null"` builds pass silently

Discovered 2026-07-27 while diagnosing a black delivery map on an AI-built APK. Two distinct
problems, one shared root: `gradle.properties`.

- **Why it falls short — (a) the leak risk.** All 18 secrets (`SUMUP_PRIVATE_KEY`,
  `KEYSTORE_PASS`, `KEYSTORE_ALIAS_PASS`, `CLOUDINARY_PRIVATE`, `MAPBOX_*`,
  `GOOGLE_PAY_*`, `OPEN_ROUTE_TOKEN`, …) live in the **root `gradle.properties`**, which is
  **tracked by git and absent from `.gitignore`**. They exist only as an *uncommitted
  working-tree modification* (` M gradle.properties`). This is the worst of both worlds:
  - one `git add -A` / `git commit -a` in the main repo publishes the SumUp private key and
    the release-keystore passwords to a public GitHub repo;
  - one `git restore`/`git stash`/`git clean` **destroys the only copy** — there is no
    backup, and the release keystore becomes unusable if its passwords are lost.
  Note the config skill previously asserted these lived in `~/.gradle/gradle.properties`;
  that file **does not exist** on this machine. Corrected in **fromagerie-config-and-secrets**.
- **Why it falls short — (b) the `"null"` sentinel.** Every secret is wired as
  `buildConfigField("String", "X", "\"${System.getenv("X") ?: findProperty("X")}\"")`. When
  the value is absent, Kotlin interpolates the **literal string `"null"`** — the build
  **succeeds** and ships a silently broken APK. Observed symptoms: black Mapbox map
  (`MapboxOptions.accessToken = "null"`), OpenRouteService `Access to this API has been
  disallowed` (Bearer `"null"`). Nothing at build time says a word.
- **The worktree multiplier:** `git worktree` checkouts get the **committed** (secret-free,
  26-line) `gradle.properties`, not Tommy's 58-line local modification. So **every AI
  worktree session builds token-less APKs by default** and any on-device map/routing check
  from such a build is meaningless. This burned a real session (2026-07-27).
- **Our leverage:** both fixes are small, local, and independent of prod. The env-var
  fallback (`System.getenv`) already exists in every consumer, so moving the store is a
  no-code change.
- **First three steps:**
  1. **Rescue the secrets first** (owner, before anything else): copy the 58-line
     `gradle.properties` somewhere durable — a password manager entry or an encrypted note.
     Everything below risks the working-tree copy.
  2. **Move them out of the repo.** Create `~/.gradle/gradle.properties` with the 18 keys,
     revert the root `gradle.properties` to its committed state. Gradle merges the user-home
     file into every build, including worktrees — which fixes the worktree trap at the same
     time. (Do NOT just add `gradle.properties` to `.gitignore`: it is already tracked, so
     ignoring it does nothing until `git rm --cached` — and that would delete the shop's
     build config for everyone.)
  3. **Fail fast on `"null"`.** In each `build.gradle.kts` that declares a secret
     `buildConfigField`, throw on a missing value for release builds and warn loudly for
     debug — so a token-less APK is impossible to ship and obvious to spot locally.
     Consumers: `delivery/presentation` (`MAPBOX_*`), `delivery/data` (`OPEN_ROUTE_TOKEN`),
     `checkout/data` (`SUMUP_*`, `GOOGLE_PAY_*`), `core/data` (`CLOUDINARY_*`, `GOOGLE_API`).
- **Result when…** (a) `git log -p -- gradle.properties` shows no secret ever committed AND
  the working tree is clean because the values now live in `~/.gradle/gradle.properties`;
  (b) a build with a secret unset **fails or warns audibly** instead of producing an APK
  whose `BuildConfig` field is `"null"`; (c) a fresh worktree builds a working map with no
  extra setup.
- **Re-verify the exposure:**
  `git ls-files --error-unmatch gradle.properties && git check-ignore -v gradle.properties; git status --short gradle.properties`
- ⚠️ **If a secret was ever committed**, rotating it is the only real remedy (history
  rewriting is not enough on a pushed public repo) — SumUp keys and the keystore passwords
  first. Verify with `git log --oneline -S 'SUMUP_PRIVATE_KEY' -- gradle.properties`.

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

## 8. Cart persistence / client notifications (notifications SHIPPED 2026-07-07; cart still product-decision-pending)

- **Client notifications — SHIPPED 2026-07-07** (owner-requested, which lifted the
  product-decision hold). FCM push via topic `clients` (no per-device token registry; no
  Firestore involvement), client-only under
  `app/src/client/.../notifications/`: `ClientMessagingService`, `NotificationLocalStore`
  (own DataStore file `client_notifications`, capped at 50, Koin single in the client
  `FlavorModules`), `NotificationViewModel`, `NotificationCenterSheet`. Toolbar bell badge
  wired to unread count; `POST_NOTIFICATIONS` runtime request in client `MainActivity`;
  system channel `fromagerie_client_general`. Send from the Firebase console → topic
  `clients`. **Caveat:** notification-messages received in background are posted by the FCM
  SDK without calling `onMessageReceived`, so they reach the tray but NOT the in-app list —
  include DATA keys `title`/`body` in campaigns if the in-app list must catch them.
- **Still open (product-decision-pending, do NOT implement autonomously):**
  - **Preferred-cart persistence:** `cart/presentation/.../CartViewModel.kt:30` (a "save a
    preferred cart" feature request "needs product/UX decisions").
  - (Related deferred: `checkout/.../CheckoutScreen.kt:243` legal/CGV decision, `:254` feature
    blocked on a state rework of the screen.)
- **Why it's ranked last:** none of these can break a payment or a delivery day; they are UX
  enhancements. Adding/removing user-facing features requires owner sign-off; see the "+
  button" doctrine in **fromagerie-change-control §3**.
- **First steps for cart persistence (only after Tommy decides scope):** define the DataStore
  key and UI entry point the TODO asks for (`shared_settings` already holds the live cart —
  see **fromagerie-run-and-operate** / **fromagerie-config-and-secrets**).
- **Result when…** the cart-persistence product decision is recorded, then (if approved) the
  feature ships behind the normal gate.

---

## Provenance and maintenance

| Item | Re-verification command |
|---|---|
| 1b. hosted-path safety net CLOSED 2026-07-08 | `grep -n "schedulePaymentFinalization" checkout/presentation/src/main/java/com/mtdevelopment/checkout/presentation/viewmodel/CheckoutViewModel.kt` (expect a hit inside `getSumUpPaymentLink`; if gone, the net was removed — reopen §1b) |
| 2. backend tracked, secrets out | `git ls-files la-fromagerie-backend | head` (expect functions source + firebase.json) and `git check-ignore la-fromagerie-backend/functions/.env` (expect a match) |
| 3. path offline support | `grep -rn 'offline' delivery --include='*.kt' \| grep -v /build/` |
| 3b. geocoding host regressed to the retired one | `grep -rn "api-adresse" --include='*.kt' . \| grep -v /build/` (any hit outside a comment = §3b reopened) |
| 3b. the two host constants still agree | `./gradlew :delivery:data:testClientDebugUnitTest --tests '*GeocodingHostTest*'` (fails if the modules drift or a host regains a `/` path segment) |
| 3. Firestore offline persistence | `grep -rin 'persistenceEnabled\|PersistentCacheSettings\|setFirestoreSettings' . \| grep -v /build/` (empty = SDK defaults, UNVERIFIED) |
| 3. admin orders are Firestore-direct | read `admin/data/.../repository/FirebaseAdminRepositoryImpl.kt` (no Room = still direct) |
| 4. no admin auth | `grep -rin 'signIn\|FirebaseAuth\|currentUser\|login' app/src/admin admin --include='*.kt' \| grep -v /build/` |
| 4b. admin modules still in the client graph | `grep -rn 'implementation(project(":admin' home details checkout delivery --include='build.gradle.kts'` (any plain `implementation` = §4b still open; expect `adminImplementation` once fixed) |
| 4b. manifest band-aid still needed | `grep -n 'node="remove"' app/src/client/AndroidManifest.xml` (hits = crutch still in place; delete only after the wiring fix) |
| 5. no CI | `ls .github/workflows 2>/dev/null; find . -maxdepth 2 -iname '*.yml' -o -iname '*.yaml' \| grep -v /build/ \| grep -vi google-services` |
| 6. failing tests | `./gradlew testAdminDebugUnitTest --continue` |
| 7. AGP opt-out flags | `grep -nE 'builtInKotlin\|newDsl' gradle.properties; grep -n '^agp' gradle/libs.versions.toml` |
| 8. flagged TODOs | `grep -rn 'TODO' . \| grep '\.kt:' \| grep -v /build/ \| grep -v src/test` |

If any output diverges from this skill, update the affected item and date-stamp it.
