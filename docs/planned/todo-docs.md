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

## Content Quality — From External Doc Audit

Issues identified by an external review of the docs from a content-creator's perspective. Nothing blocking, but each one reduces guesswork for admins authoring content.

### Ability context effect mapping table — 🟢 Easy

`docs/content/abilities.md` already documents what `AbilityContext` contains (caster, target, point, carriedDamage, bag). What's missing is a **summary table** showing which effects *read* which fields and which effects *set* which fields. Without it, admins have to hunt through individual effect descriptions to answer "where does `particle{}` fire — at caster, target, or impact point?"

Add a table to `abilities.md` immediately after the context fields list:

| Effect | Reads from context | Writes to context |
|---|---|---|
| `beam` | `caster` location + look direction | `target` (first hit entity), `point` (endpoint) |
| `projectile` | `caster` location + look direction | `target`, `point` (on impact) |
| `explode` | `point` (fallback: caster) | — |
| `aoe` | `caster` location | — |
| `damage` | `target`, `carriedDamage` (if `amount` omitted) | — |
| `heal` | `target` (default: caster) | — |
| `particles` | `point` (fallback: caster) | — |
| `apply_status` | `target` (default: caster) | — |
| `teleport` | `caster` | caster location changes |
| `summon` | `caster` location | — |

---

### Stat scaling formulas — 🟡 Medium

Content creators don't know how much a stat *matters* — is `strength: 10` weak or strong? Is `crit_chance: 25` near the cap? Add a **Formulas** section to `docs/stats.md` (or a new `docs/core/formulas.md`) documenting:

- How `strength` converts to bonus damage (e.g. `+1% damage per point`? flat add?)
- How `defense` converts to damage reduction (is it a flat reduction? diminishing returns formula?)
- What the effective cap is for `crit_chance` (100% per the table, but what happens at 100%?)
- How `intelligence` scales `ability_damage` and `max_mana` (the doc says "configurable scaling" — what's the default?)
- A worked example: a player with `strength: 50` wielding a `damage: 100` sword — what do they deal?

Without this, admins either under-tune (everything feels weak) or massively over-tune.

---

### Progression targets / stat budget guidelines — 🟡 Medium

A companion to the formulas page. Admins building their first server need to know what "normal" looks like at each level range. Add a `docs/content/progression-guide.md` or a section in `admin-guide.md`:

- Example stat budgets per tier (Tier 1 / Tier 2 / Tier 3 gear)
- Expected player HP / damage range at each tier
- How fast mobs at each level should die
- What a "fair" boss fight looks like in terms of HP pool vs player DPS

Even rough guidelines prevent the common mistake of shipping gear that trivialises all mobs.

---

### Addon dependency diagram — 🟢 Easy

Admins don't know which plugins require which. Add a `docs/installation.md` section (or a diagram in `docs/README.md`) with a dependency table:

| Plugin | Hard requires | Soft requires |
|---|---|---|
| `rpg-core` | LuckPerms | — |
| `rpg-enchanting` | `rpg-core` | — |
| `rpg-alchemy` | `rpg-core` | — |
| `rpg-cooking` | `rpg-core` | — |
| `rpg-dungeons` | `rpg-core`, `rpg-regions` | `rpg-holograms` |
| `rpg-quests` | `rpg-core` | `rpg-economy` |
| `rpg-guilds` | `rpg-core` | `rpg-economy`, `rpg-parties` |
| `rpg-fishing` | `rpg-core` | `rpg-skills` |
| ... | | |

Fill in the actual values from `plugin.yml` files.

---

### Example gallery / cookbook — 🟡 Medium

The single highest-impact doc addition. Add `docs/content/cookbook.md` with one complete, copy-pasteable example of each content type:

- Starter sword (SWORD)
- Starter bow (BOW) with ammo
- Wand with ability
- Full armor set (HELMET / CHEST / LEGS / BOOTS)
- Consumable item
- Upgrade book
- Custom mob with drops + ability
- Simple status effect
- Stat enchant + proc enchant side-by-side
- Reforge stone set (offensive / defensive / caster)
- Custom ability sequence (projectile → damage)

No new concepts — just clean, well-commented YAML that admins can drop in and immediately test. Every first-time admin will use this page before reading anything else.

---

### Cross-system end-to-end walkthrough — 🟡 Medium

Add `docs/content/first-dungeon-mob.md` (or similar name). One page, no concepts — just a complete example touching every system:

```
1. Create ability: goblin_poison_strike (apply_status on_hit)
2. Create item: goblin_dagger (SWORD, Stats, Abilities)
3. Create mob: forest_goblin (equipment: goblin_dagger, ability: goblin_poison_strike)
4. Create loot table with goblin_ear drop
5. Create spawner for the mob
6. Create quest: "Kill 10 Forest Goblins"
```

Inline YAML for each step. This answers the "how do these systems connect?" question that individual system docs can't answer.

---

### Enchanting docs — more examples, four-types table — 🟢 Easy

`docs/addons/enchanting.md` now has stat + proc + conflict examples (added in accuracy pass above). Still missing:

- An **ability enchant** example (enchant that grants or modifies an ability on the item, not just a proc chance)
- The **four enchant types table** explanation (stat / proc / ability / utility) — added in accuracy pass, but verify it's clear
- A note explaining that `levels: N: { triggers: [...] }` requires the Ability Trigger Types feature to be built first (it's in the todo — don't let admins write proc enchants expecting them to work before that feature ships)

---

### Feature status label standardization — 🟢 Easy

Pages use inconsistent status strings: `Working`, `Shipped`, `In Progress`, `In progress`, `v0.0.2 — shipped`, `Planned`. Standardize to four values across all pages:

| Label | Meaning |
|---|---|
| `Working` | Fully implemented and stable |
| `In Progress` | Partially implemented — some things work, others are deferred |
| `Planned` | Not started — documented for design reference only |
| `Deprecated` | Removed or replaced; page kept for reference |

Do a grep for `Status:` across all doc pages and fix any that don't match. This is a half-hour job.

---

### Design intent section per system — 🟢 Easy

Content creators (and future contributors) don't understand *why* the system boundaries exist. Add a short **Design Intent** paragraph near the top of:

- `enchanting.md` — why are reforges, enchants, and upgrades three separate things? (Enchants are tiered progressions; reforges are identity modifiers — one per item; upgrades are consumable stacks that compound.)
- `status-effects.md` — why not just use abilities for everything? (Effects have duration, stacking rules, and persistent stat modifiers that abilities don't model cleanly.)
- `abilities.md` — why a sequence model instead of a single effect? (Composability — you can build complex behaviors from simple primitives without writing Java.)

Two or three sentences each. Prevents constant "why does this work this way?" questions.

---

### Common patterns page — 🟢 Easy

Add `docs/content/patterns.md` — a short page of named recipes showing how to combine primitives to get common RPG mechanics:

| Pattern | How |
|---|---|
| **Life steal** | SWORD + ability `damage{} → heal{target=caster, amount=<X>}` or `lifesteal` stat |
| **Fireball** | ability `projectile{} → explode{}` |
| **Poison on hit** | mob/item ability `apply_status{id=poison} ~onHit` |
| **Boss phase change** | mob abilities with `~onHurt` + health check in ability logic (planned — not yet wired) |
| **AoE slam** | ability `aoe{radius=5, damage_multiplier=1.0}` + `particles{type=EXPLOSION_NORMAL}` |
| **Blink / dash** | ability `teleport{distance=8, mode=eyeline}` |
| **Summon minions** | ability `summon{mob=shadow_totem, count=3, lifetime=600}` |

One-line pattern each with the exact DSL. Fast reference for content creators.

---

### Content creator quick-start guide — 🟡 Medium

The existing docs assume the reader is either a developer or an experienced admin. Add `docs/content/quickstart.md` aimed at someone who just wants to create content (weapons, mobs, quests) without touching Java:

1. Install the suite (link to `installation.md`)
2. Create your first item (minimum required fields, give it to yourself)
3. Create your first mob (minimum required fields, spawn it with a command)
4. Give the mob an ability
5. Give the mob drops
6. Create a spawner so it appears in the world
7. Create a quest that tracks killing it

Five minutes of work per step. Links to the full schema docs for reference. This page becomes the "where do I start?" answer.

---

## General Consistency Pass — 🟡 Medium

- Every plugin doc page should have a **Commands** table and a **Permissions** table that matches `plugin.yml` exactly. Several pages (guilds, parties, quests) have incomplete or slightly mismatched permission nodes.
- The **Changelog** (`docs/changelog.md`) is behind — recent releases aren't all reflected in the main summary.
- **`docs/planned/backlog.md` is superseded** — delete it and add a redirect at the top: `_This file has been replaced by [todo.md](todo.md)._` to avoid confusing anyone who has a direct link to it.
- Add a `docs/planned/README.md` (or index comment at the top of `todo.md`) explaining the planned/ folder is a living working document, not shipped design specs — it changes as work is done.
- Cross-link related pages more aggressively: e.g., the enchanting page should link to the items page (for `Enchantable: true`), and the stats page should link to the items page (for stat field names).

---
