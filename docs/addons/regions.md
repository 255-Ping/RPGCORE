# Regions (`rpg-regions`)

> **Status:** In Progress — Cube-shaped regions placed via `/region define <id> <radius>`. Persistence via DataStore. `/region delete`, `/region list`, `/region info` (at your location), `/region flag <id> <flag> <value>` (boolean / int / string). Flag enforcement v1: `pvp` (cancels player-vs-player damage when false), `no-break`, `no-place`. `RegionEnterEvent` and `RegionLeaveEvent` fire from a periodic polling task (default every 5 ticks). Polygonal regions, region-bounds GUI editor, and the wand-based two-point definition come later — for v1, cube-around-player is the only definition mode.

Admin-defined 3D box regions with flags. Built ourselves rather than depending on WorldGuard.

## Region definition

Stored as YAML under `plugins/rpg-regions/regions/<id>.yml`:

```yaml
id: spawn
world: world
min: { x: -100, y: 0, z: -100 }
max: { x: 100, y: 256, z: 100 }
priority: 10                     # higher wins on overlap
flags:
  pvp: false
  no-natural-spawning: true
  no-ability-use: false
  no-break: true
  no-place: true
  no-explosion-damage: true
  block-explosion-damage: false  # vanilla flag, doesn't apply to entities
  damage-multiplier: 1.0
  health-regen-multiplier: 1.0
  no-mob-spawning: false
  no-dungeon-entry: false
  apply-status: []               # list of status effects auto-applied while inside
```

## Flags

Built-in flags (extensible by addons):

| Flag | Default | Effect |
|---|---|---|
| `pvp` | true | PvP allowed |
| `no-natural-spawning` | false | Disables natural-spawning rules in this region |
| `no-ability-use` | false | Cancels all custom abilities cast inside |
| `no-break` | false | Cancels all block breaks (including custom) |
| `no-place` | false | Cancels all block placements |
| `no-explosion-damage` | false | Cancels explosion damage to entities |
| `block-explosion-damage` | true | Whether explosions break blocks (cancelled by default per vanilla suppression) |
| `damage-multiplier` | 1.0 | Multiplies dealt damage |
| `health-regen-multiplier` | 1.0 | Multiplies out-of-combat regen |
| `no-mob-spawning` | false | Disables all mob spawning (admin spawners + natural) in this region |
| `no-dungeon-entry` | false | Disables `/dungeon join` from inside |
| `apply-status` | `[]` | Auto-applies these status effects while a player is inside the region |

### `apply-status` example

```yaml
flags:
  # Players inside this region permanently have the "haste_zone" buff applied.
  # Buff is removed when they leave. List any number of effect IDs.
  apply-status:
    - { id: strength_boost, level: 1, duration: 100 }  # refreshed every 5 ticks by the polling task
    - { id: regen,          level: 2, duration: 100 }
```

Duration should be slightly longer than the poll interval (default 5 ticks) so effects don't flicker.

## Priority

When regions overlap, the highest-priority region's flag wins per-flag. Ties resolve by alphabetical region ID.

## Region transition events

Fired when a player enters or leaves a region:

- `RegionEnterEvent(player, region)`
- `RegionLeaveEvent(player, region)`

Other addons listen to update HUDs, apply effects, etc.

## Commands

| Command | Permission |
|---|---|
| `/region create <id>` (from selection wand) | `rpg.regions.admin.create` |
| `/region edit <id>` | `rpg.regions.admin.edit` |
| `/region delete <id>` | `rpg.regions.admin.delete` |
| `/region list` | `rpg.regions.admin.list` |
| `/region flag <id> <flag> <value>` | `rpg.regions.admin.flag` |
| `/region info [id]` | `rpg.regions.admin.info` |

`/region info` with no arg shows the region(s) at the admin's current location.

## Selection wand integration

Use `/rpg wand region` to enter region-selection mode. Left-click corner 1, right-click corner 2, then `/region create <id>`.

## API

```java
RegionService svc = RpgServices.regions();
Optional<Region> r = svc.regionAt(location);
boolean pvp = svc.flag(location, "pvp", true);    // default if no region overrides
```

## Related

- [Selection wand](../core/selection-wand.md)
- [Vanilla suppression](../core/vanilla-suppression.md)
- [Dungeons](dungeons.md)
- [Spawning](../content/spawning.md)
