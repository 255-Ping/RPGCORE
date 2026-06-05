# Suite 20 — Closed

← [Back to changelog index](../changelog.md)

> This page is an archived reference. New work is in [Suite 21](suite-21.md).

## Highlights

- `rpg-homes 0.1.0` — player homes + server warps (DataStore + warps.yml, configurable max-homes, tab-complete)
- `rpg-kits 0.1.0` — one-time and cooldown kits, `/kit`, `/givenkit`, `/kitreset`, YAML item lists
- Timed cooking (`rpg-cooking 0.4.0`) — 4-tick task, 9-slot progress bar, DataStore save/restore on close/reopen
- Timed brewing (`rpg-alchemy 0.4.0`) — same model, purple panes, `BREWING_STAND_BREW` completion sound
- Damage indicator polish (`rpg-core 1.5.2`) — sin-arc position + linear scale shrink 1→0 via `setTransformation`
- Resource pack auto-delivery (`rpg-core 1.5.2`) — `resource-pack:` config block, `ResourcePackListener` on join
- Mob death animation (`rpg-core 1.6.0`) — `DeathParticle`/`DeathSound` YAML fields, velocity zeroed at death
- `rpg-crafting 0.1.0` — custom shaped/shapeless crafting recipes extracted from rpg-core
- `rpg-smelting 0.1.0` — timed smelting station, BLAST_FURNACE block, orange progress bar, optional vanilla furnace recipe registration
