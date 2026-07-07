---
name: fromagerie-architecture-contract
description: >
  Load before touching module structure, dependencies, DI, or cross-flavor code in
  LaFromagerie: the feature x layer module matrix and dependency rules, the client/admin
  flavor system (source sets, adminImplementation wiring, "which variants compile this
  file?"), Koin DI layout and the runtime-missing-definition trade-off, the ViewModel/UDF
  state conventions, and the load-bearing invariants (money-as-cents, dd/MM/yyyy Locale.ROOT,
  Firestore-source-of-truth with Room caches, both-flavors-must-compile) plus the known
  weak points (no admin auth, empty auth module, client-side secrets, layering leaks).
  Load when adding a module, moving code between layers, adding flavor-dependent code,
  wiring a Koin definition, or reasoning about why a design decision holds.
---

# LaFromagerie — Architecture Contract

The load-bearing design decisions of this app, WHY they hold, the invariants you must not
break, and the weak points stated plainly. Every claim was verified by reading the repo as
of **2026-07-06** (HEAD `b97eb83`). This is a REAL production app (production Firestore,
real SumUp money, one solo developer). "Verify by reading" beats "assume".

## Jargon, defined once

| Term | Meaning here |
|---|---|
| **Flavor** | AGP product-flavor on dimension `version`: `client` (the customer shop app) or `admin` (the owner's prep/delivery app). |
| **Variant** | flavor x build type, e.g. `clientDebug`, `adminRelease`. |
| **Source set** | A directory whose code is compiled only for matching variants: `src/main` (all), `src/client`, `src/admin`, `src/debug`, etc. |
| **Layer** | `data` (Firestore/Room/Ktor/DataStore), `domain` (use cases + models + repository interfaces), `presentation` (Compose UI + ViewModels + DI). |
| **UDF** | Unidirectional Data Flow: UI observes an immutable state object; events call ViewModel methods that emit a new state. |
| **Koin** | The DI framework. Definitions are resolved at runtime, not checked at compile time. |

## When NOT to use this skill

- Building / Gradle sync / worktree / JDK / AGP toolchain questions → **fromagerie-build-and-env**.
- Which env var feeds which BuildConfig field, secret wiring → **fromagerie-config-and-secrets**.
- Installing/running/versioning/shipping/adb device work → **fromagerie-run-and-operate**.
- How the payment chain works end to end → **fromagerie-payments-reference**.
- The Firestore field-by-field contract and collections → **fromagerie-firestore-data-model**.
- Why a past design decision was made / a settled bug → **fromagerie-failure-archaeology**.
- How a change gets classified, gated, merged → **fromagerie-change-control**.

---

## 1. The module graph (feature x layer matrix)

Multi-module Clean Architecture. Features are vertical slices; each slice is split into up
to three horizontal layers. Not every feature has every layer.

| Feature | data | domain | presentation |
|---|:---:|:---:|:---:|
| home | ✅ | ✅ | ✅ |
| details | — | — | ✅ |
| cart | — | ✅ | ✅ |
| checkout | ✅ | ✅ | ✅ |
| delivery | ✅ | ✅ | ✅ |
| admin | ✅ | ✅ | ✅ |
| **core** | ✅ | ✅ | ✅ |

Plus the top-level `app` module (wires everything, holds `MainActivity`, DI composition
root, the Room `FromagerieDatabase`) and **`auth` — an empty stub module** (verify below).

Verify the module list:
```bash
grep -E 'include\(' settings.gradle.kts
```

### 1.1 The `auth` module is empty (verify it still is)

`auth` is declared in `settings.gradle.kts` and has a `build.gradle.kts`, but its only
source is the two IDE-generated example tests — no `src/main` Kotlin at all.

```bash
find auth -type f | grep -v /build/ | grep -v -iE 'example|\.gitignore|\.pro$|build.gradle|AndroidManifest'
# As of 2026-07-06: prints nothing. If it prints files, auth has grown real code — update this.
```
No module depends on `:auth`. It is a placeholder for a future login feature. See §6 for
the "no admin auth" weak point it hints at.

### 1.2 Dependency rules (and the real violations)

The intended rule is the Clean-Architecture staple:
`presentation → domain`, `data → domain`, `app → everything`, and **domain depends on
nothing but `core:domain`**.

The actual graph (extracted from every `build.gradle.kts` dependency block, 2026-07-06):

```
admin/data            -> core:data core:domain admin:domain
admin/domain          -> core:domain delivery:domain
admin/presentation    -> admin:domain core:presentation core:domain
cart/domain           -> core:domain
cart/presentation     -> cart:domain core:presentation core:domain
checkout/data         -> core:data core:domain checkout:domain
checkout/domain       -> core:domain
checkout/presentation -> core:presentation core:domain checkout:domain cart:presentation admin:presentation
core/data             -> core:domain
core/domain           -> (none)
core/presentation     -> core:domain
delivery/data         -> core:data delivery:domain
delivery/domain       -> core:domain
delivery/presentation -> core:presentation core:domain delivery:domain cart:presentation admin:presentation
details/presentation  -> core:domain core:presentation cart:domain cart:presentation admin:presentation
home/data             -> home:domain core:presentation core:data core:domain
home/presentation     -> home:domain core:presentation core:domain cart:presentation admin:presentation
```

Regenerate this graph any time:
```bash
for f in $(find . -name build.gradle.kts | grep -vE '^\./build.gradle.kts$|/build/'); do
  mod=$(dirname "$f" | sed 's|^\./||')
  deps=$(grep -oE 'project\(":[^"]+"\)' "$f" | sed 's/project(":\(.*\)")/\1/' | tr '\n' ' ')
  printf "%-22s -> %s\n" "$mod" "$deps"
done
```

**Known violations of the pure rule — do not "clean these up" without change control:**

1. **`home/data` depends on `core:presentation`** — a data layer importing a presentation
   layer. Layering leak. (It reuses shared UI/mapping models from `core:presentation`.)
2. **Feature `*:presentation` modules depend on `admin:presentation`** (home, checkout,
   delivery, details). This is deliberate — it makes the admin-flavor screens reachable and
   is what forces admin code to compile into BOTH flavors (see §2.3). It is NOT gated by
   `adminImplementation` at the library level.
3. **`admin/domain` depends on `delivery:domain`** — a domain-to-domain cross-feature edge
   (admin routing reuses delivery path models). Acceptable but note it.
4. Domain modules are **not android-free** — see §5 invariant.

If you add an edge that points "up" a layer or creates a cycle, the build will still often
succeed (Gradle allows it); the cost is coupling. Surface such additions in the PR.

---

## 2. The flavor system IS the architecture

There is one dimension, `version`, with two flavors declared in `app/build.gradle.kts`:

```kotlin
flavorDimensions += "version"
productFlavors {
    create("client") { dimension = "version"; versionNameSuffix = "-client" }
    create("admin")  { dimension = "version"; versionNameSuffix = "-admin" }
}
```

**There is NO `applicationIdSuffix`.** Both flavors install as
`com.mtdevelopment.lafromagerie`, so client and admin **cannot coexist on one device** —
installing one replaces the other. (Shipping/adb detail lives in
**fromagerie-run-and-operate**.)

### 2.1 Library modules declare the flavor too

The flavor axis is not app-only. Nearly every Android module re-declares the same
dimension — 15 of them as of 2026-07-06 (app; admin data/domain/presentation;
checkout data/presentation; cart/presentation; core data/presentation;
delivery data/presentation; details/presentation; home data/domain/presentation) — so
their test tasks use the flavored `test<Flavor>DebugUnitTest` form. Modules WITHOUT the
dimension (checkout/domain, cart/domain, core/domain, delivery/domain, auth) use plain
`testDebugUnitTest`:

```bash
grep -rln 'flavorDimensions' */build.gradle.kts */*/build.gradle.kts | grep -v /build/
# 15 modules as of 2026-07-06 — run it, don't trust a cached list
```

Modules that actually have flavored source directories on disk:
```bash
find . -type d \( -path '*/src/client' -o -path '*/src/admin' \) | grep -v /build/ | sort
# app, core/presentation, delivery/presentation, details/presentation,
# home/domain, home/presentation  (as of 2026-07-06)
```

### 2.2 What differs per flavor

- **`app/src/client` vs `app/src/admin`**: separate `MainActivity` (client 285 lines,
  admin 267), separate `di/FlavorModules.kt`, separate navigation. The **admin** app adds
  `DeliveryTrackingService` (foreground service) and `NotificationBroadcastReceiver`.
- **`core/presentation/src/{client,admin}/.../util/CONST.kt`** each define
  `const val VARIANT = "client"` / `"admin"`.
  ⚠️ **As of 2026-07-06 `VARIANT` is referenced nowhere** (grep finds only the two
  definitions). It is vestigial — the real flavor switch is source-set selection, not a
  runtime `if (VARIANT == ...)`. Do not add runtime branches on `VARIANT` expecting them to
  gate flavor behavior; put flavor-specific code in the matching source set instead.
  Re-check: `grep -rn '\bVARIANT\b' . | grep '\.kt' | grep -v /build/`
- **`delivery/presentation/src/{client,admin}`** and **`home/presentation/src/{client,admin}`**:
  flavored screens/composables. Admin source imports `com.mtdevelopment.admin.presentation.*`
  (e.g. `AdminViewModel`, `ProductEditDialog`, `AdminUiDeliveryPath`).

### 2.3 The `adminImplementation` wiring — and its important caveat

In `app/build.gradle.kts` the admin feature modules are wired flavor-scoped:
```kotlin
"adminImplementation"(project(":admin:data"))
"adminImplementation"(project(":admin:domain"))
"adminImplementation"(project(":admin:presentation"))
```
So at the **app** level, admin modules are on the classpath only for the admin variant.

**BUT** `admin:presentation` is also a plain `implementation` dependency of
`home:presentation`, `checkout:presentation`, `delivery:presentation`, and
`details:presentation` (see §1.2 graph). Because of one main-source-set leak —
`delivery/presentation/src/main/.../model/UiDeliveryPath.kt` imports
`com.mtdevelopment.admin.presentation.model.AdminUiDeliveryPath` — admin presentation code
**compiles into the client flavor too**. The `adminImplementation` scoping is therefore not
a hard wall; treat "admin code never reaches client" as **false**.

Verify the leak:
```bash
grep -rn 'com.mtdevelopment.admin' */presentation/src/main | grep '\.kt:' | grep -v /build/
```

### 2.4 Checklist — "which variants compile this file?" (run BEFORE editing)

Given a file path, decide which variants build it:

| Path contains… | Compiles for |
|---|---|
| `src/main/` in a client-side module | both `client` and `admin` |
| `src/client/` | `client` variants only |
| `src/admin/` | `admin` variants only |
| anything under `admin/` **AND app wires it via `adminImplementation`** | `admin` variants (but see §2.3: `admin:presentation` also transitively via `src/main` leak) |
| `src/debug/` / `src/release/` | that build type only |

Then confirm with the module's build script:
```bash
# Does THIS module declare flavors (i.e. does src/client|admin apply here)?
grep -n 'flavorDimensions\|productFlavors' <module>/build.gradle.kts
```

### 2.5 Checklist — adding flavor-dependent code

1. Decide the flavor(s) it targets.
2. If the code should differ per flavor, the module MUST declare the `version` dimension
   (copy the `flavorDimensions += "version"` block from `core/presentation/build.gradle.kts`).
   Without it, `src/client`/`src/admin` are silently ignored.
3. Put shared signatures in `src/main`, flavor bodies in `src/client` / `src/admin`. If you
   need a function to exist in both, it must be present in **both** source sets (or in
   `src/main`), or one flavor fails to compile.
4. Admin-only dependencies at the app level: use `"adminImplementation"(...)`. Do NOT add
   admin deps as plain `implementation` at the app level unless you want them in the client
   APK.
5. **Compile BOTH flavors before you consider it done** (both-flavors invariant, §5):
   `./gradlew :app:assembleClientDebug :app:assembleAdminDebug` (assembling, not building
   is fine — see **fromagerie-build-and-env**).

---

## 3. Dependency Injection (Koin)

DI is Koin (BOM 4.1.1). The composition root is `CheeseApplication.onCreate()`:

```kotlin
startKoin {
    androidLogger(level = Level.DEBUG)
    androidContext(this@CheeseApplication)
    modules(appModule() + flavorModules())   // common modules + flavor-specific modules
}
```
File: `app/src/main/java/com/mtdevelopment/lafromagerie/CheeseApplication.kt`.

### 3.1 Module layout

- **Common:** `app/src/main/.../di/AppModule.kt` — `appModule()` returns a list of Koin
  modules: `mainAppModule` (repositories as `single`, use cases as `factory`, ViewModels via
  `viewModelOf(::X)`), plus `provideJson`, `provideDatastore`, `provideFirebaseDatabase`,
  `provideRoomFromagerieDatabase`, `provideGeocoder`, and one Ktor `HttpClient` per API
  (`provideOpenRouteDatasource`, `provideAddressApiDataSource`,
  `provideAutoCompleteApiDataSource`, `provideSumUpDataSource`).
- **Flavor-specific:** each flavor has its own `di/FlavorModules.kt` returning
  `flavorModules()`:
  - `app/src/client/.../di/FlavorModules.kt` — **empty** (`module { }`) as of 2026-07-06.
  - `app/src/admin/.../di/FlavorModules.kt` — composes
    `adminDataModule() + adminDomainModule() + adminPresentationModule() + mainModule`
    (the admin `mainModule` provides `DeliveryTrackingService`).
- **Per admin layer:** `admin/{data,domain,presentation}/src/main/.../di/adminModule.kt`
  each declare their own `module { }` (functions `adminDataModule()` etc.).

Find every module declaration:
```bash
grep -rln '= module {' . | grep '\.kt' | grep -v /build/
```

### 3.2 How ViewModels are injected

Declared with `viewModelOf(::HomeViewModel)` etc. in `mainAppModule`. Compose screens obtain
them via `koinViewModel()` (koin-androidx-compose-navigation). Constructor params are
resolved positionally from other Koin definitions via `get()`.

### 3.3 The trade-off you must respect: missing definition = RUNTIME crash

Koin resolves lazily. A ViewModel that requires a use case with **no matching Koin
definition compiles fine and crashes at runtime** (`NoDefinitionFoundException`) the moment
that screen/VM is constructed — typically not caught by unit tests.

**Rule:** whenever you add a constructor parameter to a repository, use case, or ViewModel,
add or update its Koin definition in the correct module (common `mainAppModule`, or the
admin `adminModule.kt` for admin-only types). For admin-only dependencies, put the
definition in the admin module, not the common one, or the client flavor will try (and
fail) to resolve it. There is no compile-time DI graph check — the safety net is running
each affected screen once per flavor.

---

## 4. State / UDF conventions (what IS, not what should be)

Read two representative ViewModels; the pattern is consistent but **not uniform**.

**`CartViewModel`** (`cart/presentation/.../viewmodel/CartViewModel.kt`) — the canonical form:
```kotlin
private val _cartUiState = MutableStateFlow(CartUiState())
val cartUiState: StateFlow<CartUiState> = _cartUiState.asStateFlow()
// mutations: _cartUiState.update { it.copy(...) }
```
- One immutable state data class per screen, defined under `presentation/state/`
  (`CartUiState`). UI observes the `StateFlow`; events call methods (`addCartObject`,
  `removeCartObject`, …). Persistence is delegated to a use case
  (`SaveToDatastoreUseCase`), and the VM re-hydrates from DataStore in `init { }` so external
  changes (e.g. cart cleared after payment) flow back in.

**`DeliveryViewModel`** (`delivery/presentation/.../viewmodel/DeliveryViewModel.kt`) — a
**mixed** form; this is real and you should mirror the local file's style, not force one:
```kotlin
var deliveryUiDataState by mutableStateOf(DeliveryUiDataState())   // Compose snapshot state
private val _deliverySearchQuery = MutableStateFlow("")            // Flow for text query
val isConnected: StateFlow<Boolean> = getIsConnectedUseCase().stateIn(...)
```
So some VMs use `MutableStateFlow` + `asStateFlow()`, others use Compose `mutableStateOf`
directly, and network-connectivity is exposed as a `StateFlow` via `stateIn`. **State
classes live in `presentation/state/` (or `presentation/model/`).**

Practical convention to follow when editing:
- Keep one state holder per screen; expose it read-only; mutate with `.update { copy() }`
  for Flows or direct assignment for `mutableStateOf`.
- Do business logic in use cases, not in the VM.
- Match the file you are editing — do not "modernize" a `mutableStateOf` VM to `StateFlow`
  as a drive-by; that is a refactor and goes through change control.

---

## 5. Invariants (break one and here is what fails)

| # | Invariant | Rationale | What breaks if violated |
|---|---|---|---|
| I1 | **Money is a cent-`Long` end-to-end.** `Double.toCentsLong()` rounds (`roundToLong`), `Long.toStringPrice()` formats, `String.toLongPrice()` parses. File: `core/domain/.../LogicUtils.kt`. | Floating-point euros lose cents; this was hardened after real rounding bugs (see failure-archaeology). | Off-by-a-cent totals, wrong SumUp charge amounts, mismatched order totals in Firestore. |
| I2 | **Dates are `"dd/MM/yyyy"` on `Locale.ROOT`** for parse/format (`DATE_FORMATTER_DDMMYYYY` in LogicUtils). | Non-Latin-digit locales silently corrupted timestamp sorts. | Delivery-date sorting/parsing breaks on some device locales; orders sort wrong on delivery day. |
| I2b | Price **display** uses `Locale.FRANCE` (`NumberFormat.getCurrencyInstance(Locale.FRANCE)`), including non-breaking spaces. This is separate from I2 (ROOT is only for date parse/format). | Correct French currency formatting for customers. | Wrong currency glyphs/grouping if changed. |
| I3 | **Firestore is the source of truth; Room is a cache invalidated by the `database_update` timestamp.** `GetLastFirestoreDatabaseUpdateUseCase` compares the remote timestamp to the local cache. `FromagerieDatabase` is built with `fallbackToDestructiveMigration(true)` (schema version 4; entities `ProductEntity`, `PathEntity`). | Offline reads + cheap reloads without hammering Firestore. | Stale product/price/path data shown to customers if the invalidation timestamp is not bumped on writes. Room schema bumps wipe the cache (acceptable, it re-fetches). Field-level contract lives in **fromagerie-firestore-data-model**. |
| I4 | **Both flavors must always compile.** | Admin code compiles into client (§2.3) and vice-versa; a broken flavor is a broken release. | CI is not there to catch it (no in-repo CI); a green client build can hide an admin compile break. Assemble both before merge. |
| I5 | **Domain modules are NOT android-free (honest state).** `core:domain/LogicUtils.kt` imports `android.location.Location` (for `calculateDistance`) and `android.util.Log`. | Pragmatic reuse; never enforced as a rule. | Do NOT assume you can run domain as pure JVM/multiplatform. Do NOT "purify" it as a drive-by — it is load-bearing for existing tests and callers. Record, don't crusade. |

Re-verify I1/I2/I5 quickly:
```bash
grep -nE 'import android|Locale|roundToLong|getCurrencyInstance' \
  core/domain/src/main/java/com/mtdevelopment/core/domain/LogicUtils.kt
```

---

## 6. Known weak points (stated plainly, not spun)

| Weak point | Evidence | Consequence |
|---|---|---|
| **No authentication in the admin flavor.** | Grep-verified: no login/auth gate anywhere in admin code; `auth` module is empty (§1.1); Firebase Auth is a dependency in `home/data` but unused for gating. | The admin app is protected ONLY by controlled APK distribution. Anyone with the admin APK has full admin power (edit products/prices, see orders, run deliveries). Roadmap item — see **fromagerie-operations-hardening-frontier**. |
| ~~Backend is not in version control~~ **RESOLVED 2026-07-07** (`ecda756`). | `la-fromagerie-backend/` (Cloud Function `createSumUpCheckout`, now the live hosted-checkout path) is git-tracked; `functions/.env` stays gitignored. | Residuals tracked in **fromagerie-operations-hardening-frontier** §2 (deploy README, stray Xcode-tool artifact). |
| **Empty `auth` module.** | §1.1. | Dead scaffolding; signals intended-but-absent auth. |
| **SumUp keys live in `BuildConfig` (client-side secrets).** | `SUMUP_PRIVATE_KEY` is read into the app's Ktor client in `AppModule.kt` `provideSumUpDataSource`. | A private payment key is baked into the APK. Mitigation/rationale is in **fromagerie-payments-reference** / **fromagerie-config-and-secrets**; do not treat it as safe. |
| **Single `applicationId` for both flavors.** | §2 (no `applicationIdSuffix`). | Cannot run client and admin side-by-side on one device; test-device juggling. |
| **DI has no compile-time graph check.** | §3.3. | Missing Koin definitions crash at runtime, not build time. |
| **Layering leaks** (`home:data → core:presentation`; admin presentation into client). | §1.2, §2.3. | Coupling; "admin code never ships to client" is false. |
| **2 failing unit tests at HEAD** (`AdminViewModelTest`, image-upload-abort). | As of 2026-07-06. | Baseline is not green; do not block on them or "fix" them blind. Triage lives in **fromagerie-change-control** / **fromagerie-operations-hardening-frontier**. |

---

## Provenance and maintenance

Re-verify each drift-prone claim (all commands from repo root):

| Claim | Re-verification command |
|---|---|
| Module list | `grep -E 'include\(' settings.gradle.kts` |
| `auth` still empty | `find auth -type f \| grep -v /build/ \| grep -viE 'example\|gitignore\|\.pro$\|build.gradle\|Manifest'` (empty = still a stub) |
| Dependency graph | the `for f in $(find . -name build.gradle.kts …)` loop in §1.2 |
| `adminImplementation` wiring | `grep -n 'adminImplementation' app/build.gradle.kts` |
| admin presentation leaks into client | `grep -rn 'com.mtdevelopment.admin' */presentation/src/main \| grep '\.kt:' \| grep -v /build/` |
| `VARIANT` is vestigial | `grep -rn '\bVARIANT\b' . \| grep '\.kt' \| grep -v /build/` (only the two `const val` = still unused) |
| Flavor declaration | `grep -n 'flavorDimensions\|productFlavors' app/build.gradle.kts` |
| No `applicationIdSuffix` | `grep -n 'applicationIdSuffix' app/build.gradle.kts` (no output = still single id) |
| Koin composition root | `grep -n 'startKoin\|modules(' app/src/main/java/com/mtdevelopment/lafromagerie/CheeseApplication.kt` |
| Koin module list | `grep -rln '= module {' . \| grep '\.kt' \| grep -v /build/` |
| Money/date/android invariants | `grep -nE 'import android\|Locale\|roundToLong\|getCurrencyInstance' core/domain/src/main/java/com/mtdevelopment/core/domain/LogicUtils.kt` |
| Room DB version/entities/destructive-migration | `grep -rn '@Database\|fallbackToDestructiveMigration' app/src/main --include='*.kt'` |
| 2 failing tests still failing | `./gradlew testClientDebugUnitTest --continue` (see change-control for the known baseline) |

If any command's output diverges from what this skill states, update this skill and
date-stamp the change.
