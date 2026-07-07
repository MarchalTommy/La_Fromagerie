---
name: fromagerie-build-and-env
description: Load when building LaFromagerie (any variant/flavor), setting up the dev environment from scratch, creating or fixing a git worktree, resolving Gradle sync/configuration failures, "SDK location not found" errors, jitpack/Mapbox 401 dependency errors, Gradle daemon lock/hang issues, or answering AGP 9 / Kotlin toolchain / JDK / Gradle wrapper questions for this repo. If you are about to BUMP AGP/Kotlin/any dependency (not just build with it), also load fromagerie-change-control first — bumps are gated (own branch, never mixed with feature work).
---

# LaFromagerie: Build and Environment Runbook

Recreate the dev environment and build every variant of this Android app from a cold start. All facts verified against this repo as of 2026-07-06.

Jargon, defined once:
- **AGP** = Android Gradle Plugin (the `com.android.application`/`com.android.library` Gradle plugins).
- **Flavor** = a product-flavor axis in AGP; here it selects the *client* app vs the *admin* app.
- **Variant** = flavor x build type, e.g. `clientDebug`.
- **Worktree** = a `git worktree` checkout (this repo uses them under `.claude/worktrees/`).
- **Toolchain** = the JDK Gradle uses to compile, which is independent of the JDK that launches Gradle.

## 1. Prerequisites checklist

Run through this in order on any fresh clone or worktree:

| # | Check | Command | Expected |
|---|-------|---------|----------|
| 1 | `local.properties` exists with SDK path | `cat local.properties` | Contains `sdk.dir=/Users/tommy/Library/Android/sdk` |
| 2 | It is gitignored (never commit it) | `git check-ignore -v local.properties` | `.gitignore:16:local.properties` |
| 3 | Gradle wrapper version | `cat gradle/wrapper/gradle-wrapper.properties` | `distributionUrl=...gradle-9.1.0-bin.zip` (as of 2026-07-06) |
| 4 | A JDK on PATH to launch Gradle | `java -version` | Any reasonably modern JDK (see toolchain note below) |
| 5 | Daemon not held by another session | `./gradlew --status` | `IDLE` or no daemons |

### local.properties — the fresh-worktree trap

`local.properties` is gitignored, so a **brand-new** git worktree may lack it, and the first Gradle invocation fails with an "SDK location not found" style error. **Check for the file first** — do not assume it is missing (as of 2026-07-06 this worktree already has one, containing `sdk.dir=/Users/tommy/Library/Android/sdk`). If it is genuinely absent, copy it from the main checkout:

```bash
cp /Users/tommy/StudioProjects/LaFromagerie/local.properties ./local.properties
```

### JDK / toolchain: target is 17, host JDK is flexible

- `gradle/libs.versions.toml` line 2: `java-level = "17"`.
- Root `build.gradle.kts` reads that value and, for every subproject applying `org.jetbrains.kotlin.android`, calls `kotlinExt.jvmToolchain(javaVersionStr.toInt())` and pins `KotlinCompile` `jvmTarget` to it; it also sets `sourceCompatibility`/`targetCompatibility` on all `com.android.application` and `com.android.library` modules.
- `settings.gradle.kts` applies `org.gradle.toolchains.foojay-resolver-convention` version `1.0.0`, which **auto-downloads JDK 17** if the host machine does not have one.

Net effect: the JDK that *launches* Gradle can be anything reasonably modern (e.g. JDK 23); the foojay resolver fetches JDK 17 for the compile toolchain. You do not need to install JDK 17 manually.

### Gradle wrapper

Always use `./gradlew` from the repo root. Never use a system-installed Gradle — the wrapper pins Gradle 9.1.0 (as of 2026-07-06) and the AGP/Kotlin combo is sensitive to the Gradle version (see the AGP section).

## 2. Credentialed Maven repositories

`settings.gradle.kts` declares two repositories that take credentials (`RepositoriesMode.FAIL_ON_PROJECT_REPOS` is set, so all repos live in settings):

| Repo | URL | Username | Password/token source |
|------|-----|----------|----------------------|
| jitpack | `https://jitpack.io` | `JITPACK_TOKEN` (env var, falls back to Gradle property `JITPACK_TOKEN`) | — (token goes in the username field) |
| Mapbox | `https://api.mapbox.com/downloads/v2/releases/maven` | literally `mapbox` — **do not change it** | `MAPBOX_SECRET_TOKEN` (env var, falls back to Gradle property) |

The settings file itself says, about the Mapbox credentials:

> "Resolved leniently so builds relying on cached artifacts still configure when the token is absent; downloads from Mapbox will then fail with 401 instead."

What this means in practice:

- A **missing** `JITPACK_TOKEN` / `MAPBOX_SECRET_TOKEN` does **not** fail Gradle sync or configuration. Both fall back to an empty string.
- The failure surfaces only at actual dependency **download** time, as an HTTP 401 — and only if the artifact is not already in the local Gradle cache (`~/.gradle/caches`). On a machine that has built this project before, builds usually succeed with no tokens set.
- So: a 401 on a Mapbox or jitpack artifact means "export the token", not "the build is broken".

```bash
export JITPACK_TOKEN=...        # or -PJITPACK_TOKEN=...
export MAPBOX_SECRET_TOKEN=...  # or -PMAPBOX_SECRET_TOKEN=...
```

Where the token values live is out of scope here — see fromagerie-config-and-secrets.

## 3. Flavor / variant matrix

From `app/build.gradle.kts`: one flavor dimension `version`, flavors `client` and `admin`. Both use applicationId `com.mtdevelopment.lafromagerie` (no `applicationIdSuffix` — client and admin **overwrite each other** on a device). They differ by `versionNameSuffix` (`-client` / `-admin`) and by dependencies: only admin gets the `:admin:*` modules via `adminImplementation`. Build types: `debug` (no minify) and `release` (minify + proguard + `release` signing config fed from `KEYSTORE_*` env/Gradle properties — see fromagerie-config-and-secrets).

Task names below were confirmed by running `./gradlew :app:tasks --all` on 2026-07-06:

| Task | Variant | Purpose |
|------|---------|---------|
| `./gradlew assembleClientDebug` | clientDebug | Build client debug APK (daily dev build) |
| `./gradlew assembleClientRelease` | clientRelease | Build client release APK (needs keystore env vars to sign) |
| `./gradlew assembleAdminDebug` | adminDebug | Build admin debug APK |
| `./gradlew assembleAdminRelease` | adminRelease | Build admin release APK (needs keystore env vars) |
| `./gradlew installClientDebug` | clientDebug | Build + install on connected device |
| `./gradlew installAdminDebug` | adminDebug | Build + install on connected device |
| `./gradlew installClientDebugAndroidTest` | clientDebug tests | Install instrumentation test APK |
| `./gradlew installAdminDebugAndroidTest` | adminDebug tests | Install instrumentation test APK |
| `./gradlew bundleClientDebug` / `bundleClientRelease` | client AABs | Build Android App Bundle |
| `./gradlew bundleAdminDebug` / `bundleAdminRelease` | admin AABs | Build Android App Bundle |

There is **no** `installClientRelease` / `installAdminRelease` task — AGP does not generate install tasks for the release variants here; install release builds manually via `adb install` on the assembled APK. (`uninstallClientRelease` / `uninstallAdminRelease` do exist.)

Also from `app/build.gradle.kts`: a task rule `bump(Major|Minor|Patch)Version` that rewrites `versionCode`/`versionName` in the build file (currently `versionCode 13`, `versionName "1.9.0"`, as of 2026-07-06).

## 4. AGP 9 landmines — read before touching gradle.properties

Root `gradle.properties` contains (verbatim comment included):

```properties
# Temporarily opt out of AGP 9.0 built-in Kotlin and new DSL
# to use explicit kotlin-android plugin (will need migration before AGP 10.0)
android.builtInKotlin=false
android.newDsl=false
```

plus `android.nonTransitiveRClass=true` and `android.useAndroidX=true`.

**Why these exist:** AGP 9 introduced a built-in Kotlin compilation model and a new DSL. This repo is **not migrated** to them. It opts out and keeps the classic `org.jetbrains.kotlin.android` plugin applied per-module (all ~19 module `build.gradle.kts` files apply `libs.plugins.kotlinAndroid`), with the toolchain wired centrally in root `build.gradle.kts`.

**Do not remove these two flags.** History of how this settled (verify with `git log --oneline`):

| Commit | What happened |
|--------|---------------|
| `78df833` | "Updated AGP to 9.1.0" — actually bumped `agp` from `8.13.2` to `9.0.0-rc01` in libs.versions.toml and the Gradle wrapper from 8.13 to 9.1.0 |
| `90cfdec` | "Migrate build configuration to AGP 9.0" — removed composeOptions blocks, removed kotlinAndroid declarations, Room to KSP, AGP to stable 9.0.1 |
| `62181e5` | "Add kotlinAndroid plugin declaration for IDE Kotlin detection" — root-level `apply false` declaration to silence the "Kotlin is not configured" IDE warning |
| `7343ca4` | "Restore kotlin-android plugin for all modules with AGP 9.0" — AGP 9's built-in Kotlin caused "Kotlin is not configured" in Android Studio and broke Kotlin compilation; added `android.builtInKotlin=false` + `android.newDsl=false` and re-applied kotlin-android to every module |
| `9d8d53e` | "Finished fixing AGP 9 for now... Temporary shit, but it will do the trick for now" — added the `java-level` toml entry and the subprojects toolchain wiring in root `build.gradle.kts` |
| `110f32c` / `645991d` | "Cleaned up gradles to try and fix the pipelines" — the earlier CI-era pass that bumped compileSdk 35→36 across ~12 modules (and `645991d` briefly *removed* kotlinAndroid from modules, later reversed by `7343ca4`) |

**Unresolved oscillation (as of 2026-07-06):** commit messages say "AGP 9.1.0" but the repo currently sits at `agp = "9.0.1"` in `gradle/libs.versions.toml` — the version went 8.13.2 → 9.0.0-rc01 → 9.0.1, while the **Gradle wrapper stayed at 9.1.0** (bumped in `78df833` and never reverted). Do not "fix" this mismatch casually; the current combination is what builds. Re-derive the history with `git log --oneline -- gradle/libs.versions.toml`.

**Known debt:** before AGP 10.0, someone must migrate to built-in Kotlin and the new DSL (re-enable `android.builtInKotlin`/`android.newDsl`, drop the per-module `kotlin-android` applications, and adapt the root `build.gradle.kts` toolchain block). Treat that as its own change — see fromagerie-change-control.

Current toolchain pins (as of 2026-07-06): `agp = "9.0.1"`, `kotlin = "2.3.0"`, `java-level = "17"`, Gradle wrapper 9.1.0, `compileSdk = 36`, `minSdk = 26`, `targetSdk = 36`.

## 5. Gradle daemon and memory

Root `gradle.properties`: `org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8` (4 GB daemon heap).

This is a **shared machine**: another agent/session or Android Studio may hold the Gradle daemon or its file locks. If a build hangs or reports a locked cache:

```bash
./gradlew --status     # list daemons and their state (IDLE / BUSY)
```

Wait for `BUSY` daemons to finish, then retry. Prefer waiting over `--stop` — killing a daemon another session is using breaks their build.

## 6. Build output locations

- APKs: `app/build/outputs/apk/<flavor><BuildType>/` — e.g. `app/build/outputs/apk/clientDebug/`.
- AABs: `app/build/outputs/bundle/<flavor><buildType>/` — e.g. `app/build/outputs/bundle/clientRelease/`.

UNVERIFIED-BY-EXECUTION (as of 2026-07-06): no APK or AAB file was observed anywhere in this worktree (`find . -iname "*.apk"` returns nothing; `app/build/outputs/` contains only `logs/manifest-merger-client-debug-report.txt`), so the path pattern above is the AGP standard convention, not an observed artifact path. Verify after your first assemble with `ls app/build/outputs/apk/*/`.

## 7. Known traps

| Trap | Symptom | Where the detail lives |
|------|---------|------------------------|
| Fresh worktree lacks `local.properties` | "SDK location not found" on first Gradle run | Section 1 above (fix: copy from main checkout) |
| Missing jitpack/Mapbox tokens | 401 at dependency download time only, sync still passes | Section 2 above |
| AGP version oscillation (9.0.1 toml vs 9.1.0-era commit messages, wrapper at 9.1.0) | Confusion when "upgrading"; builds break if you align versions blindly | Section 4 above |
| `android.builtInKotlin`/`android.newDsl` removal | "Kotlin is not configured" in Studio, Kotlin compilation failures | Section 4 above |
| Client and admin share one applicationId | Installing one flavor replaces the other on device | Section 3 above |
| Dormant CI pipeline | `git log --oneline --grep="pipeline" -i` shows a long saga ("Still trying to make pipeline work as intended" x9, "Fixed pipeline", "Cleaned up gradles to try and fix the pipelines") for a TeamCity-style pipeline; no CI config lives in-repo today (a `fastlane/` dir with a Fastfile remains) | Full fastlane/CI story: fromagerie-run-and-operate |
| Stale installed APK on test device | Device runs an old build while you debug "your" change (compare adb lastUpdateTime vs commits) | fromagerie-run-and-operate, fromagerie-debugging-playbook |

## When NOT to use this skill

Go to the sibling skill instead:

- Secrets, tokens, keystore values, API keys: **fromagerie-config-and-secrets**
- Running the app on devices, adb workflows, fastlane/CI, releases: **fromagerie-run-and-operate**
- Delivery zones/logistics domain: **fromagerie-delivery-logistics-reference**
- Module/architecture rules: **fromagerie-architecture-contract**
- What changes are allowed and how to land them: **fromagerie-change-control**
- Firestore collections/schemas: **fromagerie-firestore-data-model**
- Ops hardening backlog: **fromagerie-operations-hardening-frontier**
- Past incidents/regressions: **fromagerie-failure-archaeology**
- SumUp/payments domain: **fromagerie-payments-reference**, **fromagerie-payment-reliability-campaign**
- Debugging running-app problems: **fromagerie-debugging-playbook**, **fromagerie-diagnostics-and-tooling**
- Testing/QA strategy: **fromagerie-validation-and-qa**
- Writing docs: **fromagerie-docs-and-writing**

## Provenance and maintenance

Each fact below is drift-prone; re-verify with the given command before trusting it after 2026-07-06.

| Fact (as of 2026-07-06) | Re-verify with |
|--------------------------|----------------|
| `sdk.dir=/Users/tommy/Library/Android/sdk` present in this worktree | `cat local.properties` |
| `local.properties` gitignored at `.gitignore:16` | `git check-ignore -v local.properties` |
| Gradle wrapper = 9.1.0 | `cat gradle/wrapper/gradle-wrapper.properties` |
| `java-level = "17"`, `agp = "9.0.1"`, `kotlin = "2.3.0"` | `grep -E 'java-level|^agp|^kotlin ' gradle/libs.versions.toml` |
| foojay-resolver-convention 1.0.0 applied | `grep foojay settings.gradle.kts` |
| jitpack + Mapbox repos, lenient-token comment | `sed -n '/dependencyResolutionManagement/,/^}/p' settings.gradle.kts` |
| Flavors client/admin, shared applicationId, no suffix | `grep -A12 'productFlavors' app/build.gradle.kts; grep applicationId app/build.gradle.kts` |
| Exact assemble/install/bundle task names | `./gradlew :app:tasks --all --console=plain \| grep -iE 'assemble\|install\|bundle'` |
| `android.builtInKotlin=false`, `android.newDsl=false`, `-Xmx4096m` | `cat gradle.properties` |
| AGP oscillation history | `git log --oneline -- gradle/libs.versions.toml` and `git show 78df833` |
| AGP 9 opt-out rationale commits | `git show --stat 7343ca4 9d8d53e 90cfdec 62181e5` |
| Pipeline saga dormant, fastlane dir present | `git log --oneline --grep="pipeline" -i; ls fastlane/` |
| versionCode 13 / versionName 1.9.0 | `grep -E 'versionCode|versionName =' app/build.gradle.kts` |
| APK/AAB output paths | `ls app/build/outputs/apk/*/ app/build/outputs/bundle/*/` after an assemble/bundle |
