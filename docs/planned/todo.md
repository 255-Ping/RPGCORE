# RPGCORE — Master Todo / Backlog

_Generated from full codebase + docs audit. Use this as the single reference point before starting any session._

Each item is tagged with the plugin it lives in, a rough size estimate, and its current state.

---

## 🔴 Not Started — Full New Features

### Auction House (`rpg-auction-house`)
**New plugin — nothing exists yet.**
- Player-posted item listings with custom price (sign-UI price entry)
- Listing expiry + item return on expiry
- Browse GUI: filterable by name / category / seller
- `/ah` command: main browser, my listings, create listing, expired returns
- Admin commands: `/ah list <player>`, `/ah remove <id>`, `/ah wipe`
- Configurable listing fee (% of sale price), max listings per player, listing duration
- Non-tradeable items blocked from listing
- See `docs/planned/auction-house.md` for full spec

### Bazaar (`rpg-bazaar`)
**New plugin — nothing exists yet.**
- Admin-defined fixed-price buy/sell listings organized in categories
- Infinite or limited stock with configurable restock intervals
- Browse GUI with category tabs
- `/bazaar` command
- Admin commands: `/bazaar reload`, `/bazaar add`, `/bazaar remove`, `/bazaar stock`
- Non-tradeable items blocked
- See `docs/planned/bazaar.md` for full spec

### Sign-Entry Number Input
**Shared utility needed by Auction House, Bazaar, and potentially Guild Bank.**
- Open virtual sign via `PacketPlayOutOpenSignEditor`
- Line 1 = prompt label (e.g., `Enter Price:`)
- Parse `PacketPlayInUpdateSign`; invalid → re-open with error hint
- Likely a utility class in `rpg-core` (as a service or static helper)

---

## 🟠 In Progress — Major Missing Chunks

### Dungeon System Flesh-out (`rpg-dungeons`)
**Core paste + enter + leave + death handling all work. Missing:**

1. **Entry requirements not enforced** — `DungeonDef.requiredLevel`, item consumption on entry, currency cost, and party-size min/max gates are stored in YAML but `DungeonManager.enter()` never checks them. Any player can enter any dungeon with no gating.

2. **Per-player loot grants on completion** — `finishInstance()` calls `evict()` which teleports everyone out, but never rolls the loot pool from `DungeonDef.lootChests`. Players leave with nothing. Need to call a loot roll per player before teleporting out.

3. **Dungeon editor GUI** — docs describe `/dungeon edit <id>` opening a GUI for spawner placement, win condition picker, loot pool editor, requirements, and time limit. The command doesn't exist in `DungeonCommand`. Currently admins can only use `setentrance / setexit / setspawn` + hand-edit the YAML.

4. **Time limits** — no timer in `DungeonInstance`; no eviction when time expires.

5. **Composite win conditions** — only `KILL_ALL_MOBS` and `REACH_EXIT_BLOCK` work. `ADMIN_END` does nothing. The `all|any` composite conditions from the spec are unimplemented.

### Stats GUI Redesign (`rpg-core`)
**Currently `/stats` prints a chat dump. Planned inventory GUI needs:**
- Equipment display: 4 armor slots + main-hand slot showing actual worn items
- Companion/pet slot (placeholder for now)
- Accessories count from `rpg-accessories`
- Stats grouped into categories, each shown on a named item (Combat, Gathering, Economy, etc.)
- "Send trade request" button → fires trade invite
- "View auctions" button → opens filtered AH view for that player (blocked until AH is built)
- See `docs/planned/backlog.md → Stats GUI redesign` for spec

### RPG-Farming Redesign (`rpg-farming`)
**Current state:** XP for breaking vanilla crops + FARMING_FORTUNE drop multiplier. That's it.
**Planned redesign** (similar to custom blocks system):
- Admins assign world blocks to custom farming block types (like `/rpg block convert`)
- Custom farming blocks cycle through visual growth stages
- Not breakable until fully grown (send cancel + `§cNot ready` action bar)
- Growth time configurable per crop type in `config.yml` (game-time seconds)
- Breaking fully-grown crop drops configured loot + restarts growth cycle
- Requires `DataStore` persistence for growth timers (like `BlockPersistence` in rpg-core)

### Guild System Flesh-out (`rpg-guilds`)
**Current:** create / invite / kick / promote / demote / leave / disband / deposit / withdraw / XP / perks all work. **Missing:**

1. **Tiered bank** — item vault (configurable slot count) + currency cap per tier; upgrade requires guild level + cost; per-rank deposit/withdraw permission
2. **Configurable rank slots** — server admin defines rank names in config; guild owner renames per-guild instances of those slots
3. **Per-rank permission flags** — who can invite, kick, bank deposit/withdraw, etc. configurable per rank
4. **Audit log** — every bank transaction recorded; viewable in GUI
5. **Bank + ranks GUIs** — `/guild bank` and `/guild ranks` commands planned; currently missing

### Fishing Content Slice (`rpg-fishing`)
**Current:** XP per catch + FARMING_WISDOM scaling only. **Missing:**
- Custom fish YAML loader + registry (fish types, rarities, weights, min/max size)
- Custom loot table roll on catch (replacing vanilla fishing loot)
- Biome + time-of-day catch restrictions
- Rod item stat scaling: `fishing_speed` (time-to-bite), `fishing_fortune` (drop quantity), `sea_creature_chance`
- Sea-creature spawning on `sea_creature_chance` roll (spawn mob from mob registry at float location)

---

## 🟡 Partial / Polish Needed

### Quest Log GUI (`rpg-quests`)
**Current:** `/quest list` prints quest names to chat. Planned inventory GUI:
- Available / Active / Completed tabs
- Click quest → detail view: objectives with progress bars, rewards, accept / abandon button
- Quest progress action-bar message already works

### Hologram Editor GUI (`rpg-holograms`)
**Current:** commands (`/holograms create|delete|list|tp|move|line`) and persistence work. The "GUI editor" slice is deferred:
- Line-by-line editor via chat entry (click slot → type line text)
- Add / remove / reorder lines in GUI
- Click-action support on hologram lines (run command, open shop, etc.)

### Accessories: Tier Upgrades + Family Stacking (`rpg-accessories`)
**Current:** bag opens, only ACCESSORY items allowed, stats aggregate, persistence works. **Deferred:**
- Tier upgrades (expand bag slot count by tier)
- Family-based stacking rules (e.g., two rings can stack, three of the same family cannot)

### Regions: Polygon + Wand + GUI (`rpg-regions`)
**Current:** cube-around-player only (`/region define <id> <radius>`). **Deferred:**
- Two-point wand-based definition (left-click pos1, right-click pos2, define region from those corners)
- Polygonal region support (2D polygon + Y range)
- Region-bounds GUI editor

### Chat: Staff Channel + Custom Channels (`rpg-chat`)
**Current:** global / party / guild channels work. **Deferred:**
- Staff channel (`/chat staff`, requires `rpg.chat.use.staff`)
- Admin-defined custom channels in `config.yml` with configurable prefixes, permissions, and scope

### HUD: Nametag Status-Effect Icons (`rpg-hud`)
**Current:** nametags show name + prefix/suffix. **Deferred:**
- Active status-effect icons displayed above the nametag (or inline)

### Mob AI Profiles Flesh-out (`rpg-core`)
**Current:** `aggressive`, `passive`, `defensive`, `stationary` work. All others fall back to aggressive. **Deferred:**
- `ranged_kiter` — back up if player within melee range, fire ranged ability
- `boss` — phase transitions, ability rotations
- `swarming` — call nearby same-type mobs when aggro'd
- `pack_hunter` — coordinate target focus with nearby pack members
- `flying` — 3D pathfinding, strafe patterns

### Loot Tables: External File Reference (`rpg-core`)
**Current:** inline loot tables on mob YAML work. External `LootTable: <id>` references in mob YAML are parsed but not actually rolled — only inline tables produce drops. **Missing:**
- `LootTableRegistry` lookup by id when rolling mob drops
- Coin drops wired to economy deposit on kill

### Vanilla Suppression Remaining Flags (`rpg-core`)
**Current:** `mob-spawning`, `hunger`, `player-regen`, `xp-orbs`, `raids`, `enchanting-table`, `anvil`, `brewing-stand`, `crafting`, `fishing` all suppressed. **Accepted in config but not yet enforced:**
- Any remaining flags listed in `VanillaSuppression` that don't have event handlers yet (audit `VanillaSuppression.java` to confirm exact list)

### Economy: Vault Provider Bridge (`rpg-economy`)
**Current:** plugins can use `RpgServices.economy()` directly. External non-suite plugins (e.g., third-party shop plugins) expect a Vault `Economy` provider. **Missing:**
- Register `rpg-economy` as a Vault `Economy` service on enable so Vault-dependent plugins can use it

---

## 🔵 GUI Overhaul Conversions (from `docs/planned/gui-overhaul.md`)

All of these replace or supplement existing command-only interfaces with inventory GUIs.

| GUI | Plugin | Current state |
|---|---|---|
| Party GUI (`/party`) | `rpg-parties` | All commands work; no GUI |
| Guild GUI (`/guild`) | `rpg-guilds` | All commands work; no GUI |
| Quest GUI (`/quests`) | `rpg-quests` | Chat-list only; no GUI |
| Admin Spawner GUI (`/spawner`) | `rpg-core` | All fields set via `/spawner set`; GUI planned |
| Hologram Creation GUI (`/holo create`) | `rpg-holograms` | Command works; GUI editor deferred |

**Input primitives needed for GUIs:**
- **Chat-entry capture** — for player names, search strings: close GUI → prompt in chat → `AsyncChatEvent` capture → reopen GUI with result
- **Sign-entry capture** — for numeric values (prices, quantities): open virtual sign, parse result. Required for AH and Bazaar specifically.

---

## 📄 Documentation Stubs (no code work, just writing)

| File | Status |
|---|---|
| `docs/stats.md` | Stub — full stat reference table not written |
| `docs/installation.md` | Stub — install instructions not written |
| `docs/configuration.md` | Stub — global config reference not written |
| `docs/resource-pack.md` | Stub — resource pack setup guide not written |
| `docs/core/selection-wand.md` | Stub — wand usage guide not written |

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

## Priority Order (suggested)

1. **Dungeon entry requirements + loot grants** — dungeons are functional but unrewarded; quick win
2. **Stats GUI redesign** — highest-visibility player-facing feature
3. **Fishing content slice** — rounding out a working skill
4. **Quest log GUI** — `/quest` is the only major command still chat-only
5. **Guild bank + rank GUI** — guilds are heavily used; missing bank makes them feel incomplete
6. **RPG-Farming redesign** — current farming is bare-bones; redesign is spec'd out
7. **Auction House** — large new plugin; needs sign-entry first
8. **Bazaar** — simpler than AH; can share sign-entry utility
9. **GUI overhaul conversions** (party, guild, quest GUIs)
10. **Mob AI profile flesh-out** — content/polish; needs good example mobs to test
