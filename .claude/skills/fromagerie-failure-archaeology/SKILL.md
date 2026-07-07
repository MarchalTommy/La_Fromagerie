---
name: fromagerie-failure-archaeology
description: Chronicle of every major LaFromagerie investigation, dead end, rejected fix, and revert — SumUp 409/500/3DS saga, AGP 9 migration opt-outs, notification/foreground-service pain, money rounding, buyer duplicates, stale-APK trap, the "+ button" rollback, admin bug batches, abandoned branches. Load this BEFORE re-investigating any bug that feels familiar, before proposing a refactor or dependency change, or when git history/branches need interpreting — so you don't re-fight a settled battle.
---

# LaFromagerie Failure Archaeology

Every entry: **Symptom → Root cause → Evidence → Status**. Commit hashes verified against `git log --all` on 2026-07-06 (HEAD `b97eb83`). Where a cause is inferred rather than proven, it is labeled *inference*.

Rule of engagement: before "fixing" anything that matches a symptom below, read its entry. If the status is **settled**, the current behavior is a deliberate outcome — reopen it only with new evidence and via change control (see fromagerie-change-control).

## Reading this repo's history: two quirks

1. **Duplicate commit pairs.** Many commits appear twice with identical messages (`b97eb83`/`06c8ebd`, `01ef0af`/`30c4730`, `bb3d36a`/`8ef61e5`, `657b721`/`7887bcd`, …). *Inference:* rebase/cherry-pick between local and origin branches by a solo dev; both copies reachable via merged branches. Treat the pair as one event; cite the one reachable from `main`.
2. **Informal, diary-register commit messages are normal** — English, often self-deprecating ("big ass commit", "my own worst enemy"; French appears in code comments and UI strings, not commit subjects — see `fromagerie-docs-and-writing`). They often contain the real diagnosis — read them fully with `git log --grep`.

## 1. The SumUp 409 / 500 / 3DS saga

- **Symptom:** Google Pay + SumUp integration failing for months across late 2025: first 404s, then HTTP 409 `NON_EXTDEV_PAYMENT_METHOD` on processing checkouts, then an HTTP 500, plus unhandled 3DS challenges.
- **Root cause(s):**
  - 404 era: app not linked to the business Google Pay Console (`ce32aa8` "still 404, as I apparently need to link my app to the business Google Pay Console").
  - 409: **SumUp test merchants are not compatible with Google Pay.** Fixed only by switching to the real production merchant + real keys (`1ed111b` names the error; `2a71973`/`40312c1` record the fix).
  - 500: a SumUp account-side problem requiring their support (`40312c1` "Currently facing a 500 (need to contact them)").
  - 3DS: challenges needed a dedicated handling surface; ended as a WebView in `ThreeDSecureActivity` (commits `4abfa60`/`ccd512b` "improve 3DS").
- **Evidence:** commits `ce32aa8`, `1e3fe1e`, `e4db5b9`, `1ed111b`, `2a71973`, `40312c1`, `4abfa60`; resolution `01ef0af` "SumUp -> * Fixed the whole SumUp integration"; branch `feature/payment_last_branch` (see §11). Files: `checkout/data/.../SumUpDataSource.kt`, `checkout/presentation/.../ThreeDSecureActivity.kt`.
- **Status:** **settled.** Consequences that must not be relitigated: no test-merchant path exists for the full flow; production merchant only; never complete a real payment in testing. Mechanics: see fromagerie-payments-reference.
- **Epilogue (2026-07-07):** the hosted-checkout idea from this saga was never abandoned — it landed as a SECOND payment path in `ecda756` (Cloud Function + custom tab + deep link; works without Google Wallet; backend entered version control in the same commit). Working WIP, hardening tracked in fromagerie-payment-reliability-campaign. Do not read this chronicle as "hosted checkout was rejected."

## 2. AGP 9 migration — settled, with open opt-outs

- **Symptom:** Build breakage and IDE/Kotlin-detection issues while moving from AGP 8.13.2 to AGP 9.
- **Root cause:** AGP 9's built-in Kotlin support and new DSL conflicted with the project's plugin setup; per-module `kotlin-android` plugin declarations had to be restored.
- **Evidence:** `0283e20` (AGP 8.13.2), `78df833` (tried 9.1.0), `90cfdec` (migrate build config to 9.0), `62181e5` (add kotlinAndroid for IDE detection), `7343ca4` (restore kotlin-android for all modules), `9d8d53e` "Finished fixing AGP 9 for now... Temporary shit, but it will do the trick". Settled at **AGP 9.0.1** (`gradle/libs.versions.toml`: `agp = "9.0.1"`, verified 2026-07-06 — despite `78df833`'s message saying 9.1.0).
- **Status:** **settled, with two OPEN opt-outs** in `gradle.properties` (verified 2026-07-06): `android.builtInKotlin=false` and `android.newDsl=false`. Gradle prints a warning about them on every build. They are deliberate and **must be migrated before AGP 10** — do not remove them casually (removal requires the built-in-Kotlin migration; see https://kotl.in/gradle/agp-built-in-kotlin). Do not "upgrade AGP" as a drive-by.

## 3. Notification / foreground-service saga (admin delivery helper)

- **Symptom:** The admin's delivery-day notification assistant was extremely painful to build; AI-generated service code needed heavy debugging.
- **Root cause:** Rusty notification knowledge ("it was HARD... I relearned everything"), verbose AI-written foreground-service code, and a Ktor trap: **`expectSuccess = false` was required or the client crashed on non-2xx responses** (`a6af445` "HUGE PAIN IN THE A*S because of the expectSuccess = false else it was crashing").
- **Evidence:** `dbb2693` (feature start, Google Routes), `a6af445`, `e87e07a`, `83b4ff2` ("BUT HELL, it needs some more work... It wrote too verbose code"), `f272679` (finalized; stop-delivery on app return). Admin-flavor files: `DeliveryTrackingService`, `NotificationBroadcastReceiver` under `app/src/admin/`.
- **Status:** **settled but flagged for cleaning** by the author. If you touch this code, expect verbosity; do not rewrite it without on-device verification of notifications (see fromagerie-validation-and-qa).

## 4. Money-rounding incident

- **Symptom:** Real prices lost a cent (e.g. `19.99 * 100 = 1998.99…` truncated to 1998).
- **Root cause:** Double→cents conversion by truncation instead of rounding; ad-hoc money math scattered around.
- **Evidence:** commit `4aea6ea` "Module-by-module hardening pass: fix money rounding, payment error handling, and resolve TODOs". Current invariant in `core/domain/.../LogicUtils.kt`: money is cent-`Long` everywhere; `Double.toCentsLong()` uses `roundToLong()`; `String.toLongPrice()` strips non-breaking spaces (U+00A0/U+202F) emitted by the French currency formatter. Same commit pinned the `dd/MM/yyyy` formatter to `Locale.ROOT` because non-Latin-digit locales silently broke timestamp sorting.
- **Status:** **settled.** Never introduce new Double money math; the one sanctioned Double crossing is the SumUp `amount` field (see fromagerie-payments-reference).

## 5. Buyer-duplicate bug

- **Symptom:** Duplicate buyer entries appearing (admin-side order handling).
- **Root cause:** UNVERIFIED (as of 2026-07-06) — the commit fixes it without describing the mechanism, and the fix is bundled with other changes.
- **Evidence:** `bb3d36a`/`8ef61e5` (and pre-merge copies `db8f0f6`/`14045ee`) "feat: Fix buyer duplicate bug, persist prepared orders, add fine-grain streets".
- **Status:** **settled** (fix merged via PR #33 era). If duplicates reappear, start from `git show bb3d36a` rather than guessing.

## 6. The June-2026 stale-APK cart-emptying trap

- **Symptom:** Cart mysteriously emptied on the test device; hours spent hunting a "bug" in current code.
- **Root cause:** The installed APK on the device was **older than the code being read** — the bug had already been fixed. Not in git history (operational memory, 2026-06).
- **Evidence:** process lesson only; no commit. Guard command: `adb shell dumpsys package com.mtdevelopment.lafromagerie | grep lastUpdateTime` and compare against recent commit dates before believing any on-device repro.
- **Status:** **settled as a process rule:** before debugging any on-device symptom, verify the installed build is current (reinstall with `./gradlew :app:assembleClientDebug` + `adb install -r`). See fromagerie-debugging-playbook.

## 7. The "+ button" rollback (UX decision of record)

- **Symptom:** A "+" (quick add) button was added to the main product card, then removed days later.
- **Root cause:** Deliberate UX decision after trying it: no quick-add button on the main product card; subtler add affordances won.
- **Evidence:** `e9c8235` "Improve client UX: cart stepper, conditional back arrow, subtler add buttons" then `0049162` "Rollbacked the + button on the main product card" (touches only `home/presentation/.../ProductItem.kt`), merged via PR #41 (`ecb1e84`).
- **Status:** **settled.** Do not re-add a + button to product cards without an explicit product decision from Tommy.

## 8. Admin-mode bug batches

- **Symptom (batch A, `440670e`):** admin couldn't see the add-delivery-path card when no path existed; couldn't manually add a delivery point; couldn't mark orders "ready" on the preparation screen; preparation screen sorted dates wrong (closest date should be on top).
- **Symptom (batch B, `52eb2c1`):** infinite loader, order-card crash, availability toggle broken.
- **Root cause:** Accumulated regressions in the less-exercised admin flavor (admin is only used by the shop owner; bugs surface on delivery days). *Inference:* admin flavor gets less test coverage and later discovery.
- **Evidence:** `440670e` (PR #37 era), `52eb2c1` (PR #38 era), later `65e0d2d` "Fix order preparation sorting/filtering and mark past orders" (PR #40).
- **Status:** **settled** individually, but the pattern is open: admin-flavor regressions recur. Related open item: `admin/.../ProductEditDialog.kt:52` TODO (picture+data changed together) — needs on-device verification.

## 9. Delivery-day instability & lost code during merges

- **Symptom:** Admin delivery start-location code was lost and previously-fixed bugs reappeared.
- **Root cause:** Merge/rebase mishap re-introduced corrected bugs — the commit says so plainly.
- **Evidence:** `04e413c` "Fixed lost code for admin delivery starting location. Re-introduced corrected bugs, sadly."; branch `fix/admin_delivery_instability` (fully merged, 0 unique commits as of 2026-07-06); `fix/fixed_map_not_zooming_on_new_phones` (merged; map zoom fix for newer devices).
- **Status:** **settled**, but it is the historical justification for the change-control rule: PR-to-main only, both flavors must build (see fromagerie-change-control).

## 10. Pipelines: dormant, not broken-by-accident

- **Symptom:** Fastlane/CI references that don't work locally.
- **Root cause:** Pipelines were TeamCity-style with agent-side credentials (`/opt/buildagent/external/firebase-key.json`); no CI config exists in-repo. Fastfile churn shows repeated attempts to keep them alive before they went dormant.
- **Evidence:** `110f32c`/`645991d` "Cleaned up gradles to try and fix the pipelines", `e1eaa72`, `52e136d`, `6f0b8dc` "Changed Fastfile"; branch `fix/pipeline` (fully merged). `fastlane/Fastfile` lanes: `test`, `build`, `version`, `distribute`.
- **Status:** **settled as dormant** (as of 2026-07-06). Do not "fix CI" as a side quest; distribution is manual via fastlane/App Distribution (see fromagerie-run-and-operate).

## 11. Branch graveyard — what each stalled branch means

Verified 2026-07-06 with `git rev-list --count origin/main..origin/<branch>` (unique commits ahead of main):

| Branch | Ahead | Verdict |
|---|---|---|
| `feature/payment_last_branch` | **3** | **Superseded, never merged.** Contains the mid-saga 409/500 state (`54ef47d`, `bfa7adf`, `9bdb2c4`). The equivalent work reached main through other commits (`1ed111b`…`01ef0af`). Historical evidence only — do not merge. |
| `refact_to_state` | **2** | **Abandoned refactor.** "Started refactoring to a cleaner uiState style pattern" (`b641ec9`, `2beb125`); 240 commits behind main. *Inference:* superseded piecemeal — later work (e.g. `7d7117f` cart-state refactor, `PaymentScreenState`) adopted the pattern where it mattered. The wish survives in the TODO at `checkout/.../CheckoutScreen.kt:254` (feature blocked on state rework of that screen). |
| `feature/payment_new` | 0 | Fully merged. Early payment work; name survives only as history. |
| `feature/stock_management` | 0 | Merged (`c502327` availability toggle client+admin). |
| `fix/pipeline` | 0 | Merged; see §10. |
| `feature/recomposition_tracking` | 0 | Merged; performance investigation tooling era. |
| `feature/no_pictures_products` | 0 | Merged (`7ab2647` placeholder/wording for picture-less products). |
| `feature/agy_test` | 0 | Merged. UNVERIFIED (as of 2026-07-06) what "agy" referred to; contents reached main. |
| `fix/admin_delivery_instability` | 0 | Merged; see §9. |
| `fix/fixed_map_not_zooming_on_new_phones` | 0 | Merged; map zoom fix. |
| `feature/documentation` | 0 | Merged; the mass-KDoc effort (§12). |
| `claude/*` branches | — | AI-session worktree branches; ephemeral, merge via GitHub PRs. |

Rule: any branch with 0 unique commits is safe to ignore (and eventually delete via change control); the two non-zero branches above are archives, not work-in-progress.

## 12. Code-style regime changes (context for "why does this code look different")

- **Gemini mass refactor:** `657b721`/`7887bcd` "Let Gemini 3.5 refact a whole lot of code to help me maintain the app". Large mechanical changes; treat pre/post style differences as intentional.
- **Mass-KDoc commits:** `07dba1d` (home/detail/core), `1733286` (checkout/delivery), `ff1dc36` (cart), `548ea05` (admin). **The docs of record are KDoc in-code; there is no README.** When code and KDoc disagree, suspect the KDoc drifted — verify against behavior. Writing conventions: see fromagerie-docs-and-writing.
- **Hardening pass:** `4aea6ea` also added the first substantial unit-test batteries (AdminViewModel, CartViewModel, route use cases).
- **Durable payments:** `9e386bc` (WorkManager finalization) and `7d7117f` (cart state refactor + payment security) define the current payment architecture.

## 13. Known-failing baseline (do not silently "fix" claims)

As of 2026-07-06, `./gradlew testClientDebugUnitTest --continue` has **2 real failures** in `admin/presentation` `AdminViewModelTest` (MockK verification failures around image-upload-abort behavior: `addNewProduct aborts when local image upload fails` at line ~196 and its `updateProduct` sibling at ~173). All checkout-module tests are green. Treat these two as the documented baseline; changing them requires an actual investigation, not test deletion. Details: fromagerie-validation-and-qa.

## When NOT to use this skill

- You need current payment mechanics (not history) → **fromagerie-payments-reference**
- You are executing the reliability effort → **fromagerie-payment-reliability-campaign**
- You are triaging a fresh symptom → **fromagerie-debugging-playbook** (it may route you back here)
- Build/env failures on a fresh machine or worktree → **fromagerie-build-and-env**
- What is allowed to change and how → **fromagerie-change-control**
- Open problems that are *future* work rather than settled history → **fromagerie-operations-hardening-frontier**

## Provenance and maintenance

All hashes and branch counts verified 2026-07-06 (HEAD `b97eb83`). Re-verification one-liners:

- Any entry's commit: `git show <hash> --stat`
- Full history sweep: `git log --all --oneline | head -150`
- Branch graveyard counts: `for b in feature/payment_last_branch refact_to_state; do git rev-list --count origin/main..origin/$b; done` (expect 3 and 2; anything else means the graveyard moved)
- AGP version of record: `grep '^agp' gradle/libs.versions.toml`
- Open AGP opt-outs: `grep -E "builtInKotlin|newDsl" gradle.properties`
- Failing-test baseline: `./gradlew :admin:presentation:testClientDebugUnitTest 2>&1 | tail -20`
- Saga keyword search: `git log --all --oneline --grep="409\|500\|SumUp\|3DS" -i`
