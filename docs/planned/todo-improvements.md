# 🟠 Improvements — In Progress / Major Missing Chunks

_These systems exist and partially work, but have significant gaps._

---

### Expand Example Content — Mobs, Abilities, and Items (`rpg-core`)
The current example files are thin: 3 abilities, 2 mobs, and items that don't cover many interaction patterns. Need a richer out-of-the-box set so new server owners can see the full range of the system.

**New abilities to add (`abilities/example.yml`):**
- `fireball_barrage` — fire 3 delayed fireballs in a spread (beam × 3 with short delays between)
- `death_nova` — on-death explosion that damages all nearby players (uses `~onDeath` trigger)
- `enrage` — applies a self-buff status effect on hurt, making the mob hit harder when low HP (`~onHurt`)
- `ground_slam` — melee AoE: particles + explode in radius around caster, knocking targets back
- `soul_drain` — beam that deals damage and heals the caster for a % of damage dealt (beam + heal chained)
- `warp_strike` — teleports caster to target then immediately damages (needs `teleport` effect — see new effects below)
- `chain_lightning` — chains damage to multiple nearby targets (needs `chain` effect — see below)
- `zone_of_pain` — creates a lingering zone that pulses a status effect on everything inside (needs `zone` effect)
- `player_shield` — absorbs incoming damage for a short window (needs `shield` effect)
- `blink_forward` — short-range dash in look direction (needs `blink` effect)

**New mobs to add (`mobs/example.yml`):**
- `forest_sprite` — passive mob, flees combat, drops rare herbs; demonstrates `passive` AI + `~onHurt` flee ability
- `goblin` — fast, low-HP melee mob; uses `aggressive` AI, enrage ability `~onHurt`, small loot table
- `skeleton_archer` — ranged mob (skeleton base); uses `~onTimer` ranged ability, moderate HP
- `cave_troll` — tank mob (iron_golem base); high HP/armor, `ground_slam ~onTimer:60`, slow movement, rich loot
- `corrupted_mage` — caster mob (witch base); uses `soul_drain ~onTimer:40`, `chain_lightning ~onTimer:80`, mana-themed
- `dungeon_boss` — showcase boss: multiple abilities on different timers, `death_nova ~onDeath`, large loot pool, named with health bar

**New items to add (`items/example.yml`):**
- `goblin_fang` — material drop from goblin, used in recipes
- `troll_hide` — material drop from cave_troll
- `void_crystal` — epic material, rare drop, used in high-tier crafting
- `mages_robes` (4-piece set) — caster armor set demonstrating full intelligence/mana stats
- `shadow_dagger` — fast sword (low AttackCooldown), high crit stats, demonstrates ferocity
- `berserker_axe` — AXE type weapon, demonstrates strength + ferocity, uses `ground_slam` ability
- `soul_staff` — WAND using `soul_drain` ability, lifesteal stats
- `rangers_shortbow` — lighter bow, faster fire rate, `ammo_usage_reduction`, demonstrates BOW type fully
- `mana_potion` — CONSUMABLE that restores mana (new `restore_mana` OnConsume effect)
- `stamina_crystal` — ACCESSORY with `health_regen` + `mana_regen` + `vitality`

---

### New Built-in Ability Effects (`rpg-core`)
The current effect set (`damage`, `heal`, `beam`, `explode`, `particles`, `sound`, `delay`, `apply_status`, `mana_cost`, `cooldown`) covers the basics but needs more building blocks for interesting abilities. Proposed additions:

| Effect | Parameters | Description |
|---|---|---|
| `knockback` | `force=`, `direction=away/toward/up` | Push or pull the target. `away` = repel from caster, `toward` = pull in, `up` = launch upward. Works on both player→mob and mob→player. |
| `teleport` | `mode=to_target/behind_target/random_near`, `distance=` | Teleport the caster. `to_target` = land on top of target, `behind_target` = appear behind, `random_near` = random point within `distance`. |
| `blink` | `distance=` | Teleport the caster forward in their look direction by up to `distance` blocks, stopping at the first solid block. |
| `chain` | `targets=`, `range=`, `damage_multiplier=`, `particle=` | Bounce a damage hit to up to `targets` additional entities within `range` of each successive target. Damage decays per bounce. |
| `zone` | `radius=`, `duration_ticks=`, `interval_ticks=`, `effect_id=`, `effect_level=` | Spawn a persistent zone at the cast location. Every `interval_ticks`, applies `effect_id` at `effect_level` to all entities inside the radius. Despawns after `duration_ticks`. |
| `shield` | `amount=`, `duration_ticks=`, `target=caster/target` | Apply a damage-absorb shield that blocks up to `amount` HP of incoming damage, expiring after `duration_ticks`. |
| `drain` | `amount=`, `steal_percent=`, `target=caster/target` | Deal `amount` damage to the target and heal the caster for `steal_percent`% of the damage dealt. Stacks with `lifesteal` stat. |
| `mark` | `duration_ticks=`, `damage_amplify=` | Mark the target; all damage they receive is multiplied by `damage_amplify` while the mark is active. Visual: a particle ring around the target. |
| `restore_mana` | `amount=`, `target=caster/target` | Restore `amount` mana to caster or target. Counterpart to `mana_cost`. |
| `launch` | `force=`, `direction=up/away/toward`, `target=caster/target` | Apply velocity to caster or target. Softer than `knockback` — suitable for mobility abilities rather than combat disruption. |
| `freeze` | `duration_ticks=`, `target=caster/target` | Severely slow the target (apply a high-amplifier slowness + mining fatigue equivalent). Not the same as `apply_status slow` — `freeze` is much stronger and visually distinct (ice particle burst). |

---

### Consolidate `backend.yml` + `config.yml` Persistence Setting (`rpg-core`)
**Why there are two files — document this clearly:**
- `config.yml → persistence.backend` is the **admin's desired setting** (what the server owner configured).
- `backend.yml` is a **runtime state file written by `BackendMigrator`** at startup. It records which backend was actually active last session so the migrator can detect a YAML↔MySQL switch and auto-migrate data before anything reads it.

They intentionally serve different purposes and must stay separate. The risk of merging them is that if `config.yml` were both the setting AND the last-active record, a partial migration crash would corrupt the desired setting.

**Action:** Add a comment block near the `persistence:` section in `config.yml` explaining this, and add a similar comment at the top of `backend.yml` when it is first generated. Also document it in `docs/core/persistence.md` so admins don't think it's a bug or duplicate.

---

### Timed Cooking + Brewing with Persistent Progress (`rpg-cooking` / `rpg-alchemy`)
Currently recipes complete instantly when the player clicks the output slot. Add configurable craft time:

- Each recipe YAML gains an optional `CraftTime` field (in seconds; 0 or absent = instant, same as now)
- When a player starts a recipe in the GUI a progress bar fills over the configured duration
- **If the player closes the GUI mid-craft**, the in-progress state is saved to `DataStore` per-player per-station-block: which recipe is being crafted, how much time has elapsed, and the ingredients that were consumed
- **When the player reopens that station GUI**, it restores the in-progress state — shows the recipe filling up from where it left off (not restarting)
- On completion the output appears in the output slot as normal; the persisted state is cleared
- Applies to both cooking stations (`rpg-cooking`) and brewing stations (`rpg-alchemy`)

---

### Enchanting: Costs Minecraft XP (`rpg-enchanting`)
Currently enchanting costs in-game currency only. Add Minecraft (vanilla) XP cost:

- Each enchant YAML gains an optional `XpCost` field (levels or points; admins choose unit in config)
- The cost is deducted from the player's vanilla XP bar on apply; if they don't have enough the apply is blocked with a message
- **Mob XP drops** — admins should be able to configure how much vanilla XP custom mobs drop in the mob YAML (separate from skill XP). This feeds the pool players spend on enchanting.
- **Loot pool XP** — loot pool entries (see below) should also support an `Exp` field that drops vanilla XP orbs

---

### Loot Pool System (`rpg-core`)
Admins need a way to define reusable named loot pools and assign them to mobs (and dungeons, chests, etc.) rather than embedding loot inline everywhere. Design:

- New content folder: `plugins/rpg-core/loot-pools/<file>.yml`
- Each pool has a list of entries, each with:
  - `Item` — custom item id or vanilla material
  - `Chance` — drop chance (0.0–100.0)
  - `Amount` / `MinAmount` / `MaxAmount` — quantity range
  - `Exp` — vanilla XP to drop (orbs) on this entry rolling
  - `CombatExp` — skill XP to award to the killer's combat skill
  - `MagicFindAffected` — boolean, scales chance by killer's `MAGIC_FIND` stat
- Pools are referenced by id from mob YAML (`LootPool: my_pool_id`), dungeon loot chest config, etc.
- Multiple pools can be assigned to one mob (all roll independently)
- This is an extension of / replacement for the current inline loot table system — external `LootTable: <id>` references that currently don't work (see [Improvements — Loot Tables](todo-improvements.md)) should be consolidated into this

---

### Damage Indicators: Float Down + Shrink (`rpg-holograms`)
Current behaviour: damage numbers appear and stay in place until their duration expires.

New behaviour:
- Numbers **float downward** (not upward) over their lifetime
- Numbers **scale down** (shrink) continuously as they age via `TextDisplay` transformation
- When they reach minimum scale (configurable `min-scale` in config), they are removed immediately rather than waiting for `duration-ticks`
- All motion/scale parameters should be configurable: `float-speed`, `start-scale`, `min-scale`, `duration-ticks`

---

### Mob Death Animation (`rpg-core`)
Currently mobs play Minecraft's default death animation (fall to side, then despawn). Replace this with a custom death sequence:

- When a custom mob's HP reaches 0, **cancel the vanilla death animation** (remove the entity before it can play the fall)
- Spawn configured **particles** at the death location (admin-configurable particle type, count, spread)
- Play a configured **sound** at the death location (admin-configurable sound, volume, pitch)
- Both `Particles` and `DeathSound` fields go on the mob YAML definition
- Loot still drops as normal (triggered by the RPG death event, not the vanilla entity death)
- Example in `mobs/example.yml` should demonstrate both fields

---

### Dungeon System Flesh-out (`rpg-dungeons`)
> ⚠️ Fix the enter bug (see [Bugs](todo-bugs.md)) before working on anything below.

1. **Entry requirements not enforced** — `DungeonDef.requiredLevel`, item consumption on entry, currency cost, and party-size min/max are stored in YAML but `DungeonManager.enter()` never checks them.
2. **Per-player loot grants on completion** — `finishInstance()` evicts players without ever rolling the loot pool. Players leave with nothing.
3. **Dungeon editor GUI** — `/dungeon edit <id>` is described in docs but the command doesn't exist. Currently admins hand-edit YAML.
4. **Time limits** — no timer in `DungeonInstance`; no eviction when time expires.
5. **Composite win conditions** — only `KILL_ALL_MOBS` and `REACH_EXIT_BLOCK` work. `ADMIN_END` does nothing.

---

### Stats GUI Redesign (`rpg-core`)
Currently `/stats` prints a chat dump. Planned inventory GUI:
- 4 armor slots + main-hand slot showing actual worn items
- Companion/pet slot placeholder
- Accessories count from `rpg-accessories`
- Stats grouped into categories on named items (Combat, Gathering, Economy, etc.)
- "Send trade request" button → fires trade invite
- "View auctions" button → filtered AH view (blocked until AH is built)

---

### HUD: Scoreboard + Tablist Improvements (`rpg-hud`)
Several improvements needed:

**Scoreboard:**
- Show more useful info by default: player name, online player count, party member list + status, recently gained skill XP (last skill + amount, fades after a few seconds)
- All lines should be fully configurable as placeholder templates (RPGCORE `{placeholders}` + PAPI `%placeholders%` once PAPI support lands)

**Tablist header:**
- Show server TPS, RAM usage, and player ping
- All lines configurable as placeholder templates

**Tablist footer:**
- Show the player's currently active status effects (effect name + level + remaining duration)
- All lines configurable as placeholder templates

---

### Knockback on All Weapons + Wands (`rpg-core` / `rpg-combat`)
Currently arrows do no knockback, and most example items have no `knockback` stat defined. Fix:
- Ensure melee attacks (swords), ranged attacks (bows/arrows), and wand impacts all apply knockback proportional to the item's `knockback` stat
- Add a `knockback` stat entry to every example sword, bow, and wand in the default item YAML files so the behaviour is demonstrated out of the box

---

### RPG-Farming Redesign (`rpg-farming`)
Current state: XP for breaking vanilla crops + FARMING_FORTUNE drop multiplier only.

Planned redesign (mirrors the custom blocks system):
- Admins assign world blocks to custom farming block types (like `/rpg block convert`)
- Custom farming blocks cycle through visual growth stages
- Not breakable until fully grown (cancel + `§cNot ready` action bar message)
- Growth time configurable per crop type in `config.yml`
- Breaking a fully-grown crop drops configured loot + restarts the growth cycle
- Requires `DataStore` persistence for per-block growth timers (like `BlockPersistence` in rpg-core)

---

### Guild System Flesh-out (`rpg-guilds`)
Current: create / invite / kick / promote / demote / leave / disband / deposit / withdraw / XP / perks all work. Missing:

1. **Tiered bank** — item vault (configurable slot count) + currency cap per tier; upgrade requires guild level + cost
2. **Configurable rank slots** — server admin defines rank names; guild owner renames per-guild slot instances
3. **Per-rank permission flags** — who can invite, kick, bank deposit/withdraw, etc.
4. **Audit log** — every bank transaction recorded and viewable in GUI
5. **Bank + ranks GUIs** — `/guild bank` and `/guild ranks` commands currently missing

---

### Fishing Content Slice (`rpg-fishing`)
Current: XP per catch + FISHING_WISDOM scaling only. Missing:
- Custom fish YAML loader + registry (fish types, rarities, weights, display size)
- Custom loot table roll on each catch (replacing vanilla fishing loot)
- Biome + time-of-day catch restrictions
- Rod item stat scaling: `fishing_speed` (time-to-bite), `fishing_fortune` (drop quantity), `sea_creature_chance`
- Sea-creature spawning when `sea_creature_chance` rolls (spawn mob from mob registry at float location)

---

### Accessories: Tier Upgrades + Family Stacking + Bag Upgrade Button (`rpg-accessories`)
Current: bag opens, only ACCESSORY items allowed, stats aggregate, persistence works. Missing:

1. **Tier upgrades** — expand bag slot count when player upgrades the bag tier
2. **Family-based stacking rules** — e.g., two rings stack, three of the same family don't
3. **In-bag upgrade button** — bottom row of the accessory bag GUI should have a dedicated upgrade button so players can upgrade the bag tier without typing a command. Show current tier, cost to upgrade, and disable the button if the player can't afford it or is at max tier.

---

### Quest Log GUI (`rpg-quests`)
Current: `/quest list` prints to chat. Planned inventory GUI:
- Available / Active / Completed tabs
- Click quest → detail view with objectives (progress bars), rewards, accept / abandon button
- Quest progress action-bar messages already work

---

### Hologram Editor GUI (`rpg-holograms`)
Current: `/holograms create|delete|list|tp|move|line` commands and persistence work. GUI editor deferred:
- Line-by-line editor (click slot → chat-entry for line text)
- Add / remove / reorder lines
- Click-action support on lines (run command, open shop, etc.)

---

### Regions: Polygon + Wand + GUI (`rpg-regions`)
Current: cube-around-player only. Deferred:
- Two-point wand definition (left-click pos1, right-click pos2)
- Polygonal region support (2D polygon + Y range)
- Region-bounds GUI editor

---

### Chat: Staff Channel + Custom Channels (`rpg-chat`)
Current: global / party / guild channels work. Deferred:
- Staff channel (`/chat staff`, requires `rpg.chat.use.staff`)
- Admin-defined custom channels in `config.yml`

---

### HUD: Nametag Status-Effect Icons (`rpg-hud`)
Current: nametags show name + prefix/suffix. Deferred:
- Active status-effect icons displayed on or above the nametag

---

### Mob AI Profiles Flesh-out (`rpg-core`)
Current: `aggressive`, `passive`, `defensive`, `stationary` work. All others fall back to aggressive. Deferred:
- `ranged_kiter` — back up if player within melee range, fire ranged ability
- `boss` — phase transitions, ability rotations
- `swarming` — call nearby same-type mobs when aggro'd
- `pack_hunter` — coordinate target focus with nearby pack members
- `flying` — 3D pathfinding, strafe patterns

---

### Loot Tables: External File Reference (`rpg-core`)
Current: inline loot tables on mob YAML work. External `LootTable: <id>` references parsed but never rolled — only inline tables produce drops. Missing:
- `LootTableRegistry` lookup by id when rolling mob drops
- Coin drops wired to economy deposit on kill

---

### NPC Command Overhaul + In-Game Editing (`rpg-npcs`)
The `/npc` command is bare-bones and requires YAML editing for almost everything. Multiple gaps:

**Per-NPC entity type (currently missing entirely):**
- `NpcDef` has no per-NPC entity type field — all entity-style NPCs share a single global `display.body-entity` setting in `config.yml`. If you have a blacksmith NPC and a quest giver, they're both the same entity type.
- Add an `EntityType` field to `NpcDef` YAML and the parser
- Add `/npc setentitytype <id> <VILLAGER|ZOMBIE|IRON_GOLEM|...>` command with tab-complete for entity type names
- Default to the global config value if not specified per-NPC

**Style + skin commands (data model exists, no commands):**
- `EntityStyle` (ENTITY vs PLAYER) and `SkinDef` are in `NpcDef` but can only be set by editing YAML directly
- Add `/npc setstyle <id> entity|player` — switches between a vanilla entity body and a fake-player skin
- Add `/npc setskin <id> <playerName>` — fetches the Mojang skin for `<playerName>` and applies it (calls `SkinFetcher` which already exists)
- Add `/npc setskin <id> raw <value> <signature>` — for custom skins via raw texture data

**In-game dialogue editing (currently: YAML only):**
- `/npc setbehavior dialogue <id> <line>` overwrites all dialogue with one line — no way to add/remove individual lines
- Add `/npc dialogue add <id> <line...>` — appends a line
- Add `/npc dialogue set <id> <index> <line...>` — replaces line at index
- Add `/npc dialogue remove <id> <index>` — removes line at index
- Add `/npc dialogue clear <id>` — removes all lines
- Add `/npc dialogue list <id>` — shows all current lines with indices

**In-game shop editing (currently: YAML only + "edit npcs/all.yml" message):**
- When `setbehavior shop` is set, the only instruction is "Edit npcs/all.yml to add items." — completely unusable for non-technical admins
- Add `/npc shop add <id> <itemId> <buyPrice> <sellPrice>` — adds an item to the shop
- Add `/npc shop remove <id> <index>` — removes item at index
- Add `/npc shop list <id>` — shows current shop items with indices, prices, and whether the item exists in the registry
- Add `/npc shop clear <id>` — removes all shop items

**In-game quest assignment (partial — no tab-complete):**
- `/npc setbehavior quest <id> <questId>` works but offers no tab-complete for quest IDs
- Add tab-complete for the fourth argument pulling from the quest registry (soft-dep lookup)

**General help + info:**
- `/npc` with no args shows a one-liner — should show a formatted list of all subcommands with brief descriptions
- Add `/npc info <id>` — shows all current settings: location, entity style, entity type, skin, behavior type, dialogue line count, shop item count, quest ID

---

### Region: Enter/Exit Messages + More Flags (`rpg-regions`)
Current regions only enforce `pvp`, `no-break`, `no-place`. A lot of standard use-cases are missing:

**New flags to add:**
- `enter-message` / `leave-message` — show a title (or action bar message) when a player crosses the boundary. Configurable text with `{player}` and `{region}` placeholders.
- `no-mob-spawn` — prevent mob spawners and natural spawning inside the region
- `no-damage` — players inside take no damage (safe zones, spawn areas)
- `fly` — allow flight inside the region even without `/fly` permission
- `no-item-drop` — items dropped inside the region are immediately returned to the player (useful for arenas)
- `keep-inventory` — death inside this region doesn't drop items (overrides global death rules)

**Also:**
- Region priority field — when regions overlap, higher priority wins for conflicting flags

---

### Quest: Chains + Repeatable Quests (`rpg-quests`)
Currently all quests are one-shot and independent. Missing:

1. **Quest chains** — `Requires: [quest_id, ...]` field on a quest definition. The quest is not offerable until all prerequisites are completed.
2. **Repeatable quests** — `Repeatable: true` + `CooldownSeconds: 86400` (e.g., daily quests). After completion, the quest becomes available again after the cooldown. Per-player last-completion timestamp tracked in `DataStore`.

---

### Animated Holograms (`rpg-holograms`)
Static holograms only cycle when edited. Add support for cycling text:

- Optional `Animated: true` + `FrameInterval: 20` on a hologram definition
- Multiple entries under `Lines` become animation frames — the displayed text cycles through them at `FrameInterval` ticks
- Useful for animated signs, status displays, countdown timers

---

### Party: HP/Status Display (`rpg-parties`)
Players in a party have no way to see their teammates' health or status. Options:

- Boss bars (one per party member, shown to all other members) — simple but uses up boss bar slots fast
- Action bar or scoreboard sidebar section showing compact party HP (preferred)
- Configurable on/off in party settings; don't force it on everyone

---

### HUD: Ability Cooldown Display (`rpg-hud` / `rpg-core`)
There's currently no way for a player to see how long is left on an ability cooldown. Options:

- Dedicated scoreboard section listing active cooldowns (`testability: 2.3s`)
- Action bar suffix showing the currently-on-cooldown abilities
- Configurable placeholder `{cooldowns}` that resolves to a compact list

---

### Player Profile Command (`rpg-core`)
No way to view another player's public info. Add:

- `/profile [player]` — shows level, guild, party, top stats, recent achievements
- Opens an inventory GUI (head item for the target player, gear slots if desired)
- Respects privacy: can hide certain info via permission (`rpg.profile.private`)

---

### Unit Test Coverage (all plugins)
Almost no automated tests exist — only `QuestObjectiveTest.java`. For a codebase this size, untested code means regressions are invisible until they hit the live server. Priority areas:

- `DamageMath` — formula correctness (crit, defense reduction, level scaling)
- `SlotResolver` / `StationGui` — recipe matching logic
- `ExpressionEvaluator` — skill curve calculations
- `QuestManager` — objective progression and completion
- `BossBarService` / `SignEntryService` once built

---

### Vanilla Suppression Remaining Flags (`rpg-core`)
Audit `VanillaSuppression.java` for any flags that are accepted in config but have no event handler yet and add the missing handlers.

---

### Economy: Vault Provider Bridge (`rpg-economy`)
External non-suite plugins that expect a Vault `Economy` service can't use `rpg-economy`. Missing:
- Register `rpg-economy` as a Vault `Economy` provider on enable

---
