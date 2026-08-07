# Advanced Character Sheet: Functional Requirements

## Product outcome

Provide a polished character-sheet experience that lets a player and GM create, understand, advance and use a *Deus ex Machina* character without losing the manual's rules, GM authority or the current calculator's useful eligibility checking.

## Personas

- **Player:** creates and maintains their character, plans a level-up, consults acquired abilities and prepares a readable sheet.
- **Game Master (GM):** configures campaign constraints, approves exceptions, manages unique abilities and can inspect rule-validity warnings.

## Core flows

### 1. Start or create a character

1. Select or record campaign context: period, GM constraints and house rules.
2. Enter identity/concept/description.
3. Select race/ethnicity, profession(s) and study/ies permitted by the GM.
4. Record each source adjustment distinctly as base or extra, rather than flattening it irreversibly.
5. Allocate the verified initial pools: 175 evolution points and 3 genetic points.
6. Validate minor base caps, genetics allocation and any major-attribute deformity rule once that rule has been sourced.
7. Calculate Bifrost, Vida, defences and roll bonuses.
8. Discover eligible abilities; route unique abilities to GM approval.
9. Save a named character sheet.

### 2. Plan and confirm a level-up

1. Record XP gained and calculate all 100-XP level increments with remainder.
2. Show the evolution-point pool, including carryover, and how each change is priced.
3. Permit allocation to major/minor attributes while immediately explaining base-cap and affordability violations.
4. Allocate exactly the per-level genetic opportunity, enforcing non-carryover and no-more-than-two-per-lineage-per-level rules.
5. Recalculate values that depend on modified scores.
6. Compare eligibility before/after and show newly available abilities separately from already obtained abilities.
7. Let the player submit a proposed advancement and the GM approve or reject exceptions; only an approved plan updates the sheet.

### 3. Use the sheet in play

1. Read a compact character summary with base, extra, total, cap and roll bonuses clearly distinguished.
2. Track current versus maximum Bifrost and Vida; record ability costs before tests and resource recovery on rest.
3. Browse owned abilities by name and read their casting time, cost, test, requirements and effects.
4. Surface cooldown/maintained-ability state only when the table chooses to track it; retain manual/GM authority for adjudication.
5. Export/print/share a stable sheet without requiring a popup or fragile text serialization.

### 4. Maintain the ability catalogue

1. Preserve the editable catalogue source and a controlled published runtime version.
2. Validate required display fields, stable identifiers, duplicate handling, requirement groups and typed costs.
3. Review catalogue changes before they affect character eligibility.
4. Retain provenance/version information so a character can explain which rules/catalogue revision it uses.

## Functional requirements

### Character integrity

- A character must have a stable identity independent of display name.
- Every score change must retain its origin: initial allocation, level-up purchase, racial/professional/study modifier, ability modifier or GM override.
- The user must be able to distinguish authoritative manual rules, GM overrides and informational warnings.
- The app must not silently convert a warning into a valid state; exceptions should be deliberate and traceable.

### Rules transparency

- For every total, cap, bonus and derived statistic, reveal the inputs and formula.
- Enforce only verified rules; label unresolved or campaign-specific rules instead of inventing a constraint.
- Treat rounding, era-specific attributes, deformities and alternative ability requirements as explicit rule cases.
- Preserve the distinction between base cap and total-with-extra, since extras may legally exceed a minor cap.

### Ability management

- Eligibility must support AND within a group and OR across groups, not just flat threshold fields.
- An eligibility result should say which conditions are met/missing.
- “Eligible”, “obtained”, “unique-pending”, “GM-approved”, and “unavailable” must be distinct states.
- The ability detail view must use a stable record identity, not a name hash; duplicate display names are valid catalogue data.

### Persistence and interoperability

- Support durable saved characters, multiple characters and explicit export/import format versioning.
- Offer print-friendly output at least as complete as the current copy view.
- Preserve a documented migration path from existing `name:value&&…` backups where the data permits, with warnings for data that backup never contained.

### Accessibility and usability

- Do not depend on `onblur` calculations or colour alone to expose invalid state.
- Make Spanish game terminology first-class; English equivalents may be supplementary, never replacements for rule names.
- Make form validation, calculated values and ability information usable without popups and on smaller screens.

## Out of scope for this baseline

This document does not decide technical architecture, UI framework, database, authentication, multiplayer synchronisation or a combat virtual tabletop. It also does not turn GM XP-award guidance or adjudication into mandatory automation.
