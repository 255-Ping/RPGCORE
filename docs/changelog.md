# Changelog

Version format: `<plugin>-<pluginVersion>-<suiteVersion>`. Only notable changes listed; see git log for full diff.

---

## Suite 19 (current)

### rpg-npcs `0.2.0`
- **Damage bug fix**: `NpcProtectionListener` cancels all damage and mob-targeting events for NPC entities at `HIGHEST` priority, and respawns the NPC after one tick if it somehow dies. Belt-and-suspenders on top of `setInvulnerable(true)`.
- **Fake player NPCs**: `EntityStyle: player` spawns an NMS `ServerPlayer` with a custom `GameProfile`. Skins configured via raw texture `Value`/`Signature` or by `Name` (fetched async from Mojang API, cached). Not shown in tab list — brief ADD packet sent for skin load, removed after 2 ticks. New players on join receive the skin packet for all active fake player NPCs.
- **Banker behavior**: `Behavior.Type: banker` opens a deposit/withdraw GUI backed by `DataStore`. Bank balance persisted per-player per-NPC. Daily interest accrues at `DailyInterestPercent` on a configurable real-time interval. Requires `rpg-economy`.
- **`/npc setbehavior`** now accepts `banker [bankName]` and updates tab-complete to include it.

### rpg-api `0.2.0`
- Added `StationService` interface (`api/station/`) with `register(stationType, handler)` and `open(stationType, player, block)`. Accessible via `RpgServices.stations()`.

### rpg-core `0.7.0`
- **Station dispatch**: new `CoreStationService` implementation + `BlockInteractListener`. Right-clicking any custom block with `Interactable: true` and a non-empty `StationType` now routes through `RpgServices.stations()`. Addons register their GUI handler once in `onEnable`; rpg-core handles event cancellation.

### rpg-cooking `0.1.0`
- Station interaction now wired through `RpgServices.stations().register("cooking", ...)` — no longer needs a per-block-id config key. Removed `cooking-block` config key and `CookingStationInteractListener`.

### rpg-alchemy `0.1.0`
- Station interaction now wired through `RpgServices.stations().register("brewing", ...)`. Removed `brewing-block` config key and `BrewingStationInteractListener`.

---

## Suite 18

### rpg-api `0.1.1`
- `BuiltinStat`: added `AUTO_LOOT` — items with this stat > 0 automatically pull assigned drops into the player's inventory.

### rpg-core `0.6.0`
- **Per-player drops**: `DropManager` tags every dropped item (mob loot and block drops) with the assigned player's UUID. Only that player can pick it up until `release-seconds` expires (default 30s, configurable). A TextDisplay hologram above each drop shows the item name and who it belongs to. `AUTO_LOOT` stat bypasses the wait and gives the item directly to inventory.
- **Particle system**: `/rpg particle create <id> [type] [count] [spread] [pattern]` places a persistent particle effect at the admin's location. Patterns: `POINT`, `CIRCLE`, `SPIRAL`. Particles are saved to DataStore and respawn on reload.
- **Drop config**: `per-player-drops:` block in `rpg-core/config.yml`.

### rpg-regions `0.3.0`
- **Global default region**: `/region global flag <key> <value>` sets server-wide flag defaults that apply everywhere no region covers. `/region global` shows all current global flags. Persisted in DataStore.

### rpg-enchanting `0.1.0`
- **Vanilla block intercept**: right-clicking a vanilla Enchanting Table or Anvil now opens the custom GUI (togglable via `intercept-vanilla-enchanting/anvil` in config). Previously required a custom block placed with the correct block ID.
- Station detection now uses `StationType` field on custom blocks (preferred) instead of requiring exact block IDs in config.

### rpg-accessories `0.1.0`
- **Inventory accessories**: `inventory-accessories.enabled: true` in config makes ACCESSORY items in the player's main inventory (hotbar/storage) count their stats, not just items in the dedicated bag.

### rpg-hud `0.3.0`
- Added `{effects}` placeholder for use in tablist/scoreboard config. Shows active custom status effects: `§dStrength I §8(30s)  §cPoison II §8(5s)`.

### rpg-api `0.1.0`
- `BuiltinItemType`: added `CROSSBOW`.
- `BuiltinStat`: added `AMMO_USAGE_REDUCTION` and `PROJECTILE_SPEED`.
- `Block`: added `default long xp()` — block-level mining XP override.
- `RpgMob`: added `long xp()` — mob-level kill XP override.
- `RpgItem`: added `attackCooldownTicks()`, `itemCooldownTicks()`, `ammoType()`, `infiniteAmmo()`, `projectileType()` for attack cooldown and bow/crossbow support.
- `AbilityEffect`: added `displayName()` and `description()` default methods.
- `AbilityRegistry`: added `registerMeta`, `abilityDisplayName`, `abilityDescription`.
- `ActionBarService` (hud package): priority action bar system accessible via `RpgServices.actionBar()`.

### rpg-core `0.5.0`
- **Attack cooldown**: `AttackCooldown: N` on any item sets `generic.attack_speed` on equip. Melee damage scales by `0.2 + 0.8 * charge²` (Minecraft formula) when `attack-cooldown.scale-damage: true` in config.
- **Item cooldown**: `ItemCooldown: N` prevents rapid re-use of wands/consumables at the item level (separate from ability cooldowns).
- **Bows & crossbows**: `BowListener` handles `EntityShootBowEvent` for BOW/CROSSBOW items. Supports `AmmoType: <itemId>`, `InfiniteAmmo: true/false`, `ProjectileType: ARROW/SPECTRAL_ARROW/SNOWBALL/EGG/SMALL_FIREBALL/TRIDENT`. Arrow damage is PDC-tagged so the damage pipeline uses the bow's DAMAGE stat scaled by draw force.
- **`AMMO_USAGE_REDUCTION` stat**: percent chance to not consume ammo when firing.
- **`PROJECTILE_SPEED` stat**: multiplies arrow velocity.
- **XP in mob YAML**: `XP: N` on mob definitions overrides rpg-combat's config table.
- **XP in block YAML**: `XP: N` on block definitions overrides rpg-mining's config table.
- **`/rpg effects`**: shows a player's active custom status effects with levels and remaining duration.
- **Wand UX**: mode switch sends a 2-second action bar message + updates the wand's lore to show current mode. Corner-set confirmations also appear in the action bar.
- Ability lore, breaking power ordering, mining improvements, knockback, durability toggle, `/rpg help` (see previous entry).

### rpg-mining `0.2.0`
- Uses `block.xp()` from block definition when > 0; falls back to config.

### rpg-combat `0.2.0`
- Uses `mob.xp()` from mob definition when > 0; falls back to config.

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
