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
2. 🔵 **GUI redesigns** (brewing/cooking/enchanting pagination + layouts) — high visibility
3. 🟠 **Loot pool system** — needed by mobs, dungeons, and enchanting XP all at once
4. 🟠 **Enchanting: Minecraft XP cost** — requires loot pool XP drops first
5. 🟠 **Timed cooking + brewing** — QoL improvement, self-contained
6. 🟠 **Mob death animation** — polish, self-contained
7. 🟠 **Damage indicators: float down + shrink** — polish, self-contained
8. 🟠 **Dungeon flesh-out** — entry requirements + loot grants
9. 🟠 **Stats GUI redesign** — highest-visibility player feature
10. 🔴 **Sign-entry utility** — needed before AH, Bazaar, or Guild Bank GUI
11. 🟠 **HUD improvements** — scoreboard, tablist, PAPI support
12. 🟠 **Fishing content slice**
13. 🟠 **Quest log GUI**
14. 🟠 **Guild bank + rank GUI**
15. 🟠 **RPG-Farming redesign**
16. 🔴 **Auction House** (needs sign-entry first)
17. 🔴 **Bazaar**
18. 🔵 **Party / Guild / Quest GUI conversions**
19. 📄 **Docs pass** — fill stubs, add missing plugin pages
