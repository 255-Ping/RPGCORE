# Installation

> **Status:** Working

## Requirements

- **Server:** Paper `26.1.2` (or any Paper build matching `paperApiVersion` in `gradle.properties`)
- **Java:** 25 (toolchain auto-resolves via Gradle Foojay convention)
- **LuckPerms:** soft dependency; needed only if you want prefix/suffix in chat, tablist, scoreboard, nametags, etc. Without it, names render as `{name}` plain.
- **MySQL** (optional): if you want MySQL persistence instead of the YAML default.

## Install order

`rpg-core` must be present. Everything else is optional, drop in the jars you want.

1. Stop the server.
2. Drop `rpg-core-*.jar` into `plugins/`.
3. Drop whichever addon jars you want (see the [addon list](addons/README.md)).
4. Start the server. Default configs and example content YAMLs are generated on first run.
5. Edit configs to taste.
6. `/rpg reloadall` (or restart) to apply.

## First-run output

On first startup, `rpg-core` generates:

- `plugins/rpg-core/config.yml` — the main config
- `plugins/rpg-core/items/example.yml` — sample item
- `plugins/rpg-core/mobs/example.yml` — sample mob
- `plugins/rpg-core/abilities/example.yml` — sample custom ability
- `plugins/rpg-core/blocks/example.yml` — sample custom block
- `plugins/rpg-core/messages.yml` — i18n message keys (English)

Each addon generates its own `plugins/<addon>/config.yml` plus relevant content folders on first run.

## Verifying

After install: `/rpg version` should print every loaded module and its version. Permission `rpg.core.version` (default true).

---

## Plugin dependency table

Hard dependencies must be present or the plugin won't load. Soft dependencies are optional — features requiring them are skipped if the plugin is absent.

| Plugin | Hard requires | Soft requires |
|---|---|---|
| `rpg-core` | _(none)_ | LuckPerms (prefix/suffix in chat/tab) |
| `rpg-combat` | `rpg-core` | — |
| `rpg-economy` | `rpg-core` | — |
| `rpg-accessories` | `rpg-core` | — |
| `rpg-alchemy` | `rpg-core` | — |
| `rpg-admin` | `rpg-core` | — |
| `rpg-chat` | `rpg-core` | — |
| `rpg-cooking` | `rpg-core` | — |
| `rpg-enchanting` | `rpg-core` | — |
| `rpg-farming` | `rpg-core` | — |
| `rpg-fishing` | `rpg-core` | — |
| `rpg-foraging` | `rpg-core` | — |
| `rpg-holograms` | `rpg-core` | — |
| `rpg-hud` | `rpg-core` | — |
| `rpg-mining` | `rpg-core` | — |
| `rpg-npcs` | `rpg-core` | `rpg-economy`, `rpg-quests` |
| `rpg-parties` | `rpg-core` | — |
| `rpg-regions` | `rpg-core` | — |
| `rpg-trade` | `rpg-core` | — |
| `rpg-dungeons` | `rpg-core` | `rpg-parties` |
| `rpg-guilds` | `rpg-core` | `rpg-economy` |
| `rpg-quests` | `rpg-core` | `rpg-npcs`, `rpg-economy` |

**Recommended install order:** `rpg-core` → `rpg-economy` → `rpg-npcs` → everything else (Paper resolves load order automatically via `depend`/`softdepend`, so manual ordering is only needed if you're troubleshooting missing-service errors on startup).
