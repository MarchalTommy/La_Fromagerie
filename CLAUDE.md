# La Fromagerie — start here

Production Android app (Kotlin/Compose, multi-module Clean Architecture) for a real
French cheese shop. Two flavors from one codebase: `client` (customers order cheese,
pay via Google Pay → SumUp) and `admin` (the shop owner prepares orders and runs
delivery days). Real customers, real money, production Firestore — treat every action
accordingly.

## Docs of record: the skill library

This file is a ROUTER, not the documentation. The documentation lives in
`.claude/skills/fromagerie-*/SKILL.md` — read the frontmatter `description` of each to
know when to load it. Do not duplicate skill content here; update the skills instead
(see `fromagerie-docs-and-writing` for the maintenance protocol).

| Need | Skill |
|---|---|
| Anything is broken / crashes / "bug reproduces" — load FIRST | `fromagerie-debugging-playbook` |
| Build, env setup, worktrees, Gradle/AGP problems | `fromagerie-build-and-env` |
| Module layout, flavors, DI, invariants, weak points | `fromagerie-architecture-contract` |
| Gates, non-negotiables, what needs Tommy's sign-off | `fromagerie-change-control` |
| Test commands, evidence bar, known-failing baseline, test conventions | `fromagerie-validation-and-qa` |
| Payments (before touching ANY code under `checkout/`) | `fromagerie-payments-reference`, then `fromagerie-payment-reliability-campaign` |
| Firestore collections, order lifecycle, schema rules | `fromagerie-firestore-data-model` |
| Delivery zones/dates/routing, admin delivery day | `fromagerie-delivery-logistics-reference` |
| Secrets, BuildConfig keys, gradle flags | `fromagerie-config-and-secrets` |
| Install/run/device testing, releases, backend deploy | `fromagerie-run-and-operate` |
| Measuring device/runtime state, logs, scripts | `fromagerie-diagnostics-and-tooling` |
| History: settled battles, reverts, dead branches | `fromagerie-failure-archaeology` |
| KDoc/commit/PR style, maintaining the skills | `fromagerie-docs-and-writing` |
| What to improve next (risk-ranked roadmap) | `fromagerie-operations-hardening-frontier` |

## The three hard rules (full rationale in `fromagerie-change-control`)

1. **Production Firestore is sacred** — never write/delete from a dev session; the shop
   runs on it. Reading via the Firebase console is fine.
2. **Never complete a real payment** — Google Pay is `ENVIRONMENT_PRODUCTION` with a
   real merchant; on-device testing stops BEFORE the pay button. There is no sandbox.
3. **Both flavors must always compile**, and changes land via branch → PR to `main`
   (no in-repo CI: the merge gate is the local checklist in `fromagerie-change-control`).

## Quick facts agents ask about

- Default branch: `main`. Test baseline: 2 documented known-failing tests at HEAD
  (`AdminViewModelTest` — see `fromagerie-validation-and-qa`; don't count them against
  your change, don't fix them blind).
- Unit tests: `./gradlew testClientDebugUnitTest --continue` (flavored modules) —
  unflavored modules (`*/domain`, `auth`) use `testDebugUnitTest`.
- DI is Koin — a missing definition crashes at runtime, not compile time.
- Money is cent-`Long` end-to-end; dates are `dd/MM/yyyy` with `Locale.ROOT`
  (`core/domain/.../LogicUtils.kt` — reuse, never rewrite).
