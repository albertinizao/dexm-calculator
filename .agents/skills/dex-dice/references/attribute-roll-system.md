# Attribute Roll System

## Domain rule

An attribute roll starts from the attribute's displayed score and bonuses. Generate one D10 and `2 + plusD6` D6. The final value is:

`selectedD10 + selectedD6A + selectedD6B + plusOne`

The score and `plusD6` are displayed as context in the modal; the score itself is not added to the dice result. `plusD6` controls the number of available D6. Exactly two D6 are selected for the result regardless of how many are available.

## State and generation

`AttributeRollState` contains `key`, `name`, `score`, `plusOne`, `plusD6`, and `dice`. Each die contains `id`, `type` (`d10` or `d6`), `value`, and `selected`.

- `openAttributeRoll(key, fallback, major)` resolves the attribute detail and opens the modal.
- `attributeRollDetails` uses custom minor-attribute bonuses when present; otherwise it uses `oneBonus`/`d6Bonus` for major attributes and the corresponding minor bonus helpers.
- `randomDieValue(sides)` returns an integer from 1 through `sides`.
- `createAttributeRollDice(plusD6)` creates one selected D10 and `2 + max(0, floor(plusD6))` D6, then selects the two highest D6. Ties are deterministic by die id.
- `rerollAttribute` regenerates all dice and reapplies the defaults.
- `toggleAttributeRollDie` toggles only the clicked die. The D10 is normally one die, but selection logic must still allow zero/one selected D10 states so the incomplete message works.

## Derived selection behavior

- Selected D10: selected D10 value or `null`.
- Selected D6: values of selected D6.
- Valid result: selected D10 exists and selected D6 count is exactly 2.
- Missing text: add `1 D10` when none is selected; add the number of missing D6; if more than two D6 are selected, request deselection of the excess.
- Invalid result: render no numeric total; keep the result box in place and show `Falta seleccionar ...`.
- Critical: valid selection, D10 equals 10, and every selected D6 equals 6.

## Screen contract

Each major and minor attribute has an image-only D10 button immediately below its name/value row. The button keeps an accessible Spanish `aria-label` and opens the roll modal; no visible label or button frame is allowed.

The modal is a dialog with the attribute name and context (`score · +plusOne · +plusD6D6`), close control, and a single horizontal layout: D10 15%, D6 50%, result 35%. D6 dice wrap when needed. D10 starts at the same top position as the first D6 row. Dice use full transparent PNGs with the number overlaid; selected/unselected appearance comes from the corresponding image assets. The result box is always present. Actions are `Volver a tirar` and `Cerrar`.

## Source of truth

Implementation: `frontend/src/CharacterSheet.vue` and `frontend/src/styles.css`. Assets: `frontend/public/diceD10.png`, `diceD6.png`, `diceD10Blanco.png`, and `diceD6Blanco.png`.
