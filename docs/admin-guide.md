# Admin guide

Practical workflows for server admins. Assumes the suite is installed — see [installation](installation.md) first.

---

## First-time setup checklist

1. **Drop jars** — `rpg-core-*.jar` first, then whichever addon jars you want.
2. **Start the server** — default configs and `example.yml` files generate automatically.
3. **Stop the server** — edit configs before players join.
4. **Choose a backend** — [YAML is fine for small servers](core/persistence.md). For anything bigger, set `persistence.backend: mysql` in `plugins/rpg-core/config.yml` and fill in credentials. First start with MySQL creates the schema automatically. See [Persistence](core/persistence.md).
5. **Set vanilla suppression** — defaults cancel everything. If you want any vanilla mechanic back, set its flag to `false` in `plugins/rpg-core/config.yml` under `vanilla-suppression`. See [Vanilla suppression](core/vanilla-suppression.md).
6. **Start the server** — run `/rpg version` to confirm all modules loaded.

---

## Authoring your first custom ore

1. **Define the block** in `plugins/rpg-core/blocks/my_ores.yml` — see [Blocks](content/blocks.md) for the full schema:

```yaml
copper_ore:
  MinecraftBlock: orange_terracotta
  Toughness: 500
  RequiredPower: 0
  RequiredToolType: pickaxe
  RespawnTicks: 1200
  RespawnPlaceholder: stone
  Drops:
  - vanilla:copper_ingot 1-3
```

2. **Define the item** (if you want a custom pickaxe that can mine it) in `plugins/rpg-core/items/tools.yml` — see [Items](content/items.md):

```yaml
iron_pickaxe:
  MinecraftItem: iron_pickaxe
  Type: SWORD
  DisplayName: "&7Iron Pickaxe"
  Stats:
    mining_speed: 20
    breaking_power: 1
```

`mining_speed` and `breaking_power` are mining stats — see the [stat reference](stats.md) for the full list.

3. **Place the ore in the world**:
   - `/rpg block give copper_ore` — places one in your hand
   - Manually place it, OR
   - `/rpg block convert <radius> orange_terracotta copper_ore` — bulk-converts existing blocks

4. **Reload**: `/rpg reloadall`

---

## Authoring your first custom mob

1. Define in `plugins/rpg-core/mobs/forest_mobs.yml` — see [Mobs](content/mobs.md) for the full schema including AI profiles:

```yaml
forest_goblin:
  MinecraftMob: zombie
  DisplayName: "&2Forest Goblin"
  Health: 60
  Damage: 8
  Armor: 2
  AI:
    profile: aggressive
    aggression-range: 12
    attack-range: 2
    leash-range: 24
  LootTable:
    attribution: last-hit
    roll-mode: per-player
    coin-drop: { min: 2, max: 8 }
    rolls:
    - { item: goblin_hide, chance: 60.0, min: 1, max: 2 }
```

The inline `LootTable` block uses the same schema as standalone loot table files — see [Loot tables](content/loot-tables.md).

2. Spawn a test one: `/rpg mob spawn forest_goblin`

3. **Place a spawner**: get your wand (`/rpg wand`), stand at the spawn point, run `/spawner create forest_goblin`. See [Spawning](content/spawning.md) for spawner config options and natural-spawn rules.

---

## Authoring a custom ability

1. Define in `plugins/rpg-core/abilities/spells.yml` — see [Abilities](content/abilities.md) for the full effect library and DSL reference:

```yaml
frost_nova:
  Name: "Frost Nova"
  Description:
    - "&bBlasts ice in all directions."
  ManaCost: 40
  Cooldown: 100
  AbilitySequence:
  - aoe{radius=4.0, damage_multiplier=1.5}
  - apply_status{id=slow, level=2, duration=100, target=target}
  - particles{type=SNOWBALL, count=30, spread=2.0}
  - sound{key=block.glass.break, volume=1.0, pitch=0.8}
```

`apply_status` references a status effect by ID — see [Status effects](core/status-effects.md) for the built-in catalog and custom effect authoring.

2. Attach to a wand item in `plugins/rpg-core/items/wands.yml` — see [Items](content/items.md) for item types and stat fields:

```yaml
frost_wand:
  MinecraftItem: stick
  Type: WAND
  DisplayName: "&bFrost Wand"
  Abilities:
  - frost_nova
  Stats:
    max_mana: 50
```

3. Give it: `/rpg item give frost_wand`

---

## Live-reloading content

You never need a restart for content changes. After editing any YAML:

| Scope | Command |
|---|---|
| Single module | `/<module> reload` (e.g., `/foraging reload`) |
| Everything | `/rpg reloadall` |

Malformed YAML entries log a warning and are skipped — the plugin stays up. Fix the error and reload again.

See [Commands](commands.md) for the full command reference across all modules.

---

## Selection wand

One wand, multiple modes. Get it: `/rpg wand`. Full details at [Selection wand](core/selection-wand.md).

| Mode | How to switch | Use |
|---|---|---|
| `region` | `/rpg wand region` | L-click corner 1, R-click corner 2 → `/region create <id>`. See [Regions](addons/regions.md). |
| `dungeon` | `/rpg wand dungeon` | Same as region but for dungeon volumes. See [Dungeons](addons/dungeons.md). |
| `spawner` | `/rpg wand spawner` | R-click a location → places a spawner. See [Spawning](content/spawning.md). |
| `hologram` | `/rpg wand hologram` | R-click a location → anchor for `/hologram create <id>`. See [Holograms](addons/holograms.md). |

---

## Common admin commands cheat sheet

Quick reference — see [Commands](commands.md) for the full list with permissions.

```
/rpg version                         — list all loaded modules + versions
/rpg reloadall                       — hot-reload everything
/rpg item give <id> [player] [amt]   — spawn a custom item
/rpg mob spawn <id> [count]          — spawn a custom mob at your location
/rpg ability cast <id>               — debug-cast an ability
/rpg block give <id>                 — get a placeable custom block
/rpg block convert <r> <from> <to>   — bulk-convert blocks in radius
/rpg status apply <id> [player]      — apply a status effect
/rpg skill set <skill> level <n> [player]
/spawner create <mobId>              — place a spawner here
/spawner show                        — toggle particle markers on nearby spawners
/region create <id>                  — create region from current wand selection
/region flag <id> pvp false          — set a flag
```

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Custom block breaks instantly | Block isn't PDC-tagged at that location | Use `/rpg block give <id>` to place it, or `/rpg block convert`. See [Blocks](content/blocks.md). |
| Nametag not showing | `rpg-hud` not loaded, or player joined before hud enabled | Ensure rpg-hud jar is in plugins; ask player to relog. See [HUD](addons/hud.md). |
| Abilities not firing on right-click | Item has `Type: MATERIAL` instead of `SWORD`/`WAND` | Fix `Type:` in item YAML, reload. See [Items](content/items.md). |
| MySQL connection fails on startup | Wrong credentials, DB doesn't exist, or firewall | Check `config.yml` credentials; falls back to YAML with a warning. See [Persistence](core/persistence.md). |
| Stat shows 0 in `/stats` | Addon that sources the stat (e.g., rpg-combat for `strength`) not loaded | Install the relevant addon jar. See [Stat reference](stats.md). |
| Quest not progressing | `auto-complete: false` or wrong objective `Target` | Check target matches exact mob/block/item id; check `progress-action-bar: true` for feedback. See [Quests](addons/quests.md). |

---

## Related

- [Installation](installation.md)
- [Configuration overview](configuration.md)
- [Commands](commands.md)
- [Permissions](permissions.md)
- [Content authoring](content/README.md)
- [Stats reference](stats.md)
- [Vanilla suppression](core/vanilla-suppression.md)
- [Persistence](core/persistence.md)
