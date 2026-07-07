---
name: fromagerie-docs-and-writing
description: LaFromagerie's docs-of-record system — a root CLAUDE.md exists but is a ROUTER only; real documentation lives in KDoc (with the project's actual house style), this skill library, commit messages, and PR descriptions. Load before writing KDoc, before drafting a commit message or PR description, or when deciding whether/how something should be documented and where — including the protocol for keeping this skill library itself current.
---

# LaFromagerie Docs and Writing

This repo has **no `README.md`**; a root **`CLAUDE.md` exists (added 2026-07-07) but is a ROUTER only** — a skill-selection table plus the three hard rules, deliberately holding no documentation of its own. **Never document facts in CLAUDE.md**: one home per fact, and the homes are the four below. If CLAUDE.md and a skill ever disagree, the skill wins and CLAUDE.md gets trimmed. Documentation-of-record lives in exactly four places. This skill covers all four, plus the one meta-rule that keeps the skill library itself from rotting.

## The four docs-of-record

1. **KDoc in code** — the primary technical reference. See house style below.
2. **This skill library** (`.claude/skills/`) — architecture, domain knowledge, runbooks; what you're reading right now is part of it.
3. **Commit messages** — the change-level narrative; see conventions below.
4. **PR descriptions** — the review-level narrative; see conventions below.

There is no fifth place. If you're tempted to write a new top-level `.md` file "for reference," it either belongs in a skill (if it's durable knowledge for future sessions) or in a commit/PR body (if it's about one specific change) — not as a loose file nobody will find.

## KDoc house style

Established by four mass-documentation commits: `07dba1d` ("Added A LOT of documentation to the home, detail, and core module"), `1733286` ("Added A LOT of documentation to the checkout and delivery modules"), `ff1dc36`, `548ea05`. Sample well-documented files: `core/domain/src/main/java/com/mtdevelopment/core/domain/LogicUtils.kt`, `checkout/data/src/main/java/com/mtdevelopment/checkout/data/remote/source/SumUpDataSource.kt`.

### Language

**English**, even though UI strings, some code comments, and occasional commit-body phrasing are French (the shop and its customers are French; the code and its docs are written in English by the solo dev). Do not "translate" KDoc into French, and do not leave newly-written KDoc in French even if you were just reading French UI copy.

### Section banners

Long classes with distinct logical regions (a ViewModel handling both admin and client concerns, a repository with several unrelated method groups) use a full-width `///` banner comment to divide sections, indented to match the surrounding code:

```kotlin
    ///////////////////////////////////////////////////////////////////////////
    // ADMIN MODE LOGIC
    ///////////////////////////////////////////////////////////////////////////
```

Confirmed in use in `DeliveryViewModel.kt` (ADMIN MODE LOGIC / CLIENT MODE LOGIC / UI STATE UPDATERS / PERMISSION & PROXIMITY STATE), `AdminViewModel.kt`, `PaymentRepositoryImpl.kt`, `CheckoutViewModel.kt`, `FirebaseAdminRepositoryImpl.kt`, `FirestoreAdminDatasource.kt`, and the `Constants.kt` files in `core/data` and `delivery/data`. Use this when a file has genuinely distinct regions worth a reader jumping between — not on every file, and not to break up a short class.

### `@param` / `@return` usage

Used selectively — on public API surfaces where the parameter's *purpose* isn't obvious from its name and type alone, not mechanically on every function. Example from `SumUpDataSource.kt`:

```kotlin
/**
 * ...
 * @param requestBody Contains the checkout ID and Google Pay tokenized data.
 * @param on3DSecureRequired Callback triggered if the transaction requires 3DS verification.
 */
```

Class- and property-level KDoc is more common than exhaustive `@param`/`@return` — see `Order.kt`'s class doc (`core/domain/.../model/Order.kt`), which documents every property with `@property` instead of relying on the data class fields to speak for themselves:

```kotlin
/**
 * Core domain model representing a customer order.
 *
 * @property id Unique identifier (generated during checkout).
 * @property customerName Full name of the buyer.
 * ...
 * @property isManuallyAdded True if the order was created by an admin (e.g., phone order) rather than through the regular checkout.
 */
```

### The "comment what code can't show" rule

The most valuable KDoc in this codebase explains **why**, not what — a rule worth explicitly naming because it's easy to drift into restating the signature in prose. Real examples that earn their keep:

```kotlin
/**
 * Formatter for dates in "dd/MM/yyyy" format using the system timezone.
 * Pinned to [Locale.ROOT]: stored dates use ASCII digits, and a formatter built on the
 * device default locale fails to parse them on locales with non-Latin digits, silently
 * turning every timestamp-based sort into a no-op.
 */
```

```kotlin
/**
 * Parses a currency string into its cent-based [Long] representation.
 * Accepts the output of [Long.toStringPrice] (which uses non-breaking spaces) and rounds to the
 * nearest cent instead of truncating.
 */
```

(Both from `core/domain/.../LogicUtils.kt`.) Neither of these restates "parses a string into a long" — they explain the trap the function exists to avoid. **When you write KDoc, ask "would a reader who already knows Kotlin/Android learn something from this, or am I just re-narrating the type signature?"** If it's the latter, either omit the comment or find the non-obvious constraint/history/gotcha that actually justifies one.

### When KDoc is required vs. optional

Required (as demonstrated by the mass-documentation commits' actual coverage):

- Any class/function whose behavior depends on a non-obvious external contract (a Firestore field-name convention, an API quirk, a locale/formatting trap, an ordering invariant between two collaborating classes — e.g. `DetermineNextDeliveryStopUseCase`'s KDoc explaining the waypoint-to-order index mapping invariant established by `GoogleRouteRepositoryImpl`).
- Public use-case `operator fun invoke` entry points in `domain` modules — these are the module's real API surface.
- Anything with a documented history of confusion or a past bug (see `fromagerie-failure-archaeology` for what qualifies).

Optional/skip:
- Simple data classes with self-explanatory field names.
- Compose UI composables that are pure layout with no business logic (though a one-line "Dumb Composable (View)" vs. "Smart Composable (Controller)" distinction, as seen in `DeliveryHelperScreen.kt`, is a cheap and useful convention worth keeping).

## Commit message house style

Imperative-ish subject line, sometimes with an inline `->` continuation, occasionally with a `* bullet` body for multi-part changes. **Correction to a common assumption:** despite French appearing throughout the codebase (UI strings, some code comments), **commit message subjects and bodies in this repo are written in English**, sometimes in an informal/diary-like register — not literal French prose. Two real examples:

```
Fixed a whole lot of things in admin mode:
* Fixed a bug where the admin could not see the card to add a delivery path if no path existed.
* Fixed a bug where Admin could not manually add a delivery point. Admin can now manually add a delivery address before starting the delivery for the day.
* Fixed a bug where Admin could not mark orders "ready" on the order preparation screen.
* Fixed order date order in order preparation screen, to have the closest date on top (easier to prepare and to see next delivery).
```

```
Update foojay-resolver-convention and add UI components to ProductItem

- Bump `org.gradle.toolchains.foojay-resolver-convention` from 0.8.0 to 1.0.0 in `settings.gradle.kts`.
- Update imports in `ProductItem.kt` to include `Button`, `BorderStroke`, and additional layout/material3 components.
```

Some commits are candidly informal about difficulty or AI assistance — worth preserving that register rather than sanitizing it into corporate-speak, since it's genuinely useful signal for a future reader (e.g. `83b4ff2`: "I used AI here to help me with the service as I'm not used to do it, but as always, I spend more time debugging it than using it... It wrote too verbose code, and I'll need to clean once it all work."). French *does* appear inside code — comments like `// Vérifier la version d'Android (Oreo et plus)` and `// Le service redémarrera s'il est tué par le système` (`DeliveryTrackingService.kt`) — quote such comments as-is when referencing them in docs; don't translate them, and don't write new ones in French yourself (KDoc and skills are English; only pre-existing French comments/strings are left alone).

## PR description house style

Real merged PR structure (from `gh pr list --state merged`, PR #41 "Improve client UX: cart quantity stepper, conditional back arrow, subtler add-to-cart buttons"):

```markdown
## What changed

**1. <Feature/fix name>** (`<File.kt>`)
<Explanation of the change, referencing the specific classes/callbacks touched, e.g.
"the buttons emit the existing `onRemoveOne`/`onAddMore` callbacks that `CartView` routes to
`CartViewModel.removeCartObject()`/`addCartObject()`">

**2. <Next change>** (`<File.kt>`)
...

## Why

<User feedback or the underlying problem, in a sentence or two.>

## Verification

Verified on <emulator/device, flavor>:
- <Specific, falsifiable observation, e.g. "Cart stepper went 2 → 3 → 1 with the total updating
  instantly (7,40 € → 11,10 € → 3,70 €) and no layout shift.">
- <Build/test status, e.g. ":home:presentation and :app compile clean; existing unit tests pass.">
```

Notable conventions: numbered sub-sections under "What changed" when multiple independent changes are bundled; each references concrete file/class/function names rather than vague descriptions; "Verification" states *specific numbers observed* (real currency amounts, real state transitions) rather than "tested and works." This is the PR-level equivalent of the evidence ladder in `fromagerie-validation-and-qa` — apply the same "specific and falsifiable, not just reassuring" standard to what you write in a PR body as to what you'd accept as proof a fix works.

## Skill-library maintenance protocol

The skill library is itself a doc-of-record and needs the same discipline as code:

1. **When a session finds a skill file stale** (a "Provenance and maintenance" re-verification command returns something different than the skill claims), it should **fix the skill file in the same PR** as whatever work surfaced the drift — not leave a note for later, and not silently work around the stale fact without correcting the record.
2. **When a session fights a new multi-hour battle** (the kind of thing `fromagerie-failure-archaeology` already catalogs — a confusing bug, a rejected approach, a revert), it should **add an entry to `fromagerie-failure-archaeology`** describing what was tried, what didn't work, and why, so a future session doesn't repeat the investigation from zero. A battle that isn't recorded didn't help anyone but the session that fought it.
3. **Skills change via the same PR flow as code** — no direct-to-`main` edits, no bypassing review, because a wrong skill file actively misleads future sessions (the discovery brief's "wrong runbooks are worse than none" principle applies exactly as much after initial authoring as it did during it).
4. **One home per fact.** If you notice the same fact duplicated across two skills, that's drift waiting to happen — consolidate into the skill that owns the fact and have the other cross-reference it by name, don't maintain two copies.
5. Every skill's own "Provenance and maintenance" section is the mechanism that makes rule 1 checkable — if you're editing a skill and it doesn't have one, that's itself a defect to fix.

## When NOT to use this skill

- Deciding what counts as sufficient testing/verification evidence (as opposed to how to *write about* it in a PR) → `fromagerie-validation-and-qa`
- The actual PR/branch/review workflow mechanics (not just description style) → `fromagerie-change-control`
- Recording a newly-discovered historical battle → do it directly in `fromagerie-failure-archaeology`, this skill just tells you that you should
- Any specific domain fact (payments, delivery, config) → the domain skill that owns it; this skill is about *how* to document, not *what* the facts are

## Provenance and maintenance

Facts verified 2026-07-06 against branch `claude/distracted-chaum-0986e4`. Re-verify drift-prone claims:

- CLAUDE.md still router-sized and README still absent: `wc -l CLAUDE.md; find . -maxdepth 1 -iname "README*"` (expect CLAUDE.md well under ~80 lines and no README; if CLAUDE.md has grown, someone is documenting in the wrong home — move content into the owning skill)
- Section-banner convention still in use: `grep -rl "///////////////////////////////////////////////////////////////////////////" --include="*.kt" . | grep -v build`
- KDoc examples still present as quoted: `grep -n "Pinned to \[Locale.ROOT\]" core/domain/src/main/java/com/mtdevelopment/core/domain/LogicUtils.kt`
- Mass-doc commits still identifiable: `git log --oneline 07dba1d 1733286 ff1dc36 548ea05 -1` for each (four separate one-line lookups, since these are historical commits not necessarily on one line of ancestry)
- PR body structure still holds for recent PRs: `gh pr list --state merged --limit 5 --json number,title,body` and inspect the `## What changed` / `## Why` / `## Verification` sections
- French code-comment examples still present: `grep -n "Vérifier la version\|redémarrera" app/src/admin/java/com/mtdevelopment/lafromagerie/DeliveryTrackingService.kt`
