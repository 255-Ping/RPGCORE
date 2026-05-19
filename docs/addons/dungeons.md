# Dungeons (`rpg-dungeons`)

> **Status:** Planned

Admin-authored, instanced dungeons. Players (solo or as a party) teleport into a per-run copy of the dungeon volume. Multiple instances of the same dungeon run in parallel.

## Authoring flow

1. **Set up the build area.** Build the dungeon physically in a normal world.
2. **`/dungeon create <id>`** — registers the dungeon ID and begins authoring.
3. **`/rpg wand dungeon`** — switch the wand to dungeon mode. Left-click and right-click to mark the 3D box around the build.
4. **`/dungeon save <id>`** — captures the volume (blocks, tile entities) into a stored template.
5. **`/dungeon edit <id>`** — opens the editor GUI:
   - **Entrance**: where players teleport when entering. Drop a wand-marked location.
   - **Exit**: where players teleport on leave/complete.
   - **Requirements**: items (with `consume-on-entry` flag), skill levels, party-size limits, currency cost.
   - **Spawners**: place mob spawners inside the dungeon volume. Each is the same `Spawner` schema as [admin spawners](../content/spawning.md#admin-spawners), with `mode: continuous | one-shot | bounded:<n>`.
   - **Win condition**: pick one or compose:
     - `kill_mob:<mobId>` — kill a specific mob instance
     - `kill_all_mobs` — defeat every spawned mob
     - `reach_location:<x,y,z,radius>` — touch a point (relative to dungeon origin)
     - `collect_item:<itemId>` — pick up a target item
     - `survive_ticks:<n>` — last N ticks
     - `composite: all | any` of sub-conditions
   - **Time limit** (optional)
   - **Death rules**: solo / party / all-dead handling overrides
   - **Loot pool**: weighted item list (references `items/`), per-player roll on win
6. **`/dungeon save <id>`** again to seal.

## Play flow

1. `/dungeon join <id>` — perm `rpg.dungeons.join`.
2. If in a party, every member must pass entry requirements; otherwise refused.
3. **Instance allocation**: the saved template is pasted into the instance world `rpg_dungeon_instances` (auto-bootstrapped as a void world on first run) at a free, non-overlapping region.
4. Player(s) teleported to the entrance location of that instance.
5. Spawners activate per their config.
6. Win condition monitored.
7. On win: roll loot per player, teleport everyone to the exit, free the instance slot.

## Death handling

Configurable per-dungeon. Defaults:

- **Solo player dies** — teleport out, apply normal death rules (drops per [death tiers](../core/damage.md#death-rules)).
- **Party member dies** — switched into **spectator-leashed** mode: visible to teammates as a ghost, leashed within `spectator-leash-radius` blocks of the dungeon. Cannot rejoin combat.
- **All party members dead** — kick everyone, apply normal death rules.
- **Party wins with one or more members dead** — dead members' deaths **don't count** (no items lost). They're TP'd out with the rest at the exit.

## Instance world

`rpg_dungeon_instances` — a flat void world auto-created on first run. Sized to accommodate many instances laid out on a grid. Players can't enter it directly; `/dungeon join` is the only valid entry.

## Loot pool

```yaml
# part of the dungeon template
loot-pool:
  rolls-per-player: 3
  entries:
  - { item: rare_helmet, chance: 5.0, magic-find-affected: true }
  - { item: dungeon_coins, kind: currency, amount: 1000, chance: 100.0 }
  - { item: epic_artifact, chance: 0.5 }
```

`kind: currency` lets a dungeon mint a custom currency on win (e.g., dungeon-coins via the `Currency` registry).

## Commands

| Command | Permission |
|---|---|
| `/dungeon create <id>` | `rpg.dungeons.admin.create` |
| `/dungeon save <id>` | `rpg.dungeons.admin.save` |
| `/dungeon edit <id>` | `rpg.dungeons.admin.edit` |
| `/dungeon delete <id>` | `rpg.dungeons.admin.delete` |
| `/dungeon list` | `rpg.dungeons.list` |
| `/dungeon join <id>` | `rpg.dungeons.join` |
| `/dungeon leave` | `rpg.dungeons.leave` |
| `/dungeon admin abort <instance>` | `rpg.dungeons.admin.abort` |

## Storage

Dungeon templates stored via `DataStore`. Volume is serialized as a block-state array plus a side-channel for tile entities. Instances are runtime-only.

## Related

- [Selection wand](../core/selection-wand.md)
- [Parties](parties.md)
- [Mobs](../content/mobs.md), [Spawning](../content/spawning.md)
- [Regions](regions.md) — entrance/exit areas can be region-flagged
- [Damage / death rules](../core/damage.md#death-rules)
- [Economy](economy.md) — currency requirements & rewards
