# RPGCORE — Master Todo

> This is the single reference point before starting any session. Every known bug, missing feature, improvement, GUI redesign, and doc gap lives here or in one of the linked sub-pages below. Update this when things get done or new issues are found.

> **Difficulty scale used throughout:** 🟢 Easy (< 1 day) · 🟡 Medium (1–2 days) · 🔴 Hard (several days) · ⚫ Very Hard (week+)

---

## Sub-pages

| Page | What's in it |
|---|---|
| [🐛 Bugs](todo-bugs.md) | Confirmed broken things that need fixing |
| [🔴 New Features](todo-features.md) | Full features not yet started (new plugins, new systems) |
| [🟠 Improvements](todo-improvements.md) | In-progress systems with significant missing chunks |
| [🔵 GUI Redesigns](todo-gui.md) | GUI layout changes, pagination, new inventory screens |
| [📄 Docs](todo-docs.md) | Missing pages, stale content, inconsistencies |

---

## ✅ Recently Completed (don't re-do these)

- Hologram follows dropped item entity (passenger model)
- LuckPerms prefix/suffix in tab list player names
- `/hud` tab completions
- Mining Fatigue + custom blocks (`ignoreCancelled = false` fix)
- Item lore overhaul (stats with `(+X)` bonus indicators, enchants between stats/lore, upgrades section, reforge as name prefix)
- Enchantment descriptions in lore
- Physical reforge stones + upgrade books in anvil GUI
- `/enchanting give reforge|upgrade` admin command
- Currency formatting using `RpgServices.currencies().primary()` everywhere
- `/trade` command (rpg-trade 0.1.0)
- Non-tradeable item blocking in trade
- GUI formatting consistency pass (cooking, alchemy, enchanting, npcs)
- Shift-click prevention in GUIs
- Global region in `/region` commands
- All 20 config.yml files annotated with examples
- Docs pass: missing plugin pages (trade, admin, mining, farming, fishing, accessories, chat), stale status fixes, stat formulas, ability context table, status label standardization, design intent paragraphs, quickstart guide, patterns page, progression guide, dependency table, backlog redirect, planned/README
- Iron Shortsword attack cooldown fix — removed vanilla attribute modifiers from custom items (`rpg-core 1.0.3`)
- Mining fatigue amplifier fix — bumped to 255 to fully suppress vanilla breaking (`rpg-mining 0.2.1`)
- Coin drops deposit fix — added `currency-rolls:` loot table section that deposits via economy (`rpg-core 1.0.3`)
- NPC click fix — orphan sweep on reload + ZOMBIE default + NORMAL/ignoreCancelled=false handler (`rpg-npcs 0.5.1`)
- Silent stats wired up — `ferocity` (extra melee swings), `speed` (movement speed), `swing_range` (melee reach) all implemented in `DamagePipelineListener` / `EquipmentListener`
- NPC command overhaul — per-NPC entity type, style/skin commands, dialogue/shop in-game editing, look-at-player, `/npc info` (`rpg-npcs 0.6.0`)
- 10 new built-in ability effects — `knockback`, `blink`, `chain`, `zone`, `shield`, `drain`, `mark`, `launch`, `freeze`, `restore_mana` (`rpg-core 1.3.0`)
- Expanded example mobs — 5 showcase mobs using the new effects; berserker set passives clarified; items updated (`rpg-core 1.3.0`)
- Frost Golem permanent Slowness fix — zone pulse now uses null attacker to prevent `~onHit` cascade (`rpg-core 1.3.1`)
- Loot pool system — named reusable `LootPool:`/`LootPools:`, vanilla XP + combat XP per pool (`rpg-core 1.4.0` / `rpg-api 0.4.3`)
- Enchanting vanilla XP cost — `XpCost:` wired; levels shown in GUI and deducted on apply (`rpg-enchanting 0.5.0`)
- Dual-Cast Wand Solar Beam bug — appended `damage{}` to right-click chain; beam now deals damage (`rpg-core 1.5.0`)
- Ability trigger types expansion — `~on_attack` / `~onAttack`, `~on_kill` / `~onKill`, `~on_block`, `~onJump` added to both player item and mob trigger systems (`rpg-core 1.5.0`, `rpg-api 0.5.0`)
- Ability DSL: `chance{}` gate — `AbilityContext.blocked` field + `AbilityPipeline` skip check + `ChanceEffect`; 2 showcase items (`rpg-core 1.5.1`, `rpg-api 0.5.1`)
- Timed cooking + brewing — `CraftProgress` timer (4-tick task), 9-slot progress bar row 0, DataStore persist/restore on close/reopen, ingredient locking, cook time in recipe lore (`rpg-cooking 0.4.0`, `rpg-alchemy 0.4.0`)
- Damage indicators: float-down + shrink — originally sin-arc in 1.5.2; changed to linear downward drift + shrink in `rpg-core 1.10.3` (config key `rise-blocks` → `drop-blocks`)
- Resource pack auto-delivery — `resource-pack:` config block + `ResourcePackListener` on join (`rpg-core 1.5.2`)
- Player homes + warps — `rpg-homes 0.1.0`; DataStore homes + warps.yml; max-homes config; `/home`, `/warp`, `/setwarp`, `/delwarp`, `/warps`
- Starter kits — `rpg-kits 0.1.0`; one-time + cooldown modes; RPG/vanilla items; `/kit`, `/givenkit`, `/kitreset`; suiteVersion bumped to 20
- Mob death animation — `DeathParticle`/`DeathParticleCount`/`DeathParticleSpread`/`DeathSound` YAML fields; `MobDeathAnimListener` plays particle burst + sound, zeroes knockback velocity at death (`rpg-core 1.6.0`)
- Extract crafting — `rpg-crafting 0.1.0`; shaped + shapeless recipes from `plugins/rpg-crafting/recipes/`; `/crafting reload|list`; removed from rpg-core loader
- Timed smelting — `rpg-smelting 0.1.0`; BLAST_FURNACE station block, orange progress-bar GUI, single input slot, DataStore persist/restore, optional vanilla FurnaceRecipe registration, XP → Mining skill; suiteVersion bumped to 21
- Doc note: `backend.yml` vs `config.yml` — added callout to `docs/core/persistence.md` clarifying that `backend.yml` is internal `BackendMigrator` bookkeeping; admins only touch `config.yml`
- Vault provider bridge — `VaultEconomyProvider` wraps `CoreEconomy`; registered at `ServicePriority.Normal` when Vault is present; no bank support; `rpg-economy 0.2.0`
- Vanilla suppression remaining flags — `BeaconEffectEvent` handler added (was the only unwired flag); dead `onPortalCreate` removed; doc status updated; `rpg-core 1.6.1`
- Permission system consistency audit — `rpg.core.particle` + `rpg.regions.admin.global` added to plugin.ymls; `docs/permissions.md` fully rewritten covering all 25 plugins; `rpg-regions 0.5.1`
- Telekinesis effect — `telekinesis` enchant + `telekinetic` reforge + `telekinesis_scroll` upgrade all give `auto_loot: 1`; deployed via `ensureExample` on startup; `rpg-enchanting 0.6.0`
- Ability DSL: Target selection — `nearest_enemy{}`, `farthest_enemy{}`, `nearest_ally{priority=nearest|lowest_health|lowest_mana}`, `random_enemy{}`, `self{}`; each sets `ctx.target` via radius query; `allow_pvp=true` opt-in; null-safe (no target = downstream no-ops); `rpg-core 1.7.0`
- Ability DSL: Conditional flow — `if_health_below/above{percent=}`, `if_mana_below/above{percent=}`, `if_marked{}`, `if_target_has_status{id=}`, `if_flag{name=}`, `if_not_flag{name=}`, `set_flag{name=}`, `clear_flag{name=}`; all use same `blocked` mechanism as `chance{}`; flags stored in entity metadata (auto-cleared on death); boss phase-transition pattern documented; `rpg-core 1.7.0`
- Region enter/exit messages + more flags — `enter-message`/`leave-message` (title or `[actionbar]` prefix; `{player}`/`{region}` placeholders); `no-mob-spawn`, `no-damage`, `fly` (granted/revoked on enter/leave), `no-item-drop`, `keep-inventory`; flag table in docs rewritten; `rpg-regions 0.6.0`
- Stats GUI redesign — `/stats [player]` now opens 54-slot inventory GUI; gear column shows equipped items; 7 stat-category items (Combat/Survival/Caster/Mobility/Loot/Wisdom/Skills) with full lore breakdowns; Trade button when viewing another player; nav bar with Close; `rpg-core 1.7.0`
- Sign-entry utility — `SignInputService` in `rpg-api 0.5.2` + `CoreSignInputService` in `rpg-core 1.8.0`; virtual sign editor, 60s timeout, null on cancel/disconnect; `RpgServices.signInput().ask(player, label, callback)`
- Ability DSL: `spawn_mob{}` — `id=,count=1,at=caster|target|point,radius=0,offset_y=0,owned=false`; `OwnedMobTracker` enforces per-caster cap + despawns on logout; `rpg-core 1.8.0`
- Boss bar system — `rpg-bossbar 0.1.0`; `BossBar: {Color,Style,Range}` in mob YAML; proximity task shows/hides per player; `PostDamageEvent` updates HP fraction; `RpgServices.bossBar()`; `ancient_golem` boss example added
- Fishing content slice — `rpg-fishing 0.1.0`; custom catch tables (`catch-tables/*.yml`); `FISHING_FORTUNE` boosts fortune-affected entry chances; `FISHING_SPEED` shortens bobber wait; `FISHING_WISDOM` scales XP; catch message on land; `/fishing reload`; bundled `default.yml` with fish/junk/treasure
- `~onLogin` trigger + Ability Pierce Cap — `rpg-api 0.5.5`, `rpg-core 1.10.4`; `ON_LOGIN` enum + `PlayerLoginAbilityListener`; `pierce_cap` int param on `beam{}` (default 1 = single-target, 0 = unlimited); repeated-rayTrace exclusion loop; knockback to all hits
- Item Browser GUI — `rpg-core 1.10.5`; `/rpg items` opens 54-slot paginated GUI; Type filter (cycle BuiltinItemType), Rarity filter (cycle distinct rarities), sign-entry Search; click-to-give ×1 / shift ×64 (requires `rpg.core.item.give`); in-place repopulate (no flicker); `ItemBrowserGui`; permission `rpg.core.items.browse` (default op)

---

## Suggested Priority Order

1. 🐛 🟡 **Fix all confirmed bugs first** — see [Bugs](todo-bugs.md)
2. ✅ **Wire up silent stats** (speed, ferocity, swing_range) — already implemented
3. ✅ **NPC command overhaul + in-game editing** — per-NPC entity type (`/npc setentitytype`), style/skin commands, dialogue/shop in-game editing, look-at-player task, `/npc info` (`rpg-npcs 0.6.0`)
4. ✅ **GUI redesigns** (brewing/cooking/enchanting pagination + layouts, nav bar standard on all GUIs)
5. ✅ **New built-in ability effects** — 10 effects shipped: `knockback`, `blink`, `chain`, `zone`, `shield`, `drain`, `mark`, `launch`, `freeze`, `restore_mana` (rpg-core 1.3.0)
6. ✅ **Expand example mobs, abilities, items** — 5 showcase mobs added (`frost_golem`, `chain_wraith`, `blood_shade`, `shield_golem`, `void_phantom`); items updated with berserker set passives + DualCast wand
7. ✅ **Loot pool system** — named reusable pools, `LootPool:`/`LootPools:` on mobs, vanilla XP + combat XP per pool (rpg-core 1.4.0 / rpg-api 0.4.3)
8. ✅ **Enchanting: Minecraft XP cost** — `XpCost:` field wired; levels deducted + shown in GUI (rpg-enchanting 0.5.0)
9. ✅ **Ability trigger types expansion** (`~onAttack`, `~onKill`, `~onBlock`, `~onJump`) — player items and mob triggers both expanded; 3 showcase items added (`rpg-core 1.5.0`)
10. ✅ **Ability DSL: `chance{}` gate** — `chance{percent=N}` sets `ctx.blocked` on a failed roll; `AbilityPipeline` skips all downstream effects; stacking = AND logic; 2 showcase items added (`rpg-core 1.5.1`, `rpg-api 0.5.1`)
11. ✅ **Timed cooking + brewing** — `CraftProgress` timer, progress bar in row 0, DataStore save/restore on close/reopen, ingredient locking, cook time shown in recipe lore (`rpg-cooking 0.4.0`, `rpg-alchemy 0.4.0`)
12. ✅ **Mob death animation** — `DeathParticle` / `DeathSound` YAML fields on mobs; `MobDeathAnimListener` zeroes velocity + spawns burst + plays sound (`rpg-core 1.6.0`)
13. ✅ **Damage indicators: float down + shrink** — linear downward drift + shrink; originally sin-arc (1.5.2), revised to float-down in `rpg-core 1.10.3`; config key `rise-blocks` → `drop-blocks`
14. ✅ **Player homes + warps** — `rpg-homes 0.1.0`; `/home [set|delete|list|<name>]`, `/warp`, `/setwarp`, `/delwarp`, `/warps`; DataStore-backed per-player homes + warps.yml for server warps; configurable max-homes
15. ✅ **Starter kits** — `rpg-kits 0.1.0`; `/kit`, `/givenkit`, `/kitreset`; one-time + cooldown kits; YAML-driven items (RPG + vanilla); DataStore-backed claim state
16. ✅ **Resource pack auto-delivery** — `resource-pack:` block in rpg-core config; `ResourcePackListener` fires on join if enabled (`rpg-core 1.5.2`)
17. ✅ **Extract smelting + crafting to own plugins** — `rpg-crafting 0.1.0` (shaped/shapeless recipes); `rpg-smelting 0.1.0` scaffolded; `VanillaSuppressionListener.onSmelt` updated to allow any non-minecraft namespace
18. ✅ **Timed smelting** — `rpg-smelting 0.1.0`; single input slot GUI, orange progress bar, BLAST_FURNACE station block, DataStore save/restore, vanilla FurnaceRecipe registration toggle; XP → Mining skill
19. ✅ **Permission system consistency audit** — added `rpg.core.particle` + `rpg.regions.admin.global` to plugin.ymls; `docs/permissions.md` fully rewritten to match all 25 plugins (added Admin, Homes, Kits, Cooking, Alchemy, Crafting, Smelting, skill addons; fixed Dungeons/Regions mismatches; removed stale nodes); `rpg-regions 0.5.1`
20. ✅ **Telekinesis effect** — ships as `telekinesis` enchant + `telekinetic` reforge + `telekinesis_scroll` upgrade; all give `auto_loot: 1` which `DropManager` already checks; deployed via `ensureExample`; applies to any item; `rpg-enchanting 0.6.0`
21. ✅ **Document `backend.yml` vs `config.yml`** — callout added to `docs/core/persistence.md`; `backend.yml` is internal `BackendMigrator` state — do not edit manually
22. ✅ **Vault provider bridge** — `VaultEconomyProvider` registered at `ServicePriority.Normal` when Vault is on the server; no banks; `rpg-economy 0.2.0`
23. ✅ **Vanilla suppression remaining flags** — added `BeaconEffectEvent` handler (only truly missing flag); removed dead `onPortalCreate`; updated Javadoc + doc status; `rpg-core 1.6.1`
24. ✅ **Ability DSL: Target selection effects** — `nearest_enemy{}`, `farthest_enemy{}`, `nearest_ally{}`, `random_enemy{}`, `self{}` each set `ctx.target`; unlocks targeting logic in mob timers + passive procs. `rpg-core 1.7.0`
25. ✅ **Ability DSL: Conditional flow** — `if_health_below/above{}`, `if_mana_below/above{}`, `if_marked{}`, `if_target_has_status{}`, `if_flag{}`, `if_not_flag{}`, `set_flag{}`, `clear_flag{}`; flags in entity metadata; phase-transition pattern documented. `rpg-core 1.7.0`
26. ✅ **Region enter/exit messages + more flags** — `enter-message`, `leave-message`, `no-mob-spawn`, `no-damage`, `fly`, `no-item-drop`, `keep-inventory`. `rpg-regions 0.6.0`
27. 🟠 ⚫ **Dungeon flesh-out** — entry requirements + loot grants (fix enter bug first)
28. ✅ **Stats GUI redesign** — 54-slot inventory GUI with gear column + 7 stat categories + Trade button. `rpg-core 1.7.0`
29. ✅ **Achievement system** — `CoreAchievementService` (DataStore-backed grant/increment/persist, rewards, GUI notification), `AchievementGui`, `AchievementLoader`; wired in `RpgCorePlugin`; `/achievements [player]`; `MobLootListener` increments `mob_kills` + grants `elite_slayer`; event-based unlocks at all action call sites. `rpg-core 1.7.0`
30. 🔴 🟡 **Leaderboards** — community engagement
31. ✅ **Boss bar system** — `rpg-bossbar 0.1.0`; `BossBar:` YAML section on mobs; proximity task shows/hides bar; `PostDamageEvent` updates HP fraction; `RpgServices.bossBar()`
32. ✅ **Sign-entry utility** — `SignInputService` interface in `rpg-api 0.5.2` + `CoreSignInputService` in `rpg-core 1.8.0`; virtual sign, 60s timeout, null callback on cancel/disconnect; `RpgServices.signInput().ask(player, label, callback)`
33. 🔴 🔴 **Offline mail / inbox system** — needed before AH and offline achievement rewards
34. ✅ **HUD improvements** — scoreboard, tablist, PAPI support, ability cooldowns (`rpg-hud 0.4.1`)
35. ✅ **PlaceholderAPI support** — `RpgPlaceholderExpansion` registers `%rpg_X%` placeholders from all HUD keys; softdepend on PlaceholderAPI; skill keys translate `skill_id_prop` → `skill:id:prop` (`rpg-hud 0.4.1`)
36. ✅ **MagicFind stat implementation** — configurable `loot.max-magic-find-multiplier` cap applied in both SHARED and PER_PLAYER roll modes (`rpg-core 1.8.2`)
37. ✅ **Economy transaction log** — `TxLog` with DataStore persistence; `/money log [player] [page]`; reason-tagged deposits/withdrawals/transfers; mob drops tagged `mob_drop` (`rpg-economy 0.2.1`)
38. ✅ **Item set bonuses** — `ArmorSetListener` / `ArmorSetLoader` / `SetBonus` already in rpg-core
39. ✅ **Fishing content slice** — `rpg-fishing 0.1.0`: custom catch tables (`catch-tables/*.yml`), `FISHING_FORTUNE` scales fortune-affected entry chances, `FISHING_SPEED` shortens bobber wait time, `FISHING_WISDOM` scales XP (existing); catch-message on land; `/fishing reload`; bundled `default.yml` table with common fish/junk/treasure
40. 🟠 🟡 **Quest log GUI + chains + repeatable quests**
41. 🟠 🔴 **Guild bank + rank GUI**
42. ✅ **Ability DSL: `spawn_mob{}` effect** — `spawn_mob{id=,count=1,at=caster|target|point,radius=0,offset_y=0,owned=false}`; `owned=true` tags with caster UUID + `OwnedMobTracker` cap; despawns on caster logout; `rpg-core 1.8.0`
43. 🔴 🔴 **Custom enchantment ability triggers** — ability-fire enchants (on_hit, on_kill, etc.)
44. 🟠 🔴 **RPG-Farming redesign**
45. ✅ **Elite/champion mob variants** — `EliteService.promote()` called from `SpawnerManager` + `NaturalSpawnTask`; PDC tags prefix name + glow + HP/damage/loot multipliers; `damageMultiplier()` in `DamagePipelineListener`; `lootMultiplier()` + `elite_slayer` grant in `MobLootListener`. `rpg-core 1.7.0`
46. 🟠 🟡 **Mob factions + AI goals** — `Faction:` string tag on mobs; `AiGoals:` list replaces the blunt kind enum with composable targeting rules: `attack_player`, `attack_faction{faction=X}`, `defend_faction{}`, `assist_faction{}`, `flee_from{}`, `call_for_help{}`, `guard_radius{}`, `idle`; faction awareness extends into ability DSL target selection. Full spec in [Improvements](todo-improvements.md)
47. 🟠 🔴 **Mob AI profiles flesh-out** — `ranged_kiter`, `boss`, `flying`; `swarming` + `pack_hunter` now implementable via faction goals (#46). Full spec in [Improvements](todo-improvements.md)
48. 🟠 🔴 **Mob patrol waypoints** — admin-defined walk paths for mobs + NPCs
49. ✅ **Ability DSL: Context variables — Tier 1 (boolean flags)** — `if_flag{}`, `if_not_flag{}`, `set_flag{}`, `clear_flag{}`; entity PDC-backed (`rpg_flag_<name>`), auto-cleared on death; registered in `ItemAbilityListener`; boss phase-transition pattern in `FlagEffect.java` javadoc. `rpg-core 1.7.0`  
    🔴 🟡 **Tier 2 still open**: numeric context vars — `increment{}`, `decrement{}`, `if_var_gte{}`
50. 🔴 ⚫ **World events + world boss**
51. 🔴 🔴 **Salvaging system** — Salvager block → 54-slot GUI; 36 input slots; live yield preview; Scrap All button; coins by rarity, XP from enchants, chance to recover reforges/upgrades; new `salvaging` skill boosts yield + recovery; full spec in [New Features](todo-features.md)
52. 🔴 ⚫ **Auction House** (needs sign-entry ✅ + mail first) — reference SurvivalCore `auction/` + `gui/AuctionGui.java`; swap `CoinProvider` → `RpgServices.economy()`
53. 🔴 🔴 **Bazaar** — reference SurvivalCore `bazaar/` + `gui/BazaarGui.java`; swap `CoinProvider` → `RpgServices.economy()`
54. 🟠 🔴 **Display entity suite** (`rpg-holograms`) — ItemDisplay, BlockDisplay, physical `/de` editor with inventory replacement, fine-detail GUI, YAML persistence for all types
55. 🔵 🟡 **Party / Guild / Quest GUI conversions**
56. 🔴 ⚫ **Pets system** (`rpg-pets`) — long-term, build in phases
57. 🟠 🟡 **Unit test coverage** — ongoing, add tests alongside any new system
58. 📄 🟢 **Docs pass** — fill stubs, add missing plugin pages
59. ✅ **Main Menu Item + GUI** — persistent COMPASS item in slot 8; right-click opens hub GUI with Stats, Skills, Achievements, Economy (nested with Back), plus grayed stubs for Quests/Party/Guild/Mail. Warps removed (admin-only). `/menu` command also opens it. `MainMenuGui`, `MainMenuListener`, `MenuCommand`. coreVersion 1.10.0.
60. ✅ **GUI Navigation Standard** — all GUIs have bottom-row nav bar (`placeNavBar` / `placeNavBarNested`). Back-callback pattern (`Map<UUID, Runnable> backCallbacks`, `runTask` on close) consistent across `StatsGui`, `SkillsGui`, `AchievementGui`, `WalletGui`, `MainMenuGui`. `SkillsGui` and `WalletGui` are new. coreVersion 1.10.0.
61. 🔵 🟡 **Main Menu Configurability** — all button slots, names, and visibility flags configurable in `config.yml` under `main-menu.buttons.<id>`. `MainMenuConfig` class reads config; Waypoints + Vault buttons pre-wired but disabled until those systems ship. See [GUI Redesigns](todo-gui.md).
62. 🔵 🟡 **Waypoints System** — custom `waypoint` block; configurable particles + hologram; players right-click to set spawn point; admin-configurable cost (coins, XP, item, or free); `/waypoint setname|setcost|list|reload` commands. Player-facing replacement for warps in the main menu. See [New Features](todo-features.md).
63. 🔵 🔴 **Skills GUI Redesign + Per-Skill Detail GUI** — improved overview with category colors + formatted progress bars; clicking a skill opens a per-skill snake-path GUI (Hypixel SkyBlock style) showing each level milestone with rewards. See [GUI Redesigns](todo-gui.md).
64. 🔵 🔴 **Vault / Storage System** — per-player persistent storage vaults; unlimited vault count (defined by however many entries are in `vaults.yml`); configurable rows per vault (default 6); unlock requirements per vault (coins, MC XP, item, permission); `VaultSelectorGui` → `VaultGui`; `/vault [n]` command. See [New Features](todo-features.md).
