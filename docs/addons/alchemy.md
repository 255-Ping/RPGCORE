![banner-alchemy](../assets/banners/banner-alchemy.png)

# Alchemy (`rpg-alchemy`)

> **Status:** Working — Station right-click dispatch, timed brewing with progress bar + DataStore persistence, recipe matching, and XP all implemented. All vanilla brewing-stand mechanics are cancelled.

Custom potion-style brewing at a brewing station block. Vanilla brewing stand recipes are fully replaced; admins define every recipe.

## Config

`plugins/rpg-alchemy/config.yml`:

```yaml
features:
  brewing: true       # brewing station block + recipe-driven brewing
  drinking: true      # right-click custom potions to consume

# Default brew time if a recipe doesn't specify its own
default-brew-ticks: 200

# Item returned to the player's inventory after drinking a custom potion.
# Individual potions can override this with a per-potion ReturnItem: field.
#
#   none            — give nothing (vanilla glass-bottle return is already suppressed)
#   glass_bottle    — give one glass bottle (mirrors vanilla behaviour)
#   <material>      — any vanilla Material name (e.g. GLASS_BOTTLE, BUCKET)
#   <rpg-item-id>   — a custom item id from rpg-core's items/ folder
drink-return:
  item: none

# Skill XP awards
xp:
  per-brew: 30
  per-drink: 0
```

## Recipes

Files under `plugins/rpg-alchemy/recipes/<file>.yml`. See also [Recipes (brewing)](../content/recipes.md#brewing-recipes).

```yaml
strength_potion_t1:
  Inputs:
  - { Item: strength_essence, Amount: 1 }
  - { Item: glass_bottle, Amount: 1 }
  BrewTicks: 600
  Output: { Item: strength_potion_t1, Amount: 1 }
  RequiredLevel: 5

speed_elixir:
  Inputs:
  - { Item: wind_crystal, Amount: 1 }
  - { Item: silver_dust, Amount: 2 }
  - { Item: glass_bottle, Amount: 1 }
  BrewTicks: 800
  Output: { Item: speed_elixir, Amount: 1 }
  RequiredLevel: 20
```

## Timed brewing

When `BrewTicks` is greater than 0 on a recipe, clicking it starts a **timed brew**:

1. Ingredients are consumed immediately.
2. A 9-slot progress bar fills row 0 — purple/gray glass panes plus a brewing stand icon showing the recipe name and seconds remaining.
3. Ingredient slots show locked display copies (visual only).
4. Closing the GUI mid-brew saves progress to DataStore; reopening any brewing station resumes.
5. On completion the `BLOCK_BREWING_STAND_BREW` sound plays and the output is delivered.

`BrewTicks: 0` (or omitting) = instant brew.

## Station block

Define the brewing station as a custom block in `plugins/rpg-core/blocks/`:

```yaml
brewing_station:
  MinecraftBlock: brewing_stand
  Toughness: 300
  RequiredToolType: any
  Interactable: true
  StationType: brewing
  Drops:
  - vanilla:brewing_stand 1
```

## Potion definitions

Drinkable potions are defined separately from brew recipes in `plugins/rpg-alchemy/potions/<file>.yml`. Each top-level key is a potion id:

```yaml
# potions/example.yml
strength_potion:
  DisplayName: "&6Potion of Strength"
  CustomModelData: 0          # optional — links to a resource-pack model
  ConsumeOnDrink: true        # if false, drinking does nothing (preview-only item)
  ReturnItem: none            # optional — overrides the global drink-return.item for this potion only
                              # "none" = nothing | "glass_bottle" = vanilla bottle | rpg item id | material name
  Effects:
    - { Id: strength_boost, Level: 1, DurationSeconds: 60 }
    - { Id: regen,          Level: 2, DurationSeconds: 30 }

# Potion that gives back a custom empty vial instead of a glass bottle
premium_elixir:
  DisplayName: "&bPremium Elixir"
  ConsumeOnDrink: true
  ReturnItem: empty_vial      # hands the player this rpg item id after drinking
  Effects:
    - { Id: haste, Level: 2, DurationSeconds: 120 }
```

### `ReturnItem` resolution

When a potion is consumed, the return item is resolved in this order:

1. **Per-potion `ReturnItem:`** — if present, used as-is (overrides global config)
2. **Global `drink-return.item`** — the fallback for potions without their own `ReturnItem:`
3. Resolved against the **RPG item registry** first, then as a **vanilla Material name**
4. If neither matches, a warning is logged and nothing is given

Any returned items that don't fit the player's inventory are dropped naturally at their feet.

## Custom status effects

Alchemy ships its own default status effect definitions (samples in `plugins/rpg-core/status-effects/example.yml`). Admins can define more under `plugins/rpg-core/status-effects/`. See [Status effects](../core/status-effects.md).

Common effects shipped with alchemy:
- `strength_boost` — flat `strength` modifier while active
- `regen` — periodic HP heal via tick action
- `slow` — movement speed reduction (percent modifier on `speed`)
- `poison` — periodic true damage via tick action

## Stats

| Stat | Effect |
|---|---|
| `alchemy_wisdom` | % bonus alchemy XP per brew |
| `intelligence` | Scales `ability_damage` and can scale status-effect durations in the formula |

## Commands

| Command | Permission |
|---|---|
| `/alchemy reload` | `rpg.alchemy.admin.reload` |
| `/alchemy list` | `rpg.alchemy.admin.list` |

## Related

- [Recipes](../content/recipes.md)
- [Items (CONSUMABLE)](../content/items.md#consumable)
- [Status effects](../core/status-effects.md)
- [Blocks (StationType)](../content/blocks.md)
- [Skills framework](../core/skills.md)
- [Vanilla suppression](../core/vanilla-suppression.md)
