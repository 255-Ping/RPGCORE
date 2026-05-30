# RPG Plugins — Admin & Developer Documentation

A suite of Paper Minecraft plugins (Paper API `26.1.2`, Java 25) that together build an entirely custom RPG framework. The goal is **total vanilla replacement** in heavily moderated, admin-built areas — every gameplay mechanic (damage, mana, skills, mob spawning, crafting, enchanting, brewing, regen, etc.) is custom-coded and configurable.

> **Project status:** Actively in development — core systems, all skill addons, guilds, parties, dungeons, enchanting, accessories, NPCs, holograms, quests, economy, chat, HUD, and regions are all shipped. Each page has a `Status:` header showing where each individual subsystem stands.

---

## Quick links

- [Installation](installation.md) — setup & dependencies
- [Configuration overview](configuration.md) — config file layout, reload commands
- [Master command reference](commands.md) — every command across every module
- [Master permission reference](permissions.md) — every permission node
- [Master stat reference](stats.md) — every stat with effect
- [Resource pack & CustomModelData ranges](resource-pack.md)
- [Development](development.md) — build, test, contribute
- [Admin guide](admin-guide.md) — workflows, authoring walkthrough, troubleshooting
- [Changelog](changelog.md) — version history
- [Formatting guide](formatting.md) — GUI layouts, text colors, item lore, message standards

---

## Core

The `rpg-core` plugin owns every shared service. Other modules depend on it.

| Page | What it covers |
|---|---|
| [Core overview](core/README.md) | What lives in core, service surface |
| [Persistence](core/persistence.md) | YAML/MySQL toggle, schema versioning, migrations |
| [Vanilla suppression](core/vanilla-suppression.md) | The master toggle list for cancelling vanilla mechanics |
| [Damage pipeline](core/damage.md) | How damage is computed and routed |
| [Status effects](core/status-effects.md) | Custom (de)buff framework |
| [Skills framework](core/skills.md) | XP, levels, curve equations, milestones |
| [Health display](core/health-display.md) | Heart-as-percent (20 hearts max display) |
| [Selection wand](core/selection-wand.md) | Single admin wand with modes |

---

## Content authoring

Admins author content as YAML files. Files can be named anything; admins create as many as they want with as many entries as they want.

| Page | Folder under plugin data dir | What you author |
|---|---|---|
| [Items](content/items.md) | `items/` | Custom items (swords, armor, materials, consumables, upgrades, accessories) |
| [Mobs](content/mobs.md) | `mobs/` | Custom mobs with stats, equipment, abilities, AI profiles |
| [Abilities](content/abilities.md) | `abilities/` | Custom ability sequences using the ability DSL |
| [Blocks](content/blocks.md) | `blocks/` | Custom blocks (ores, logs, stations) tagged onto vanilla bases |
| [Recipes](content/recipes.md) | `recipes/crafting/`, `recipes/cooking/`, `recipes/brewing/` | Crafting, cooking, and brewing recipes |
| [Loot tables](content/loot-tables.md) | `loot-tables/` (or inline in mobs) | Drop tables with damager attribution |
| [Spawning](content/spawning.md) | `spawners/`, `natural-spawning/` | Admin spawners and natural-spawn rules |

---

## Addons

Each addon is its own plugin jar. They all hard-depend on `rpg-core`. Toggle addons by adding/removing their jar from the plugins folder.

### Skill addons

| Module | Skill | Page |
|---|---|---|
| `rpg-combat` | Combat | [Skills overview](addons/skills.md#combat) |
| `rpg-mining` | Mining | [Skills overview](addons/skills.md#mining) |
| `rpg-foraging` | Foraging | [Skills overview](addons/skills.md#foraging) |
| `rpg-farming` | Farming | [Skills overview](addons/skills.md#farming) |
| `rpg-fishing` | Fishing | [Fishing](addons/fishing.md) |
| `rpg-cooking` | Cooking | [Cooking](addons/cooking.md) |
| `rpg-alchemy` | Alchemy | [Alchemy](addons/alchemy.md) |
| `rpg-enchanting` | Enchanting (+ reforge + upgrades) | [Enchanting](addons/enchanting.md) |

### Feature addons

| Module | Page |
|---|---|
| `rpg-economy` | [Economy](addons/economy.md) |
| `rpg-chat` | [Chat](addons/chat.md) |
| `rpg-hud` | [HUD](addons/hud.md) |
| `rpg-parties` | [Parties](addons/parties.md) |
| `rpg-guilds` | [Guilds](addons/guilds.md) |
| `rpg-regions` | [Regions](addons/regions.md) |
| `rpg-dungeons` | [Dungeons](addons/dungeons.md) |
| `rpg-accessories` | [Accessories](addons/accessories.md) |
| `rpg-npcs` | [NPCs](addons/npcs.md) |
| `rpg-holograms` | [Holograms & damage indicators](addons/holograms.md) |
| `rpg-quests` | [Quests](addons/quests.md) |

### Deferred

- **rpg-pets** — companions / mounts. Planned, not yet scaffolded.

---

## Conventions used in these docs

- **Status header** at the top of every page:
  - `Status: Planned` — designed but not implemented yet
  - `Status: In progress` — partially implemented
  - `Status: Stable` — implemented and considered stable
- **YAML examples** are the canonical schema. If a feature is in `Status: Planned`, the YAML reflects the target schema as specified.
- **Permissions** follow `rpg.<module>.<command>[.<subcommand>]`. Self-use commands default to true; admin commands default to op.
- **Commands** are listed with their permission and default in [commands.md](commands.md).
