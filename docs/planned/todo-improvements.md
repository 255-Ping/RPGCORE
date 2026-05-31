# 🟠 Improvements — In Progress / Major Missing Chunks

_These systems exist and partially work, but have significant gaps._

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

### Vanilla Suppression Remaining Flags (`rpg-core`)
Audit `VanillaSuppression.java` for any flags that are accepted in config but have no event handler yet and add the missing handlers.

---

### Economy: Vault Provider Bridge (`rpg-economy`)
External non-suite plugins that expect a Vault `Economy` service can't use `rpg-economy`. Missing:
- Register `rpg-economy` as a Vault `Economy` provider on enable

---
