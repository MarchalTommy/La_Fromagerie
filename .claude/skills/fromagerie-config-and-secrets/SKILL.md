---
name: fromagerie-config-and-secrets
description: >
  Catalog of every configuration axis in the LaFromagerie Android repo: secret env vars
  (SUMUP_*, CLOUDINARY_*, GOOGLE_API, GOOGLE_PAY_*, OPEN_ROUTE_TOKEN, MAPBOX_*, JITPACK_TOKEN,
  KEYSTORE_*), BuildConfig field wiring, gradle.properties build flags, google-services.json,
  release signing, and Cloud Function backend env (SUMUP_SECRET_KEY, SUMUP_MERCHANT_CODE).
  Load when: adding a new secret or API key, a BuildConfig field is "null" at runtime,
  a build fails with missing-credential/401/signing errors, wiring an env var into a module,
  changing gradle.properties flags, touching google-services.json, or auditing what config
  exists and where it is consumed.
---

# LaFromagerie — Configuration and Secrets Catalog

Ground truth as of 2026-07-06. Every file:line below was verified by reading the actual
build scripts. Paths are relative to the repo root unless stated otherwise.

## SECURITY RULE — read this first

**NEVER print, copy, log, commit, or embed an actual secret VALUE anywhere** — not in
chat, not in a skill file, not in a commit, not in a debug `println`. Document and grep
key NAMES only. When checking whether a key is configured, use existence-only commands
(`grep -c "^KEY="`, `[ -n "$KEY" ] && echo set`), never `grep KEY` or `echo $KEY`.
This is a production app: production Firestore, real SumUp payments, a real Play Store
signing key. A leaked value here is a real incident, not a sandbox annoyance.

## Definitions (once)

- **Config axis**: any named knob that changes what the build or app does — an env var,
  a Gradle property, a build flag, a JSON config file, or a backend env var.
- **BuildConfig field**: a constant baked into a module's generated `BuildConfig` class
  at compile time via `buildConfigField("String", "NAME", "\"$value\"")`. App code reads
  it as `BuildConfig.NAME`.
- **Fallback pattern**: the repo-wide idiom `System.getenv("X") ?: project.findProperty("X")?.toString()` —
  environment variable wins, Gradle project property is the fallback.
- **Gradle project property**: resolved by Gradle from (in order) `-PX=...` on the command
  line, `ORG_GRADLE_PROJECT_X` env vars, root `gradle.properties`, and
  `~/.gradle/gradle.properties`. **`local.properties` is NOT a source** — Gradle's
  `findProperty` never reads it, and this repo has no custom code that loads it
  (verified: `local.properties` here contains only `sdk.dir`).

## The two fallback patterns

| Where | Pattern | Why it differs |
|---|---|---|
| `settings.gradle.kts` | `System.getenv("X") ?: providers.gradleProperty("X").orNull.orEmpty()` | Settings scripts have no `project`; they use the Provider API. `.orEmpty()` makes the credential an empty string instead of null so configuration never fails (lenient resolution — see repo credentials below). |
| Module `build.gradle.kts` | `System.getenv("X") ?: project.findProperty("X")?.toString()` | Standard project-scope lookup. If both sources are absent the value is Kotlin `null`, and string interpolation bakes the literal string `"null"` into BuildConfig — the build still succeeds; the feature fails at runtime. |

One deviation: `delivery/data/build.gradle.kts:19-21` omits `?.toString()` on
`OPEN_ROUTE_TOKEN`. Functionally identical (string interpolation calls `toString()`
anyway; a missing key still yields `"null"`), but it is a style inconsistency — use the
`?.toString()` form for new keys.

## Master catalog — build-time keys

Every `System.getenv` site in the repo (exhaustive as of 2026-07-06; re-derive with the
provenance command at the bottom).

| Key | Consumed in (file:line) | Becomes | Needed for | If missing |
|---|---|---|---|---|
| `JITPACK_TOKEN` | `settings.gradle.kts:25-26` | jitpack.io Maven repo username credential | Dependency resolution from JitPack | Empty string; build configures, JitPack downloads may fail if artifact not cached |
| `MAPBOX_SECRET_TOKEN` | `settings.gradle.kts:36-37` | Mapbox Maven repo password (username is hardcoded `"mapbox"` — do not change, per in-file comment) | Downloading Mapbox SDK artifacts | Lenient by design (in-file comment): cached artifacts still configure; fresh downloads fail with 401 |
| `MAPBOX_SECRET_TOKEN` (2nd use) | `delivery/presentation/build.gradle.kts:21-23,25-28` | `BuildConfig.MAPBOX_SECRET_TOKEN` (String) | delivery map features, both flavors | Field = `"null"`; build OK, Mapbox calls fail at runtime |
| `MAPBOX_PUBLIC_TOKEN` | `delivery/presentation/build.gradle.kts:18-20,24` | `BuildConfig.MAPBOX_PUBLIC_TOKEN` (String) | delivery map rendering, both flavors | Field = `"null"`; map fails at runtime |
| `SUMUP_PRIVATE_KEY` | `checkout/data/build.gradle.kts:18-21` | `BuildConfig.SUMUP_PRIVATE_KEY` (String) | SumUp payments (checkout), both flavors | Field = `"null"`; payments fail at runtime |
| `SUMUP_PUBLIC_KEY` | `checkout/data/build.gradle.kts:23-26` | `BuildConfig.SUMUP_PUBLIC_KEY` (String) | SumUp payments | Field = `"null"`; payments fail at runtime |
| `SUMUP_MERCHANT_ID` | `checkout/data/build.gradle.kts:28-31` | `BuildConfig.SUMUP_MERCHANT_ID` (String) | SumUp payments (production merchant) | Field = `"null"`; payments fail at runtime |
| `SUMUP_REDIRECT_URL` | `checkout/data/build.gradle.kts:33-36` AND `checkout/presentation/build.gradle.kts:19-22` | `BuildConfig.SUMUP_REDIRECT_URL` in BOTH modules (same key, two BuildConfig classes — set once, both pick it up) | SumUp checkout redirect flow | Fields = `"null"`; redirect flow breaks at runtime |
| `SUMUP_MERCHANT_ID_TEST` | `checkout/data/build.gradle.kts:38-40` | `BuildConfig.SUMUP_MERCHANT_ID_TEST` (String) | SumUp test-merchant path | Field = `"null"`; test payment path breaks |
| `GOOGLE_PAY_PROFILE_ID` | `checkout/data/build.gradle.kts:42-45` | `BuildConfig.GOOGLE_PAY_PROFILE_ID` (String) | Google Pay via SumUp | Field = `"null"`; Google Pay fails at runtime |
| `GOOGLE_PAY_MERCHANT_ID` | `checkout/data/build.gradle.kts:47-49` | `BuildConfig.GOOGLE_PAY_MERCHANT_ID` (String) | Google Pay | Field = `"null"`; Google Pay fails at runtime |
| `CLOUDINARY_URL` | `admin/data/build.gradle.kts:20-23` | `BuildConfig.CLOUDINARY_URL` (String) | Admin image upload (module linked via `adminImplementation` only) | Field = `"null"`; admin uploads fail at runtime |
| `CLOUDINARY_PUBLIC` | `admin/data/build.gradle.kts:25-28` | `BuildConfig.CLOUDINARY_PUBLIC` (String) | Admin image upload | Field = `"null"` |
| `CLOUDINARY_PRIVATE` | `admin/data/build.gradle.kts:30-33` | `BuildConfig.CLOUDINARY_PRIVATE` (String) | Admin image upload | Field = `"null"` |
| `GOOGLE_API` | `admin/data/build.gradle.kts:35-38` AND `admin/presentation/build.gradle.kts:18-21` | `BuildConfig.GOOGLE_API` in BOTH admin modules | Admin-only Google API features | Fields = `"null"` |
| `OPEN_ROUTE_TOKEN` | `delivery/data/build.gradle.kts:19-21` | `BuildConfig.OPEN_ROUTE_TOKEN` (String) — note missing `?.toString()`, cosmetic only | OpenRouteService routing (delivery), both flavors | Field = `"null"`; routing fails at runtime |
| `KEYSTORE_ALIAS` | `app/build.gradle.kts:18-19` | Release signing `keyAlias` | `assembleRelease` / `bundleRelease` only | Signing config invalid; release build fails at signing step. Debug builds unaffected |
| `KEYSTORE_ALIAS_PASS` | `app/build.gradle.kts:22-24` | Release signing `keyPassword` | Release builds | Release signing fails |
| `KEYSTORE_PATH` | `app/build.gradle.kts:27-32` | Release signing `storeFile` — a filesystem path resolved via `file(it)`, relative paths resolve against `app/` | Release builds | `storeFile` = null; release signing fails |
| `KEYSTORE_PASS` | `app/build.gradle.kts:35-36` | Release signing `storePassword` (uses `as? String` cast variant of the pattern) | Release builds | Release signing fails |

Key takeaway: **a missing BuildConfig secret never fails the build** — it silently bakes
the string `"null"` and the feature breaks at runtime. Only the four `KEYSTORE_*` keys
(release only) and a cold-cache Maven credential can actually fail a build.

### Known code smell — signing debug scaffolding (do not "fix" casually)

`app/build.gradle.kts:20-38` contains four live `println("... hidden, go to code and
uncomment to find it back")` lines, each paired with a commented-out `println` of the
actual signing value (`app/build.gradle.kts:21,26,34,38`). Uncommenting them prints
secrets to the build log. Documented here factually as left-in debug scaffolding; leave
as-is unless a change is explicitly scoped to remove it, and NEVER commit them uncommented.

## gradle.properties flags (root)

| Flag | Value | Status |
|---|---|---|
| `org.gradle.jvmargs` | `-Xmx4096m -Dfile.encoding=UTF-8` | Permanent |
| `android.useAndroidX` | `true` | Permanent (mandatory for AndroidX) |
| `kotlin.code.style` | `official` | Permanent |
| `android.nonTransitiveRClass` | `true` | Permanent / intentional (smaller per-module R classes) |
| `android.builtInKotlin` | `false` | **Temporary / must migrate** — AGP 9 opt-out to keep the explicit `kotlin-android` plugin; removal required before AGP 10 (per in-file comment) |
| `android.newDsl` | `false` | **Temporary / must migrate** — AGP 9 new-DSL opt-out, same AGP 10 deadline |

Root `gradle.properties` is git-tracked — never put secrets in it. As of 2026-07-06 it
contains only the flags above (verified: no ALL_CAPS secret keys present).

## google-services.json (Firebase config)

Exactly **one** copy exists: `app/google-services.json`. There are NO per-flavor copies —
verify with:

```bash
find . -name google-services.json -not -path "*/build/*"
```

This is expected and consistent with the one-applicationId design: both `client` and
`admin` flavors share `applicationId = "com.mtdevelopment.lafromagerie"`
(`app/build.gradle.kts:42`; flavors add only a `versionNameSuffix`,
`app/build.gradle.kts:70-80`), so both resolve the same Firebase app entry in the same
file. The `google-services` Gradle plugin is applied in `app/build.gradle.kts:6` and also
in `admin/data/build.gradle.kts:5`. The file is git-tracked (not in `.gitignore`); it
contains identifiers, not credentials, but do not paste its contents into logs or docs.

## Backend (Cloud Function) env — separate axis, not Gradle

The `createSumUpCheckout` HTTPS function (Firebase Functions v2, `invoker: "public"`,
`cors: true`) reads two env vars via `process.env`:

| Key | Consumed in | Purpose |
|---|---|---|
| `SUMUP_SECRET_KEY` | `functions/src/index.ts:14` | SumUp API secret for checkout creation |
| `SUMUP_MERCHANT_CODE` | `functions/src/index.ts:15` | SumUp merchant code |

Facts verified 2026-07-06, updated 2026-07-07 (commit `ecda756`):

- The backend at `la-fromagerie-backend/` is **git-tracked since `ecda756`** (functions
  source, `firebase.json`, `tsconfig.json`, `package.json`) and therefore present in
  worktrees too. `functions/.env` remains **gitignored**
  (`la-fromagerie-backend/.gitignore:66`, verified with `git check-ignore`) — it exists
  only in the main checkout; deploys must run from there.
- New hardcoded (non-env) config axis from `ecda756`: the deployed function URL
  `https://createsumupcheckout-ocgett4nrq-uc.a.run.app` is a string literal in
  `checkout/data/.../SumUpDataSource.kt` (`getSumUpPaymentLink`) — not a BuildConfig
  key. If the function is redeployed to a different region/project, this constant must
  change with it. Candidate for promotion to a BuildConfig axis.
- `functions/.env` exists locally and defines exactly those two key names (names
  verified by pattern-matching key prefixes only; values never read). Firebase Functions
  v2 loads `functions/.env` at deploy time, so deploying from the main checkout supplies
  the function's env. These are NOT Gradle keys — setting them in gradle.properties does
  nothing.

## Where values live locally — as of 2026-07-06

Verified by existence-only checks on this machine: none of the Gradle keys above are
present in the worktree `local.properties` (only `sdk.dir`), the root
`gradle.properties`, or `~/.gradle/gradle.properties`, and none are exported in a
non-interactive shell. UNVERIFIED: the exact supply mechanism on Tommy's machine —
presumably the interactive shell profile or Android Studio's run environment exports
them as env vars (the `System.getenv` branch). Consequence for agents: **a CLI Gradle
build from a fresh non-interactive shell will bake `"null"` into every secret BuildConfig
field** and cannot sign a release. Do not conclude "config is broken" from that alone;
confirm with the wiring checks below and ask before touching anything.

## Checklist — add a new secret/config axis correctly

1. **Name it** `SCREAMING_SNAKE_CASE`, prefixed by service (`SUMUP_`, `CLOUDINARY_`, ...).
2. **Pick the right module**: the `data` (or `presentation`) module of the feature that
   consumes it — never `app` unless it is app-wide, never a domain module.
3. **Read it with the standard pattern** inside that module's `defaultConfig` block:

   ```kotlin
   val MY_NEW_KEY =
       System.getenv("MY_NEW_KEY") ?: project.findProperty("MY_NEW_KEY")
           ?.toString()
   buildConfigField("String", "MY_NEW_KEY", "\"$MY_NEW_KEY\"")
   ```

   In `settings.gradle.kts` (repo credentials only) use the Provider-API variant instead:
   `System.getenv("MY_NEW_KEY") ?: providers.gradleProperty("MY_NEW_KEY").orNull.orEmpty()`.
4. **Ensure BuildConfig is enabled** in that module: `android.buildFeatures.buildConfig = true`
   (already present in every module that reads keys today).
5. **Supply the value locally** (pick ONE, never a tracked file):
   - `~/.gradle/gradle.properties`: add `MY_NEW_KEY=...` (gitignored by nature, machine-global), or
   - export `MY_NEW_KEY` in the shell/IDE environment.
   NEVER in root `gradle.properties` (tracked) and not in `local.properties` (Gradle
   does not read it — see Definitions).
6. **Consume it in code** as `BuildConfig.MY_NEW_KEY` from that module's namespace, and
   handle the `"null"` sentinel defensively.
7. **CI/fastlane**: no CI config exists in-repo as of 2026-07-06 and `fastlane/Fastfile`
   reads no `ENV[...]` (verified). If CI is added later, export the key there too —
   process detail lives in fromagerie-run-and-operate.
8. **Update the catalog table in this skill.**
9. Backend variant: if the key is for the Cloud Function, add it to
   `la-fromagerie-backend/functions/.env` in the main checkout (name only documented
   here) and read via `process.env` — no Gradle involvement.

## Checklist — verify your secrets are wired

Run from the repo root. All commands are value-safe (existence/counts only).

1. **The read site exists**:

   ```bash
   grep -rn "System.getenv(\"MY_NEW_KEY\")" --include="*.kts" .
   ```

2. **A local source supplies it** (counts, never values):

   ```bash
   grep -c "^MY_NEW_KEY=" ~/.gradle/gradle.properties   # 1 = present, 0/error = absent
   [ -n "$MY_NEW_KEY" ] && echo "env: set" || echo "env: unset"
   ```

3. **The generated BuildConfig got a real value, not the "null" sentinel** — after any
   debug build, check the generated file (path pattern:
   `<module>/build/generated/source/buildConfig/<flavor>/<buildType>/<namespace-path>/BuildConfig.java`):

   ```bash
   find checkout/data/build/generated -name BuildConfig.java \
     -exec grep -c 'MY_NEW_KEY = "null"' {} +   # 0 = correctly wired, 1 = missing value
   ```

4. **Repo credentials** (JITPACK_TOKEN / MAPBOX_SECRET_TOKEN): force a fresh resolve and
   watch for 401s:

   ```bash
   ./gradlew :delivery:presentation:dependencies --refresh-dependencies 2>&1 | grep -ci "401" 
   ```

5. **Release signing** (KEYSTORE_*): do NOT run `assembleRelease` just to test on this
   production repo; instead confirm all four keys resolve (step 2, one per key) and that
   the path in `KEYSTORE_PATH` points to an existing file
   (`[ -f "$KEYSTORE_PATH" ] && echo ok` — path is semi-sensitive, avoid echoing it into
   logs that get committed).
6. **Backend**: key names present in the gitignored env file (main checkout only):

   ```bash
   grep -c "^SUMUP_SECRET_KEY=" /Users/tommy/StudioProjects/LaFromagerie/la-fromagerie-backend/functions/.env
   grep -c "^SUMUP_MERCHANT_CODE=" /Users/tommy/StudioProjects/LaFromagerie/la-fromagerie-backend/functions/.env
   ```

## When NOT to use this skill

Use the sibling skills instead for anything outside the config/secrets axis:
fromagerie-build-and-env, fromagerie-run-and-operate,
fromagerie-delivery-logistics-reference, fromagerie-architecture-contract,
fromagerie-change-control, fromagerie-firestore-data-model,
fromagerie-operations-hardening-frontier, fromagerie-failure-archaeology,
fromagerie-payments-reference, fromagerie-payment-reliability-campaign,
fromagerie-debugging-playbook, fromagerie-diagnostics-and-tooling,
fromagerie-validation-and-qa, fromagerie-docs-and-writing.

## Provenance and maintenance

Every fact above was derived by reading the repo on 2026-07-06. Re-verify drift-prone
facts with (run from repo root):

| Fact | Re-verification command |
|---|---|
| Exhaustive list of env-read sites | `grep -rn "System.getenv" --include="*.kts" --include="*.gradle" .` |
| Exact catalog line numbers | `grep -n "getenv\|buildConfigField\|findProperty" checkout/data/build.gradle.kts admin/data/build.gradle.kts admin/presentation/build.gradle.kts delivery/data/build.gradle.kts delivery/presentation/build.gradle.kts checkout/presentation/build.gradle.kts app/build.gradle.kts settings.gradle.kts` |
| Single google-services.json | `find . -name google-services.json -not -path "*/build/*"` |
| Shared applicationId / flavor suffixes | `grep -n "applicationId\|versionNameSuffix" app/build.gradle.kts` |
| gradle.properties flags & AGP opt-outs | `cat gradle.properties` |
| Signing scaffolding printlns still present | `grep -n "println" app/build.gradle.kts` |
| No CI / fastlane env usage yet | `ls .github 2>/dev/null; grep -n "ENV\[" fastlane/Fastfile` |
| Backend env key names | `grep -n "process.env" la-fromagerie-backend/functions/src/index.ts` |
| Backend tracked, `.env` still ignored | `git ls-files la-fromagerie-backend/functions` (expect source files) and `git check-ignore la-fromagerie-backend/functions/.env` (expect a match) |
| Hardcoded function URL unchanged | `grep -n "createsumupcheckout" checkout/data/src/main/java/com/mtdevelopment/checkout/data/remote/source/SumUpDataSource.kt` |
| local.properties still sdk.dir-only | `grep -o "^[A-Za-z._-]*=" local.properties` |

If any command's output no longer matches this file, update the file — key names and
line numbers here are load-bearing for other agents.
