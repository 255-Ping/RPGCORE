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
- Damage indicators: sin-arc + linear shrink — replaces linear rise; scale 1→0 via `setTransformation` each tick (`rpg-core 1.5.2`)
- Resource pack auto-delivery — `resource-pack:` config block + `ResourcePackListener` on join (`rpg-core 1.5.2`)
- Player homes + warps — `rpg-homes 0.1.0`; DataStore homes + warps.yml; max-homes config; `/home`, `/warp`, `/setwarp`, `/delwarp`, `/warps`
- Starter kits — `rpg-kits 0.1.0`; one-time + cooldown modes; RPG/vanilla items; `/kit`, `/givenkit`, `/kitreset`; suiteVersion bumped to 20
- Mob death animation — `DeathParticle`/`DeathParticleCount`/`DeathParticleSpread`/`DeathSound` YAML fields; `MobDeathAnimListener` plays particle burst + sound, zeroes knockback velocity at death (`rpg-core 1.6.0`)
- Extract crafting — `rpg-crafting 0.1.0`; shaped + shapeless recipes from `plugins/rpg-crafting/recipes/`; `/crafting reload|list`; removed from rpg-core loader
- Timed smelting — `rpg-smelting 0.1.0`; BLAST_FURNACE station block, orange progress-bar GUI, single input slot, DataStore persist/restore, optional vanilla FurnaceRecipe registration, XP → Mining skill; suiteVersion bumped to 21

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
13. ✅ **Damage indicators: float down + shrink** — sin-arc position + linear scale shrink 1→0; `riseBlocks` config unchanged (`rpg-core 1.5.2`)
14. ✅ **Player homes + warps** — `rpg-homes 0.1.0`; `/home [set|delete|list|<name>]`, `/warp`, `/setwarp`, `/delwarp`, `/warps`; DataStore-backed per-player homes + warps.yml for server warps; configurable max-homes
15. ✅ **Starter kits** — `rpg-kits 0.1.0`; `/kit`, `/givenkit`, `/kitreset`; one-time + cooldown kits; YAML-driven items (RPG + vanilla); DataStore-backed claim state
16. ✅ **Resource pack auto-delivery** — `resource-pack:` block in rpg-core config; `ResourcePackListener` fires on join if enabled (`rpg-core 1.5.2`)
17. ✅ **Extract smelting + crafting to own plugins** — `rpg-crafting 0.1.0` (shaped/shapeless recipes); `rpg-smelting 0.1.0` scaffolded; `VanillaSuppressionListener.onSmelt` updated to allow any non-minecraft namespace
18. ✅ **Timed smelting** — `rpg-smelting 0.1.0`; single input slot GUI, orange progress bar, BLAST_FURNACE station block, DataStore save/restore, vanilla FurnaceRecipe registration toggle; XP → Mining skill
19. 🟠 🟡 **Permission system consistency audit** — every command gets a permission, all nodes follow `rpg.<plugin>.<verb>[.<qualifier>]` convention, add `docs/permissions.md`
20. 🟠 🟡 **Telekinesis effect** — drops → inventory enchant/reforge/upgrade; ships as enchant + reforge stone + upgrade scroll
21. 🟠 🟢 **Document `backend.yml` vs `config.yml`** — quick doc note only
22. 🟠 🟢 **Vault provider bridge** — quick adapter, enables third-party plugin compatibility
23. 🟠 🟢 **Vanilla suppression remaining flags** — audit + wire missing handlers
24. 🟠 🟡 **Ability DSL: Target selection effects** — `nearest_enemy{}`, `farthest_enemy{}`, `nearest_ally{}`, `random_enemy{}`, `self{}` each set `ctx.target`; unlocks targeting logic in mob timers + passive procs. Full spec in [Improvements](todo-improvements.md)
25. 🟠 🟡 **Ability DSL: Conditional flow** — `if_health_below{}`, `if_health_above{}`, `if_mana_*{}`, `if_marked{}`, `if_flag{}`, `if_not_flag{}`; same `BLOCKED_KEY` mechanism as `chance{}`; enables phase-transition boss logic. Full spec in [Improvements](todo-improvements.md)
26. 🟠 🟡 **Region enter/exit messages + more flags** — high-value QoL
27. 🟠 ⚫ **Dungeon flesh-out** — entry requirements + loot grants (fix enter bug first)
28. 🟠 🔴 **Stats GUI redesign** — highest-visibility player feature
29. 🔴 🔴 **Achievement system** — player retention + milestone tracking
30. 🔴 🟡 **Leaderboards** — community engagement
31. 🔴 🟡 **Boss bar system** — needed by dungeons + world events
32. 🔴 🟡 **Sign-entry utility** — needed before AH, Bazaar, or Guild Bank GUI
33. 🔴 🔴 **Offline mail / inbox system** — needed before AH and offline achievement rewards
34. 🟠 🟡 **HUD improvements** — scoreboard, tablist, PAPI support, ability cooldowns
35. 🔴 🟡 **PlaceholderAPI support** — integrates with HUD improvements
36. 🟠 🟡 **MagicFind stat implementation** — wire up the `MagicFindAffected` loot pool flag
37. 🟠 🟡 **Economy transaction log** — admin debugging + player history
38. 🟠 🟡 **Item set bonuses**
39. 🟠 🟡 **Fishing content slice**
40. 🟠 🟡 **Quest log GUI + chains + repeatable quests**
41. 🟠 🔴 **Guild bank + rank GUI**
42. 🟠 🟡 **Ability DSL: `spawn_mob{}` effect** — `spawn_mob{id=,count=1,at=caster,radius=0,owned=false}`; `owned=true` tags mob with caster UUID for ally semantics + max-per-caster safety cap in config. Full spec in [Improvements](todo-improvements.md)
43. 🔴 🔴 **Custom enchantment ability triggers** — ability-fire enchants (on_hit, on_kill, etc.)
44. 🟠 🔴 **RPG-Farming redesign**
45. 🔴 🟡 **Elite/champion mob variants**
46. 🟠 🟡 **Mob factions + AI goals** — `Faction:` string tag on mobs; `AiGoals:` list replaces the blunt kind enum with composable targeting rules: `attack_player`, `attack_faction{faction=X}`, `defend_faction{}`, `assist_faction{}`, `flee_from{}`, `call_for_help{}`, `guard_radius{}`, `idle`; faction awareness extends into ability DSL target selection. Full spec in [Improvements](todo-improvements.md)
47. 🟠 🔴 **Mob AI profiles flesh-out** — `ranged_kiter`, `boss`, `flying`; `swarming` + `pack_hunter` now implementable via faction goals (#46). Full spec in [Improvements](todo-improvements.md)
48. 🟠 🔴 **Mob patrol waypoints** — admin-defined walk paths for mobs + NPCs
49. 🔴 🔴 **Ability DSL: Context variables + flags** — Tier 1 (boolean flags: `set_flag{}`, `clear_flag{}`, `if_flag{}`; ship first), Tier 2 (numeric: `increment{}`, `decrement{}`, `if_var_gte{}`); ship Tier 1 first. Full spec in [Improvements](todo-improvements.md)
50. 🔴 ⚫ **World events + world boss**
51. 🔴 🟡 **Salvaging system**
52. 🔴 ⚫ **Auction House** (needs sign-entry + mail first)
53. 🔴 🔴 **Bazaar**
54. 🟠 🔴 **Display entity suite** (`rpg-holograms`) — ItemDisplay, BlockDisplay, physical `/de` editor with inventory replacement, fine-detail GUI, YAML persistence for all types
55. 🔵 🟡 **Party / Guild / Quest GUI conversions**
56. 🔴 ⚫ **Pets system** (`rpg-pets`) — long-term, build in phases
57. 🟠 🟡 **Unit test coverage** — ongoing, add tests alongside any new system
58. 📄 🟢 **Docs pass** — fill stubs, add missing plugin pages
59. 🔵 🟡 **Main Menu Item + GUI** — persistent item locked to hotbar slot 8, right-click opens a hub GUI with buttons to every major player-facing feature; see [GUI Redesigns](todo-gui.md)
60. 🔵 🟢 **GUI Navigation Standard** — all GUIs get a bottom-bar close button; nested GUIs (opened from inside another GUI) also get a back button that returns to the previous screen; see [GUI Redesigns](todo-gui.md)
