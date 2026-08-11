---
name: dex-dice
description: "Trigger: tirada de atributo, dados D10, dados D6, crítico, modal de tiradas. Mantén o implementa la lógica y la pantalla de tiradas de atributos de Deus ex Machina."
license: Apache-2.0
metadata:
  author: dexm-calculator
  version: "1.0"
---

## Activation Contract

Use this skill whenever a task changes, debugs, tests, documents, or reuses the attribute-dice system in the character sheet.

## Hard Rules

- Preserve the rule: `1D10 + 2D6 seleccionados + plusOne`; the attribute's `plusD6` adds extra available D6, not extra selected D6.
- Default-select the only D10 and the two highest D6 results.
- Show the result only with exactly one selected D10 and exactly two selected D6. Otherwise keep the fixed result box visible and show what is missing.
- Critical means selected D10 = 10 and both selected D6 = 6; style the result gold and bold and label it `Crítico`.
- Keep die values over the full die image. Selected assets are red/gold; unselected assets are white/black. Do not recreate their colors with CSS.
- Keep the feature in the Vue/browser-global frontend style and preserve accessible labels and `aria-pressed` state.

## Decision Gates

| Change | Required location |
| --- | --- |
| Roll state, randomization, selection, result, critical | `frontend/src/CharacterSheet.vue` |
| Modal/card layout and responsive sizing | `frontend/src/styles.css` |
| Die images | `frontend/public/diceD10.png`, `diceD6.png`, `diceD10Blanco.png`, `diceD6Blanco.png` |
| Attribute entry point | Image-only D10 button below each major/minor attribute name |

## Execution Steps

1. Read `references/attribute-roll-system.md` before modifying the feature.
2. Keep `AttributeRollState` and `AttributeRollDie` compatible with the documented behavior.
3. Validate selection edge cases, critical styling, full-image rendering, mobile wrapping, and keyboard/screen-reader behavior.
4. Run `npm run typecheck` and `npm run build` from `frontend`.

## Output Contract

Report changed files, rule changes, visual changes, and verification results. Call out any intentional deviation from the reference.

## References

- `references/attribute-roll-system.md` — complete dice rules, state transitions, formulas, assets, and modal layout contract.
