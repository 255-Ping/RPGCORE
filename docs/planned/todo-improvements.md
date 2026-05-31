# 🟠 Improvements — In Progress / Major Missing Chunks

_These systems exist and partially work, but have significant gaps._

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
