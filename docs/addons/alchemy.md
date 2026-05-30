# Alchemy (`rpg-alchemy`)

> **Status:** In progress — Plugin module ships. XP on brewing-station recipe completion, scaled by `ALCHEMY_WISDOM`. Custom station block (`StationType: brewing`) defined in `blocks/`. Station right-click interaction and recipe matching are pending the station-dispatch slice. All vanilla brewing-stand mechanics are cancelled.

Custom potion-style brewing at a brewing station block. Vanilla brewing stand recipes are fully replaced; admins define every recipe.

## Config

`plugins/rpg-alchemy/config.yml`:

```yaml
skill:
  id: alchemy
  max-level: 50
  curve: "100 * level ^ 1.5"
  milestones:
    10: { message: "&aYour brewing improves!" }
    25: { stats: { alchemy_wisdom: 10 } }
  implicit-per-level:
    stats:
      alchemy_wisdom: 0.5

# Default brew time if a recipe doesn't specify its own
default-brew-ticks: 600
```

## Recipes

Files under `plugins/rpg-alchemy/recipes/<file>.yml`. See also [Recipes (brewing)](../content/recipes.md#brewing-recipes).

```yaml
strength_potion_t1:
  Ingredients:
  - { item: strength_essence, amount: 1 }
  - { item: water_bottle, amount: 1 }
  BrewTicks: 600
  Output: { item: strength_potion_t1, amount: 1 }
  Requirements:
    alchemy-level: 5

speed_elixir:
  Ingredients:
  - { item: wind_crystal, amount: 1 }
  - { item: silver_dust, amount: 2 }
  - { item: glass_bottle, amount: 1 }
  BrewTicks: 800
  Output: { item: speed_elixir, amount: 1 }
  Requirements:
    alchemy-level: 20
    permission: rpg.alchemy.brew.speed_elixir
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
  Consumable:
    Duration: 0
    Effects:
    - { id: strength_boost, level: 1, duration: 1200 }
    Cooldown: 100
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

## Related

- [Recipes](../content/recipes.md)
- [Items (CONSUMABLE)](../content/items.md#consumable)
- [Status effects](../core/status-effects.md)
- [Blocks (StationType)](../content/blocks.md)
- [Skills framework](../core/skills.md)
- [Vanilla suppression](../core/vanilla-suppression.md)
