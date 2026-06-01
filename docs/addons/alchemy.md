![banner-alchemy](../assets/banners/banner-alchemy.png)

# Alchemy (`rpg-alchemy`)

> **Status:** Working — Station right-click dispatch active via `StationType: brewing` on the block definition. Recipe matching (ingredient check + consume + output potion) fully implemented. XP on brewing-station recipe completion, scaled by `ALCHEMY_WISDOM`. All vanilla brewing-stand mechanics are cancelled.

Custom potion-style brewing at a brewing station block. Vanilla brewing stand recipes are fully replaced; admins define every recipe.

## Config

`plugins/rpg-alchemy/config.yml`:

```yaml
features:
  brewing: true       # brewing station block + recipe-driven brewing
  drinking: true      # right-click custom potions to consume

# Default brew time if a recipe doesn't specify its own
default-brew-ticks: 200

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

## Output items

Outputs should be `CONSUMABLE`-type items that apply status effects. The `INTELLIGENCE` stat can scale effect duration in the effect definition. See [Items (CONSUMABLE)](../content/items.md#consumable).

```yaml
strength_potion_t1:
  MinecraftItem: potion
  Type: CONSUMABLE
  DisplayName: "&cStrength Potion I"
  Rarity: "&7&lCOMMON"
  OnConsume:
    Effects:
    - { effect: strength_boost, level: 1, duration: 1200 }
```

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
