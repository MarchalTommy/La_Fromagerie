---
name: fromagerie-debugging-playbook
description: Symptom-to-cause triage table for LaFromagerie's recurring failure modes — stale APK repro, any crash at screen open, Koin DI crashes, client-vs-admin flavor divergence, payment stuck/failing, an order missing or never appearing for the admin, empty product list, blank delivery map, notification assistant not updating, money formatting bugs. Load this FIRST when told "X is broken", "X crashes", or "bug reproduces" — before reading feature code — to route to the right discriminating check instead of guessing.
---

# LaFromagerie Debugging Playbook

Symptom-first triage for this specific repo's failure modes, verified against code and git history as of 2026-07-06. This is a **router**, not a fix-it manual — each row ends by either giving you the fix directly (if trivial) or pointing at the skill that owns the deep mechanics.

## General discipline (read this before the table)

1. **Reproduce first.** Do not reason about a fix from a description alone — get the actual symptom on a freshly-installed debug build. Step 1 of triage is always "is this even the current code" (see row 1).
2. **One mechanism must explain ALL observations.** If your theory explains 3 of 4 symptoms, it's the wrong theory — keep looking before writing a fix.
3. **Check `fromagerie-failure-archaeology` before re-investigating anything that feels familiar.** This project has a documented history of the same battles (SumUp 409/500, AGP 9 opt-outs, notification service pain, money rounding, buyer duplicates, the stale-APK trap, the "+ button" rollback). Re-fighting a settled battle wastes a session.
4. **Do not silently "fix" the two known-failing `AdminViewModelTest` cases** (`admin/presentation`, "addNewProduct aborts when local image upload fails") as a side effect of unrelated work — see `fromagerie-validation-and-qa` for the documented baseline and why it's tracked as a frontier item, not a bug to grab.

## Triage table

| Symptom | First check | Likely cause | Discriminating experiment | See also |
|---|---|---|---|---|
| Bug reproduces on device but the code looks fixed | Compare installed APK's `lastUpdateTime` against the current commit date | **Stale APK.** A previous debug build is still installed; your fresh code was never actually deployed to the device you're testing on. This caused the real June-2026 cart-emptying incident. | `adb shell dumpsys package com.mtdevelopment.lafromagerie \| grep -A2 lastUpdateTime` vs `git log -1 --format=%cd`. If the install predates your last relevant commit, reinstall (`./gradlew :app:installClientDebug` or `installAdminDebug`) before debugging further. | `fromagerie-failure-archaeology` (§6), `fromagerie-diagnostics-and-tooling` |
| Crash at screen open, Koin stack trace (`NoDefinitionFoundException`, `InstanceCreationException`, or similar) | Which module owns the screen; is a use case/repository/datasource missing a `factory {}`/`single {}` binding | **Missing DI definition.** Koin only fails at the moment a graph is resolved (runtime), not at compile time — a class can compile fine and still crash the first time its ViewModel/screen is opened if a dependency was added to a constructor but never registered in a module. | Grep every DI module for `module {` and confirm the new class has a binding: `grep -rln "module {" --include="*.kt" . \| grep -v build` (module file locations below). Add the missing `factory { YourClass(get(), get()) }` line to the right layer's module. | `fromagerie-build-and-env` (Koin/DI wiring conventions) |
| "Works in client, broken in admin" (or vice versa) | Is the file under a flavor-specific source set (`src/client/`, `src/admin/`) rather than `src/main/` | **Flavored source-set divergence.** Several modules (`delivery/presentation`, `core/presentation`, `app`) have separate `client`/`admin` implementations of the same class name (e.g. two different `DeliveryOptionScreen.kt`) that can silently drift apart. | Diff the two variants directly: `diff <(cat delivery/presentation/src/client/.../DeliveryOptionScreen.kt) <(cat delivery/presentation/src/admin/.../DeliveryOptionScreen.kt)` (adjust path per screen). Also check `core/presentation/src/client\|admin/.../CONST.kt` (`VARIANT` constant) if variant-conditional logic is involved. | `fromagerie-delivery-logistics-reference`, `fromagerie-architecture-contract` |
| Payment stuck ("spinning") or failing for a customer | FIRST: which of the TWO payment paths (since `ecda756`, 2026-07-07)? Google Pay button = direct-API path; "Payer par Carte / SumUp" button = hosted-checkout path (custom tab + `lafromagerie://checkout-callback` deep link) | Direct path: 409 = test-merchant/Google-Pay incompatibility (should not occur in production); 500 = SumUp-account-side issue; 202 = normal 3DS/processing, not an error; `POLLING_TIMEOUT` = ambiguous, **not proof the customer wasn't charged**. Hosted path: failure to obtain the link = function/network issue (tag `PaymentRepository`); "paiement non validé" after returning = single-shot verification miss or genuinely unpaid — check the SumUp dashboard by `checkout_reference` (= orderId on this path) | `adb logcat` filtered to the payment tags (canonical table in `fromagerie-diagnostics-and-tooling`); check the HTTP code in the log line. Full triage and safety-net status per path are in the payments skill — this row is a pointer, not the full answer. | `fromagerie-payments-reference` (mechanics + path-differences table), `fromagerie-payment-reliability-campaign` (hardening), `fromagerie-failure-archaeology` (409/500/3DS saga history) |
| Product list empty, or prices look stale/wrong | Compare `database_update/products_timestamp` (Firestore) against `shared_settings` DataStore's `firestore_product_update` value | **Room cache not invalidated**, or a genuine Firestore read failure being silently swallowed | Force refresh by clearing the `should_refresh_products` DataStore flag or reinstalling; inspect `shared_settings.preferences_pb` via run-as (see diagnostics skill) for the stored timestamp vs. the Firestore value in the console. | `fromagerie-diagnostics-and-tooling`, `fromagerie-firestore-data-model` |
| Delivery map blank, or a delivery path missing from the picker | Is `MAPBOX_PUBLIC_TOKEN` a real value or the literal string `"null"`; did every city in that path resolve via the gouv address API | Two independent causes: (1) **`MAPBOX_PUBLIC_TOKEN` baked as `"null"`** — happens on a non-interactive build with no env var/local.properties set; the map SDK gets a bogus token and renders blank with no explicit app-level error. (2) **One city failed to geocode** — `FirestorePathRepositoryImpl.getAllDeliveryPaths` drops an *entire path* from the result if any one of its cities fails reverse-geocoding via `api-adresse.data.gouv.fr`. | For (1): `grep -rn "MAPBOX_PUBLIC_TOKEN" delivery/presentation/build.gradle.kts` and check the resolved BuildConfig value in a build log. For (2): check the path's `cities`/`postcodes` arrays in the `delivery_paths` Firestore doc for a typo'd city name or zip, and test that exact `(city, zip)` pair against `https://api-adresse.data.gouv.fr/search/?q=<city>-<zip>&type=municipality` directly. | `fromagerie-delivery-logistics-reference`, `fromagerie-config-and-secrets` |
| Notification assistant not updating mid-delivery (stuck on "Calcul de l'itinéraire...") | Is `DeliveryTrackingService` still alive as a foreground service | **Foreground service died or was never promoted**, or route optimization returned an empty route (falls into the "Aucun itinéraire optimisé trouvé" branch and stops there) | `adb shell dumpsys activity services \| grep -i delivery` to confirm the service process is running; check Logcat for `Error fetching optimized route in service` or `Aucun itinéraire optimisé`. Remember `START_STICKY` restart means a full route recompute from scratch, not a resume. | `fromagerie-delivery-logistics-reference` (service lifecycle + known fragility commits), `fromagerie-diagnostics-and-tooling` |
| Money off by a cent, or a price string fails to parse / a date-based sort silently does nothing | Where in the chain the value crosses a `Double`↔`String`↔`Long`-cents boundary | Two known traps in `core/domain/.../LogicUtils.kt`: (a) `String.toLongPrice()` must strip **non-breaking spaces** (` `, ` `) that `NumberFormat.getCurrencyInstance(Locale.FRANCE)` emits — a naive `.replace(" ", "")` misses them and the parse silently produces garbage; (b) the `dd/MM/yyyy` formatter is pinned to `Locale.ROOT` deliberately — building one on the device's default locale breaks on non-Latin-digit locales and turns every timestamp-based sort into a no-op with no crash. | Read `LogicUtils.kt` directly — every conversion function has a KDoc explaining the trap it guards against. If you're about to write a new money or date conversion, reuse these functions; do not write a new one. | `fromagerie-payments-reference` (money-at-the-boundary rules) |
| Order paid but never appeared for the admin (or "customer says they paid, no order in preparation") | `orders` collection in the Firebase console (READ-ONLY): does a doc exist for that customer/date, and with which `status`? | Orphan `PENDING` order (customer bailed or the app died mid-flow), or the charge succeeded but finalization never flipped the status — the traced payment-loss scenarios live in the campaign skill's failure-mode map | Cross-reference SumUp-dashboard transaction timestamps against the order doc's timestamps (note `checkout_reference` ≠ `orderId` — see `fromagerie-payments-reference`); check whether `FinalizePaymentWorker` ran (logcat). Never edit the order doc from a dev session. | `fromagerie-payment-reliability-campaign` (failure-mode map F1–F12), `fromagerie-firestore-data-model` (order lifecycle, who writes which status) |
| Gradle/build failure (sync error, SDK not found, dependency 401, daemon hang) | — | Environment/toolchain issue, not app logic | — | `fromagerie-build-and-env` (full runbook) |

## DI module file locations (for the Koin-crash row)

```
admin/data/src/main/java/com/mtdevelopment/admin/data/di/adminModule.kt
admin/domain/src/main/java/com/mtdevelopment/admin/domain/di/adminModule.kt
admin/presentation/src/main/java/com/mtdevelopment/admin/presentation/di/adminModule.kt
app/src/admin/java/com/mtdevelopment/lafromagerie/di/FlavorModules.kt
app/src/client/java/com/mtdevelopment/lafromagerie/di/FlavorModules.kt
app/src/main/java/com/mtdevelopment/lafromagerie/di/AppModule.kt
```

Other feature modules (`home`, `details`, `cart`, `checkout`, `delivery`) each have their own `di` package per layer following the same `data`/`domain`/`presentation` split — grep `module {` from the repo root to find all of them at once: `grep -rln "module {" --include="*.kt" . | grep -v build | grep -v Test`.

## Payment log tags, quick reference

The canonical payment log-tag table (with logcat filter commands) lives in
**fromagerie-diagnostics-and-tooling** — go there for observation. Quick orientation:
tags come from `SumUpDataSource.kt` / `PaymentRepositoryImpl.kt` in `checkout/data`
(`CreateNewCheckout`, `ProcessCheckoutInfo`, `PollCheckoutStatus`, …); re-derive with
`grep -rn 'Log\.[dewi]' checkout/data/src/main | grep -o '"[A-Za-z]*"' | sort -u`.

HTTP status quick meanings (full state machine in `fromagerie-payments-reference`): **200** terminal or pending-then-poll: **202** accepted, 3DS or backend processing, polling starts regardless; **409** wrong-merchant-type error (should never happen against the real production merchant — if seen, suspect a misconfigured `SUMUP_MERCHANT_ID`, not new code); **500** historically required contacting SumUp support about account-side state.

## Room cache / staleness quick facts

- Single `@Database` class: `app/src/main/java/com/mtdevelopment/lafromagerie/FromagerieDatabase.kt`, physical file name `lafromagerie_database` (see `provideDataBase` in `AppModule.kt`), holding both `ProductEntity` and `PathEntity` tables (`fallbackToDestructiveMigration(true)` — schema changes wipe local data instead of migrating, by design for a solo-dev cache-only database).
- Invalidation is driven by comparing Firestore's `database_update` collection (documents `products_timestamp`, `path_timestamp`, field `last_update`) against `shared_settings` DataStore keys `firestore_product_update` / `firestore_path_update` — see `fromagerie-delivery-logistics-reference` for the full mechanism (paths) and `fromagerie-firestore-data-model` for products.

## Known open TODOs relevant to debugging (do not silently "fix" without on-device verification, per repo convention)

- `admin/presentation/src/main/java/com/mtdevelopment/admin/presentation/composable/ProductEditDialog.kt:52` — suspected-but-unverified bug: picture+data changed together in the same edit may not save correctly; the KDoc names a likely root cause already fixed in `AdminViewModel.uploadLocalImageIfAny` but flags it needs on-device confirmation before the comment is removed.
- `app/src/client/java/com/mtdevelopment/lafromagerie/MainActivity.kt:227` — notifications feature (client-side) not built yet; don't assume push notifications work for customers.

## When NOT to use this skill

- You need the actual mechanics of a subsystem (not just triage) → the domain-reference skill for that subsystem (`fromagerie-payments-reference`, `fromagerie-delivery-logistics-reference`, `fromagerie-firestore-data-model`)
- You need to measure something rather than guess (adb commands, log filters, DataStore dumps, test reports) → `fromagerie-diagnostics-and-tooling`
- You're deciding whether a fix is verified enough to call done → `fromagerie-validation-and-qa`
- The bug has already been fought before and you want the history/dead-ends → `fromagerie-failure-archaeology`
- The problem is Gradle/toolchain/environment, not app behavior → `fromagerie-build-and-env`
- The problem is a missing/misconfigured secret or BuildConfig field → `fromagerie-config-and-secrets`

## Provenance and maintenance

All facts verified 2026-07-06 against branch `claude/distracted-chaum-0986e4`. Re-verify drift-prone claims:

- Payment log tags still current: `grep -n "Log\.[dewi](" checkout/data/src/main/java/com/mtdevelopment/checkout/data/remote/source/SumUpDataSource.kt checkout/data/src/main/java/com/mtdevelopment/checkout/data/repository/PaymentRepositoryImpl.kt`
- Single Room database, filename, entities: `grep -n "@Database\|databaseBuilder\|lafromagerie_database" app/src/main/java/com/mtdevelopment/lafromagerie/FromagerieDatabase.kt app/src/main/java/com/mtdevelopment/lafromagerie/di/AppModule.kt`
- LogicUtils traps still documented/present: `grep -n "toLongPrice\|DATE_FORMATTER_DDMMYYYY\|Locale.ROOT" core/domain/src/main/java/com/mtdevelopment/core/domain/LogicUtils.kt`
- DI module file list still complete: `grep -rln "module {" --include="*.kt" . | grep -v build | grep -v Test`
- ProductEditDialog TODO still open: `grep -n "TODO" admin/presentation/src/main/java/com/mtdevelopment/admin/presentation/composable/ProductEditDialog.kt`
- Notifications-not-built TODO still open: `grep -n "TODO" app/src/client/java/com/mtdevelopment/lafromagerie/MainActivity.kt`
- Known-failing AdminViewModelTest baseline still 2 failures: `./gradlew :admin:presentation:testClientDebugUnitTest --continue` (cross-ref `fromagerie-validation-and-qa` for the documented baseline before treating a failure as new)
