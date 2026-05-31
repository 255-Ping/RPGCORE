# RPGCORE — Admin & Developer Documentation

A suite of Paper Minecraft plugins (Paper API `26.1.2`, Java 25) that together build an entirely custom RPG framework. The goal is **total vanilla replacement** — every gameplay mechanic (damage, mana, skills, mob spawning, crafting, enchanting, brewing, regen, abilities, etc.) is custom-coded and configurable via YAML. No Java required to author content.

> **Project status:** Actively in development. Core systems, all skill addons, economy, chat, HUD, parties, guilds, dungeons, enchanting, accessories, NPCs, holograms, quests, regions, trade, and admin utilities are all shipped. Each page has a `Status:` header showing where individual subsystems stand.

---

## New here? Start with content creation

| Goal | Where to go |
|---|---|
| **Get something playable in 30 minutes** | [Quick-Start Guide](content/quickstart.md) |
| **Copy-paste examples for every content type** | [Cookbook](content/cookbook.md) |
| **Full mob walkthrough (end-to-end)** | [First Mob Walkthrough](content/first-mob.md) |
| **Common ability patterns (fireball, AoE, etc.)** | [Patterns](content/patterns.md) |
| **How to tune gear and mob difficulty** | [Progression Guide](content/progression-guide.md) |

---

## Reference links

- [Installation](installation.md) — setup, dependencies, plugin order
- [Configuration overview](configuration.md) — config file layout, reload commands
- [Stats reference](stats.md) — every stat, its effect, and damage formulas
- [Resource pack & CustomModelData ranges](resource-pack.md)
- [Master command reference](commands.md)
- [Master permission reference](permissions.md)
- [Admin guide](admin-guide.md) — authoring workflows and troubleshooting
- [Changelog](changelog.md) — version history
- [Formatting guide](formatting.md) — GUI layouts, text colors, message standards
- [Development](development.md) — build, test, contribute
- [**Roadmap**](planned/README.md) — everything still to be built ([bugs](planned/todo-bugs.md) · [features](planned/todo-features.md) · [improvements](planned/todo-improvements.md) · [GUI](planned/todo-gui.md) · [docs](planned/todo-docs.md))

---

## Core systems

`rpg-core` owns every shared service. All addons hard-depend on it.

| Page | What it covers |
|---|---|
| [Core overview](core/README.md) | Service surface, what lives in core |
| [Persistence](core/persistence.md) | YAML/MySQL toggle, schema versioning, migrations |
| [Damage pipeline](core/damage.md) | How damage is computed and routed |
| [Status effects](core/status-effects.md) | Custom (de)buff framework |
| [Skills framework](core/skills.md) | XP, levels, curve equations, milestones |
| [Health display](core/health-display.md) | Custom HP display (hearts as percent) |
| [Selection wand](core/selection-wand.md) | Single admin wand with multiple modes |
| [Vanilla suppression](core/vanilla-suppression.md) | Master toggle list for cancelling vanilla mechanics |

---

## Content authoring

Admins author content as YAML files in the plugin data directory. Any number of files, any number of entries per file.

| Page | Folder | What you author |
|---|---|---|
| [Items](content/items.md) | `items/` | Weapons, armor, materials, consumables, upgrades, accessories |
| [Mobs](content/mobs.md) | `mobs/` | Custom mobs with stats, equipment, abilities, AI profiles |
| [Abilities](content/abilities.md) | `abilities/` | Custom ability sequences using the effect DSL |
| [Blocks](content/blocks.md) | `blocks/` | Custom blocks (ores, logs, crafting stations) |
| [Recipes](content/recipes.md) | `recipes/crafting/`, `recipes/cooking/`, `recipes/brewing/` | All recipe types |
| [Loot tables](content/loot-tables.md) | `loot-tables/` (or inline in mobs) | Drop tables with damager attribution |
| [Spawning](content/spawning.md) | `spawners/`, `natural-spawning/` | Admin spawners and natural-spawn rules |

---

## Addons

Each addon is its own jar. Toggle them by adding or removing jars from the `plugins/` folder. All hard-depend on `rpg-core`; see [Installation](installation.md#plugin-dependency-table) for soft dependencies.

### Skill addons

| Module | Skill | Page |
|---|---|---|
| `rpg-combat` | Combat | [Skills](addons/skills.md) |
| `rpg-mining` | Mining | [Mining](addons/mining.md) |
| `rpg-foraging` | Foraging | [Skills](addons/skills.md) |
| `rpg-farming` | Farming | [Farming](addons/farming.md) |
| `rpg-fishing` | Fishing | [Fishing](addons/fishing.md) |
| `rpg-cooking` | Cooking | [Cooking](addons/cooking.md) |
| `rpg-alchemy` | Alchemy | [Alchemy](addons/alchemy.md) |
| `rpg-enchanting` | Enchanting (+ reforge + upgrades) | [Enchanting](addons/enchanting.md) |

### Feature addons

| Module | What it does | Page |
|---|---|---|
| `rpg-economy` | Currency, balances, shop integration | [Economy](addons/economy.md) |
| `rpg-chat` | Chat format, channels, `/msg`, moderation | [Chat](addons/chat.md) |
| `rpg-hud` | Scoreboard, action bar, tablist | [HUD](addons/hud.md) |
| `rpg-parties` | Session-only player groups, XP sharing | [Parties](addons/parties.md) |
| `rpg-guilds` | Persistent communities, bank, ranks, perks | [Guilds](addons/guilds.md) |
| `rpg-regions` | Named world regions with flags | [Regions](addons/regions.md) |
| `rpg-dungeons` | Instanced dungeon templates | [Dungeons](addons/dungeons.md) |
| `rpg-accessories` | Accessory bag, `ACCESSORY` item type, stat aggregation | [Accessories](addons/accessories.md) |
| `rpg-npcs` | Persistent NPCs with shop/dialogue/quest handoff | [NPCs](addons/npcs.md) |
| `rpg-holograms` | Static holograms + damage indicators | [Holograms](addons/holograms.md) |
| `rpg-quests` | Kill/mine/collect/talk objectives, rewards | [Quests](addons/quests.md) |
| `rpg-trade` | Player-to-player item trade GUI | [Trade](addons/trade.md) |
| `rpg-admin` | Admin utility commands (fly, god, heal, speed, etc.) | [Admin Utilities](addons/admin.md) |

### Planned (not yet scaffolded)

- **rpg-pets** — companions / mounts
- **rpg-auction-house** — see [design spec](planned/auction-house.md)
- **rpg-bazaar** — see [design spec](planned/bazaar.md)

---

## Conventions used in these docs

**Status header** at the top of every page:

| Label | Meaning |
|---|---|
| `Working` | Fully implemented and stable |
| `In Progress` | Partially implemented — some things work, others are deferred |
| `Planned` | Not started — documented for design reference only |
| `Deprecated` | Removed or replaced; page kept for historical reference |

**YAML examples** are the canonical schema. If a feature is `Planned`, the YAML reflects the target schema as designed.

**Permissions** follow `rpg.<module>.<verb>[.<qualifier>]`. Player commands default to `true`; admin/op commands default to `op`.
