---
name: fromagerie-change-control
description: >
  Load before merging, releasing, or classifying ANY change to LaFromagerie: the real
  git flow (feature / claude worktree branches -> PR to main on GitHub MarchalTommy),
  the fact that there is NO in-repo CI so the merge gate is a LOCAL checklist, the
  per-change-class gates (UI/copy vs business logic vs payment chain vs dependency/AGP
  bumps vs Firestore schema), the non-negotiables (prod Firestore is sacred, never
  complete a real payment, never run bump*Version casually, "+ button" UX sign-off,
  releases via fastlane/App Distribution), the 2 known-failing AdminViewModelTest tests
  at HEAD, and what an AI session may do autonomously vs must surface to Tommy first.
---

# LaFromagerie — Change Control

How changes get classified, gated, reviewed, and merged in this REAL production app
(production Firestore, real SumUp money, one solo developer, AI-assisted). Verified against
the repo as of **2026-07-06** (HEAD `b97eb83`). Wrong process advice here can lose real
money or corrupt live customer data — treat the non-negotiables as hard rules.

## When NOT to use this skill

- HOW to build / assemble / fix Gradle → **fromagerie-build-and-env**.
- HOW to install / version-bump mechanics / release steps / adb → **fromagerie-run-and-operate**.
- Module/layer/DI/flavor design rules → **fromagerie-architecture-contract**.
- Payment-chain internals & safety rails → **fromagerie-payments-reference** and the
  **fromagerie-payment-reliability-campaign**.
- Firestore field contract & schema-evolution rules → **fromagerie-firestore-data-model**.
- Why a past change was reverted / settled → **fromagerie-failure-archaeology**.
- Secrets/config wiring → **fromagerie-config-and-secrets**.

---

## 1. The actual flow

```
feature branch  ─┐
claude/* worktree branch ─┼──►  PR on GitHub (MarchalTommy/LaFromagerie)  ──►  merge to main
                 ─┘             (self-reviewed by Tommy)
```

- **`claude/*` branches** are AI-session worktree branches (created under
  `.claude/worktrees/`). They are the normal way an AI session works. Reversible.
- Merges happen via **GitHub Pull Requests** to `main` (e.g. `ecb1e84 Merge pull request
  #41 from MarchalTommy/claude/festive-goldstine-a81d21`). The reviewer is Tommy (solo dev).
- Verify the pattern: `git log --oneline --merges -10`.

### 1.1 There is NO CI in this repo — the gate is LOCAL

```bash
ls .github/workflows 2>/dev/null   # nothing as of 2026-07-06
find . -maxdepth 2 -iname '*.yml' -o -iname '*.yaml' | grep -v /build/ | grep -vi google-services
# no CI descriptors
```
`fastlane/Fastfile` exists but its `distribute` lane expects a CI-agent credentials path
(`/opt/buildagent/external/firebase-key.json`) — a dormant TeamCity-style pipeline, **not
wired to anything running today**. **Do not assume a bot will catch a broken build, a
failing test, or a broken flavor.** You are the CI.

### 1.2 Mandatory pre-merge checklist (run locally, every PR)

```bash
# 1. BOTH flavors must assemble (both-flavors-compile invariant).
./gradlew :app:assembleClientDebug :app:assembleAdminDebug

# 2. Unit tests for the client variant.
./gradlew testClientDebugUnitTest --continue
#    Admin-variant tests when admin code changed:
./gradlew testAdminDebugUnitTest --continue
```
- If either flavor fails to assemble, the PR is **not** mergeable — a green client build can
  hide an admin compile break (admin code compiles into client too; see
  **fromagerie-architecture-contract §2.3**).
- **Known-failing baseline (as of 2026-07-06, HEAD `b97eb83`):** `AdminViewModelTest` in
  `admin/presentation` has **2 pre-existing failures** around the "abort when local image
  upload fails" behaviour (MockK verification failures). The candidate methods are
  `addNewProduct aborts when local image upload fails` and `updateProduct aborts when local
  image upload fails` (exact failing pair: re-verify by running the suite).
  - **Do NOT block your PR on these** — they are baseline, not something you broke.
  - **Do NOT silently "fix" them** — triage first (code wrong vs test wrong; see
    **fromagerie-operations-hardening-frontier**). If your change legitimately alters that
    behaviour, say so in the PR.
  - Sanity that you only inherited them, not caused new ones:
    `./gradlew testClientDebugUnitTest testAdminDebugUnitTest --continue` and diff the
    failure set against these two.
- **On-device sanity for UI changes** (client debug is debuggable; drive via adb). Full
  device workflow, DataStore inspection, and the stale-APK trap are in
  **fromagerie-run-and-operate**. ⚠️ Stop before the Google Pay button — see §3.

---

## 2. Change classes and their gates

Different kinds of change carry different risk and get different gates.

| Class | Examples | Gate (in addition to §1.2) |
|---|---|---|
| **(a) UI / copy tweak** | text, spacing, colors, a button style | Assemble both flavors; on-device visual check. No new tests required. **But user-facing UX removals/additions to product cards need owner sign-off — see §3 and the "+ button" incident.** |
| **(b) Business logic** (money / dates / orders) | anything touching `LogicUtils`, cart totals, order construction, delivery-date math | **Unit tests required.** Money stays cent-`Long`; dates stay `dd/MM/yyyy` `Locale.ROOT` (invariants I1/I2 in architecture-contract). Add/extend tests in the module's `src/test`. |
| **(c) Payment-chain change** | anything under `checkout/`, SumUp, Google Pay, 3DS, `FinalizePaymentWorker`, order-write-on-payment | **Highest bar.** Read **fromagerie-payments-reference** first; coordinate with the **fromagerie-payment-reliability-campaign**. Never complete a real payment to test (§3). Prefer changes that fail safe (money not lost, order reconcilable). Surface to Tommy before merge. |
| **(d) Dependency / AGP bump** | AGP, Kotlin, Compose BOM, Koin BOM, any `libs.versions.toml` change | **Its own branch, never mixed with feature work.** This repo has scar tissue: `agp = "9.0.1"` was settled only after a painful AGP 9 migration (`9d8d53e "Finished fixing AGP 9 for now... Temporary shit"`, `7343ca4`/`62181e5`/`90cfdec`, `78df833` tried 9.1.0 then reverted). The `gradle.properties` flags `android.builtInKotlin=false` and `android.newDsl=false` are deliberate AGP-9 opt-outs that MUST be migrated before AGP 10 — treat that migration as a dedicated project (see **fromagerie-operations-hardening-frontier**), not a drive-by bump. |
| **(e) Firestore schema change** | new/renamed/removed field or collection in `products`, `orders`, `delivery_paths`, `preparation_status`, `database_update` | **Must stay backward-compatible with APKs already in the field.** Additive-only: add fields, don't rename/remove/repurpose. Old client and admin APKs are still reading/writing prod. Full rules in **fromagerie-firestore-data-model**. Surface to Tommy before merge. |

Whenever a change spans classes, apply the **strictest** applicable gate.

---

## 3. Non-negotiables (with rationale and incident)

1. **Production Firestore is sacred.** There is no staging. Never write to or delete from
   prod collections from a dev/AI session. Inspect read-only (Firebase console). A stray
   write corrupts a real shop's real orders. (Enforced convention; see
   **fromagerie-firestore-data-model** for the read-only inspection method.)

2. **Never complete a real payment.** The client app uses a **production** SumUp merchant
   and Google Pay `ENVIRONMENT_PRODUCTION` (the SumUp test merchant is not Google-Pay
   compatible — that is *why* production creds are used even in dev; see failure-archaeology
   §1). When driving the UI by hand or via `adb shell input tap`, **stop BEFORE the Google
   Pay / pay button, always.** Tapping it charges real money.

3. **Never run `bump*Version` tasks casually.** `./gradlew bumpMajorVersion|bumpMinorVersion|
   bumpPatchVersion` **rewrites `app/build.gradle.kts` in place** — the task rule does
   `buildFile.writeText(...)`, editing `versionName`/`versionCode` on disk (see
   `app/build.gradle.kts`, the `addRule("Pattern: bump<TYPE>Version")` block). Only bump as
   a deliberate release step (mechanics: **fromagerie-run-and-operate**), never as a
   side-effect of exploring gradle tasks.

4. **The "+ button" rollback is doctrine.** Commit `0049162 "Rollbacked the + button on the
   main product card"` reverted an add-to-cart "+" button on product cards — a UX decision
   the owner made. Product-card UX (what buttons appear, add-to-cart affordances) needs
   **owner sign-off**; do not reintroduce removed user-facing affordances on your own. See
   **fromagerie-failure-archaeology** for the full story.

5. **Releases go through fastlane / Firebase App Distribution**, not ad-hoc APK sharing.
   `fastlane/Fastfile` lanes: `test` (`gradle test`), `build` (`gradle clean
   assembleRelease`), `version`, `distribute` (`firebase_app_distribution`). Release signing
   needs `KEYSTORE_*` env vars. Do not hand-build and sideload a "release" outside this path.
   Mechanics: **fromagerie-run-and-operate**.

---

## 4. Autonomy boundary — what an AI session may do vs must surface first

| An AI session MAY do autonomously | An AI session MUST surface to Tommy first |
|---|---|
| Create/edit/delete `claude/*` worktree branches (reversible). | Anything touching **production Firestore data** (writes/deletes/migrations). |
| Read code, run builds, run unit tests, inspect prod **read-only**. | Any change to **payment-live behaviour** (checkout/, SumUp, Google Pay, finalization). |
| Write UI/copy/logic changes on a branch, open a PR. | **Releasing** or **version bumping** (fastlane distribute, `bump*Version`). |
| Add/extend tests. | **Removing or altering user-facing features/affordances** (e.g. product-card buttons — the "+ button" rule). |
| Local, reversible experiments in worktrees/build dirs. | **Firestore schema changes** (even additive — confirm field names/compat). |
| Triage the known-failing tests (diagnose, propose). | Completing a real payment on a device (never — do NOT even ask to). |

Default: if an action is reversible and stays on a branch, proceed and report. If it can
touch real money, real customer data, or a shipped release, **stop and surface** with a
clear description and your recommended action.

---

## Provenance and maintenance

| Claim | Re-verification command |
|---|---|
| No in-repo CI | `ls .github/workflows 2>/dev/null; find . -maxdepth 2 -iname '*.yml' -o -iname '*.yaml' \| grep -v /build/ \| grep -vi google-services` |
| PR-to-main flow, MarchalTommy | `git log --oneline --merges -10` |
| fastlane lanes | `grep -nE 'lane :\|gradle\(\|firebase' fastlane/Fastfile` |
| `bump*Version` rewrites the build file | `grep -n 'buildFile.writeText\|addRule' app/build.gradle.kts` |
| current versionName/Code | `grep -nE 'versionCode =\|versionName =' app/build.gradle.kts` |
| AGP pinned + opt-out flags | `grep -n '^agp' gradle/libs.versions.toml; grep -nE 'builtInKotlin\|newDsl' gradle.properties` |
| "+ button" rollback exists | `git log --oneline --all \| grep -i 'button'` |
| 2 known-failing tests | `./gradlew testAdminDebugUnitTest --continue` then read the `AdminViewModelTest` results (expect 2 "aborts when local image upload fails" failures as of 2026-07-06) |

If any output diverges from this skill, update and date-stamp it.
