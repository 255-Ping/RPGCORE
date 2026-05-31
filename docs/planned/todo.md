# RPGCORE — Master Todo

> This is the single reference point before starting any session. Every known bug, missing feature, improvement, GUI redesign, and doc gap lives here or in one of the linked sub-pages below. Update this when things get done or new issues are found.

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

---

## Suggested Priority Order

1. 🐛 **Fix all confirmed bugs first** — see [Bugs](todo-bugs.md)
2. 🐛 **Wire up silent stats** (speed, ferocity, swing_range) — showing stats that do nothing is misleading
3. 🟠 **NPC command overhaul + in-game editing** — per-NPC entity type, style/skin commands, dialogue/shop editing in-game
3. 🔵 **GUI redesigns** (brewing/cooking/enchanting pagination + layouts) — high visibility
4. 🟠 **New built-in ability effects** — unlocks all the interesting new ability content
5. 🟠 **Expand example mobs, abilities, items** — depends on new effects
6. 🟠 **Loot pool system** — needed by mobs, dungeons, and enchanting XP all at once
7. 🟠 **Enchanting: Minecraft XP cost** — requires loot pool XP drops first
8. 🟠 **Timed cooking + brewing** — QoL, self-contained
9. 🟠 **Mob death animation** — polish, self-contained
10. 🟠 **Damage indicators: float down + shrink** — polish, self-contained
11. 🔴 **Player homes + warps** — expected baseline for any server
12. 🔴 **Starter kits** — new player experience
13. 🔴 **Extract smelting + crafting to own plugins** — cleanup, low risk
14. 🟠 **Document `backend.yml` vs `config.yml`** — quick doc note only
15. 🟠 **Region enter/exit messages + more flags** — high-value QoL
16. 🟠 **Dungeon flesh-out** — entry requirements + loot grants
17. 🟠 **Stats GUI redesign** — highest-visibility player feature
18. 🔴 **Achievement system** — player retention + milestone tracking
19. 🔴 **Leaderboards** — community engagement
20. 🔴 **Boss bar system** — needed by dungeons + world events
21. 🔴 **Sign-entry utility** — needed before AH, Bazaar, or Guild Bank GUI
22. 🟠 **HUD improvements** — scoreboard, tablist, PAPI support, ability cooldowns
23. 🟠 **Item set bonuses**
24. 🟠 **Fishing content slice**
25. 🟠 **Quest log GUI + chains + repeatable quests**
26. 🟠 **Guild bank + rank GUI**
27. 🟠 **RPG-Farming redesign**
28. 🔴 **Elite/champion mob variants**
29. 🔴 **World events + world boss**
30. 🔴 **Salvaging system**
31. 🔴 **Auction House** (needs sign-entry first)
32. 🔴 **Bazaar**
33. 🔵 **Party / Guild / Quest GUI conversions**
34. 🟠 **Unit test coverage** — ongoing, add tests alongside any new system
35. 📄 **Docs pass** — fill stubs, add missing plugin pages
