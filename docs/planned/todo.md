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
2. 🔵 **GUI redesigns** (brewing, cooking, enchanting pagination) — high visibility
3. 🟠 **Dungeon flesh-out** — entry requirements + loot grants
4. 🟠 **Stats GUI redesign** — highest-visibility player feature
5. 🔴 **Sign-entry utility** — needed before AH, Bazaar, or Guild Bank GUI
6. 🟠 **HUD improvements** — scoreboard, tablist, PAPI support
7. 🟠 **Fishing content slice**
8. 🟠 **Quest log GUI**
9. 🟠 **Guild bank + rank GUI**
10. 🟠 **RPG-Farming redesign**
11. 🔴 **Auction House** (needs sign-entry first)
12. 🔴 **Bazaar**
13. 🔵 **Party / Guild / Quest GUI conversions**
14. 📄 **Docs pass** — fill stubs, add missing plugin pages
