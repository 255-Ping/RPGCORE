# Changelog

Version format: `<plugin>-<pluginVersion>-<suiteVersion>`. Only notable changes listed; see git log for full diff.

---

## Suite 18 (current)

### rpg-api `0.0.9`
- `AbilityEffect`: added `displayName()` and `description()` default methods for item lore.
- `AbilityRegistry`: added `registerMeta`, `abilityDisplayName`, `abilityDescription` for custom ability lore.
- `ActionBarService` interface: priority action bar system; `RpgServices.actionBar()` lets any plugin override the idle HUD format for a duration.
- `BuiltinStat`: added `KNOCKBACK` stat.

### rpg-core `0.4.0`
- **Air-click fix**: abilities now fire on right-click-air and right-click-block equally. Added `EquipmentSlot.HAND` filter to prevent double-firing.
- **Beam damage**: `BeamEffect` now applies damage directly to the hit entity. No separate `damage{}` step needed for a basic beam wand.
- **Explode/aoe fix**: `ExplodeEffect` registered under both `explode` and `aoe` names. Particle spawning fixed — now emits a burst spread across the radius instead of a single point particle. Default particle changed to `EXPLOSION_EMITTER`.
- **Ability lore**: custom abilities with `Name:` and `Description:` in their YAML now show that text in item lore below the ability line.
- **Breaking power at top**: `BREAKING_POWER` stat is always rendered first in item lore.
- **Mining hold-to-break**: progress now cancels if no `BlockDamageEvent` arrives in the last 400 ms (player released mouse). Added 6-block distance check.
- **Action bar priority**: `CoreActionBarService` + `RpgServices.actionBar()`. Wrong-tool and insufficient-power messages now show for 1 second without being overridden by the idle HUD stats.
- **Knockback stat**: attacking entities with `KNOCKBACK > 0` applies velocity knockback to the victim (scaled: 100 = 1.0 strength).
- **Mob level in nameplate**: level shown for all custom mobs (previously hidden for level-1 mobs).
- **Durability suppression**: `vanilla-suppression.durability: false` in config; set to `true` to cancel all item durability loss.
- **`/rpg help`**: new subcommand listing every main command across the suite.

### rpg-hud `0.2.0`
- Nametag now re-attaches correctly after player death/respawn (`PlayerRespawnEvent` handler).
- `HudTask.updateActionBar` checks `RpgServices.actionBar()` first; priority messages are shown instead of the idle format when pending.

### rpg-regions `0.2.0`
- New flag `no-break-vanilla`: cancels block breaks only for non-custom blocks (creative players exempt). Use to protect the map while still allowing custom block interaction.
- `KNOWN_FLAGS` table expanded with all documented flags from `regions.md`.

### rpg-mining `0.1.0`
- Mining Fatigue applied while holding a gathering tool (RPG item with `MINING_SPEED`, `BREAKING_POWER`, or `FORAGING_SPEED`). Configurable via `mining-fatigue.enabled` and `mining-fatigue.amplifier` (default: Fatigue II, severely slows vanilla block mining).

### rpg-api `0.0.8`
- Added `MobStatService` interface — per-entity stat holders for custom mobs, accessible via `RpgServices.mobStats()`.
- Added `GuiConfig` interface — GUI pane materials and fill helpers, accessible via `RpgServices.guiConfig()`.

### rpg-core `0.3.0`
- **Mob stat-holder**: custom mobs now register a `MutableStatHolder` on spawn (keyed by UUID in `CoreMobStatService`). The damage pipeline reads `DAMAGE`, `DEFENSE`, `STRENGTH`, and any other `Stats:` map entries from the mob definition. Mob-vs-player and player-vs-mob fights now correctly apply the mob's defense and base damage instead of returning 0 across the board.
- Mob stat holders are cleaned up on `EntityDeathEvent` and `EntitiesUnloadEvent` to prevent memory leaks.
- **`GuiConfig` service**: pane materials and fill helpers for all inventory GUIs. Configurable via `gui:` block in `rpg-core/config.yml`. All GUIs should use `RpgServices.guiConfig()` instead of hardcoding materials.

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
