# Cooking (`rpg-cooking`)

> **Status:** Working — Station right-click dispatch active via `StationType: cooking` on the block definition. Recipe matching (ingredient check + consume + output) fully implemented. XP on cooking-station recipe completion, scaled by `COOKING_WISDOM`.

Custom food / consumable crafting at a cooking station block. All vanilla furnace recipes are cancelled; admins define every recipe.

## Config

`plugins/rpg-cooking/config.yml`:

```yaml
skill:
  id: cooking
  max-level: 50
  curve: "100 * level ^ 1.5"
  milestones:
    10: { message: "&aYour cooking improves!" }
    25: { stats: { cooking_wisdom: 10 } }
  implicit-per-level:
    stats:
      cooking_wisdom: 0.5

# Default cook time if a recipe doesn't specify its own
default-cook-ticks: 200
```

## Recipes

Files under `plugins/rpg-cooking/recipes/<file>.yml`. See also [Recipes (cooking)](../content/recipes.md#cooking-recipes).

```yaml
roasted_rabbit:
  Inputs:
  - { item: raw_rabbit, amount: 1 }
  CookTicks: 160
  Output: { item: roasted_rabbit, amount: 1 }
  Requirements:
    cooking-level: 5

hearty_stew:
  Inputs:
  - { item: raw_meat, amount: 2 }
  - { item: mushroom, amount: 1 }
  - { item: bowl, amount: 1 }
  CookTicks: 300
  Output: { item: hearty_stew, amount: 1 }
  Requirements:
    cooking-level: 20
```

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
  Consumable:
    Heal: 30
    Duration: 0
    Effects:
    - { id: strength_boost, level: 1, duration: 1200 }
    - { id: regen, level: 1, duration: 400 }
    Cooldown: 200
```

## Stats

| Stat | Effect |
|---|---|
| `cooking_wisdom` | % bonus cooking XP per recipe |

## Commands

| Command | Permission |
|---|---|
| `/cooking reload` | `rpg.cooking.admin.reload` |

## Related

- [Recipes](../content/recipes.md)
- [Items (CONSUMABLE)](../content/items.md#consumable)
- [Blocks (StationType)](../content/blocks.md)
- [Status effects](../core/status-effects.md)
- [Skills framework](../core/skills.md)
- [Vanilla suppression](../core/vanilla-suppression.md)
