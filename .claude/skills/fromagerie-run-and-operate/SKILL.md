---
name: fromagerie-run-and-operate
description: >
  Running and shipping LaFromagerie. Load when installing or running the app on a
  device (installClientDebug / installAdminDebug), doing device testing over adb
  (DataStore inspection, input tap, screencap), bumping the version
  (bumpMajorVersion / bumpMinorVersion / bumpPatchVersion), using fastlane lanes
  (test, build, version, distribute), preparing a release or shipping (signing,
  KEYSTORE_* env vars, assembleRelease, Firebase App Distribution), or deploying
  the Cloud Functions backend (firebase deploy --only functions). Also covers the
  single-applicationId constraint (client and admin flavors cannot coexist on one
  device) and the release checklist.
---

# LaFromagerie: Run and Operate

Runbook for installing, running, versioning, and shipping the LaFromagerie Android app (two flavors: `client` and `admin`) and deploying its Cloud Functions backend. This is a REAL production app: production Firestore, real SumUp payments, one developer (Tommy). Treat every on-device action accordingly.

> **HARD RULE — NEVER tap or complete the Google Pay button on a device.**
> The client app talks to a REAL production SumUp merchant account with `ENVIRONMENT_PRODUCTION` Google Pay. Tapping the pay button triggers a REAL payment with real money. When driving the UI via adb (`input tap`) or by hand, stop BEFORE the pay button, always. There is no test/sandbox merchant in play (the SumUp test merchant is not Google Pay-compatible, which is why production credentials are used even in development).

## Install and run each flavor

Both flavors build from the `:app` module. AGP only generates `install*` tasks for debuggable variants, so only the Debug variants are installable via Gradle (verified against `./gradlew :app:tasks --all`, as of 2026-07-06). This table covers install/uninstall only — the full assemble/bundle task matrix is owned by **fromagerie-build-and-env**:

| Action | Command |
|---|---|
| Install client (debug) | `./gradlew :app:installClientDebug` |
| Install admin (debug) | `./gradlew :app:installAdminDebug` |
| Install instrumented tests | `./gradlew :app:installClientDebugAndroidTest` / `:app:installAdminDebugAndroidTest` |
| Uninstall client debug | `./gradlew :app:uninstallClientDebug` |
| Uninstall admin debug | `./gradlew :app:uninstallAdminDebug` |
| Uninstall release builds | `./gradlew :app:uninstallClientRelease` / `:app:uninstallAdminRelease` |
| Uninstall everything | `./gradlew :app:uninstallAll` |

There is **no** `installClientRelease` / `installAdminRelease` task. To put a release build on a device, assemble it (`./gradlew :app:assembleClientRelease` — requires the signing env vars, see below) and sideload the APK:

```bash
adb install -r app/build/outputs/apk/client/release/app-client-release.apk
```

## The one-applicationId constraint

Read from `app/build.gradle.kts`: `applicationId = "com.mtdevelopment.lafromagerie"` in `defaultConfig`, and the flavors only set `versionNameSuffix` (`-client` / `-admin`) — there is **no `applicationIdSuffix`**. Both flavors therefore install as the *same* Android package.

Practical consequences:

- Installing `adminDebug` on a device that has `clientDebug` **replaces** it (and vice versa). Android sees one app.
- You **cannot** have client and admin side by side on one device. Testing the full loop (client places order → admin sees it) on a single physical device means uninstall/reinstall to switch context, losing local state (DataStore, cart) each time.
- Workarounds: a second device or an emulator for the other flavor, or adding an `applicationIdSuffix` to one flavor — a change nobody has made (it would also require a matching Firebase Android app registration for the new package name).

Also factual, from the same file: the **admin flavor has no in-app authentication layer** (verified: no auth code in the `admin/*` modules or `app/src/admin`, as of 2026-07-06). Access to admin features is protected only by controlling who has the admin APK. This is a known, accepted risk — do not silently "fix" it as part of unrelated work.

## Device testing convention

Current convention (a habit, not a hard requirement): a physical **Pixel 7 Pro** is usually adb-connected. Check with `adb devices`.

Because debug builds are debuggable, app-private files are inspectable via `run-as`. DataStore preference files (names verified in source, as of 2026-07-06):

| DataStore file | Owner | Contents |
|---|---|---|
| `shared_settings.preferences_pb` | `SharedDatastoreImpl` (core:data) | cart, user info, delivery date |
| `checkout_settings.preferences_pb` | `CheckoutDatastorePreferenceImpl` (checkout:data) | checkout session state |
| `admin_data.preferences_pb` | `AdminDatastorePreferenceImpl` (admin:data) | admin-flavor preferences |

```bash
# Inspect the shared DataStore (cart / user info / delivery date):
adb shell run-as com.mtdevelopment.lafromagerie \
  cat files/datastore/shared_settings.preferences_pb | strings

# Same pattern for checkout_settings.preferences_pb and admin_data.preferences_pb
```

Drive the UI and capture the screen:

```bash
adb shell input tap <x> <y>
adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png
```

Remember the hard rule above: never tap through Google Pay. Also beware stale installs — an old APK left on the device from a previous session can masquerade as your current build; reinstall before concluding anything.

## Version bumping

`app/build.gradle.kts` currently has `versionCode = 13`, `versionName = "1.9.0"` (as of 2026-07-06). At the bottom of that file, a Gradle task rule (`tasks.addRule("Pattern: bump<TYPE>Version")`) matches `bump(Major|Minor|Patch)Version`, giving three tasks: `bumpMajorVersion`, `bumpMinorVersion`, `bumpPatchVersion`.

What the task actually does:

1. Reads `android.defaultConfig.versionCode` / `versionName`, parses versionName as `major.minor.patch`.
2. Bumps the requested segment — major resets minor+patch to 0, minor resets patch to 0, patch just increments. **versionCode increments by 1 regardless of bump type.**
3. Rewrites `app/build.gradle.kts` **in place** via `buildFile.readText().replaceFirst(...)` + `buildFile.writeText(...)` — plain string replacement, not an AGP API.

> **Warning:** running any `bump*Version` task mutates tracked source (`app/build.gradle.kts`) as a side effect, recoverable only via git. Do not run it casually, in a loop, or from an AI session without the user's explicit intent to cut a version.

Invocation: the rule lives in the `:app` module, so `./gradlew :app:bumpPatchVersion` is the canonical form; the unqualified `./gradlew bumpPatchVersion` should also resolve via Gradle's cross-project task-name matching. UNVERIFIED-BY-EXECUTION (as of 2026-07-06): neither form has been executed to confirm, because running it rewrites a tracked file — confirm the syntax only when you actually intend to bump.

## Fastlane

Four lanes in `fastlane/Fastfile` (platform `:android`; `fastlane/Appfile` sets `package_name("com.mtdevelopment.lafromagerie")` and an empty `json_key_file`). Plugins from `fastlane/Pluginfile`: `firebase_app_distribution`, `increment_version_code`, `versioning_android`, `android_gradle_commiter`. A `before_all` block echoes lane options (`APP_ID`, `TESTERS`, `ARTIFACT_PATH`, `CHANGELOG`, `VERSION_CHANGE`).

| Lane | Invocation | What it does | Caveats |
|---|---|---|---|
| `test` | `fastlane android test` | `gradle(task: "test")`, then android_get/set_version_name and get/set_version_code calls | Contains **hardcoded** `version_name: "1.23.4"` and `version_code: 17` — this looks like leftover example/debug code, not real automation. If run, it would *set* those values in `app/build.gradle.kts`. Do not run without inspecting first. |
| `build` | `fastlane android build` | `gradle(task: "clean assembleRelease")` | Not flavor-scoped: builds release for **all** flavors (client + admin). Requires signing env vars. |
| `version` | `fastlane android version VERSION_CHANGE:3` | Private lane `determine_release_name` computes new semver from `VERSION_CHANGE` ("1"=major, "2"=minor, "3"=patch, "0"=none), then `increment_version_code`, `android_set_version_name`, and `android_gradle_commiter` | `android_gradle_commiter` **creates a git commit** of the version bump. Do not run this lane from an assistant session. |
| `distribute` | `fastlane android distribute APP_ID:... TESTERS:... CHANGELOG:... ARTIFACT_PATH:...` | `firebase_app_distribution` — all params passed as lane options | Hardcodes `service_credentials_file: "/opt/buildagent/external/firebase-key.json"`, a CI build-agent path that does not exist locally, so this lane fails on a dev machine unless that file is created or the path changed. Commented-out OLD lane below it used `firebase_cli_token` — evidence of a migration from token-based to service-account auth for Firebase App Distribution. |

**These pipelines are DORMANT.** No CI configuration exists in-repo (verified as of 2026-07-06: no `.github/workflows`, `.teamcity`, `.circleci`, and no `*.yml`/`*.yaml` at repo depth ≤2 outside build dirs). The `/opt/buildagent/...` path suggests a TeamCity-style agent that lived elsewhere. Git history records a long fight to make it work — e.g. commits `110f32c` / `645991d` "Cleaned up gradles to try and fix the pipelines", `00b7a54` "Pipeline is good, now just resetting versions", and a run of `05e9885`, `32ba49d`, `3e6f7d7`… "Still trying to make pipeline work as intended". Today, releasing is a manual, local process.

## Release signing

The `release` signing config in `app/build.gradle.kts` reads four values, each from an env var with a Gradle-property fallback (`System.getenv(...) ?: project.findProperty(...)`):

`KEYSTORE_ALIAS`, `KEYSTORE_ALIAS_PASS`, `KEYSTORE_PATH`, `KEYSTORE_PASS`

All four must be set for `assembleClientRelease` / `assembleAdminRelease` / `bundleRelease` to produce a signed artifact. For where these values live and the full configuration-axis catalog, see the **fromagerie-config-and-secrets** skill — do not duplicate that here.

## Backend deploy (Cloud Functions)

The backend at `la-fromagerie-backend/` is **git-tracked since commit `ecda756` (2026-07-07)** — source, `firebase.json`, `tsconfig.json`, `package.json` all versioned, so backend changes go through the normal branch → PR flow like app code. `functions/.env` (the secrets) remains gitignored and exists **only in the main checkout**, and Firebase Functions v2 loads it at deploy time — so **deploys still run from the main checkout**, even though the code is now visible in worktrees.

From `la-fromagerie-backend/functions/package.json` (verified 2026-07-06 — Node 24, firebase-functions ^7, TypeScript):

```bash
cd /Users/tommy/StudioProjects/LaFromagerie/la-fromagerie-backend

npm --prefix functions run build    # tsc → functions/lib
npm --prefix functions run deploy   # firebase deploy --only functions
npm --prefix functions run logs    # firebase functions:log
npm --prefix functions run serve   # build + local emulator (functions only)
```

`deploy` pushes straight to the production Firebase project — there is no staging environment. Build first; `firebase deploy` deploys whatever is in `functions/lib`.

## Build artifacts

Standard AGP output locations (per-flavor, per-buildType):

- APKs: `app/build/outputs/apk/<flavor>/<buildType>/` e.g. `app/build/outputs/apk/client/release/`
- AABs: `app/build/outputs/bundle/<flavor><BuildType>/` e.g. `app/build/outputs/bundle/clientRelease/`

For deeper build-system detail, see **fromagerie-build-and-env**.

## Release checklist (manual as of 2026-07-06 — CI is dormant)

| # | Step | Command / action | Verifies |
|---|---|---|---|
| 1 | Clean state | `git status` clean, on an up-to-date branch | No accidental changes ship |
| 2 | Bump version | `./gradlew :app:bumpPatchVersion` (or Minor/Major) — only with explicit intent; review the resulting diff to `app/build.gradle.kts` | versionCode +1, versionName correct |
| 3 | Unit tests | `./gradlew test` | Nothing broken across modules |
| 4 | Signing env | Confirm `KEYSTORE_ALIAS`, `KEYSTORE_ALIAS_PASS`, `KEYSTORE_PATH`, `KEYSTORE_PASS` are set | Release build can be signed |
| 5 | Build release | `./gradlew :app:assembleClientRelease` (and `:app:assembleAdminRelease` if shipping admin) | Signed APKs in `app/build/outputs/apk/<flavor>/release/` |
| 6 | Smoke test | `adb install -r` the client release APK on the device; launch, browse products, add to cart, walk to checkout — **stop before Google Pay** | Release build (minified, R8) actually runs |
| 7 | Distribute | Manual today. `fastlane android distribute ...` exists but expects a nonexistent CI credentials path — fix or distribute the APK by hand / via Firebase console | Testers get the build |
| 8 | Commit + tag | Commit the version-bump diff; tag the release | Traceability |
| 9 | Backend (if changed) | From the main checkout: `npm --prefix functions run build && npm --prefix functions run deploy` in `la-fromagerie-backend/` | Cloud Functions match the app; backend is git-tracked since `ecda756` — deployed code must equal tracked source at HEAD |

## When NOT to use this skill

Consult the sibling skill instead when the task is primarily about that area:

- fromagerie-build-and-env
- fromagerie-config-and-secrets
- fromagerie-delivery-logistics-reference
- fromagerie-architecture-contract
- fromagerie-change-control
- fromagerie-firestore-data-model
- fromagerie-operations-hardening-frontier
- fromagerie-failure-archaeology
- fromagerie-payments-reference
- fromagerie-payment-reliability-campaign
- fromagerie-debugging-playbook
- fromagerie-diagnostics-and-tooling
- fromagerie-validation-and-qa
- fromagerie-docs-and-writing

## Provenance and maintenance

Facts verified against the repo on 2026-07-06. Re-verify drift-prone facts with:

| Fact | Re-verification command |
|---|---|
| Install/uninstall task names (no install*Release) | `./gradlew :app:tasks --all 2>/dev/null \| grep -iE "^(un)?install"` |
| applicationId + no applicationIdSuffix | `grep -n "applicationId\|Suffix" app/build.gradle.kts` |
| Current versionCode / versionName | `grep -n "versionCode\|versionName =" app/build.gradle.kts` |
| bump task rule still string-replaces the build file | `grep -n "addRule\|replaceFirst\|writeText" app/build.gradle.kts` |
| DataStore file names | `grep -rn "preferencesDataStore(name" --include='*.kt' core/data checkout/data admin/data` |
| Fastlane lanes and hardcoded values | `grep -n "lane :\|service_credentials_file\|1.23.4\|version_code: 17" fastlane/Fastfile` |
| Signing env var names | `grep -n "KEYSTORE_" app/build.gradle.kts` |
| CI still absent | `ls -d .github .teamcity .circleci 2>&1; find . -maxdepth 2 \( -iname "*.yml" -o -iname "*.yaml" \) 2>/dev/null \| grep -v build` |
| Backend deploy script | `cat /Users/tommy/StudioProjects/LaFromagerie/la-fromagerie-backend/functions/package.json` |
| Backend tracked, `.env` still ignored | `git ls-files la-fromagerie-backend | head` (expect functions source) and `git check-ignore la-fromagerie-backend/functions/.env` (expect a match) |
| Admin flavor still has no auth | `grep -rln "FirebaseAuth\|signInWith" admin/ app/src/admin 2>/dev/null \| grep -v build` (empty ⇒ still none) |
