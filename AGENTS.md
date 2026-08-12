# Agent Instructions

## Project
- Static GitHub Pages calculator for the Spanish *Deus ex Machina* RPG; `index.html` is the current entry point.
- No package manager, build script, linter, formatter, test suite, or CI workflow is present. Do not invent package commands.
- Verify changes manually in a browser, including calculator totals, ability listings, and `habilidad.html` detail pop-ups when relevant.

## Layout
- `index.html` — current calculator UI, styles, inline browser-global calculation/persistence logic.
- `calc2.js` — ability eligibility and listing; `calc3.js`/`calc4.js` load newly available/current abilities.
- `habilidad.html` and `hashCode.js` — ability-detail popup and stable name hash.
- `CompendioHabilidadesExport.json` — runtime ability catalog; `docs/CompendioHabilidades.xlsx` is its spreadsheet source.
- `calc.html`, `fichaPersonaje_OLD.html`, and `hoja.html` are older/alternative pages. Do not change them unless the task explicitly targets them.
- `getEBCN.js` and `getElementsByClassName-1.0.1.js` are identical legacy compatibility helpers; leave them intact unless explicitly requested.

## Immutable Files
- `calc.html`, `calc2.js`, `calc3.js`, `calc4.js`, `CompendioHabilidadesExport.json`, `hashCode.js`, `hoja.html`, `index.html`, and `tiradasHabilidades.js` are **totally immutable**. It is **strictly forbidden** to modify them.
- `habildiad.html` was requested with that spelling, but the existing file is `habilidad.html`; both names are considered protected, and `habilidad.html` must not be modified.

## Implementation Conventions
- Keep this dependency order in `index.html`: `calc2.js`, `calc3.js`, `calc4.js`, `hashCode.js`, compatibility helper.
- Existing code is plain browser-global JavaScript with inline HTML handlers and Spanish identifiers; preserve this integration style in touched legacy code.
- Calculation code derives related element IDs by suffix (`Actual`, `New`, `Extra`, `Total`, `Max`). Add or rename a field only with its complete ID family and the required CSS classes/handlers.
- Ability checks consume JSON property names exactly (for example `Enganno`, `EvolutivoCurva`, `SentirYggdrasil`) while DOM IDs use lowercase Spanish names. Keep both mappings aligned.
- Ability data is fetched from the published GitHub Pages JSON URL; update the root JSON whenever the spreadsheet catalog is intentionally changed.
- Preserve the current file encoding: `index.html`, `habilidad.html`, and JSON are UTF-8; older `calc.html`, `hoja.html`, and `fichaPersonaje_OLD.html` declare ISO-8859-1.

## Commits
- Use conventional commit messages only.
- Never add `Co-Authored-By` trailers or AI attribution.

## Execution Workflow
- Use subagents for non-trivial multi-file work when they are available and materially improve execution.
- Under a direct user order, work directly in the repository without subagents, even when delegation would otherwise be the preferred option.
- If delegation fails, do not block the requested work solely because a subagent was unavailable; continue directly while preserving the verification requirements above.

## Project Skills
- `dex-dice` — reusable contract for attribute-roll rules, dice selection, critical results, assets, and modal layout. Read `.agents/skills/dex-dice/SKILL.md` before changing this feature.
