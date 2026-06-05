![banner-cooking](../assets/banners/banner-cooking.png)

# Cooking (`rpg-cooking`)

> **Status:** Working — Station right-click dispatch, timed crafting with progress bar + DataStore persistence, recipe matching, and XP all implemented.

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
4. If the player closes the GUI mid-craft, progress is saved to the DataStore. Reopening any cooking station resumes from where it left off.
5. On completion a chime plays and the output is delivered to the player's inventory (overflow drops at their feet).

`CookTicks: 0` (or omitting the field) means instant — output delivered immediately on click, the same as before.

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
