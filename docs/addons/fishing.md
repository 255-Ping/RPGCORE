# Fishing (`rpg-fishing`)

> **Status:** In Progress — Fishing XP awarded on every successful `PlayerFishEvent` catch (state `CAUGHT_FISH`). Configurable `xp-per-catch`, scaled by `FISHING_WISDOM`. Custom fish loot table + sea-creature spawning + rod stat scaling are planned for a content slice.

Replaces vanilla fishing loot with a configurable catch system. Fish types, rarities, and biome/time restrictions are admin-defined in YAML.

## Config

`plugins/rpg-fishing/config.yml`:

```yaml
xp-per-catch: 10                 # base XP per successful catch
# FISHING_WISDOM stat scales: finalXp = xp-per-catch * (1 + wisdom / 100)
```

> **Planned:** `catch-table` and `biome-overrides` keys for custom fish loot will be added in a future content slice.

## Content

Fish definitions in `plugins/rpg-fishing/fish/<file>.yml` (planned):

```yaml
bass:
  display: "&fBass"
  weight: 60                     # relative spawn weight
  min-size: 0.3                  # purely cosmetic display size in lbs (randomised between min/max)
  max-size: 2.5
  loot-table: fish_drop_common   # what drops when this fish is caught
  biomes: [river, swamp, ocean]  # empty list = any biome
  time: [any]                    # day | night | any

treasure_chest:
  display: "&6Treasure Chest"
  weight: 2
  loot-table: treasure_chest_drops
  biomes: []
  time: [any]
```

## Stats

| Stat | Effect |
|---|---|
| `fishing_speed` | Multiplier on time-to-bite. 100 = 0% reduction; 200 = 50% reduction (configurable formula) |
| `fishing_fortune` | Extra roll multiplier on catch quantity. Formula: floor(fortune/100) guaranteed + fractional probabilistic extra |
| `sea_creature_chance` | % chance per cast that the catch becomes a mob from the mob registry instead of a fish item |

## Rod items

```yaml
enchanted_fishing_rod:
  MinecraftItem: fishing_rod
  Type: MATERIAL
  DisplayName: "&bEnchanted Rod"
  Stats:
    fishing_speed: 50
    fishing_fortune: 100
    sea_creature_chance: 5
```

## Sea creatures

When `SEA_CREATURE_CHANCE` rolls successfully, a mob from the mob registry spawns at the float location in place of a fish item. Which mob spawns is configurable per biome and time-of-day (planned).

```yaml
# In rpg-fishing/config.yml (planned)
sea-creatures:
  default:
    - { mob: sea_guardian, weight: 80 }
    - { mob: kraken_spawn, weight: 20 }
  ocean-biomes:
    - { mob: megalodon, weight: 5 }
```

## Commands

| Command | Permission |
|---|---|
| `/fishing reload` | `rpg.fishing.admin.reload` |

## Related

- [Stats reference](../stats.md)
- [Skills framework](../core/skills.md)
- [Loot tables](../content/loot-tables.md)
- [Mobs](../content/mobs.md) — sea-creature mob definitions
- [Vanilla suppression](../core/vanilla-suppression.md)
