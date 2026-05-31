# 🟠 Improvements — In Progress / Major Missing Chunks

_These systems exist and partially work, but have significant gaps._

> **Difficulty scale:** 🟢 Easy (< 1 day) · 🟡 Medium (1–2 days) · 🔴 Hard (several days) · ⚫ Very Hard (week+)

---

### Expand Example Content — Mobs, Abilities, and Items (`rpg-core`) — 🟡 Medium
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

### New Built-in Ability Effects (`rpg-core`) — 🔴 Hard
The current effect set (`damage`, `heal`, `beam`, `explode`, `particles`, `sound`, `delay`, `apply_status`, `mana_cost`, `cooldown`) covers the basics but needs more building blocks for interesting abilities. Proposed additions:

| Effect | Parameters | Description | Est. |
|---|---|---|---|
| `knockback` | `force=`, `direction=away/toward/up` | Push or pull the target. `away` = repel from caster, `toward` = pull in, `up` = launch upward. Works on both player→mob and mob→player. | 🟢 Easy |
| `teleport` | `mode=to_target/behind_target/random_near`, `distance=` | Teleport the caster. `to_target` = land on top of target, `behind_target` = appear behind, `random_near` = random point within `distance`. | 🟢 Easy |
| `blink` | `distance=` | Teleport the caster forward in their look direction by up to `distance` blocks, stopping at the first solid block. | 🟢 Easy |
| `chain` | `targets=`, `range=`, `damage_multiplier=`, `particle=` | Bounce a damage hit to up to `targets` additional entities within `range` of each successive target. Damage decays per bounce. | 🟡 Medium |
| `zone` | `radius=`, `duration_ticks=`, `interval_ticks=`, `effect_id=`, `effect_level=` | Spawn a persistent zone at the cast location. Every `interval_ticks`, applies `effect_id` at `effect_level` to all entities inside the radius. Despawns after `duration_ticks`. | 🟡 Medium |
| `shield` | `amount=`, `duration_ticks=`, `target=caster/target` | Apply a damage-absorb shield that blocks up to `amount` HP of incoming damage, expiring after `duration_ticks`. | 🟡 Medium |
| `drain` | `amount=`, `steal_percent=`, `target=caster/target` | Deal `amount` damage to the target and heal the caster for `steal_percent`% of the damage dealt. Stacks with `lifesteal` stat. | 🟢 Easy |
| `mark` | `duration_ticks=`, `damage_amplify=` | Mark the target; all damage they receive is multiplied by `damage_amplify` while the mark is active. Visual: a particle ring around the target. | 🟡 Medium |
| `restore_mana` | `amount=`, `target=caster/target` | Restore `amount` mana to caster or target. Counterpart to `mana_cost`. | 🟢 Easy |
| `launch` | `force=`, `direction=up/away/toward`, `target=caster/target` | Apply velocity to caster or target. Softer than `knockback` — suitable for mobility abilities rather than combat disruption. | 🟢 Easy |
| `freeze` | `duration_ticks=`, `target=caster/target` | Severely slow the target (apply a high-amplifier slowness + mining fatigue equivalent). Not the same as `apply_status slow` — `freeze` is much stronger and visually distinct (ice particle burst). | 🟢 Easy |

---

### Ability Trigger Types: Expand (`rpg-core`) — 🟡 Medium
Currently supported triggers: `~onTimer`, `~onHurt`, `~onDeath`. Several common RPG triggers are missing:

| Trigger | When it fires | Notes |
|---|---|---|
| `~onAttack` | When the caster lands a melee hit | Separate from the hit registration — fires for the attacker, not the defender |
| `~onKill` | When the caster kills an entity | Useful for "on-kill effects" like lifesteal, speed burst on kill, death mark explosion |
| `~onBlock` | When the caster right-clicks with the item (wand/tool use, not bow) | Already close to existing item-use logic; formalize as a trigger |
| `~onJump` | When the player jumps | Blink / launch builds — relatively niche but useful |
| `~onLogin` | Once per server join | Apply persistent buffs on login (e.g. guild bonuses, rested XP) |

- Each trigger maps to a Bukkit event or a custom RPG event
- Triggers that make no sense for mobs (e.g. `~onLogin`) should be skipped silently if the caster is a mob

---

### MagicFind Stat: Implement or Suppress (`rpg-core`) — 🟡 Medium
`magic_find` is referenced in the loot pool spec as `MagicFindAffected: true` on individual loot entries, but there's no evidence the stat is actually read when rolling those entries. Confirm and implement:

- Read the caster's effective `magic_find` stat value when rolling a loot pool
- For entries marked `MagicFindAffected: true`, multiply the roll chance by `(1 + magic_find / 100.0)` — e.g. `+50 magic_find` → 1.5× chance on affected drops
- Cap the multiplier at a configurable max (default `max-magic-find-multiplier: 3.0` in config) to prevent absurd stacking
- If the stat isn't worth implementing yet, suppress it from item lore (same `hidden` flag approach as other unimplemented stats — see [Bugs](todo-bugs.md))

---

### Consolidate `backend.yml` + `config.yml` Persistence Setting (`rpg-core`) — 🟢 Easy
**Why there are two files — document this clearly:**
- `config.yml → persistence.backend` is the **admin's desired setting** (what the server owner configured).
- `backend.yml` is a **runtime state file written by `BackendMigrator`** at startup. It records which backend was actually active last session so the migrator can detect a YAML↔MySQL switch and auto-migrate data before anything reads it.

They intentionally serve different purposes and must stay separate. The risk of merging them is that if `config.yml` were both the setting AND the last-active record, a partial migration crash would corrupt the desired setting.

**Action:** Add a comment block near the `persistence:` section in `config.yml` explaining this, and add a similar comment at the top of `backend.yml` when it is first generated. Also document it in `docs/core/persistence.md` so admins don't think it's a bug or duplicate.

---

### Timed Cooking + Brewing with Persistent Progress (`rpg-cooking` / `rpg-alchemy`) — 🔴 Hard
Currently recipes complete instantly when the player clicks the output slot. Add configurable craft time:

- Each recipe YAML gains an optional `CraftTime` field (in seconds; 0 or absent = instant, same as now)
- When a player starts a recipe in the GUI a progress bar fills over the configured duration
- **Visual progress feedback** — the output slot item cycles through a configurable set of `CustomModelData` values (e.g., empty flask → quarter full → half full → full) so players can see progress visually. Alternatively, show a dedicated progress-bar item in a fixed slot using filled/unfilled block items (e.g., lime vs gray glass panes). The approach should be consistent between cooking and brewing.
- **If the player closes the GUI mid-craft**, the in-progress state is saved to `DataStore` keyed by `<playerUUID>:<stationBlockLocation>`: which recipe is being crafted, how much time has elapsed, and the ingredients that were consumed (so they can't be double-spent)
- **When the player reopens that station GUI**, it restores the in-progress state — progress resumes from where it left off (not restarting). The ingredient slots show the items locked in for the current craft; players can't swap them out mid-craft.
- On completion the output appears in the output slot with a sound cue; the persisted state is cleared
- If the station block is destroyed mid-craft, the ingredients should be dropped at the block location and the persisted state cleared
- Applies to both cooking stations (`rpg-cooking`) and brewing stations (`rpg-alchemy`)

---

### Enchanting: Costs Minecraft XP (`rpg-enchanting`) — 🟡 Medium
Currently enchanting costs in-game currency only. Add Minecraft (vanilla) XP cost:

- Each enchant YAML gains an optional `XpCost` field (integer levels; e.g., `XpCost: 5` costs 5 XP levels). The unit is always levels, not raw points, to match the mental model players have from vanilla enchanting.
- The XP cost is shown on the **apply button** in the enchanting GUI — e.g., `&aApply &7| &b5 XP levels &7| &e250 coins`. Both costs must be met; if either is insufficient the button shows in red with the blocking reason.
- The cost is deducted from the player's vanilla XP bar on apply; if they don't have enough XP the apply is blocked with a clear message
- **Mob XP drops** — add an `Exp` field to mob YAML (separate from skill XP / `CombatExp`). This is vanilla XP orbs dropped on death. Currently custom mobs drop 0 XP. This feeds the enchanting economy.
- **Loot pool XP** — loot pool entries (see Loot Pool System below) support an `Exp` field for vanilla XP orbs on that entry rolling
- Admins can set `XpCost: 0` or omit the field to keep an enchant currency-only

---

### Loot Pool System (`rpg-core`) — 🔴 Hard
Admins need a way to define reusable named loot pools and assign them to mobs (and dungeons, chests, etc.) rather than embedding loot inline everywhere. This also fixes the broken external `LootTable: <id>` reference system (see Loot Tables entry below — both should be consolidated into this).

**Pool definition** — new content folder: `plugins/rpg-core/loot-pools/<file>.yml`

```yaml
goblin_drops:
  Attribution: last-hit           # last-hit | top-damager | split-equal | weighted-by-damage
  RollMode: per-player            # per-player | shared
  Rolls:
    - { Item: goblin_fang,    Chance: 60.0, Min: 1, Max: 2 }
    - { Item: gold_nugget,    Chance: 40.0, Min: 1, Max: 5, MagicFindAffected: true }
    - { Item: rare_goblin_hat, Chance: 1.0, Min: 1, Max: 1, MagicFindAffected: true }
  Guaranteed:
    - { Item: coin_pouch, Min: 1, Max: 1 }
  Exp: 15              # vanilla XP orbs dropped on kill (separate from any entry-level Exp)
  CombatExp: 50        # skill XP awarded to killer's combat skill
```

Each entry can also carry its own `Exp` field (vanilla XP orbs if *that specific entry* rolls).

**Usage in mob YAML:**
```yaml
goblin:
  LootPool: goblin_drops          # single pool by id
  # or
  LootPools:                      # multiple pools, all roll independently
    - goblin_drops
    - rare_event_pool
```

**Attribution modes** (same as inline — important to document clearly):
- `last-hit` — loot goes to whoever landed the killing blow
- `top-damager` — loot goes to whoever dealt the most damage
- `split-equal` — every damager gets the same loot roll
- `weighted-by-damage` — each damager's chance is proportional to % of damage dealt

**Also applies to:** dungeon loot chests, loot chest blocks, future fishing loot, future farming loot drops.

---

### Damage Indicators: Float Down + Shrink (`rpg-holograms`) — 🟢 Easy
Current behaviour: damage numbers appear and stay in place until their duration expires.

New behaviour:
- Numbers **float downward** (not upward) over their lifetime
- Numbers **scale down** (shrink) continuously as they age via `TextDisplay` transformation
- When they reach minimum scale (configurable `min-scale` in config), they are removed immediately rather than waiting for `duration-ticks`
- All motion/scale parameters should be configurable: `float-speed`, `start-scale`, `min-scale`, `duration-ticks`

---

### Mob Death Animation (`rpg-core`) — 🟡 Medium
Currently mobs play Minecraft's default death animation (fall to side, then despawn). Replace this with a custom death sequence:

- When a custom mob's HP reaches 0, **cancel the vanilla death animation** (remove the entity before it can play the fall)
- Spawn configured **particles** at the death location (admin-configurable particle type, count, spread)
- Play a configured **sound** at the death location (admin-configurable sound, volume, pitch)
- Both `Particles` and `DeathSound` fields go on the mob YAML definition
- Loot still drops as normal (triggered by the RPG death event, not the vanilla entity death)
- Example in `mobs/example.yml` should demonstrate both fields

---

### Dungeon System Flesh-out (`rpg-dungeons`) — ⚫ Very Hard
> ⚠️ Fix the enter bug (see [Bugs](todo-bugs.md)) before working on anything below.

1. **Entry requirements not enforced** — `DungeonDef.requiredLevel`, item consumption on entry, currency cost, and party-size min/max are stored in YAML but `DungeonManager.enter()` never checks them.
2. **Per-player loot grants on completion** — `finishInstance()` evicts players without ever rolling the loot pool. Players leave with nothing.
3. **Dungeon editor GUI** — `/dungeon edit <id>` is described in docs but the command doesn't exist. Currently admins hand-edit YAML.
4. **Time limits** — no timer in `DungeonInstance`; no eviction when time expires.
5. **Composite win conditions** — only `KILL_ALL_MOBS` and `REACH_EXIT_BLOCK` work. `ADMIN_END` does nothing.

---

### Stats GUI Redesign (`rpg-core`) — 🔴 Hard
Currently `/stats` prints a chat dump. Planned 54-slot (6-row) inventory GUI layout:

```
[ Helmet ]  [ Empty ]  [ Empty ]  [ Combat ]  [ Survival ]  [ Caster  ]  [ Empty ]  [ Empty ]  [ Empty ]
[ Chest  ]  [ Empty ]  [ Empty ]  [ Gather ]  [ Loot     ]  [ Wisdom  ]  [ Empty ]  [ Empty ]  [ Empty ]
[ Legs   ]  [ Empty ]  [ Empty ]  [  ...   ]  [   ...    ]  [  ...    ]  [ Empty ]  [ Empty ]  [ Empty ]
[ Boots  ]  [ Empty ]  [ Empty ]  [ Empty ]  [ Empty ]  [ Empty ]  [ Empty ]  [ Empty ]  [ Empty ]
[ Weapon ]  [ Offhand]  [ Pet ▫️]  [ Empty ]  [ Empty ]  [ Empty ]  [ Empty ]  [Trade⚔️]  [  AH 🏪]
[ BG ][ BG ][ BG ][ BG ][ BG ][ BG ][ BG ][ BG ][ BG ]
```

- **Left column (rows 1–4):** actual worn gear items (helmet/chest/legs/boots) — clicking does nothing, just shows the item tooltip
- **Row 5 left:** main-hand weapon + offhand + companion/pet slot placeholder (BARRIER item until rpg-pets exists)
- **Centre columns:** stat category items — one named item per category (Combat, Survival, Caster, Mobility, Gathering, Loot, Wisdom). Hovering shows all stats in that category with current value
- **Bottom-right:** "Send Trade Request" button (fires `/trade <player>` if viewing someone else; hidden when viewing self); "View Auctions" button (grayed out until AH is built)
- **Active set bonuses** displayed as a named item in one of the centre slots if any sets are active
- Title: `<PlayerName>'s Stats` (supports viewing other players — `/stats <player>`)
- `/stats` with no args opens your own; `/stats <player>` opens theirs (requires `rpg.core.stats.other` permission)

---

### HUD: Scoreboard + Tablist Improvements (`rpg-hud`) — 🟡 Medium
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

### Knockback on All Weapons + Wands (`rpg-core` / `rpg-combat`) — 🟢 Easy
Currently arrows do no knockback, and most example items have no `knockback` stat defined. Fix:
- Ensure melee attacks (swords), ranged attacks (bows/arrows), and wand impacts all apply knockback proportional to the item's `knockback` stat
- Add a `knockback` stat entry to every example sword, bow, and wand in the default item YAML files so the behaviour is demonstrated out of the box

---

### RPG-Farming Redesign (`rpg-farming`) — 🔴 Hard
Current state: XP for breaking vanilla crops + FARMING_FORTUNE drop multiplier only.

Planned redesign (mirrors the custom blocks system):
- Admins assign world blocks to custom farming block types (like `/rpg block convert`)
- Custom farming blocks cycle through visual growth stages
- Not breakable until fully grown (cancel + `§cNot ready` action bar message)
- Growth time configurable per crop type in `config.yml`
- Breaking a fully-grown crop drops configured loot + restarts the growth cycle
- Requires `DataStore` persistence for per-block growth timers (like `BlockPersistence` in rpg-core)

---

### Guild System Flesh-out (`rpg-guilds`) — 🔴 Hard
Current: create / invite / kick / promote / demote / leave / disband / deposit / withdraw / XP / perks all work. Missing:

1. **Tiered bank** — item vault (configurable slot count) + currency cap per tier; upgrade requires guild level + cost
2. **Configurable rank slots** — server admin defines rank names; guild owner renames per-guild slot instances
3. **Per-rank permission flags** — who can invite, kick, bank deposit/withdraw, etc.
4. **Audit log** — every bank transaction recorded and viewable in GUI
5. **Bank + ranks GUIs** — `/guild bank` and `/guild ranks` commands currently missing

---

### Fishing Content Slice (`rpg-fishing`) — 🟡 Medium
Current: XP per catch + FISHING_WISDOM scaling only. Missing:
- Custom fish YAML loader + registry (fish types, rarities, weights, display size)
- Custom loot table roll on each catch (replacing vanilla fishing loot)
- Biome + time-of-day catch restrictions
- Rod item stat scaling: `fishing_speed` (time-to-bite), `fishing_fortune` (drop quantity), `sea_creature_chance`
- Sea-creature spawning when `sea_creature_chance` rolls (spawn mob from mob registry at float location)

---

### Accessories: Tier Upgrades + Family Stacking + Bag Upgrade Button (`rpg-accessories`) — 🟡 Medium
Current: bag opens, only ACCESSORY items allowed, stats aggregate, persistence works. Missing:

1. **Tier upgrades** — expand bag slot count when player upgrades the bag tier
2. **Family-based stacking rules** — e.g., two rings stack, three of the same family don't
3. **In-bag upgrade button** — bottom row of the accessory bag GUI should have a dedicated upgrade button so players can upgrade the bag tier without typing a command. Show current tier, cost to upgrade, and disable the button if the player can't afford it or is at max tier.

---

### Quest Log GUI (`rpg-quests`) — 🔴 Hard
Current: `/quest list` prints to chat. Planned 54-slot inventory GUI:

**Main list view:**
- Three tab buttons at the top: `Active`, `Available`, `Completed`
- Quest entries fill the remaining slots — each is a named item (book for active, map for available, checkmark for completed)
- Item lore shows: quest display name, brief description (first line), objective count or `Completed` tag
- Pagination if more quests than slots (Previous/Next buttons in bottom corners)
- `/quests` command opens it; `/quest <id>` opens directly to that quest's detail view

**Detail view (click a quest):**
- Quest display name as inventory title
- Description lines shown on a named item in the top-left
- Objectives listed as separate items with current/required count (e.g., `Kill Goblin: 3/10`)
  - Completed objectives show a green checkmark; incomplete show a red X
  - Progress bar in the item lore using filled/unfilled block characters
- Rewards shown as a separate item listing currency, skill XP, and item rewards
- Accept button (green) / Abandon button (red) / Back button
- If a quest requires a prerequisite not yet completed, the accept button is grayed out and says which quest is blocking

---

### Hologram Editor GUI (`rpg-holograms`) — 🟡 Medium
Current: `/holograms create|delete|list|tp|move|line` commands and persistence work. GUI editor deferred:
- Line-by-line editor (click slot → chat-entry for line text)
- Add / remove / reorder lines
- Click-action support on lines (run command, open shop, etc.)

---

### Regions: Polygon + Wand + GUI (`rpg-regions`) — 🔴 Hard
Current: cube-around-player only. Deferred:
- Two-point wand definition (left-click pos1, right-click pos2)
- Polygonal region support (2D polygon + Y range)
- Region-bounds GUI editor

---

### Chat: Staff Channel + Custom Channels (`rpg-chat`) — 🟢 Easy
Current: global / party / guild channels work. Deferred:
- Staff channel (`/chat staff`, requires `rpg.chat.use.staff`)
- Admin-defined custom channels in `config.yml`

---

### HUD: Nametag Status-Effect Icons (`rpg-hud`) — 🟡 Medium
Current: nametags show name + prefix/suffix. Deferred:
- Active status-effect icons displayed on or above the nametag

---

### Mob AI Profiles Flesh-out (`rpg-core`) — 🔴 Hard
Current: `aggressive`, `passive`, `defensive`, `stationary` work. All others fall back to aggressive. Deferred:
- `ranged_kiter` — back up if player within melee range, fire ranged ability
- `boss` — phase transitions, ability rotations
- `swarming` — call nearby same-type mobs when aggro'd
- `pack_hunter` — coordinate target focus with nearby pack members
- `flying` — 3D pathfinding, strafe patterns

---

### Mob Patrol Waypoints (`rpg-core`) — 🔴 Hard
Currently mobs (and NPCs) stand still when no player is nearby. A patrol behaviour lets admins define a list of waypoints a mob walks between, making the world feel more alive.

- New AI profile: `patrol` — cycles through a list of `Waypoints` defined in mob YAML (world + x/y/z coordinates)
- Can optionally pause at each waypoint for a configurable `WaypointPauseTicks` before moving on
- If a player gets within aggro range, the mob switches to `aggressive` temporarily; on losing aggro, returns to patrol
- Admin commands: `/mob setpatrol <mobId> add` (adds player's current location as next waypoint), `/mob setpatrol <mobId> clear`
- NPCs with patrol defined follow the same waypoint path (compatible with `LookAtPlayers` — look at nearest player while patrolling, continue walking otherwise)

---

### Loot Tables: External File Reference (`rpg-core`) — 🟢 Easy
Current: inline loot tables on mob YAML work. External `LootTable: <id>` references parsed but never rolled — only inline tables produce drops. Missing:
- `LootTableRegistry` lookup by id when rolling mob drops
- Coin drops wired to economy deposit on kill

---

### NPC Command Overhaul + In-Game Editing (`rpg-npcs`) — 🔴 Hard
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

**NPC look-at-player (new):**
- NPCs should smoothly rotate to face the nearest player within a configurable radius, making them feel alive
- Add a `LookAtPlayers: true` boolean field per NPC YAML (default from `config.yml → npc.look-at-players.enabled`)
- Add `LookRadius: 8` (blocks) per NPC, also with a global default in `config.yml`
- A repeating Bukkit task (interval configurable, e.g., every 2 ticks) scans all loaded NPC entities that have `LookAtPlayers: true`, finds the nearest online player within `LookRadius`, and rotates the entity to face them
- For **entity-style NPCs**: update `yaw` via `entity.teleport(entity.getLocation().setDirection(dir))` — this is the cleanest way to rotate an entity without moving it
- For **PLAYER-style NPCs**: requires sending a head-rotation packet (`ClientboundRotateHeadPacket` / `ClientboundMoveEntityPacket`) to all nearby players each tick — NMS, same pattern as `FakePlayerNpc`
- If no player is within `LookRadius`, the NPC returns to its default facing direction (stored `yaw`/`pitch` from YAML)
- Add `/npc setlook <id> true|false` command and include it in tab-complete
- Add it to `/npc info <id>` output

**General help + info:**
- `/npc` with no args shows a one-liner — should show a formatted list of all subcommands with brief descriptions
- Add `/npc info <id>` — shows all current settings: location, world, entity style, entity type, skin name, behavior type, look-at-players enabled, dialogue line count, shop item count, quest ID

---

### Region: Enter/Exit Messages + More Flags (`rpg-regions`) — 🟡 Medium
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

### Quest: Chains + Repeatable Quests (`rpg-quests`) — 🟡 Medium
Currently all quests are one-shot and independent. Missing:

1. **Quest chains** — `Requires: [quest_id, ...]` field on a quest definition. The quest is not offerable until all prerequisites are completed.
2. **Repeatable quests** — `Repeatable: true` + `CooldownSeconds: 86400` (e.g., daily quests). After completion, the quest becomes available again after the cooldown. Per-player last-completion timestamp tracked in `DataStore`.

---

### Animated Holograms (`rpg-holograms`) — 🟡 Medium
Static holograms only cycle when edited. Add support for cycling text:

- Optional `Animated: true` + `FrameInterval: 20` on a hologram definition
- Multiple entries under `Lines` become animation frames — the displayed text cycles through them at `FrameInterval` ticks
- Useful for animated signs, status displays, countdown timers

---

### Party: HP/Status Display (`rpg-parties`) — 🟡 Medium
Players in a party have no way to see their teammates' health or status. Options:

- Boss bars (one per party member, shown to all other members) — simple but uses up boss bar slots fast
- Action bar or scoreboard sidebar section showing compact party HP (preferred)
- Configurable on/off in party settings; don't force it on everyone

---

### HUD: Ability Cooldown Display (`rpg-hud` / `rpg-core`) — 🟡 Medium
There's currently no way for a player to see how long is left on an ability cooldown. Options:

- Dedicated scoreboard section listing active cooldowns (`testability: 2.3s`)
- Action bar suffix showing the currently-on-cooldown abilities
- Configurable placeholder `{cooldowns}` that resolves to a compact list

---

### Player Profile Command (`rpg-core`) — 🟡 Medium
No way to view another player's public info. Add `/profile [player]`:

- No args = your own profile; with a player name = their profile (requires `rpg.profile.view.others`)
- **GUI layout (27 or 54 slots):**
  - Player head item (top-left) with name, guild tag, party status in lore
  - Top skill levels shown as named items (e.g., "⚔ Combat Lv.12", "⛏ Mining Lv.8")
  - Most valuable equipped gear slot items (display only)
  - Balance shown on a gold coin item
  - Recent achievements (last 3 unlocked) shown as named items
  - "Send Trade Request" button if viewing another player
- Target player must be online to view their profile (or show a last-known snapshot if offline data is cached)
- Players can opt out of public profiles via `rpg.profile.private` permission — their profile shows "This player's profile is private."
- Tab-complete for the player argument lists online player names

---

### Economy: Transaction Log (`rpg-economy`) — 🟡 Medium
Admins and players have no visibility into their currency history. Add a transaction log:

- Every `deposit`, `withdraw`, and `transfer` call on `CoreEconomy` appends a log entry: timestamp, type, amount, source/target player, reason string
- Log stored in `DataStore` per player, capped at a configurable max entries (default 100)
- `/money log [player]` — shows the last N transactions in chat or a GUI
  - No `[player]` arg = view your own; with arg requires `rpg.economy.log.others`
- Reason strings: calling systems should pass a human-readable tag (e.g., `"quest:first_kill reward"`, `"npc:shop purchase"`, `"auction:sale proceeds"`)
- Useful for diagnosing currency duplication bugs and support tickets

---

### Unit Test Coverage (all plugins) — 🟡 Medium (ongoing)
Almost no automated tests exist — only `QuestObjectiveTest.java`. For a codebase this size, untested code means regressions are invisible until they hit the live server. Priority areas:

- `DamageMath` — formula correctness (crit, defense reduction, level scaling)
- `SlotResolver` / `StationGui` — recipe matching logic
- `ExpressionEvaluator` — skill curve calculations
- `QuestManager` — objective progression and completion
- `BossBarService` / `SignEntryService` once built

---

### Vanilla Suppression Remaining Flags (`rpg-core`) — 🟢 Easy
Audit `VanillaSuppression.java` — these flags are accepted in `config.yml` but likely have no event handler wired yet:

| Flag | Config key | Likely missing handler |
|---|---|---|
| Villager trading | `villager-trading` | `VillagerAcquireTradeEvent` + `VillagerReplenishTradeEvent` + `InventoryOpenEvent` for villager GUIs |
| Beacons | `beacons` | `BeaconEffectEvent` |
| Pillager patrols | `pillager-patrols` | `EntitySpawnEvent` filtering `PILLAGER` patrol spawns |
| Block explosion damage | `block-explosion-damage` | `EntityDamageByBlockEvent` for explosion sources |
| Durability | `durability` | `PlayerItemDamageEvent` |
| Death drops | `death-drops` | `PlayerDeathEvent` item drop handling (separate from the custom death-rules system — this is the vanilla drop specifically) |

Verify each against the actual `VanillaSuppression.java` event listener list and add any confirmed-missing handlers.

---

### Economy: Vault Provider Bridge (`rpg-economy`) — 🟢 Easy
External non-suite plugins (third-party shops, job plugins, etc.) that expect a Vault `Economy` service can't interact with `rpg-economy`. Missing:

- Add Vault as a `softDepend` in `rpg-economy/plugin.yml`
- On enable, if Vault is present, register a `net.milkbowl.vault.economy.Economy` provider via `getServer().getServicesManager().register(Economy.class, new VaultEconomyAdapter(coreEconomy), this, ServicePriority.Normal)`
- `VaultEconomyAdapter` wraps `CoreEconomy` — implement `has()`, `getBalance()`, `withdrawPlayer()`, `depositPlayer()`, `format()` using `RpgServices.economy()`
- Methods Vault doesn't support (multi-world, banks) can return `false` / throw `UnsupportedOperationException`
- This is one-way compatibility: Vault plugins can read/write rpg-economy balances; rpg-economy doesn't need to depend on Vault at compile time beyond the soft-dep

---

### Status Effects: Catalog + New Built-in Types (`rpg-core`) — 🟢 Easy
The `apply_status` effect is used in several example abilities, but there's no documented catalog of what built-in status IDs exist and what their parameters mean. Also several common RPG statuses are missing.

**Needed:**
- Write a reference table (inline in `config.yml` comments or `docs/core/status-effects.md`) listing every built-in status: id, description, `Level` meaning, `DurationTicks` behavior
- New built-in statuses to add:
  - `burning` — sets entity on fire for duration ticks (maps to Bukkit `setFireTicks`)
  - `frozen` — applies high-amplifier Slowness + Mining Fatigue; blue particle burst on apply
  - `marked` — damage amplification debuff (used by `mark` ability effect above); ring particles on target
  - `silenced` — prevents ability use for duration (check in `AbilityService.invoke`)
  - `haste` — positive buff: increased mining speed (Haste potion effect)
  - `shield_buff` — absorbed damage indicator (visual only — used internally by `shield` effect)

---
