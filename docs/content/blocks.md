# Custom blocks

> **Status:** In progress — YAML loader, registry, `/rpg block convert`, and BlockBreakHandler all working. v1 is **instant break on click** (hold-to-break with the vanilla 0-9 progress packet arrives in a polish slice — `Toughness` and `MINING_SPEED` ticking are not yet wired). `BREAKING_POWER` and `RequiredToolType` gates are enforced. Drops + respawn timer working. Right-click `Interactable: true` / `StationType` handling not yet implemented. Per-location tagging is in-memory only for v1 (lost on restart) — `DataStore`-backed persistence comes later.

Custom blocks are defined in YAML under `plugins/rpg-core/blocks/`. A custom block is a vanilla block tagged with PDC at its world location, with custom HP, drops, and break behavior.

## Schema

```yaml
red_gem_block:
  MinecraftBlock: redstone_ore   # base block
  Toughness: 1000                # HP — MINING_SPEED HP/sec ticks this down
  RequiredPower: 0               # BREAKING_POWER (player stat) must be >= this to break at all
  RequiredToolType: pickaxe      # pickaxe | axe | shovel | hoe | any | none
  RespawnTicks: 600              # ticks to respawn after broken (0 = never)
  RespawnPlaceholder: bedrock    # what's shown while respawning (any vanilla Material)
  Interactable: false            # if true, right-click runs OnRightClick instead of placing
  OnRightClick: ""               # ability invocation or station type (see below)
  StationType: ""                # crafting | cooking | brewing | enchanting | anvil — opens that GUI on right-click
  Drops:
  - gems red_gem 1-3             # "[itemFile] <itemId> <min>[-<max>]" — itemFile optional, registry lookup is by itemId
  - vanilla:emerald 1            # vanilla materials valid as item IDs
```

## Drop syntax

`[itemFile] <itemId> <min>[-<max>]`

- `itemFile` (optional): human-readable scoping. The runtime lookup is by `itemId` against the global item registry; `itemFile` is for admin organization only and ignored at resolution.
- `itemId`: can be a custom item ID (from `items/`) or a vanilla `Material` name.
- Quantity: a fixed integer (`5`) or a range (`1-3`).
- Fortune (`MINING_FORTUNE`, `FORAGING_FORTUNE`, etc.) rolls extra drops on top of this base — applied per-roll, not as a flat multiplier.

## Break flow

1. Player left-clicks the block. Vanilla break is cancelled.
2. If `RequiredPower > player.BREAKING_POWER`, refuse with a message (configurable text).
3. If `RequiredToolType` doesn't match the held tool, refuse.
4. Block has a per-instance HP counter (initialized to `Toughness`).
5. While the player holds attack, HP decreases by `(skill_speed_stat) / 20` per tick (e.g., `MINING_SPEED` for ore-type blocks).
6. Break progress is rendered to the player via vanilla packet 0–9 stages.
7. Released attack: HP decay starts after 1s (configurable).
8. HP reaches 0: block is broken; drops roll; placeholder spawns; `RespawnTicks` countdown begins.

The "skill speed stat" used depends on the tool type. Defaults: `pickaxe` → `MINING_SPEED`; `axe` → `FORAGING_SPEED`; `hoe` → `FARMING_SPEED` *(no farming-speed stat in v1 — falls back to vanilla swing rate)*; `shovel` → vanilla; `any`/`none` → vanilla swing rate.

## Right-click interactions

Blocks marked `Interactable: true` route right-click through one of:

- `StationType: <type>` — opens the matching GUI (crafting bench, cooking station, brewing stand, enchanting table, anvil).
- `OnRightClick: <ability DSL>` — runs the ability with the player as caster.

Examples:

```yaml
custom_brewing_stand:
  MinecraftBlock: brewing_stand
  Toughness: 100
  RequiredToolType: any
  Interactable: true
  StationType: brewing
  Drops:
  - vanilla:brewing_stand 1
```

```yaml
warpstone:
  MinecraftBlock: end_stone
  Toughness: 200
  Interactable: true
  OnRightClick: teleport{distance=20, mode=eyeline}
  Drops:
  - vanilla:end_stone 1
```

## Placing & converting

- `/rpg block give <id>` — gives the placeable item form. Placing it in the world creates a tagged custom block.
- `/rpg block convert <radius> <fromMaterial> <toBlockId>` — bulk-convert nearby vanilla blocks matching `fromMaterial` into the specified custom block.

## Respawn behavior

When a custom block is broken with `RespawnTicks > 0`:

- The world block is replaced with `RespawnPlaceholder` (default: keep the original `MinecraftBlock`, or admin can pick `bedrock`/`barrier`).
- After `RespawnTicks`, the placeholder is replaced with the custom block again (re-tagged via PDC).
- During respawn, the location remains "claimed" by the block ID — `/rpg block convert` won't double-tag it.

## Related

- [Stats reference](../stats.md) — `MINING_SPEED`, `BREAKING_POWER`, `MINING_FORTUNE`, etc.
- [Items](items.md)
- [Recipes](recipes.md)
- [rpg-mining addon](../addons/skills.md#mining)
