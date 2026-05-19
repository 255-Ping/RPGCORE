# Spawning

> **Status:** Planned

Mob spawning is fully custom. Two independent mechanisms:

1. **Admin spawners** — invisible markers placed by an admin at specific locations.
2. **Natural spawning rules** — admin-defined replacement for vanilla biome-based spawning.

Vanilla natural spawning is cancelled when `vanilla-suppression.mob-spawning: true`.

---

## Admin spawners

Stored persistently via `DataStore`. Files live conceptually under `plugins/rpg-core/spawners/` (storage format depends on the backend).

### Schema (per spawner)

```yaml
id: spider_cave_01
location: world,123,40,-89
mob: cave_spider                # references mob registry
mode: continuous                # continuous | one-shot | bounded:<count>
max-alive: 5
spawn-radius: 6
cooldown-ticks: 100
batch-size: 1
on-mob-death: respawn-after-cooldown
persist-across-restart: false
conditions:                     # see "Spawn conditions" below
  time-of-day: [night]
  light-level: { min: 0, max: 7 }
  weather: [any]
  biomes: [any]
  region: any
  forbid-region: spawn
  y-range: { min: -30, max: 60 }
  moon-phase: [any]
  players-within-blocks: { min: 0, max: 32 }
  min-distance-from-players: 4
```

### Commands

| Command | Permission |
|---|---|
| `/spawner create <mobId>` | `rpg.spawners.admin.create` |
| `/spawner edit <id>` | `rpg.spawners.admin.edit` |
| `/spawner delete <id>` | `rpg.spawners.admin.delete` |
| `/spawner list [near\|world]` | `rpg.spawners.admin.list` |
| `/spawner tp <id>` | `rpg.spawners.admin.tp` |
| `/spawner show` | `rpg.spawners.admin.show` |

`/spawner show` toggles a debug particle column at every nearby spawner — handy for finding them again.

### Visual

Spawners themselves are **invisible markers**, not placed blocks. Admins place a decoration block via the regular block system if they want a visible cog/skull.

---

## Natural spawning rules

Toggleable globally. When on, replaces vanilla mob spawning entirely. Files live under `plugins/rpg-core/natural-spawning/`.

### Schema

```yaml
cave_zombies:
  enabled: true
  mobs:
  - { mob: cave_zombie, weight: 70 }
  - { mob: zombie_brute, weight: 30 }
  conditions:                   # same shape as admin spawners
    time-of-day: [night]
    light-level: { min: 0, max: 7 }
    weather: [any]
    biomes: [plains, forest, dark_forest]
    region: any
    forbid-region: spawn
    y-range: { min: -30, max: 60 }
    moon-phase: [any]
  rate:
    per-chunk-per-tick: 0.0005
    max-per-chunk: 4
    min-distance-from-player: 24
    max-distance-from-player: 80
```

### Core config knob

`plugins/rpg-core/config.yml`:

```yaml
natural-spawning:
  enabled: true                 # master toggle
```

### Per-region override

A `rpg-regions` flag `no-natural-spawning: true` disables natural spawning in that region.

---

## Spawn conditions (shared schema)

Used by both admin spawners and natural spawning rules.

| Field | Values | Meaning |
|---|---|---|
| `time-of-day` | `[day]`, `[night]`, `[dusk]`, `[dawn]`, or `"13000-23000"` numeric range | Tick window |
| `light-level` | `{min, max}` | Block light level at spawn location |
| `weather` | `clear`, `rain`, `storm`, `any` | World weather |
| `biomes` | list of biome names, or `[any]` | Biome restriction |
| `region` | region ID, or `any` | Must be inside this region |
| `forbid-region` | region ID, or `none` | Must NOT be inside this region |
| `y-range` | `{min, max}` | Y-coordinate range |
| `moon-phase` | list of 0–7, or `[any]` | Lunar phase (vanilla `World#getMoonPhase`) |
| `players-within-blocks` | `{min, max}` | Spawner inactive if no players in range (max), refuses if players too close (min) |
| `min-distance-from-players` | int | Don't spawn within this distance of any player |

All conditions are AND-combined. A spawn proceeds only if every condition passes.

## Related

- [Mobs](mobs.md)
- [Vanilla suppression](../core/vanilla-suppression.md)
- [Selection wand](../core/selection-wand.md)
- [Regions addon](../addons/regions.md)
