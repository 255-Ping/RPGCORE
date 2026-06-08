![banner-cooking](../assets/banners/banner-cooking.png)

# Cooking (`rpg-cooking`)

> **Status:** Working — Station right-click dispatch, timed crafting with progress bar + DataStore persistence, dedicated output slot, offline timer advancement, recipe matching, and XP all implemented.

Custom food / consumable crafting at a cooking station block. All vanilla furnace recipes are cancelled; admins define every recipe.

## Config

`plugins/rpg-cooking/config.yml`:

```yaml
features:
  cooking: true

# Default cook time if a recipe doesn't specify its own
default-cook-ticks: 200

# Skill XP awarded on successful cook
xp:
  per-cook: 20
```

## Recipes

Files under `plugins/rpg-cooking/recipes/<file>.yml`. See also [Recipes (cooking)](../content/recipes.md#cooking-recipes).

```yaml
roasted_rabbit:
  Inputs:
  - { Item: raw_rabbit, Amount: 1 }
  CookTicks: 160
  Output: { Item: roasted_rabbit, Amount: 1 }
  RequiredLevel: 5

hearty_stew:
  Inputs:
  - { Item: raw_meat, Amount: 2 }
  - { Item: mushroom, Amount: 1 }
  - { Item: bowl, Amount: 1 }
  CookTicks: 300
  Output: { Item: hearty_stew, Amount: 1 }
  RequiredLevel: 20
```

## Timed crafting

When `CookTicks` is greater than 0 on a recipe, clicking it starts a **timed craft**:

1. Ingredients are consumed immediately (can't be taken back).
2. A 9-slot progress bar fills row 0 of the GUI — lime/gray glass panes plus a clock item showing the recipe name and seconds remaining.
3. The ingredient slots show locked copies of what was consumed (visual only).
4. If the player closes the GUI mid-craft, progress and a `timestamp_ms` (real epoch milliseconds) are saved to the DataStore. Reopening any cooking station resumes — including **offline advancement**: elapsed time since closing is computed from wall-clock time, so cooking continues while the player is offline.
5. On completion a chime plays and the output is placed in the **output slot** (see layout below). The player must click it to collect. If the player closes the GUI with a finished item in the output slot, it is auto-collected to their inventory first.

`CookTicks: 0` (or omitting the field) means instant — output delivered immediately on click.

## GUI layout

```
Row 0: [progress bar — lime/gray panes + clock item]
Row 1: [bg] [bg] [bg] [ingredient 12] [ingredient 13] [ingredient 14] [→ arrow] [output slot] [bg]
Row 2–4: recipe tiles (27 per page)
Row 5: ← PREV | bg | bg | bg | ✖ CLOSE | bg | bg | bg | NEXT →
```

- **Output slot** (slot 16): holds a gray-dye placeholder until the craft completes. Click to collect. Starting a new recipe is blocked while the output slot contains a finished item — collect it first (`cook.collect-output` message).
- **Arrow** (slot 15): orange dye, decorative indicator between ingredients and output.

## Station block

Define the cooking station as a custom block in `plugins/rpg-core/blocks/`:

```yaml
cooking_station:
  MinecraftBlock: furnace
  Toughness: 500
  RequiredToolType: pickaxe
  Interactable: true
  StationType: cooking
  Drops:
  - vanilla:furnace 1
```

Players right-click the block to open the cooking GUI.

## Output items

Outputs should be `CONSUMABLE`-type items that apply stat buffs or status effects. See [Items (CONSUMABLE)](../content/items.md#consumable).

```yaml
hearty_stew:
  MinecraftItem: mushroom_stew
  Type: CONSUMABLE
  DisplayName: "&6Hearty Stew"
  Rarity: "&a&lUNCOMMON"
  OnConsume:
    Effects:
    - { effect: strength_boost, level: 1, duration: 1200 }
    - { effect: regen, level: 1, duration: 400 }
```

## Stats

| Stat | Effect |
|---|---|
| `cooking_wisdom` | % bonus cooking XP per recipe |

## Commands

| Command | Permission |
|---|---|
| `/cooking reload` | `rpg.cooking.admin.reload` |
| `/cooking list` | `rpg.cooking.admin.list` |

## Related

- [Recipes](../content/recipes.md)
- [Items (CONSUMABLE)](../content/items.md#consumable)
- [Blocks (StationType)](../content/blocks.md)
- [Status effects](../core/status-effects.md)
- [Skills framework](../core/skills.md)
- [Vanilla suppression](../core/vanilla-suppression.md)
