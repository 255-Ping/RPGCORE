![banner-dungeons](../assets/banners/banner-dungeons.png)

# Dungeons (`rpg-dungeons`)

> **Status:** In Progress — Template commands (create, setentrance, setexit, setspawn, list, reload) all working. `/dungeon enter` teleports the player into the instance world but gameplay systems (mob spawning, win conditions, completion rewards, death handling) are not yet wired — entering gives an empty void. Full dungeon gameplay is a major planned flesh-out. See [todo-improvements](../planned/todo-improvements.md).

Admin-authored, instanced dungeons. Players (solo or as a party) teleport into a per-run copy of the dungeon volume. Multiple instances of the same dungeon run in parallel.

## What works now

**Template authoring:**
1. Build the dungeon physically in a normal world.
2. `/dungeon create <id>` — registers the dungeon.
3. `/rpg wand dungeon` — switch the wand to dungeon mode. Left-click + right-click to mark the 3D volume.
4. `/dungeon setentrance <id>` — while standing at the entrance point, sets where players teleport in.
5. `/dungeon setexit <id>` — sets the exit/completion teleport destination.
6. `/dungeon setspawn <id>` — sets the mob spawn anchor (used once mob spawning is wired).
7. `/dungeon enter <id>` — teleports you into the instance world copy. **Currently drops you into a void — no mobs, no win condition, no exit trigger. Use for layout testing only.**

**Instance world:**
`rpg_dungeon_instances` — a flat void world auto-created on first run. Template blocks are pasted there on entry. Players can only enter via `/dungeon enter`.

## What's planned (not yet working)

The following systems are designed but not yet implemented. They will be built as part of the dungeon flesh-out:

- **Mob spawning inside the instance** — spawner definitions in the dungeon YAML, activated on entry
- **Win conditions** — kill all mobs, reach a location, collect an item, survive N ticks, or composite conditions
- **Completion rewards** — per-player loot pool roll on win
- **Death handling** — spectator-leash mode for party members, all-dead wipe condition
- **Entry requirements** — item cost, skill level gates, party size limits, currency cost
- **Loot pool** — weighted item list rolled per player on win

See [todo-improvements](../planned/todo-improvements.md) for the full flesh-out spec.

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
| `/dungeon delete <id>` | `rpg.dungeons.admin.delete` |
| `/dungeon setentrance <id>` | `rpg.dungeons.admin.set` |
| `/dungeon setexit <id>` | `rpg.dungeons.admin.set` |
| `/dungeon setspawn <id>` | `rpg.dungeons.admin.set` |
| `/dungeon list` | `rpg.dungeons.admin.list` |
| `/dungeon reload` | `rpg.dungeons.admin.reload` |
| `/dungeon enter <id>` | `rpg.dungeons.use.enter` |
| `/dungeon leave` | `rpg.dungeons.use.leave` |
| Alias: `/dgn` | — | Short alias for all subcommands |

## Storage

Dungeon templates stored via `DataStore`. Volume is serialized as a block-state array plus a side-channel for tile entities. Instances are runtime-only.

## Related

- [Selection wand](../core/selection-wand.md)
- [Parties](parties.md)
- [Mobs](../content/mobs.md), [Spawning](../content/spawning.md)
- [Regions](regions.md) — entrance/exit areas can be region-flagged
- [Damage / death rules](../core/damage.md#death-rules)
- [Economy](economy.md) — currency requirements & rewards
