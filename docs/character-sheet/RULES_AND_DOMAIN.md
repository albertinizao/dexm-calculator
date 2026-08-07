# Character Sheet Rules and Domain Model

## Source notation

`Manual §` cites `docs/Manual.docx`; page numbers are the manual's printed/table-of-contents locations where available. The document is a DOCX, so visual pagination must be rechecked if an exact page citation is needed in a user-facing product.

## Domain vocabulary

| Spanish term | Helpful English equivalent | Meaning / source |
|---|---|---|
| `Einherjer` | player character archetype | The manual describes player characters as Einherjer. Manual § “El papel del jugador”. |
| Atributo Mayor | major attribute | `Físico`, `Agilidad`, `Percepción`, `Mente`, `Estudio`, `Carisma`. Manual ch. 1, p. 14. |
| Atributo Menor | minor attribute | Era-dependent specialised attribute. The current calculator implements the modern/futuristic subset below. Manual ch. 1, pp. 14–18. |
| Genética | genetic lineage score | Six lineages grouped under Æsir, Vanir and Jotun. Manual ch. 1, p. 23. |
| Base | purchased/base value | Value paid with evolution points; the manual explicitly limits this value for minor attributes. Manual § “Gasto de Puntos de Evolución”, p. 28. |
| Extra | modifier | Racial, profession, study or other adjustment recorded separately from base. Manual chs. 2–3, pp. 30–32. |
| Bifrost | Bifrost resource | Spendable resource for abilities; it recovers on rest. Manual §§ “Atributos Calculados”, “Gasto de Bifrost”. |

## Character-sheet aggregate

A complete character sheet needs at least:

- Identity and description: name, player, concept, age, sex/appearance/personality/background; the manual directs the player to set these during creation (Manual “Resumen de la creación de personaje”, p. 11; ch. 5, pp. 69–77).
- Campaign context: period/setting and GM-approved house rules. The period changes which minor attributes are relevant; race/profession/studies availability is GM-controlled.
- Origins: race/ethnicity, profession(s), study/ies and their adjustments to attributes, genetics, abilities and equipment.
- Advancement state: level, current experience, unspent evolution points and the per-level genetic allocation.
- Scores: all major/minor attributes, genetics, base and extra components, totals, caps and derived roll bonuses.
- Derived resources/statistics: maximum/current Bifrost, maximum/current Vida, melee/ranged defence.
- Abilities: obtained abilities, unique-ability campaign ownership, and operational state needed during play (Bifrost expenditure, cooldowns and maintained costs when those capabilities are in scope).
- Equipment and money: creation explicitly includes GM-assigned equipment/money, though the current calculator does not model it.

## Verified creation rules

Manual § “Resumen de la creación de personaje” (TOC p. 11):

1. Consult the GM/campaign constraints, define a character concept, choose race/ethnicity, profession and/or studies.
2. Apply racial/professional/study adjustments to attributes, genetics, abilities and equipment.
3. A new character starts with **175 evolution points** and **3 genetic points**.
4. Check obtained abilities, then calculate combat statistics and record description/equipment.
5. Humans of Midgard are the normal player option; additional races are GM-approved. A character may have several professions/studies only if permitted and appropriate to age.

## Scores and formulas

### Major attributes

`Fis`, `Agi`, `Pcn`, `Mnt`, `Est`, `Car` are the six major attributes (Manual ch. 1, pp. 14–18). They constrain minor-attribute base caps and contribute to derived statistics.

### Minor attributes implemented by the current page

The manual's era table includes additional historical attributes such as `Forja` and `Montar`; the active page implements these 24:

| Minor attribute | Verified maximum base formula | Manual location |
|---|---:|---|
| Astronavegar | `(Agi + Pcn) / 2` | ch. 1, p. 15 |
| Atractivo | `Car × 2` | ch. 1, p. 15 |
| Buscar | `Pcn` | ch. 1, p. 15 |
| Conducción | `(Agi + Pcn) / 2` | ch. 1, p. 15 |
| Cruzar Bifrost | `(Mnt + Est) / 2` | ch. 1, p. 15 |
| Deporte | `Fis` | ch. 1, p. 15 |
| Destreza | `(Fis + Agi) / 2` | ch. 1, p. 15 |
| Diplomacia | `Car` | ch. 1, p. 15 |
| Einherjer | `(Mnt + Fis) / 2` | ch. 1, p. 15 |
| Engaño | `(Pcn + Mnt) / 2` | ch. 1, p. 15 |
| Esconderse | `(Agi + Mnt) / 2` | ch. 1, p. 15 |
| Evolutivo Curva | `(highest major + lowest major) / 2` | ch. 1, pp. 15–16 |
| Esquiva | `(Fis + Agi) / 2` | ch. 1, p. 15 |
| Física/Química | `Est` | ch. 1, p. 15 |
| Fuerza | `Fis` | ch. 1, p. 15 |
| Informática | `Est` | ch. 1, p. 15 |
| Intimidar | `(Fis + Car) / 2` | ch. 1, p. 15 |
| Labia | `Car` | ch. 1, p. 15 |
| Liderazgo | `Car` | ch. 1, p. 15 |
| Medicina | `Est` | ch. 1, p. 15 |
| Provocar | `max((Mnt + Fis) / 2, (Mnt + Car) / 2)` | ch. 1, p. 15 |
| Puntería | `Pcn` | ch. 1, p. 15 |
| Resistencia | `Fis` | ch. 1, p. 15 |
| Sentir Yggdrasil | `(Mnt + Pcn) / 2` | ch. 1, p. 15 |

**Verified constraint:** spending evolution points may not increase a **minor attribute's base** above its maximum. Extra modifiers may take a minor attribute's total beyond that cap; those extras still matter to major-attribute deformity checks. Manual chs. 1–3, pp. 15, 28, 30–32.

**Open question:** the manual states `/ 2` but does not state how fractional results round. The current UI truncates `.5` textual results. This must become an explicit rules decision.

### Total and derived values

- Current score total in the page is `base + extra`; prospective total is `current total + prospective base + prospective extra`. **Current behaviour**, `index.html` `calcularTotalAtributo`.
- `maximum Bifrost = Mnt × 10`. **Verified rule**, Manual ch. 1 p. 24.
- `maximum Vida = 70 + (Fis × 5)`. **Verified rule**, Manual ch. 1 p. 24.
- `melee defence = 10 + Esquiva +1 bonuses + Destreza +1 bonuses`. **Verified rule**, Manual ch. 1 p. 24.
- `ranged defence = 15 + Esquiva +1 bonuses`. **Verified rule**, Manual ch. 1 p. 24. The manual sentence labels this “defensa cuerpo a cuerpo” a second time, but the heading identifies it as ranged defence.
- An attribute test rolls `2 + number of +D6 bonuses` d6 and adds at most the number of `+1` bonuses. **Verified rule**, Manual § “Pruebas de atributos mayores o menores”, p. 9.

### Bonus thresholds

The manual contains a per-attribute +1/+D6 threshold table (ch. 1, pp. 22–23). The active implementation encodes these arrays in `index.html#getArrayBonosAtributosMenores()` and counts every threshold at or below the total. The major-attribute arrays are +1 at `5,11,17,…,71` and +D6 at `2,8,14,…,68`; all minor arrays are individually encoded there.

Do not treat the current arrays as automatically authoritative: the documented discrepancies in [Gaps](GAPS_RISKS_DECISIONS.md) include threshold differences and a likely `Destreza` typo.

## Progression rules

| Rule | Verified manual source |
|---|---|
| Each 100 XP increases level and consumes 100 XP. | § “Experiencia” / “Nivel”, p. 25 |
| Per level, evolution points are `35 + Evolutivo Curva`. | § “Puntos de evolución”, p. 25 |
| One major point costs 10 evolution points; one minor point costs 5. | § “Gasto de Puntos de Evolución”, p. 28 |
| Unspent evolution points carry forward. | p. 28 |
| Per level, genetic points are 3 unless an ability changes this. | § “Puntos de genética”, p. 25 |
| At most two of those three points may go to one genetic score. | § “Gasto de Puntos de Genética”, p. 28 |
| Genetic points do **not** carry forward. | p. 28 |
| After levelling, re-check abilities and recalculate affected derived values. | § “Después de subir de nivel”, p. 28 |

The manual offers GM guidance for XP awards and optional individual bonuses (pp. 25–28); this is adjudication support rather than an automatic character-sheet formula.

## Genetics

The six scores are `Héroe`, `Norna` (Æsir); `Álfar`, `Valkiria` (Vanir); and `Risa`, `Dvergr` (Jotun). Manual ch. 1 pp. 23–24. Names appear without accents in DOM/data keys (`Heroe`, `Alfar`).

## Ability rules

- To obtain an ability, all stated requirements normally must be met; requirements are predominantly minimum major/minor attributes and genetics. Manual ch. 4 “Obtención de Habilidades”, p. 34.
- A visual separating line indicates alternative requirement groups: meeting either group is enough. The example is `Cruzar Bifrost = 4` **or** `Risa = 10`. Manual p. 34.
- A unique ability belongs to the first eligible character in a campaign and is GM-private; the GM must review it manually. Manual pp. 34–35.
- Use requires casting time, stated requirements, Bifrost cost and any required ability test. Bifrost is paid before the test; failure still spends it. No sufficient Bifrost means the ability cannot be used. Manual pp. 35–36.
- Casting time can be `Inst`, `Normal`, `Completa`, `Pasiva`, `Mantener`, or an ability-specific duration. The same action/ability cannot be used twice consecutively; cooldowns are complete turns unless stated otherwise. Manual p. 35.

## Rule boundary

Combat, equipment, vehicles and GM adjudication contain further rules, but only their persistent character-sheet consequences above are in scope for this documentation.
