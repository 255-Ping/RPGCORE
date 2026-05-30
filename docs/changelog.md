# Changelog

Version format: `<plugin>-<pluginVersion>-<suiteVersion>`. Only notable changes listed; see git log for full diff.

---

## Suite 18 (current)

### rpg-farming `0.1.0`
- Added `FARMING_FORTUNE` drop multiplier via `BlockDropItemEvent`. Same formula as mining/foraging — only applies on mature crops (Ageable max-age gate).

### rpg-parties `0.2.0`
- Added XP sharing: `SkillXpAwardEvent` now distributes bonus XP to in-range party members. Configurable scope (`all-skills` / `combat-only` / `list`), range, and `split-formula`. Disabled by default — opt in via `xp-sharing.enabled: true`.

### rpg-core `0.2.0`
- **MySQL backend** implemented (`MysqlDataStore`, HikariCP). Set `persistence.backend: mysql` to switch.
- **BackendMigrator**: detects a backend change on startup and automatically migrates all data forward (YAML→MySQL or MySQL→YAML). Recorded in `plugins/rpg-core/backend.yml`.
- **YamlMigrationRunner**: versioned code-level YAML schema migrations per repository; version tracked in `plugins/rpg-core/yaml-migrations/<name>.yml`.

### rpg-combat `0.1.0`
- Added kill XP: flat award on `EntityDeathEvent` for the killing blow. Scaled by `COMBAT_WISDOM`. Per-mob override table via `xp-per-kill` config; `default-kill-xp` fallback.

### rpg-foraging `0.1.0`
- Added `FORAGING_FORTUNE` drop multiplier via `BlockDropItemEvent`.

### rpg-hud `0.1.1`
- Fixed nametag y-offset: now applied via `Display.setTransformation()` so the position is accurate regardless of player model height. `y-offset` config key controls vertical translation.
- Fixed `ChatColor.translateAlternateColorCodes` deprecation — migrated to `LegacyComponentSerializer`.

---

## Suite 16

### rpg-core `0.1.1`
- Initial shipping build. Damage pipeline, stat registry, skills framework, block break handler, health display, content loaders (items/mobs/abilities/blocks/recipes), loot tables, admin spawners, natural spawning.

### rpg-mining `0.0.2`
- `MINING_FORTUNE` drop multiplier in `BlockBreakHandler.rollDrops()`.
- `MINING_SPEED` hold-to-break ticking live.
- `BREAKING_POWER` and `RequiredToolType` gates enforced.

### rpg-combat `0.0.1`
- Initial ship: damage XP from `PostDamageEvent` proportional to damage dealt. `COMBAT_WISDOM` scaling.

### rpg-guilds `0.1.0`
- Persistent guilds via DataStore. Guild XP from member skill gains (`SkillXpAwardEvent`). Guild level curve + per-stat perk injection (`StatRecalcEvent`). `/guild create|invite|accept|kick|promote|demote|leave|disband|info|list|deposit|withdraw`.

### rpg-foraging `0.0.1` / rpg-farming `0.0.1`
- XP on log/crop harvest. `FORAGING_WISDOM` / `FARMING_WISDOM` scaling.

### rpg-enchanting `0.0.2`
- Shipped: custom enchants, reforges, item upgrades, anvil GUI.

### rpg-hud `0.1.0`
- Shipped: scoreboard, tablist, action bar, `TextDisplay` nametags above player head.

### rpg-dungeons, rpg-npcs, rpg-quests, rpg-economy, rpg-chat, rpg-accessories, rpg-holograms, rpg-regions, rpg-parties
- Initial ships — see individual addon pages for feature status.
