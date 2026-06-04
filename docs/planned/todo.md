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

---

## Suggested Priority Order

1. 🐛 🟡 **Fix all confirmed bugs first** — see [Bugs](todo-bugs.md)
2. ✅ **Wire up silent stats** (speed, ferocity, swing_range) — already implemented
3. ✅ **NPC command overhaul + in-game editing** — per-NPC entity type (`/npc setentitytype`), style/skin commands, dialogue/shop in-game editing, look-at-player task, `/npc info` (`rpg-npcs 0.6.0`)
4. ✅ **GUI redesigns** (brewing/cooking/enchanting pagination + layouts, nav bar standard on all GUIs)
5. 🟠 🔴 **New built-in ability effects** — unlocks all the interesting new ability content
6. 🟠 🟡 **Expand example mobs, abilities, items** — depends on new effects
7. 🟠 🔴 **Loot pool system** — needed by mobs, dungeons, and enchanting XP all at once
8. 🟠 🟡 **Enchanting: Minecraft XP cost** — requires loot pool XP drops first
9. 🟠 🟡 **Ability trigger types expansion** (`~onAttack`, `~onKill`, `~onBlock`, `~onJump`) — small but unlocks good mob/item designs
10. 🟠 🟡 **Timed cooking + brewing** — QoL, self-contained
11. 🟠 🟡 **Mob death animation** — polish, self-contained
12. 🟠 🟢 **Damage indicators: float down + shrink** — polish, self-contained
13. 🔴 🟢 **Player homes + warps** — expected baseline for any server
14. 🔴 🟢 **Starter kits** — new player experience
15. 🔴 🟢 **Resource pack auto-delivery** — practical baseline, 30-minute job
16. 🔴 🟢 **Extract smelting + crafting to own plugins** — cleanup, low risk; build timed crafting into `rpg-smelting` from day one
17. 🟠 🟡 **Timed smelting** — same CraftTime + DataStore persistence model as cooking/brewing; goes in `rpg-smelting`
18. 🟠 🟡 **Permission system consistency audit** — every command gets a permission, all nodes follow `rpg.<plugin>.<verb>[.<qualifier>]` convention, add `docs/permissions.md`
19. 🟠 🟡 **Telekinesis effect** — drops → inventory enchant/reforge/upgrade; ships as enchant + reforge stone + upgrade scroll
20. 🟠 🟢 **Document `backend.yml` vs `config.yml`** — quick doc note only
21. 🟠 🟢 **Vault provider bridge** — quick adapter, enables third-party plugin compatibility
22. 🟠 🟢 **Vanilla suppression remaining flags** — audit + wire missing handlers
23. 🟠 🟡 **Region enter/exit messages + more flags** — high-value QoL
24. 🟠 ⚫ **Dungeon flesh-out** — entry requirements + loot grants (fix enter bug first)
25. 🟠 🔴 **Stats GUI redesign** — highest-visibility player feature
26. 🔴 🔴 **Achievement system** — player retention + milestone tracking
27. 🔴 🟡 **Leaderboards** — community engagement
28. 🔴 🟡 **Boss bar system** — needed by dungeons + world events
29. 🔴 🟡 **Sign-entry utility** — needed before AH, Bazaar, or Guild Bank GUI
30. 🔴 🔴 **Offline mail / inbox system** — needed before AH and offline achievement rewards
31. 🟠 🟡 **HUD improvements** — scoreboard, tablist, PAPI support, ability cooldowns
32. 🔴 🟡 **PlaceholderAPI support** — integrates with HUD improvements
33. 🟠 🟡 **MagicFind stat implementation** — wire up the `MagicFindAffected` loot pool flag
34. 🟠 🟡 **Economy transaction log** — admin debugging + player history
35. 🟠 🟡 **Item set bonuses**
36. 🟠 🟡 **Fishing content slice**
37. 🟠 🟡 **Quest log GUI + chains + repeatable quests**
38. 🟠 🔴 **Guild bank + rank GUI**
39. 🔴 🔴 **Custom enchantment ability triggers** — ability-fire enchants (on_hit, on_kill, etc.)
40. 🟠 🔴 **RPG-Farming redesign**
41. 🔴 🟡 **Elite/champion mob variants**
42. 🟠 🔴 **Mob AI profiles flesh-out** (ranged_kiter, boss, swarming, pack_hunter)
43. 🟠 🔴 **Mob patrol waypoints** — admin-defined walk paths for mobs + NPCs
44. 🔴 ⚫ **World events + world boss**
45. 🔴 🟡 **Salvaging system**
46. 🔴 ⚫ **Auction House** (needs sign-entry + mail first)
47. 🔴 🔴 **Bazaar**
48. 🟠 🔴 **Display entity suite** (`rpg-holograms`) — ItemDisplay, BlockDisplay, physical `/de` editor with inventory replacement, fine-detail GUI, YAML persistence for all types
49. 🔵 🟡 **Party / Guild / Quest GUI conversions**
50. 🔴 ⚫ **Pets system** (`rpg-pets`) — long-term, build in phases
51. 🟠 🟡 **Unit test coverage** — ongoing, add tests alongside any new system
52. 📄 🟢 **Docs pass** — fill stubs, add missing plugin pages
53. 🔵 🟡 **Main Menu Item + GUI** — persistent item locked to hotbar slot 8, right-click opens a hub GUI with buttons to every major player-facing feature; see [GUI Redesigns](todo-gui.md)
54. 🔵 🟢 **GUI Navigation Standard** — all GUIs get a bottom-bar close button; nested GUIs (opened from inside another GUI) also get a back button that returns to the previous screen; see [GUI Redesigns](todo-gui.md)
