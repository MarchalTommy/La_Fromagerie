---
name: fromagerie-payments-reference
description: How payments actually work in the LaFromagerie Android app — Google Pay + SumUp direct-API integration, 3DS WebView flow, status polling, Firestore order writes, WorkManager finalization, money-as-cents rules, and safety rails. Load this before touching ANY code under checkout/, before debugging a payment bug, before reasoning about SumUp API behavior, or when someone asks "how does the payment flow work" or "is the Cloud Function used".
---

# LaFromagerie Payments Reference

This is the domain-knowledge pack for the payment chain of a **production app charging real money** (SumUp production merchant, Google Pay `ENVIRONMENT_PRODUCTION`). Read the Safety Rails section before running anything on a device.

## Jargon, defined once

| Term | Meaning here |
|---|---|
| **SumUp** | The payment gateway. The app talks to its REST API at `https://api.sumup.com`. |
| **Checkout (SumUp sense)** | A payment session object on SumUp's servers (`POST v0.1/checkouts` creates one, `PUT v0.1/checkouts/{id}` charges it). NOT the same thing as the app's `checkout` Gradle module. |
| **checkout_reference** | Client-chosen unique string identifying a SumUp checkout session. In this app: `"<buyerName-with-dashes>_<epochMillis>"`. |
| **Hosted checkout** | A SumUp feature where SumUp hosts the payment web page and returns a `hosted_checkout_url`. **Live second payment path since commit `ecda756` (2026-07-07, working WIP)** — see verdict below. |
| **3DS (3D Secure)** | Bank-side cardholder challenge (SMS code, banking app tap). Surfaces as SumUp returning HTTP 202 with a `next_step` object. |
| **PAN_ONLY / CRYPTOGRAM_3DS** | Google Pay auth methods: PAN_ONLY = card-on-file (bank will likely force 3DS), CRYPTOGRAM_3DS = device-tokenized card (3DS already satisfied by the device). Both are enabled. |
| **Google Pay token** | Encrypted payload from the Google Pay sheet; forwarded verbatim to SumUp in the PUT body. |
| **Polling** | Repeated `GET v0.1/checkouts/{id}` until status is terminal (PAID/FAILED). |
| **Finalization** | Reconciling the Firestore order status (PENDING → PAID or CANCELED) with the SumUp outcome, plus clearing the cart. |

## VERDICT: TWO payment paths since commit `ecda756` (merged to main 2026-07-07)

1. **Google Pay → direct SumUp API (the original path).** A Ktor `HttpClient` with `DefaultRequest` host `api.sumup.com` and Bearer auth from `BuildConfig.SUMUP_PRIVATE_KEY` is built in `app/src/main/java/com/mtdevelopment/lafromagerie/di/AppModule.kt` (module `provideSumUpDataSource`) and injected into `SumUpDataSource`. Requires the customer to have Google Pay (a Google Wallet). Protected by the WorkManager finalization safety net.
2. **Hosted checkout via the Cloud Function (landed `ecda756`, owner-labeled "working WIP, close to finish").** The broader-reach path — works for ANY customer, no Google Wallet needed, and the SumUp secret stays server-side. Flow: `CheckoutScreen` "Payer par Carte / SumUp" button (`testTag("sumUpPayButton")`) → `createOrder()` → `GetSumUpPaymentLinkUseCase` → `SumUpDataSource.getSumUpPaymentLink()` POSTs `{amount: Double, orderId}` to the deployed function (URL hardcoded in `SumUpDataSource.kt`: `https://createsumupcheckout-ocgett4nrq-uc.a.run.app`, via a separate unauthenticated CIO client) → function reads the amount from the Firestore order doc (`orders/{orderId}.total_price`, cents — the client-sent amount is IGNORED server-side, amount-integrity fix 2026-07-07) and calls SumUp with `hosted_checkout.enabled=true` and **`checkout_reference = orderId`** → returns `payment_url` → Chrome Custom Tab (`androidx.browser` 1.8.0) → customer pays on SumUp's page → redirect `lafromagerie://checkout-callback` → intent-filter on `MainActivity` (in `app/src/main` manifest, so BOTH flavors; `launchMode="singleTask"`, `onNewIntent` → `handleDeepLink`) → `MainViewModel.triggerSumUpCallback()` → NavGraph navigates to checkout → `CheckoutViewModel.verifySumUpWebCheckoutStatus()` fetches checkouts by reference (authed app-side client) and finalizes only on a `PAID` hit whose `amount` matches the saved order's `totalPrice` (cents comparison via `toCentsLong()` — a PAID checkout with the right reference but wrong amount is rejected).

Path differences that matter (as of `ecda756`):

| | Google Pay path | Hosted-checkout path |
|---|---|---|
| `checkout_reference` | `"<buyerName>_<epochMillis>"` | **`orderId`** (identifiers unified) |
| Secret location | `SUMUP_PRIVATE_KEY` in APK (creation + verification) | Server-side for creation; app-side key still used for **verification** (`getCheckoutsByReference`) |
| Outcome detection | Polling loop (`MAX_POLLING_ATTEMPTS`) | Single check on deep-link return (no poll loop) |
| Durable safety net | `FinalizePaymentWorker` scheduled with the session id | **Scheduled since 2026-07-08** before the tab opens: marker has `checkoutId = null` + `expectedAmountCents`; the worker reconciles by reference (= orderId) via `SumUpDataSource.pollHostedCheckoutStatus` and only trusts a PAID session whose amount matches the order total |

The backend is under version control since `ecda756` (`la-fromagerie-backend/` tracked; `functions/.env` correctly gitignored). Rotating `SUMUP_PRIVATE_KEY` still requires an app release while verification uses the app-side key. Remaining hardening items for the hosted path (single-check vs poll on return, hardcoded function URL, `Double` amount at the boundary — the durable safety net was closed 2026-07-08) are tracked in fromagerie-payment-reliability-campaign.

## The happy path, file by file

Numbered steps match the KDoc in `CheckoutViewModel`.

| # | Step | Where |
|---|---|---|
| 1 | Google Pay readiness check (`isReadyToPay`) | `CheckoutViewModel.verifyGooglePayReadiness()` → `PaymentRepositoryImpl.canUseGooglePay()` |
| 2 | **Order written to Firestore first**, status `PENDING`, id `"<cleanname>#<epochMillis>"` | `CheckoutViewModel.createOrder()` → `FirestoreOrderDataSource.createOrder()` (`orders` collection, `.document(id).set(...)`) |
| 3 | SumUp checkout session created (`POST v0.1/checkouts`), reference `"<Buyer-Name>_<epochMillis>"` | `CheckoutViewModel.createCheckout()` → `PaymentRepositoryImpl.createNewCheckout()` → `SumUpDataSource.createNewCheckout()` |
| 4 | Google Pay sheet launched | `CheckoutViewModel.getLoadPaymentDataTask(priceCents)` → `paymentsClient.loadPaymentData` |
| 5 | Sheet result received; **durable finalization scheduled BEFORE submitting** | `CheckoutViewModel.setPaymentData()` → `SchedulePaymentFinalizationUseCase` (marker + WorkManager) |
| 6 | Token sent to SumUp (`PUT v0.1/checkouts/{id}`), 202/3DS/polling handled | `SumUpDataSource.processCheckout()` (see below) |
| 7 | On PAID: order → `PAID`, cart cleared, marker cleared | `CheckoutViewModel.resetAppStateAfterSuccess()` |

Key files:

- `checkout/data/src/main/java/com/mtdevelopment/checkout/data/remote/source/SumUpDataSource.kt` — all SumUp HTTP + polling
- `checkout/data/src/main/java/com/mtdevelopment/checkout/data/repository/PaymentRepositoryImpl.kt` — Google Pay JSON config + SumUp orchestration + Firestore order calls
- `checkout/data/src/main/java/com/mtdevelopment/checkout/data/remote/source/FirestoreOrderDataSource.kt` — `orders` collection writes
- `checkout/data/src/main/java/com/mtdevelopment/checkout/data/work/FinalizePaymentWorker.kt` + `WorkManagerPaymentFinalizationScheduler.kt` — durable finalization
- `checkout/domain/src/main/java/com/mtdevelopment/checkout/domain/model/PendingPaymentFinalization.kt` — the persisted marker (24 h max age)
- `checkout/presentation/src/main/java/com/mtdevelopment/checkout/presentation/viewmodel/CheckoutViewModel.kt` — flow orchestration
- `checkout/presentation/src/main/java/com/mtdevelopment/checkout/presentation/ThreeDSecureActivity.kt` — 3DS challenge UI
- `core/data/src/main/java/com/mtdevelopment/core/data/Constants.kt` — all payment constants

## Google Pay configuration (as of 2026-07-06)

All in `PaymentRepositoryImpl` + `core/data/.../Constants.kt`:

- Environment: `WalletConstants.ENVIRONMENT_PRODUCTION` (`Constants.PAYMENTS_ENVIRONMENT`) — **real cards, real money**
- Networks: VISA, MASTERCARD; auth methods: PAN_ONLY, CRYPTOGRAM_3DS
- Tokenization: `PAYMENT_GATEWAY`, `gateway: "sumup"`, `gatewayMerchantId: "MFHN73AC"`
- Merchant name shown on sheet: `"EARL Des Laizinnes"`; country FR, currency EUR
- `shippingAddressRequired: false`

## Money at the payment boundary

Money is **cent-`Long` throughout the app** (`core/domain/.../LogicUtils.kt`: `Double.toCentsLong()` rounds via `roundToLong`, `Long.toPriceDouble()`, `Long.toStringPrice()` French locale). Hardened after a real rounding incident (commit `4aea6ea`). Two conversions happen at the payment boundary — keep them intact:

- **Google Pay** requires a decimal string: `PaymentRepositoryImpl.centsToString()` — `BigDecimal(cents) / 100`, scale 2, `HALF_EVEN` (e.g. `1050` → `"10.50"`).
- **SumUp** takes `amount` as a JSON Double: `CheckoutViewModel.createCheckout()` calls `totalPrice.toPriceDouble()` (cents/100). This Double crossing is the one place floating point re-enters; the amounts are ≤ 2 decimal places so it is safe in practice, but never add arithmetic on that Double.

## SumUp API behavior learned the hard way

- **Test merchant + Google Pay = HTTP 409 `NON_EXTDEV_PAYMENT_METHOD`.** SumUp's test merchant accounts are NOT compatible with Google Pay. You MUST use the real production merchant + real keys (`BuildConfig.SUMUP_MERCHANT_ID_TEST` exists in `checkout/data/build.gradle.kts` but is unused by the live path). There is therefore **no sandbox for the full flow** — see Safety Rails.
- **A 500 from SumUp once required contacting their support** to fix account-side state. If you hit an unexplained 500 on `PUT v0.1/checkouts/{id}` with a correct request, suspect the merchant account before the code.
- The commit-level history of this saga (hashes, chronology, dead branches) is owned by `fromagerie-failure-archaeology` §1 — cite it from there, not from here.

## The PUT / 202 / 3DS / polling machine (`SumUpDataSource`)

`processCheckout` sends `PUT v0.1/checkouts/{id}` with the Google Pay token. Outcomes:

| HTTP | Meaning | What the code does |
|---|---|---|
| 200 + status PAID/FAILED | Terminal immediately | Emits terminal result |
| 200 + status PENDING | Processing | Starts polling |
| 202 | Accepted — 3DS or backend processing | Parses `ProcessCheckoutResponse.next_step`; if present, fires `on3DSecureRequired` callback; **starts polling regardless** |
| other | Error | Emits `NetWorkResult.Error(message, httpCode)` |

Polling (`pollCheckoutStatus`): `GET v0.1/checkouts/{id}` every `POLLING_INTERVAL_MS = 3000` ms, up to `MAX_POLLING_ATTEMPTS = 40` (~2 minutes). Emits exactly once: terminal `CheckoutResponse`, an error, or `POLLING_TIMEOUT`.

**3DS flow — WebView, not a Chrome custom tab.** `ThreeDSecureActivity` hosts a `WebView`. If `next_step.method` is POST, it auto-submits a hidden HTML form with the payload (`PaReq`, `MD`, `TermUrl`); otherwise it loads `next_step.url`. Completion is detected by `shouldOverrideUrlLoading` seeing a URL that `startsWith(targetRedirect)` (`next_step.redirect_url`, falling back to `BuildConfig.SUMUP_REDIRECT_URL`), then the activity finishes. **No deep link is involved**: the redirect never leaves the WebView, and no manifest declares the scheme. The ViewModel's polling (already running since the 202) picks up the final status — the 3DS activity result itself is not what completes the payment.

## Status & error taxonomy (as encoded in the models)

`checkout/data/.../response/sumUp/CheckoutResponse.kt`:

- `CHECKOUT_STATUS`: `PENDING`, `FAILED`, `PAID` — the only three; PAID/FAILED are terminal.
- `CHECKOUT_TRANSACTION_STATUS`: `PENDING`, `FAILED`, `SUCCESSFUL`, `CANCELLED`
- `CHECKOUT_TRANSACTION_ENTRY_MODE`: `CUSTOMER_ENTRY`, `BOLETO`, `GOOGLE_PAY`; payment types `ECOM`, `RECURRING`, `BOLETO`

Error codes emitted by `SumUpDataSource` as `NetWorkResult.Error(message, code)`: HTTP status number (as string), `MISSING_ID`, `EXCEPTION`, `GET_STATUS_EXCEPTION`, `POLLING_TIMEOUT`, `POLLING_LOGIC_ERROR_OR_UNEXPECTED_STATUS`, `MISSING_CHECKOUT_ID_IN_RESPONSE`, `INTERNAL_TYPE_ERROR`. `PaymentRepositoryImpl.processCheckout` converts errors into flow exceptions; `CheckoutViewModel` catches them, records to Crashlytics, and shows a generic French error. Note the copy claims "Vous n'avez pas été débité" (you were not charged) — **on `POLLING_TIMEOUT` that claim is not guaranteed true** (see the reliability campaign skill).

## Durable finalization (WorkManager safety net)

Introduced in commit `9e386bc`. Mechanics:

- Right before the PUT (step 5), `SchedulePaymentFinalizationUseCase` persists a `PendingPaymentFinalization(checkoutId, orderId, createdAtMillis)` marker in the `checkout_settings` DataStore and enqueues `FinalizePaymentWorker` as unique work `"payment_finalization"` (policy REPLACE, network-CONNECTED constraint, linear 30 s backoff).
- WorkManager persists enqueued work across app kill **and device reboot**.
- The worker re-polls SumUp (`pollCheckoutStatus`, up to ~2 min per attempt). On PAID → Firestore order → `PAID` + clear cart; on FAILED → order → `CANCELED` (cart kept for retry); then clears the marker. Non-terminal/errored → `Result.retry()` up to `MAX_RUN_ATTEMPTS = 5`, after which it returns failure **but leaves the marker in place**.
- At every app launch, `CheeseApplication` (line ~62) runs `ResumePendingPaymentFinalizationUseCase`: re-enqueues the work if a non-expired marker exists; markers older than 24 h (`PendingPaymentFinalization.MAX_AGE_MILLIS`) are dropped — beyond that, the SumUp dashboard is the source of truth.
- Everything is idempotent, so the worker can race the in-app flow: whoever finishes first clears the marker, the other no-ops.

What is **guaranteed**: if the app dies any time after the PUT, the order status will eventually be reconciled — provided the device regains network and either WorkManager runs the job or the user reopens the app within 24 h. What is **assumed, not guaranteed**: that the user reopens within 24 h after the 5-attempt give-up, and that the order document exists (the worker only `update()`s an existing doc). Gap analysis lives in fromagerie-payment-reliability-campaign.

## Firestore ordering: can money move without an order, or vice versa?

- Order (status `PENDING`) is written **before** any money movement (step 2 before steps 3–6). So a successful charge always has a pre-existing order doc — money-taken-with-no-order requires the earlier `createOrder` to have succeeded then the doc to be deleted externally, which normal flow never does.
- The inverse is routine: a user who bails at the Google Pay sheet leaves an orphan `PENDING` order in `orders`. No cleanup exists (as of 2026-07-06).
- `FirestoreOrderDataSource.createOrder` uses `.document(id).set()` — idempotent per orderId (retry overwrites, never duplicates). But each tap of the flow generates a **new** orderId and a **new** checkout_reference, so user-level retries create new documents/sessions (duplicate-payment analysis: see the campaign skill).

## Config & secrets touchpoints

The canonical key catalog (names, file:line, missing-key behavior) is owned by **fromagerie-config-and-secrets** — do not maintain a copy here. Payments-specific notes only: `SUMUP_PRIVATE_KEY` is the Bearer token for every SumUp call (the crown jewel); `SUMUP_MERCHANT_ID_TEST` is unused by the live path (test merchant is Google Pay-incompatible). Ktor logs full bodies **only in debug builds**; Authorization header is always sanitized (`AppModule.kt`).

## Safety rails — read before touching a device

1. **NEVER complete a real payment.** The Google Pay environment is PRODUCTION and the merchant is real. On-device testing stops at the checkout screen, **before tapping the Google Pay button**. There is no test-merchant path (409, see above).
2. **Never write/delete production Firestore collections from a dev session** (`orders` is live customer data). Reading is acceptable.
3. What CAN be tested safely:
   - All unit tests: `./gradlew :checkout:data:testClientDebugUnitTest :checkout:domain:testDebugUnitTest :checkout:presentation:testClientDebugUnitTest` (note: `checkout:domain` has **no flavors** — its task is `testDebugUnitTest`, not `testClientDebugUnitTest`). All green at HEAD as of 2026-07-06.
   - Code-path reading and MockK-based tests for any new logic (Turbine for flows).
   - adb-driving the client debug UI through cart → delivery → checkout screen, inspecting `checkout_settings.preferences_pb` via `adb shell run-as com.mtdevelopment.lafromagerie cat files/datastore/checkout_settings.preferences_pb | strings` (current device convention: Pixel 7 Pro).
   - `GET`-only SumUp API calls (list/fetch checkouts) are read-only on the merchant account. UNVERIFIED (as of 2026-07-06): whether creating (POST) an unprocessed checkout session has any merchant-side cost or dashboard noise — treat POST/PUT as off-limits outside the real app flow.

## When NOT to use this skill

- Making the payment chain *more reliable* (gap analysis, instrumentation, fixes) → **fromagerie-payment-reliability-campaign**
- History of why payments look the way they do (409/500 saga details, reverts) → **fromagerie-failure-archaeology**
- Firestore schema of `orders` and other collections → **fromagerie-firestore-data-model**
- Where secrets live and how to set them up → **fromagerie-config-and-secrets** / **fromagerie-build-and-env**
- Cart/delivery screens upstream of checkout → **fromagerie-architecture-contract**, **fromagerie-delivery-logistics-reference**
- Triage of a live symptom ("payment failed for a customer today") → start with **fromagerie-debugging-playbook**, come back here for mechanics

## Provenance and maintenance

All facts verified 2026-07-06 against the working tree (branch `claude/distracted-chaum-0986e4`, HEAD `b97eb83`). Re-verify drift-prone claims:

- Hosted path still wired as described: `grep -rn "getSumUpPaymentLink\|checkout-callback\|createsumupcheckout" --include="*.kt" --include="*.xml" . | grep -v "/build/"` (expect hits in SumUpDataSource, PaymentRepositoryImpl, CheckoutViewModel, both MainActivities, AndroidManifest.xml)
- Hosted path safety net wired (since 2026-07-08): `grep -n "schedulePaymentFinalization" checkout/presentation/src/main/java/com/mtdevelopment/checkout/presentation/viewmodel/CheckoutViewModel.kt` (expect a hit inside `getSumUpPaymentLink`; if gone, update the path-differences table)
- No deep link for redirect: `grep -rn "lafromagerie://" --include="AndroidManifest.xml" app/src checkout` (expect empty)
- Direct-API base URL + Bearer: `grep -n "SUM_UP_BASE_URL\|SUMUP_PRIVATE_KEY" core/data/src/main/java/com/mtdevelopment/core/data/Constants.kt app/src/main/java/com/mtdevelopment/lafromagerie/di/AppModule.kt`
- Google Pay env still PRODUCTION: `grep -n "PAYMENTS_ENVIRONMENT" core/data/src/main/java/com/mtdevelopment/core/data/Constants.kt`
- Polling constants: `grep -n "MAX_POLLING_ATTEMPTS\|POLLING_INTERVAL_MS" checkout/data/src/main/java/com/mtdevelopment/checkout/data/remote/source/SumUpDataSource.kt`
- Worker guarantees: `grep -n "MAX_RUN_ATTEMPTS\|UNIQUE_WORK_NAME\|MAX_AGE_MILLIS" checkout/data/src/main/java/com/mtdevelopment/checkout/data/work/FinalizePaymentWorker.kt checkout/domain/src/main/java/com/mtdevelopment/checkout/domain/model/PendingPaymentFinalization.kt`
- Test-task names: `./gradlew :checkout:domain:tasks | grep -i unittest` (domain = `testDebugUnitTest`; data/presentation = flavored)
- Backend function content (tracked since `ecda756`): `cat la-fromagerie-backend/functions/src/index.ts`
