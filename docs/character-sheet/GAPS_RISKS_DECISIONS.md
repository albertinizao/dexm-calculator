# Gaps, Risks, and Open Decisions

## Priority 0 — rules correctness before rebuild

| Issue | Evidence | Required decision / validation |
|---|---|---|
| `Evolutivo Curva` cap differs from the active page | Manual ch. 1 pp. 15–16: `(highest major + lowest major) / 2`; `index.html` declares `evolcurvaMax` as `Est`. | **Resolved:** use `(highest major + lowest major) / 2`; specify rounding before implementation. |
| Ability alternatives are encoded as separate catalogue entries | Manual p. 34 supports OR-separated groups; `calc2.js` applies AND to all populated fields, while the current catalogue models each alternative as a separate ability record. | Preserve that compatibility rule or define a requirement-group model before consolidating duplicate entries. |
| Several manual/UI bonus thresholds diverge | Example: extracted manual table lists `Cruzar Bifrost` +1 at `9,16,23…`; active array begins `6,13,21…`. | Compare visually against the authoritative manual/errata and approve one complete threshold source. |
| `Destreza` +D6 has a suspect threshold | Active array contains `…52,59,56,73,80`, a non-monotonic `56`; manual table must be visually verified. | Correct only after source verification; add rule tests in the future product. |
| Rounding is unspoken | The manual uses many `/2` formulas but does not state rounding; current page strips `.5` from rendered caps. | Record an official floor/ceil/other rule, including negative scores. |
| Major-attribute deformities are referenced but not implemented/documented here | Manual says extras affect them (chs. 1–3); current page does not calculate them. | Extract/approve exact deformity rules before claiming complete creation validation. |

## Priority 1 — data and integrity

| Risk | Evidence | Consequence |
|---|---|---|
| Ability names are not unique | JSON has duplicate names (`Detectar mentira`, `Falsa curación`, `Pequeñas mentirijillas`). | Name-hash detail lookup is ambiguous and a name cannot be a future primary key. |
| Catalogue has weak/variable typing | `Coste` is integer/string/absent; optional fields are omitted; one `Unica` value is absent. | UI validation and rules parsing need normalisation and clear missing-data behaviour. |
| Runtime data may differ from repository data | Browser fetches a fixed GitHub Pages URL rather than local JSON. | A local edit is not proof of deployed behaviour; version/provenance is required. |
| Spreadsheet-to-JSON process is external/manual | README links a third-party web converter; no command or validation is tracked. | Catalogue changes are non-reproducible and can silently break field contracts. |
| Legacy backup loses information | Current delimiter string contains scores/XP/level but no identity, GM context, ability ownership, resource state or structured modifiers. | Migration can be partial only; users need an explicit loss report. |

## Priority 2 — product completeness

1. **No creation workflow:** the active calculator starts from manually entered current values and does not enforce the 175/3 starting pools.
2. **No origin modifiers:** race, profession and studies are manual “Extra” entries without source, stacking or GM approval.
3. **Missing calculated combat data:** Bifrost, Vida and both defences are in the manual but absent from the active sheet.
4. **No owned-ability state:** it displays eligibility, not a maintained acquired-ability list; unique abilities cannot be safely managed.
5. **No resource/session tracking:** Bifrost spending/rest recovery, cooldowns and maintained costs are only prose in ability records.
6. **No durable characters:** reload loses state unless the user manually exports/imports a fragile string.
7. **No era handling:** manual says minor attributes depend on setting; the active page hard-codes one set.
8. **No equipment/money/description:** creation and resulting play sheet are incomplete.

## Priority 3 — current-code constraints to avoid copying

- Calculations are coupled to hundreds of DOM IDs/classes and inline handlers, not explicit character data.
- `eval` evaluates maximum formulas stored in rendered HTML; the formula is presentation text and business logic at once.
- Validation happens mostly on blur and uses red styling (`sobrepasadoMaximo`) rather than a structured validation result.
- The duplicate legacy compatibility helper and legacy pages should be treated as historical reference, not new dependencies.
- `habilidad.html` inserts catalogue strings through `innerHTML`; a future catalogue/editor must define safe rendering rules.

## Decisions to obtain from the GM/product owner

1. Which manual edition and errata are authoritative, especially for bonuses, `Evolutivo Curva`, rounding and deformities?
2. Which campaign eras and optional races are in the first product slice?
3. Are race/profession/study changes selected from curated data, entered manually, or both? Who approves them?
4. Must the first version support a GM role and campaign-scoped unique abilities, or only record a GM approval state?
5. Is combat-time resource/cooldown tracking part of the sheet, or intentionally a table-side responsibility?
6. What compatibility promise is owed to legacy backup strings and existing spreadsheet/JSON data?

## Verified contradictions / uncertainties at a glance

- **Contradiction:** manual `Evolutivo Curva` formula versus current `Est` cap.
- **Contradiction pending visual source check:** selected bonus thresholds in manual extraction versus hard-coded arrays.
- **Representation trade-off:** manual OR requirement groups are represented by separate catalogue records because the current predicate is flat-AND. This preserves eligibility, but duplicate records need stable identifiers if the model is consolidated later.
- **Uncertainty:** `/2` rounding and deformity rules are not sufficiently explicit in the reviewed material for automatic enforcement.
- **Manual editorial typo:** ranged-defence formula is preceded by a repeated “defensa cuerpo a cuerpo” phrase; the section title/formula make the intended statistic clear.
