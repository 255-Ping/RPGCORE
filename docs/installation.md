# Installation

> **Status:** Planned

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
