---
name: fromagerie-validation-and-qa
description: What counts as evidence that a LaFromagerie fix or feature actually works — the evidence ladder (unit test > on-device verification > "it compiles"), exact test commands and known-failing baseline, MockK/Turbine test-writing conventions from real tests, the checklist for adding tests to a stub module, and the on-device verification protocol. Load before declaring any change "done", before writing new tests, or when asked "is this actually fixed" / "how do we know this works".
---

# LaFromagerie Validation and QA

What it takes to call a change on this repo actually verified, not just written. This is a **production app** with real customers and real money — "it compiles" is never sufficient evidence that something works.

## The evidence ladder

From weakest to strongest. A change is "done" only when it clears the bar appropriate to what it touches — not every change needs the top rung, but every change needs more than the bottom one.

| Rung | What it proves | What it does NOT prove |
|---|---|---|
| 1. "It compiles" | The types line up | Nothing about behavior. Never cite this alone as evidence a fix works. |
| 2. Unit test (MockK + Turbine, coroutines-test) | The unit's logic does what the test asserts, in isolation | Integration with real Android framework classes, real network behavior, real device conditions |
| 3. On-device verification (screenshots, DataStore/Room dumps, logcat) | The behavior holds in a real running app | Nothing beyond the device/account state you actually exercised — a single manual pass is not a regression suite |

A fix without a discriminating verification step (2 and/or 3, matched to what actually changed) is not done — regardless of how confident the diff looks.

## Test commands (verified 2026-07-06, branch `claude/distracted-chaum-0986e4`)

```bash
./gradlew testClientDebugUnitTest --continue    # client-flavor unit tests, all modules
./gradlew testAdminDebugUnitTest --continue     # admin-flavor unit tests, all modules
```

- `testClientDebugUnitTest --continue`: **367 tasks, 112 tests, 2 real failures** (both in `admin/presentation` `AdminViewModelTest`).
- `testAdminDebugUnitTest --continue`: **371 tasks**; same `AdminViewModelTest` class fails the same way (2 failures: `` addNewProduct aborts when local image upload fails `` and `` updateProduct aborts when local image upload fails ``). Everything else green. (Task count differs slightly from the client run because flavor-specific compilation graphs differ — this is expected, not a sign of drift.)
- `--continue` is essential: without it, Gradle stops at the first module failure and you don't get the full picture across ~20+ modules.
- Some modules (e.g. `checkout:domain`) have **no product flavors**, so their task is the unflavored `testDebugUnitTest`, not `testClientDebugUnitTest` — check `./gradlew :module:tasks | grep -i unittest` if a flavored task name 404s.

## The known-failing baseline — do not touch without on-device verification

**`AdminViewModelTest`** (`admin/presentation/src/test/java/com/mtdevelopment/admin/presentation/viewmodel/AdminViewModelTest.kt`), two tests, both fail the same way on both flavors as of 2026-07-06:

- `` `updateProduct aborts when local image upload fails` `` (line 173)
- `` `addNewProduct aborts when local image upload fails` `` (line 196)

**The disagreement, in one paragraph:** both tests mock `uploadImageUseCase.invoke(...)` to call its result callback with `Result.failure(...)`, then assert that the corresponding `updateProductUseCase`/`addNewProductUseCase` is **never** invoked (`coVerify(exactly = 0)`) and that an error/loading signal fires. But the actual implementation, `AdminViewModel.uploadProductImageIfLocal()` (`admin/presentation/.../viewmodel/AdminViewModel.kt`, ~line 228), only handles the **success** branch of the upload result (`result.onSuccess { onlineUrl -> product.imageUrl = onlineUrl }`) — there is no `.onFailure { }` branch, no thrown exception, and no early return on failure. Both `updateProduct()` and `addNewProduct()` then unconditionally proceed to call `updateProductUseCase.invoke(...)` / `addNewProductUseCase.invoke(...)` regardless of whether the image upload succeeded. So the code's actual behavior is "save the product anyway, keeping whatever `imageUrl` it already had" (silent partial success), while the tests encode a design intent of "abort the whole save if the image upload fails" (fail loud). The tests and the code disagree about the intended product behavior, not about a trivial implementation bug — deciding which one is *right* is a product decision (should a failed image upload block saving the rest of the product data, or not?), which is why this is tracked as an open frontier item rather than something to silently patch.

**Rule for any session touching this area:** do not "fix" either side unilaterally. If your own change happens to touch `AdminViewModel.addNewProduct`/`updateProduct`/`uploadProductImageIfLocal`, re-run this test class and note whether your change altered the failure (still 2 failures with the same message = pre-existing, not yours; a new/different failure = yours, investigate). Never let CI-style "all green" pressure cause a session to add an `.onFailure` branch or delete the assertions without an explicit human decision.

## Test-writing conventions (from real tests, not stubs)

Every module still carrying only `ExampleUnitTest.kt`/`ExampleInstrumentedTest.kt` is untested scaffolding, not a signal of "nothing to test here." Before testing a ViewModel, check its actual state-holder style first — ViewModels in this repo are NOT uniform (some expose `StateFlow`, some Compose `mutableStateOf`, some a mix; see `fromagerie-architecture-contract` §4) and the assertion approach differs accordingly. The following conventions are drawn from three real test files:

- `cart/presentation/src/test/java/com/mtdevelopment/cart/presentation/viewmodel/CartViewModelTest.kt`
- `admin/domain/src/test/java/com/mtdevelopment/admin/domain/usecase/GetOptimizedDeliveryUseCaseTest.kt`
- `checkout/data/src/test/java/com/mtdevelopment/checkout/data/repository/PaymentRepositoryImplTest.kt`

Conventions common to all three:

1. **Backtick-named test functions** describing behavior in plain English: `` fun `setCartVisibility updates visibility state correctly`() ``, `` fun `fetches fresh route when no cache exists`() ``, `` fun `createNewCheckout emits mapped result on success`() ``. Not `test1`, not `testSetCartVisibility`.
2. **MockK for all collaborators** — `mockk()` for strict mocks that must have every used method stubbed, `mockk(relaxed = true)` for dependencies whose calls you don't care to verify (seen throughout `AdminViewModelTest`'s many collaborators). `coEvery { ... } returns ...` / `coEvery { ... } answers { ... }` for suspend functions; plain `every` for non-suspend.
3. **Turbine for Flow-returning code**: `repository.createNewCheckout(...).test { val result = awaitItem(); ...; awaitComplete() }` (`PaymentRepositoryImplTest`). Don't manually `.first()`/`.toList()` a flow under test when Turbine is available in that module.
4. **`StandardTestDispatcher` + `Dispatchers.setMain`/`resetMain`** for ViewModel tests that launch coroutines in `viewModelScope` (`CartViewModelTest`, `AdminViewModelTest`): set up in `@Before`, tear down in `@After`, and call `testScheduler.advanceUntilIdle()` after invoking the method under test to let launched coroutines actually run before asserting.
5. **Plain `runTest { }`** (no explicit dispatcher) is fine for pure use-case tests with no `viewModelScope` involvement (`GetOptimizedDeliveryUseCaseTest`).
6. **`coVerify(exactly = N) { ... }`** to assert a collaborator was or wasn't called — used both to prove a short-circuit path (`exactly = 0`) and to prove exactly one network call happened despite multiple triggers (`exactly = 1`).
7. **Mocking Android statics when needed**: `mockkStatic(Uri::class)` (`AdminViewModelTest`) and `mockkStatic(Log::class)` with `every { Log.e(...) } returns 0` (`PaymentRepositoryImplTest`) — both cleaned up in `@After` with `unmockkAll()` / `unmockkStatic(...)`.
8. **`slot<T>()` + `capture(...)`** to inspect an argument passed to a mocked collaborator after the fact (`AdminViewModelTest`'s `savedProduct` capture to check the final `imageUrl`).

### Where tests live

Standard Gradle/Android convention, per module per source set: `<module>/src/test/java/<package>/...Test.kt` for JVM unit tests, `<module>/src/androidTest/java/<package>/...` for instrumented tests (most of the latter are still `ExampleInstrumentedTest.kt` stubs — 19 files repo-wide as of 2026-07-06). Flavor-specific test sources would live under `src/test<Flavor>/...` but this repo's tests are written against shared/flavor-agnostic classes, so this pattern is not currently in use — verify with `find . -path "*/src/test*" -type d | grep -v build` if a module surprises you.

### Checklist: adding tests to a module that only has `ExampleUnitTest`

Compare a module with real tests (`cart/presentation`) against one still stub-only (`auth`) — the delta in `build.gradle.kts` is exactly:

```kotlin
// cart/presentation/build.gradle.kts (has real tests) — testImplementation block:
testImplementation(libs.junit)
testImplementation(libs.mockk)
testImplementation(libs.turbine)
testImplementation(libs.coroutines.test)

// auth/build.gradle.kts (stub-only) — testImplementation block:
testImplementation(libs.junit)
```

To bring a stub module up to the same standard:

1. Add `testImplementation(libs.mockk)`, `testImplementation(libs.turbine)`, `testImplementation(libs.coroutines.test)` to that module's `build.gradle.kts` (all three aliases already exist in the root `gradle/libs.versions.toml` — no new version pinning needed).
2. Delete or leave `ExampleUnitTest.kt` alongside your real test files (it's harmless scaffolding, not a placeholder you must preserve).
3. Follow the 8 conventions above.
4. Run the module's specific test task before the full suite to iterate fast: `./gradlew :module:path:testClientDebugUnitTest` (or `:testDebugUnitTest` for flavorless modules).
5. Run the full `--continue` suite once before calling the work done, to confirm you didn't regress anything elsewhere.

## On-device verification protocol

Cross-reference `fromagerie-diagnostics-and-tooling` for the *how* (scripts, DataStore/Room dump commands, log tags) — this section is the *protocol*, i.e. what to do and in what order.

1. Install the flavor you're verifying: `./gradlew :app:installClientDebug` or `:app:installAdminDebug` (remember: same applicationId, installing one flavor replaces the other on a device — see `fromagerie-run-and-operate`).
2. Run `scripts/fresh-install-check.sh` (from `fromagerie-diagnostics-and-tooling`) to rule out the stale-APK trap before you draw any conclusion from what you see.
3. Drive the UI: `adb shell input tap <x> <y>`, `adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png`, or read the Compose semantics tree if using an interactive preview tool.
4. Inspect state directly rather than trusting what the UI shows: DataStore dumps (`shared_settings`, `checkout_settings`, `admin_data`) and, if relevant, the Room DB (`lafromagerie_database`).
5. **STOP before the Google Pay button.** This is a hard rule repeated across every skill that touches checkout for a reason: the merchant and Google Pay environment are both real production, there is no sandbox (SumUp's test merchant is not Google-Pay-compatible), and tapping through completes a real charge. Verification of the checkout flow ends at the point the Pay sheet would be summoned, or is done by reading the flow's unit tests instead.
6. Capture what you saw (screenshot, DataStore dump excerpt, logcat snippet) as your evidence — not just "I tapped through and it looked fine."

## What a PR must show before merge

Full merge-gate criteria (branch protections, review expectations, commit hygiene) live in `fromagerie-change-control` — don't duplicate that here. From a pure evidence standpoint, before asking for merge a change should be able to answer:

- Which rung of the evidence ladder was cleared, and for which specific behavior (not "tests pass" in the abstract — which test, asserting what).
- If the known-failing `AdminViewModelTest` baseline was touched: is the failure count still 2, with the same messages, or did something change — and if it changed, was that the explicit goal of the PR?
- If the change touches checkout/payment code: was verification done via unit tests and/or a manual pass that stopped before the Pay button — never "I completed a test payment."
- If the change touches production-facing data (Firestore reads/writes): confirmation that no write/delete happened against the shared production project during verification, beyond what the PR's actual feature requires.

## When NOT to use this skill

- The actual adb/log/DataStore *commands* to run → `fromagerie-diagnostics-and-tooling`
- Deciding *what's wrong* given a symptom → `fromagerie-debugging-playbook`
- Branch/PR/review process itself → `fromagerie-change-control`
- Whether the payment flow's specific mechanics are even understood correctly → `fromagerie-payments-reference`
- Whether a past bug like this one was already fought and settled → `fromagerie-failure-archaeology`

## Provenance and maintenance

Facts verified 2026-07-06 against branch `claude/distracted-chaum-0986e4`, both flavors. Re-verify drift-prone claims:

- Client baseline still 112 tests / 2 failures: `./gradlew testClientDebugUnitTest --continue` then check the summary line and `admin/presentation/build/reports/tests/testClientDebugUnitTest/index.html`
- Admin baseline still 2 failures, same names: `./gradlew testAdminDebugUnitTest --continue` then check `admin/presentation/build/reports/tests/testAdminDebugUnitTest/index.html`
- The upload-failure code path is unchanged: `grep -n "uploadProductImageIfLocal" -A 12 admin/presentation/src/main/java/com/mtdevelopment/admin/presentation/viewmodel/AdminViewModel.kt` (expect no `.onFailure` branch — if one now exists, the disagreement described above may be resolved and this section needs updating)
- Test dependency delta (real-test module vs. stub module): `grep -n "testImplementation" cart/presentation/build.gradle.kts auth/build.gradle.kts`
- Instrumented test stub count: `find . -iname "ExampleInstrumentedTest.kt" | grep -v build | wc -l`
