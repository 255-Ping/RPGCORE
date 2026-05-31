# 📄 Documentation Work

_Missing pages, stale content, and inconsistencies. No code required — just writing._

> **Difficulty scale:** 🟢 Easy (< 1 day) · 🟡 Medium (1–2 days) · 🔴 Hard (several days) · ⚫ Very Hard (week+)
> All doc work is 🟢 Easy–🟡 Medium since it's writing, not coding.

---

## Missing Plugin Pages

~~All major plugins now have doc pages.~~ ✅

---

## Stale / Incorrect Doc Content

~~All known stale pages fixed in previous session.~~ ✅

---

## Stub Pages (need content written from scratch)

| File | What to write | Difficulty |
|---|---|---|
| `docs/core/abilities.md` | All built-in effect types: parameters table, examples. Updated as new effects are added. Different from `content/abilities.md` (which is the authoring guide) — this is the canonical effect reference. | 🟡 Medium |
| `docs/core/loot-pools.md` | Loot pool YAML reference, attribution modes, `MagicFindAffected` flag. Write alongside the Loot Pool System feature. | 🟢 Easy |

Everything else in the original stub list now has content. Status headers on `installation.md`, `configuration.md`, `resource-pack.md`, and `core/selection-wand.md` updated to Working. ✅

---

## Content Quality — Remaining Items

### Enchanting docs — ability enchant example — 🟢 Easy

`docs/addons/enchanting.md` still needs:

- An **ability enchant** example (enchant that grants or modifies an ability on the item, not just a proc chance)
- A note clarifying that `levels: N: { triggers: [...] }` proc enchants require the Custom Enchantment Ability Triggers feature (item 39 in todo.md) before they'll work

---

### Design intent sections — ✅ Done

Added to `enchanting.md`, `status-effects.md`, `content/abilities.md`.

---

### General Consistency Pass — 🟡 Medium

- The **Changelog** (`docs/changelog.md`) is behind — recent releases aren't all reflected in the main summary.
- Add a `docs/core/loot-pools.md` alongside the Loot Pool System feature (item 7 in todo.md).
- Cross-link related pages more aggressively: e.g., the enchanting page should link to the items page (for `Enchantable: true`), and the stats page should link to the items page (for stat field names).
- `docs/permissions.md` and `docs/commands.md` — master reference pages for the whole suite; currently thin. Flesh out alongside the Permission System Consistency Audit (item 18 in todo.md).

---

## ✅ Completed This Session

- Created `docs/content/quickstart.md` — beginner content creator guide (item + mob + quest in 30 min)
- Created `docs/content/patterns.md` — named ability patterns (fireball, poison-on-hit, life steal, AoE slam, blink, beam, summon, delayed combo)
- Created `docs/content/progression-guide.md` — stat budget guidelines per tier, defense/strength tables, mob design guidelines
- Created `docs/planned/README.md` — folder overview and context note
- Added dependency table to `docs/installation.md`; status updated to Working
- Updated status headers to Working: `configuration.md`, `resource-pack.md`, `core/selection-wand.md`
- Added `/accessories reload` to `docs/addons/accessories.md`
- Added `rpg.chat.mute.bypass`, `rpg.chat.admin.reload`, `/chat reload` to `docs/addons/chat.md`
- Added `/party reload` to `docs/addons/parties.md`
- Fixed `docs/addons/guilds.md` — marked unimplemented commands as planned
- Rewrote `docs/addons/quests.md` commands + permissions to match actual `plugin.yml` (had `/quest give`, `/quest reset`, wrong permission nodes)
- Added Design Intent sections to `enchanting.md`, `core/status-effects.md`, `content/abilities.md`
- Added redirect notice to `docs/planned/backlog.md`
- Updated `mkdocs.yml` nav — added quickstart, patterns, progression-guide; added planned/README.md

---

## ✅ Completed Previous Sessions

- Created `docs/content/cookbook.md` — copy-paste examples for every content type
- Created `docs/content/first-mob.md` — Forest Goblin end-to-end walkthrough
- Created `docs/addons/trade.md`
- Created `docs/addons/admin.md`
- Created `docs/addons/mining.md`
- Created `docs/addons/farming.md`
- Fixed stale status in: `holograms.md`, `dungeons.md`, `enchanting.md`, `hud.md`, `health-display.md`
- Status label standardization — "In progress" → "In Progress" across 21 files
- Added stat scaling formulas to `docs/stats.md`
- Added ability context effect mapping table to `docs/content/abilities.md`
