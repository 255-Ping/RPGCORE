# Addons

> **Status:** Planned (some skeletons exist)

Each addon is its own plugin jar. All addons hard-depend on `rpg-core`. To enable an addon, drop its jar in `plugins/`. To disable, remove it.

## Feature addons

| Module | Status | Page | Brief |
|---|---|---|---|
| `rpg-economy` | Planned | [economy](economy.md) | Currency, balances, `/pay`, `/balance` |
| `rpg-chat` | Planned | [chat](chat.md) | Chat format, channels, moderation, LuckPerms |
| `rpg-hud` | Planned | [hud](hud.md) | Scoreboard, tablist, action bar, nametags |
| `rpg-parties` | Planned | [parties](parties.md) | Session-only player groups |
| `rpg-guilds` | Planned | [guilds](guilds.md) | Persistent guilds with bank, ranks, XP/level |
| `rpg-regions` | Planned | [regions](regions.md) | 3D box regions with flags |
| `rpg-dungeons` | Planned | [dungeons](dungeons.md) | Instanced dungeons authored in-game |
| `rpg-accessories` | Planned | [accessories](accessories.md) | Accessory bag + `ACCESSORY` item type |
| `rpg-enchanting` | Planned | [enchanting](enchanting.md) | Enchanting skill + reforges + upgrades + anvil |
| `rpg-npcs` | Planned | [npcs](npcs.md) | Shop / dialogue / quest NPCs (replaces villagers) |
| `rpg-holograms` | Planned | [holograms](holograms.md) | DisplayEntity holograms + damage indicators |
| `rpg-quests` | Planned (skeleton) | [quests](quests.md) | Quest system (deferred for full design) |

## Skill addons

All on one page: [skills](skills.md). Each skill is its own addon (`rpg-combat`, `rpg-mining`, `rpg-foraging`, `rpg-farming`, `rpg-fishing`, `rpg-cooking`, `rpg-alchemy`). `rpg-enchanting` is also a skill addon but documented separately because it bundles reforges + upgrades + anvil.

## Deferred

- **rpg-pets** — companions / mounts. Planned as its own addon. Not yet scaffolded.
- **rpg-clans** — explicitly off the table; [guilds](guilds.md) replaces this concept.
