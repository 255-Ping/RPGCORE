# RPGCORE

> Modular RPG framework for Paper Minecraft — 30+ independent plugins that together replace every vanilla gameplay mechanic with a fully configurable RPG layer.

![Java](https://img.shields.io/badge/Java-25-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Paper](https://img.shields.io/badge/Paper_API-26.1.2-brightgreen?style=flat)
![Modules](https://img.shields.io/badge/Modules-30%2B-blue?style=flat)
![Status](https://img.shields.io/badge/Status-Active-success?style=flat)

---

## What it is

RPGCORE is a multi-module Paper plugin suite built for **total vanilla replacement**. Every gameplay system — damage calculation, stat modeling, skills, custom mobs, combat, economy, crafting, enchanting, dungeons, and more — is custom-built and driven by YAML config files. No Java required to author content; admins define everything from items to dungeon layouts as data.

**Design goals:**
- **Modular** — each feature is an independent jar, toggled by adding or removing it from `plugins/`
- **Data-driven** — all content (items, mobs, abilities, loot tables, recipes, spawners) defined in YAML
- **Total vanilla replacement** — suppresses vanilla mechanics wholesale and replaces them
- **Plugin dependency graph** — `rpg-core` exposes a service registry; all addons consume it as a hard dependency

---

## Architecture

```
rpg-api        <- shared types and service interfaces
rpg-core       <- service implementations: damage pipeline, stat engine, persistence, status effects, skills framework
rpg-combat     <- combat skill + ability system
rpg-*          <- 25+ feature and skill addons
```

Each addon module is a Gradle subproject. The build produces one jar per module; server operators drop only the jars they want.

---

## Modules

### Skill addons

| Module | Skill |
|---|---|
| `rpg-combat` | Combat — ability casts, damage routing, cooldowns |
| `rpg-mining` | Mining |
| `rpg-foraging` | Foraging |
| `rpg-farming` | Farming |
| `rpg-fishing` | Fishing |
| `rpg-cooking` | Cooking |
| `rpg-alchemy` | Alchemy / brewing |
| `rpg-enchanting` | Enchanting, reforging, item upgrades |

### Feature addons

| Module | What it adds |
|---|---|
| `rpg-economy` | Custom currency, player balances, shop integration |
| `rpg-chat` | Channels, `/msg`, chat formatting, moderation hooks |
| `rpg-hud` | Scoreboard, action bar, tablist driven by stats |
| `rpg-parties` | Session groups with shared XP split |
| `rpg-guilds` | Persistent communities — bank, ranks, member perks |
| `rpg-regions` | Named world regions with configurable behavior flags |
| `rpg-dungeons` | Instanced dungeon templates — spawn, clear, reset cycle |
| `rpg-accessories` | Accessory bag slot system with stat aggregation |
| `rpg-npcs` | Persistent NPCs with shop, dialogue, and quest handoff |
| `rpg-holograms` | Static holograms and floating damage indicators |
| `rpg-quests` | Kill / collect / mine / talk objectives with configurable rewards |
| `rpg-trade` | Player-to-player GUI item trade |
| `rpg-admin` | Admin utility commands (fly, god, heal, speed, etc.) |
| `rpg-bossbar` | Custom boss bars attached to entities and world events |
| `rpg-kits` | Starter kit system |
| `rpg-homes` | Player home teleport system |

---

## Core systems (rpg-core)

| System | What it owns |
|---|---|
| **Damage pipeline** | Intercepts all damage events; routes through stat modifiers, armor calculation, crit logic, ability overrides |
| **Stat engine** | Per-player and per-mob stat maps; base + equipment + buff layers combined each tick |
| **Persistence** | YAML storage with optional MySQL backend; schema-versioned with migration support |
| **Status effects** | Custom buff/debuff framework — apply, tick, expire, stack |
| **Skills framework** | XP gain, level curves (configurable equations), milestone rewards |
| **Selection wand** | Single multi-mode admin wand (point, region, entity selection) |
| **Vanilla suppression** | Master toggle list for cancelling vanilla mechanics on a per-feature basis |

---

## Content authoring

Server admins author content as YAML files in the plugin data directory. Any number of files, any number of entries per file. The engine hot-reloads on `/rpg reload`.

| Content type | Folder | Examples |
|---|---|---|
| Items | `items/` | Weapons, armor, consumables, accessories, upgrade scrolls |
| Mobs | `mobs/` | Custom mobs with stats, equipment, ability lists, AI profiles |
| Abilities | `abilities/` | Sequences of effects using the ability DSL |
| Blocks | `blocks/` | Custom ore types, crafting stations |
| Recipes | `recipes/` | Crafting, cooking, and brewing recipes |
| Loot tables | `loot-tables/` | Drop tables with per-damager attribution |
| Spawners | `spawners/` | Admin-placed and natural spawn rules |

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Server API | Paper 26.1.2 |
| Build system | Gradle (multi-project, one jar per module) |
| Config / persistence | YAML + optional MySQL |
| Documentation | MkDocs Material — see `/docs` |

---

## Documentation

Full documentation lives in [`/docs`](docs/README.md), built with MkDocs Material. Covers:

- [Installation & plugin dependency order](docs/installation.md)
- [Quick-start: get something playable in 30 minutes](docs/content/quickstart.md)
- [Stats & damage formula reference](docs/stats.md)
- [Content cookbook — copy-paste examples](docs/content/cookbook.md)
- [Admin guide](docs/admin-guide.md)
- [Changelog](docs/changelog.md)
- [Roadmap](docs/planned/README.md)

---

## Status

Actively in development. Core systems, all skill addons, economy, chat, HUD, parties, guilds, dungeons, enchanting, accessories, NPCs, holograms, quests, regions, trade, and admin utilities are all shipped. See the [roadmap](docs/planned/README.md) for what is planned next.
