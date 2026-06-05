# Regions (`rpg-regions`)

> **Status:** Working (v0.6.0+) — Cube-shaped regions placed via `/region define <id> <radius>` or wand selection. Persistence via DataStore. `/region delete`, `/region list`, `/region info`, `/region flag <id> <flag> <value>`. Enter/exit messages, `no-mob-spawn`, `no-damage`, `fly`, `no-item-drop`, `keep-inventory` all enforced. `RegionEnterEvent` and `RegionLeaveEvent` fire from a periodic polling task (default every 5 ticks). Polygonal regions and a bounds GUI editor come later — for v1, cube-around-player is the only definition mode.

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

### Block / interaction
| Flag | Default | Effect |
|---|---|---|
| `no-break` | false | Cancel all block breaks (including custom blocks) |
| `no-break-vanilla` | false | Cancel vanilla block breaks; custom block breaks still allowed; creative players bypass |
| `no-place` | false | Cancel all block placements |

### Combat / damage
| Flag | Default | Effect |
|---|---|---|
| `pvp` | true | PvP allowed. Set `false` to prevent player-on-player damage |
| `no-damage` | false | Players inside take **no damage** from any source (safe zone) |
| `damage-multiplier` | 1.0 | Multiplies all damage dealt inside the region |

### Spawning
| Flag | Default | Effect |
|---|---|---|
| `no-mob-spawn` | false | Prevents natural spawning and spawner-based spawning. Admin `/summon` and plugin spawns bypass this |

### Player state
| Flag | Default | Effect |
|---|---|---|
| `fly` | false | Grants flight to players while inside; revoked on exit (unless they have flight from permission) |
| `no-item-drop` | false | Items the player tries to drop are returned to their inventory immediately |
| `keep-inventory` | false | Players who die inside keep their items and XP |

### Messages
| Flag | Type | Effect |
|---|---|---|
| `enter-message` | String | Shown as a title when a player enters the region. Supports `{player}` and `{region}` placeholders and `&` colour codes. Prefix with `[actionbar]` to show in the action bar instead. Use `\n` to split title and subtitle: `Welcome to {region}\nWatch your step.` |
| `leave-message` | String | Same, shown on exit |

**Setting a message flag:**
```
/region flag spawn enter-message &6Welcome to Spawn, {player}!
/region flag spawn enter-message [actionbar]&aYou are now in the &eSpawn&a zone.
/region flag spawn leave-message &7Leaving Spawn...
```

### Misc
| Flag | Default | Effect |
|---|---|---|
| `no-ability-use` | false | Cancels all custom abilities cast inside |
| `health-regen-multiplier` | 1.0 | Multiplies out-of-combat regen rate |
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
