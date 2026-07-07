---
name: fromagerie-diagnostics-and-tooling
description: How to MEASURE LaFromagerie's runtime state instead of guessing — on-device DataStore/Room inspection, installed-version-vs-HEAD checks, adb logcat filtering by real log tags, Crashlytics coverage, unit-test HTML reports, and Firestore console inspection. Includes runnable scripts/device-state.sh and scripts/fresh-install-check.sh. Load when asked to inspect device state, check for a stale install, read logs, find a test report, or verify something empirically rather than by reading code.
---

# LaFromagerie Diagnostics and Tooling

How to get ground truth out of a running LaFromagerie install or a test run, instead of guessing from code alone. Pairs with `fromagerie-debugging-playbook` (which tells you *when* to reach for each of these) and `fromagerie-validation-and-qa` (which tells you what counts as *enough* evidence).

## Scripts in this skill

Two runnable scripts live in `scripts/` next to this file. Both are safe to run with no device attached — they detect that and print a clear message instead of hanging or dumping a stack trace (verified 2026-07-06 by running each with no device connected and, separately, with `adb` removed from `PATH`).

```bash
.claude/skills/fromagerie-diagnostics-and-tooling/scripts/device-state.sh [package-name]
.claude/skills/fromagerie-diagnostics-and-tooling/scripts/fresh-install-check.sh [package-name]
```

Both default `package-name` to `com.mtdevelopment.lafromagerie` (the single applicationId shared by both flavors — see `fromagerie-run-and-operate`).

### `device-state.sh`

Dumps, for whatever is currently installed on the attached device:

1. `versionName` / `versionCode` / `firstInstallTime` / `lastUpdateTime` from `adb shell dumpsys package <pkg>`.
2. The current local HEAD commit (hash + date) for side-by-side comparison.
3. The contents of all three known DataStore preference files via `run-as` + `strings` (works only on debuggable builds): `shared_settings.preferences_pb`, `checkout_settings.preferences_pb`, `admin_data.preferences_pb`.

Each DataStore read fails independently and gracefully (missing file, non-debuggable app, `run-as` denied) without aborting the rest of the script.

### `fresh-install-check.sh`

Purpose-built guard against the **stale-APK trap** (see `fromagerie-debugging-playbook` and `fromagerie-failure-archaeology` §6 — the June-2026 cart-emptying incident was exactly this: debugging an old install while believing it was current code). Compares `lastUpdateTime` from `dumpsys package` against `git log -1`'s commit timestamp and prints an explicit warning if the install is older than the latest local commit.

Run this **before** trusting any on-device repro, especially after switching branches or when a bug "shouldn't still exist."

## On-device inspection (manual, when you need more than the scripts give you)

### DataStore files

| File | Owner class | Contents |
|---|---|---|
| `shared_settings.preferences_pb` | `SharedDatastoreImpl` (`core/data`) | cart items, user info (name/address/last selected path), delivery date, refresh flags, last Firestore update timestamps |
| `checkout_settings.preferences_pb` | checkout module's datastore impl | pending payment finalization marker (see `fromagerie-payments-reference`) |
| `admin_data.preferences_pb` | `AdminDatastorePreferenceImpl` (`admin/data`) | admin-only prefs: `is_in_tracking_mode`, cached optimized delivery route |

```bash
adb shell run-as com.mtdevelopment.lafromagerie \
  cat files/datastore/shared_settings.preferences_pb | strings
```

Swap the filename for the other two. This only works on a **debuggable** build (`clientDebug`/`adminDebug`) — release builds are not `run-as`-accessible.

### Room database

Single `@Database`-annotated class: `app/src/main/java/com/mtdevelopment/lafromagerie/FromagerieDatabase.kt`, holding `ProductEntity` (home module) and `PathEntity` (delivery module) tables. Physical database file name is `lafromagerie_database` (from `Room.databaseBuilder(application, FromagerieDatabase::class.java, "lafromagerie_database")` in `app/src/main/java/com/mtdevelopment/lafromagerie/di/AppModule.kt`).

```bash
# Pull the whole SQLite file off a debuggable install for offline inspection:
adb shell run-as com.mtdevelopment.lafromagerie \
  cat databases/lafromagerie_database > /tmp/lafromagerie_database.db
sqlite3 /tmp/lafromagerie_database.db ".tables"
sqlite3 /tmp/lafromagerie_database.db "SELECT * FROM paths;"
```

`fallbackToDestructiveMigration(true)` is set — a schema version bump wipes local Room data on next launch rather than migrating it. That's an accepted tradeoff for what is purely a network-backed cache (see `fromagerie-delivery-logistics-reference` / `fromagerie-firestore-data-model` for what invalidates it).

### Installed-version-vs-HEAD, by hand

```bash
adb shell dumpsys package com.mtdevelopment.lafromagerie | grep -A2 lastUpdateTime
git log -1 --format=%cd
```

If `lastUpdateTime` predates the commit you think you're testing, reinstall first (`./gradlew :app:installClientDebug` / `installAdminDebug`) — or just run `scripts/fresh-install-check.sh`, which does this comparison for you.

## Log observation

### App log tags

Grep `Log\.[dewi]\(` in a source directory to find the tag constants actually in use — tags are ad hoc string literals, not a central enum, so this is the only reliable way to enumerate them. Payment-chain tags (the ones you'll want most often) as of 2026-07-06:

| Tag | File | Meaning |
|---|---|---|
| `PaymentRepository` | `checkout/data/.../repository/PaymentRepositoryImpl.kt` | Google Pay readiness/request-building errors |
| `CreateNewCheckout` | `checkout/data/.../remote/source/SumUpDataSource.kt` | `POST v0.1/checkouts` failures |
| `GetCheckoutsList`, `GetCheckoutError`, `GetCheckoutException` | same file | `GET` checkout list/detail failures |
| `ProcessCheckoutInfo` | same file | Normal-path info logs for PUT/202/3DS |
| `PollCheckoutStatus` | same file | Polling loop progress/cancellation/errors |

```bash
adb logcat -s PaymentRepository:* CreateNewCheckout:* GetCheckoutError:* ProcessCheckoutInfo:* PollCheckoutStatus:*
```

Grep for more tags in any module: `grep -rn "Log\.[dewi](" --include="*.kt" <module-path> | grep -v build`.

### Ktor client logging

Configured per-`HttpClient` instance in `app/src/main/java/com/mtdevelopment/lafromagerie/di/AppModule.kt`, each with `install(Logging) { logger = Logger.ANDROID; ... }`:

- OpenRouteService, gouv address API, and geopf autocomplete clients: `level = LogLevel.ALL` **unconditionally** (not gated by build type) — full request/response bodies land in Logcat even in a release-ish debug build.
- SumUp client: `level = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.NONE` — deliberately silenced in release builds because bodies contain payment tokens and buyer details. All four clients sanitize the `Authorization` header regardless of level (`sanitizeHeader { it == HttpHeaders.Authorization }`).

```bash
adb logcat -s Ktor:*
```

## Crashlytics

- Enabled via `libs.plugins.firebase.crashlytics.plugin` (root `build.gradle.kts`, applied in `app/build.gradle.kts`) plus the `firebase.crashlytics` runtime dependency.
- Explicit `Firebase.crashlytics.recordException(e)` calls exist in at least `home/data/.../FirestoreDatabase.kt` and the checkout ViewModel — grep `Firebase.crashlytics` / `FirebaseCrashlytics` to find all call sites: `grep -rln "Firebase.crashlytics\|FirebaseCrashlytics" --include="*.kt" . | grep -v build`.
- Where to look: Firebase console → Crashlytics, for the relevant Firebase project.
- **What is NOT instrumented today**: there is no dedicated payment-funnel event tracking (e.g. "checkout started" / "3DS shown" / "polling timed out") beyond ad hoc `Log.e`/`recordException` calls at failure points. If you need funnel visibility to debug a payment reliability problem, that's a gap — cross-reference `fromagerie-payment-reliability-campaign` for the plan to close it, don't assume the instrumentation already exists.

## Test reports

HTML reports land per-module, per-variant at:

```
<module>/build/reports/tests/<taskName>/index.html
```

e.g. `admin/presentation/build/reports/tests/testClientDebugUnitTest/index.html`, `checkout/domain/build/reports/tests/testDebugUnitTest/index.html` (note: `domain` modules with no product flavors use the unflavored `testDebugUnitTest` task name — see `fromagerie-payments-reference` for the concrete example that tripped this up once).

Run with `--continue` so one module's failure doesn't stop the rest of the suite from executing (this is how the documented test baseline in `fromagerie-validation-and-qa` was produced):

```bash
./gradlew testClientDebugUnitTest --continue
./gradlew testAdminDebugUnitTest --continue
```

### Prior art for performance measurement

Remote branch `feature/recomposition_tracking` exists — evidence of a prior attempt at measuring Compose recomposition counts rather than eyeballing jank. If a future session wants to add performance instrumentation, look at that branch's diff before starting from scratch; UNVERIFIED (as of 2026-07-06) whether it was ever merged or abandoned mid-flight — `git log --oneline feature/recomposition_tracking -5` to check its relationship to `main`.

## Firestore inspection

- Firebase console, read-only, is the sanctioned way to look at live collections (`products`, `orders`, `delivery_paths`, `preparation_status`, `database_update`).
- **Production-data prohibition**: there is no staging Firestore project. Reading is fine; writing or deleting from a dev/debugging session is not — this is a hard rule carried from the payments and delivery skills, restated here because "just poke it in the console to see" is a tempting diagnostic shortcut that crosses the line. If a write is genuinely needed to reproduce or fix something, that's a decision for the human operator, not an automated session.

## When NOT to use this skill

- Deciding whether evidence is *sufficient* to call a fix done → `fromagerie-validation-and-qa`
- Routing a symptom to a likely cause → `fromagerie-debugging-playbook`
- Installing/running/versioning the app → `fromagerie-run-and-operate`
- Firestore schema/field names themselves (not just how to look at them) → `fromagerie-firestore-data-model`
- Payment-specific mechanics beyond log tags → `fromagerie-payments-reference`
- Delivery-specific mechanics beyond log tags → `fromagerie-delivery-logistics-reference`

## Provenance and maintenance

All facts verified 2026-07-06 against branch `claude/distracted-chaum-0986e4`. Re-verify drift-prone claims:

- DataStore file names: `grep -rn "preferencesDataStore(name" --include="*.kt" core/data checkout/data admin/data | grep -v build`
- Room database name + entities: `grep -n "@Database\|databaseBuilder" app/src/main/java/com/mtdevelopment/lafromagerie/FromagerieDatabase.kt app/src/main/java/com/mtdevelopment/lafromagerie/di/AppModule.kt`
- Ktor logging levels per client: `grep -n "install(Logging)" -A3 app/src/main/java/com/mtdevelopment/lafromagerie/di/AppModule.kt`
- Payment log tags still current: `grep -n "Log\.[dewi](" checkout/data/src/main/java/com/mtdevelopment/checkout/data/remote/source/SumUpDataSource.kt checkout/data/src/main/java/com/mtdevelopment/checkout/data/repository/PaymentRepositoryImpl.kt`
- Crashlytics plugin still applied: `grep -n "crashlytics" build.gradle.kts app/build.gradle.kts`
- Test report path convention: `ls app/build/reports/tests/ 2>/dev/null` after running a test task
- `feature/recomposition_tracking` branch still exists: `git log --oneline feature/recomposition_tracking -3`
- Scripts still handle "no device" gracefully: re-run them with no device attached; expect an exit code of 0 and a one-line explanation, not a hang or stack trace
