# Deus ex Machina Character Sheet Documentation

## Purpose

This documentation records the verified behaviour of the current static calculator and the **character-sheet rules** relevant to a future, more complete application. It is a product and rules baseline, not an implementation proposal.

## Source hierarchy

1. `docs/Manual.docx` — primary rules source. References use its table-of-contents page number where available; the DOCX has reflowable layout, so section names are the durable locator.
2. `index.html` and its browser-global scripts — source of current application behaviour, including any divergence from the manual.
3. `CompendioHabilidadesExport.json` — runtime skill catalogue and eligibility-data contract.
4. `docs/CompendioHabilidades.xlsx` — editable catalogue source, but its conversion procedure is not automated in this repository.

A statement marked **Verified rule** comes from the manual. A statement marked **Current behaviour** comes from the code. **Open question** means the sources do not settle it or they conflict.

## Documents

- [Rules and domain model](RULES_AND_DOMAIN.md) — entities, verified formulas, progression, and ability rules.
- [Current implementation map](CURRENT_IMPLEMENTATION.md) — pages, scripts, DOM and catalogue contracts.
- [Advanced product requirements](ADVANCED_PRODUCT_REQUIREMENTS.md) — user flows and functional expectations for a complete sheet manager.
- [Gaps, risks, and open decisions](GAPS_RISKS_DECISIONS.md) — prioritised work that must be resolved before rebuild planning.

## Current scope

The active entry point is `index.html`, a static GitHub Pages calculator. It lets a user enter an existing sheet plus a prospective level-up, shows attribute totals/bonuses/caps, lists catalogue abilities, exports a compact backup string, imports that string, and renders a copy-friendly view.

It is **not** a complete character-sheet manager: it has no character identity, race/profession/studies workflow, equipment, calculated combat statistics, ability ownership, session state, or persistent character collection.

## Intended advanced-scope boundary

A more polished successor should preserve rules fidelity and make the full character lifecycle manageable: create a character, apply approved modifiers, progress it transactionally, maintain acquired abilities and resources, and create a shareable/printable sheet. This documentation deliberately does not choose frameworks, storage, data schemas, or architecture.

## Evidence reviewed

All tracked source files were inspected, including the current page, three legacy/alternative pages, all calculator scripts, the JSON catalogue, both legacy compatibility helpers, and static assets. The manual was extracted directly from DOCX XML (6,606 non-empty paragraphs; 147 rendered-page markers) and limited to character-sheet-relevant rules.
