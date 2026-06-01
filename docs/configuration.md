![banner-configuration](assets/banners/banner-configuration.png)

# Configuration overview

> **Status:** Working

## File layout

Each plugin has its own data folder under `plugins/`. Per-plugin layout:

```
plugins/
├── rpg-core/
│   ├── config.yml              ← main core config (persistence backend, suppression toggles, stat defaults)
│   ├── messages.yml            ← user-facing strings (i18n keys)
│   ├── items/                  ← custom item YAMLs (any number of files)
│   ├── mobs/
│   ├── abilities/
│   ├── blocks/
│   ├── recipes/
│   │   ├── crafting/
│   │   ├── cooking/
│   │   └── brewing/
│   ├── loot-tables/
│   ├── spawners/               ← persisted admin spawner definitions
│   ├── natural-spawning/       ← natural spawn rules
│   ├── data/                   ← player files (YAML backend) or empty (MySQL backend)
│   └── (other registry data)
├── rpg-economy/
│   ├── config.yml
│   └── data/                   ← per-player balances (YAML backend)
├── rpg-chat/
│   └── config.yml
├── rpg-hud/
│   └── config.yml
├── rpg-parties/
│   └── config.yml
├── rpg-guilds/
│   ├── config.yml
│   └── data/
├── rpg-regions/
│   ├── config.yml
│   └── regions/                ← per-region YAMLs
├── rpg-dungeons/
│   ├── config.yml
│   └── dungeons/               ← captured dungeon templates
├── rpg-accessories/
│   └── config.yml
├── rpg-enchanting/
│   ├── config.yml
│   ├── enchants/
│   ├── reforges/
│   └── upgrades/
├── rpg-npcs/
│   ├── config.yml
│   └── npcs/                   ← per-NPC YAMLs
├── rpg-holograms/
│   ├── config.yml
│   └── holograms/
├── rpg-quests/
│   ├── config.yml
│   └── quests/
└── (skill addons each have their own config.yml + content folders as relevant)
```

## Reload

- `/<module> reload` — reloads one module
- `/rpg reloadall` — reloads everything
- Permission: `rpg.<module>.reload` (per module), `rpg.core.reload-all` (master)
- Both default-op.

## Editing YAML safely

- All content YAMLs are read at startup and on `/<module> reload`.
- Malformed files log a clear error including file + key + reason, and are **skipped** — the plugin continues to load with the rest of the content. A single bad file never crashes the server.
- After editing, run `/<module> reload` to apply changes without a restart.

## Properties for `gradle.properties` (developers only)

| Property | Default | Description |
|---|---|---|
| `suiteVersion` | `1` | Per-plugin version suffix (every module versions as `0.0.0-${suiteVersion}`) |
| `paperApiVersion` | `26.1.2.build.+` | Paper API compileOnly dep used by all modules |
| `minecraftVersion` | `26.1.2` | MC version used by `run-paper` in `rpg-core` |
| `testServerPluginsDir` | *(set per-machine — no default)* | Where built jars get symlinked on `assemble` |

## Related

- [Master command reference](commands.md)
- [Master permission reference](permissions.md)
- [Persistence (YAML vs MySQL)](core/persistence.md)
- [Vanilla suppression](core/vanilla-suppression.md)
