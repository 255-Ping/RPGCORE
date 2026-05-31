# 📄 Documentation Work

_Missing pages, stale content, and inconsistencies. No code required — just writing._

> **Difficulty scale:** 🟢 Easy (< 1 day) · 🟡 Medium (1–2 days) · 🔴 Hard (several days) · ⚫ Very Hard (week+)
> All doc work is 🟢 Easy–🟡 Medium since it's writing, not coding.

---

## Missing Plugin Pages

These plugins ship in the suite but have no doc page at all:

| Plugin | What needs writing | Difficulty |
|---|---|---|
| `rpg-trade` | Full page: commands, GUI layout, tradeable flag, permission nodes | 🟢 Easy |
| `rpg-admin` | Full page: all admin utility commands (fly, gmc, heal, feed, speed, broadcast, sudo, tp), permission nodes | 🟢 Easy |
| `rpg-mining` | Full page: custom blocks, tools, breaking power, mining fatigue, gathering skill integration | 🟢 Easy |
| `rpg-fishing` | Full page: current state (XP + fortune only), fishing_wisdom stat, planned future scope | 🟢 Easy |
| `rpg-farming` | Full page: current state (XP + fortune only), planned redesign, farming_fortune stat | 🟢 Easy |
| `rpg-accessories` | Full page: bag commands, ACCESSORY item type, stat aggregation, tier upgrade system (when built) | 🟢 Easy |
| `rpg-chat` | Full page: channels (global/party/guild), format config, planned staff channel | 🟢 Easy |

---

## Stale / Incorrect Doc Content

These pages have a `Status:` header that no longer matches the actual code, or describe features that have since changed:

| Page | Problem | Difficulty |
|---|---|---|
| `docs/addons/holograms.md` | Says "Damage indicators only for v1" but `/holograms create|line|move|tp|delete` commands and persistence are all implemented. Status needs updating to reflect that static holograms work; only the GUI editor is still deferred. | 🟢 Easy |
| `docs/addons/dungeons.md` | Status says "shipped" but enter is confirmed broken in testing (see [Bugs](todo-bugs.md)). Also lists `/dungeon edit` as a command when it doesn't exist in code. | 🟢 Easy |
| `docs/addons/enchanting.md` | Status says v0.0.2 but the plugin is now at 0.4.0 with major changes (physical stones/books, anvil GUI redesign, enchant descriptions). Needs a full rewrite of the GUI section. | 🟡 Medium |
| `docs/addons/hud.md` | `{coins}` example in config template shows `"&e{coins} coins"` — was fixed to `"&e{coins}"` but the doc may still show the old format. | 🟢 Easy |
| `docs/core/health-display.md` | Says "stat aggregation from gear/effects pending" — verify whether gear-equipped `max_health` is now being read correctly via `EquipmentListener` and update status accordingly. | 🟢 Easy |

---

## Stub Pages (need content written from scratch)

| File | What to write | Difficulty |
|---|---|---|
| `docs/stats.md` | Full table of every built-in stat: id, display name, group, description of effect, which systems read it. **Must note which stats are currently unimplemented** (speed, ferocity, swing_range, pristine — see [Bugs](todo-bugs.md)) so admins know not to put them on items yet. | 🟡 Medium |
| `docs/installation.md` | Step-by-step: download jars, place in plugins/, LuckPerms requirement, first-run checklist | 🟢 Easy |
| `docs/configuration.md` | All config.yml files across the suite, key options in each, cross-references | 🟡 Medium |
| `docs/resource-pack.md` | CustomModelData range conventions, how to assign CMD ranges per plugin, how to reference in item YAML | 🟢 Easy |
| `docs/core/selection-wand.md` | How to get the wand (`/rpg wand`), modes (block, region, dungeon), left/right click behaviour per mode | 🟢 Easy |
| `docs/core/status-effects.md` | Catalog of all built-in status effect IDs, what `Level` controls for each, duration behaviour. Should be written alongside or after the Status Effects catalog improvement. | 🟢 Easy |
| `docs/core/abilities.md` | All built-in effect types: parameters table, examples. Updated as new effects are added. | 🟡 Medium |
| `docs/core/loot-pools.md` | Loot pool YAML reference, attribution modes, `MagicFindAffected` flag. Write alongside the Loot Pool System feature. | 🟢 Easy |

---

## General Consistency Pass — 🟡 Medium

- Every plugin doc page should have a **Commands** table and a **Permissions** table that matches `plugin.yml` exactly. Several pages (guilds, parties, quests) have incomplete or slightly mismatched permission nodes.
- The **Changelog** (`docs/changelog.md`) is behind — recent releases aren't all reflected in the main summary.
- **`docs/planned/backlog.md` is superseded** — delete it and add a redirect at the top: `_This file has been replaced by [todo.md](todo.md)._` to avoid confusing anyone who has a direct link to it.
- Add a `docs/planned/README.md` (or index comment at the top of `todo.md`) explaining the planned/ folder is a living working document, not shipped design specs — it changes as work is done.
- Cross-link related pages more aggressively: e.g., the enchanting page should link to the items page (for `Enchantable: true`), and the stats page should link to the items page (for stat field names).

---
