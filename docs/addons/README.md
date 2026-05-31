# Addons

> **Status:** In Progress — all addons below are shipped and functional. See individual pages for per-feature status.

Each addon is its own plugin jar. All addons hard-depend on `rpg-core`. To enable an addon, drop its jar in `plugins/`. To disable, remove it.

## Feature addons

| Module | Status | Page | Brief |
|---|---|---|---|
| `rpg-economy` | Shipped | [economy](economy.md) | Currency, balances, `/pay`, `/balance` |
| `rpg-chat` | Shipped | [chat](chat.md) | Chat format, channels, moderation, LuckPerms |
| `rpg-hud` | Shipped | [hud](hud.md) | Scoreboard, tablist, action bar, nametags |
| `rpg-parties` | Shipped | [parties](parties.md) | Session-only player groups |
| `rpg-guilds` | Shipped | [guilds](guilds.md) | Persistent guilds with bank, ranks, XP/level |
| `rpg-regions` | Shipped | [regions](regions.md) | 3D box regions with flags |
| `rpg-dungeons` | Shipped | [dungeons](dungeons.md) | Instanced dungeons authored in-game |
| `rpg-accessories` | Shipped | [accessories](accessories.md) | Accessory bag + `ACCESSORY` item type |
| `rpg-enchanting` | Shipped | [enchanting](enchanting.md) | Enchanting skill + reforges + upgrades + anvil |
| `rpg-alchemy` | Shipped | [alchemy](alchemy.md) | Alchemy skill + brewing station + custom potions |
| `rpg-cooking` | Shipped | [cooking](cooking.md) | Cooking skill + cooking station + custom consumables |
| `rpg-fishing` | Shipped | [fishing](fishing.md) | Fishing skill + sea creatures + custom catches |
| `rpg-npcs` | Shipped | [npcs](npcs.md) | Shop / dialogue / quest NPCs (replaces villagers) |
| `rpg-holograms` | Shipped | [holograms](holograms.md) | DisplayEntity holograms + damage indicators |
| `rpg-quests` | Shipped | [quests](quests.md) | Quest system with objective DSL, NPC hand-off |

## Skill addons

Combat, mining, foraging, and farming overviews: [skills.md](skills.md).
Fishing, cooking, and alchemy have their own pages (linked in the table above).
Enchanting is separately documented at [enchanting.md](enchanting.md) because it bundles reforges + upgrades + anvil.

## Deferred

- **rpg-pets** — companions / mounts. Planned as its own addon. Not yet scaffolded.
- **rpg-clans** — explicitly off the table; [guilds](guilds.md) replaces this concept.
