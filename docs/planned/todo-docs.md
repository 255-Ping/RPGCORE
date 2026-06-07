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
| ~~`docs/core/abilities.md`~~ → created as `docs/content/ability-effects.md` | ✅ Done — full per-effect reference with parameters + context reads/writes + examples | |
| ~~`docs/core/loot-pools.md`~~ → exists as `docs/content/loot-pools.md` | ✅ Done — full loot pool YAML reference, attribution modes, XP rewards, mob references, reload command, examples | |

Everything else in the original stub list now has content. Status headers on `installation.md`, `configuration.md`, `resource-pack.md`, and `core/selection-wand.md` updated to Working. ✅

---

## Content Quality — Remaining Items

### ~~Enchanting docs — ability enchant example~~ ✅ Done

Added ability enchant example YAML block to `docs/addons/enchanting.md` + prominent ⚠️ callout on proc enchants noting they require the Custom Enchantment Ability Triggers feature.

---

### Design intent sections — ✅ Done

Added to `enchanting.md`, `status-effects.md`, `content/abilities.md`.

---

### ~~General Consistency Pass~~ ✅ Done (this session)

- Changelog summary table updated — Suite 19 highlights now accurate (trade plugin, cooldown fix, currency drops, GUI pass, config docs).
- `docs/commands.md` — status fixed to Working; quests, enchanting, NPC, chat sections rewritten to match actual plugin.ymls; trade section added; accessories/parties reload commands added.
- `docs/permissions.md` — status fixed; quests, enchanting, NPC, chat permissions rewritten from plugin.ymls; trade section added; fake permissions (chat.mute, chat.unmute, chat.slowmode, chat.socialspy, chat.use.staff, enchanting.open, enchanting.reforge, enchanting.anvil, npcs.admin.edit, npcs.admin.tp, quests.open, quests.admin.give, quests.admin.reset) removed.

### Still open

- ~~Add a `docs/core/loot-pools.md` alongside the Loot Pool System feature (item 7 in todo.md).~~ ✅ Exists at `docs/content/loot-pools.md`.
- Cross-link related pages more aggressively (e.g. enchanting ↔ items for `Enchantable: true`; stats ↔ items for stat field names).
- `docs/permissions.md` and `docs/commands.md` — flesh out further alongside Permission System Consistency Audit (item 18 in todo.md).

---

## ✅ Completed This Session

- Created `docs/content/ability-effects.md` — dedicated built-in effects reference: per-effect parameter tables, context reads/writes, examples
- Expanded `AbilityContext` section in `docs/content/abilities.md` — full field table with types/mutability/nullability, flow diagram, and null-handling summary
- Created `docs/content/learning-path.md` — ordered content creator reading path (Quick Start → Items → Abilities → Status Effects → Mobs → Loot Tables → Spawning → Quests → Patterns → Cookbook)
- Added Events section to `docs/development.md` — all 9 API events with code examples (`PreDamageEvent`, `PostDamageEvent`, `StatRecalcEvent`, `SkillXpAwardEvent`, `SkillLevelUpEvent`, `RpgBlockBreakEvent`, `CombatTagEvent`, `RegionEnterEvent`, `RegionLeaveEvent`)
- Added Extension Points section to `docs/development.md` — how to implement `AbilityEffect` and `StatusEffect` with registration examples
- Updated `mkdocs.yml` nav — added Learning Path and Effects Reference to Content Creation
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
