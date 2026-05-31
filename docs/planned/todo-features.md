# 🔴 New Features — Not Started

_Full features or systems that don't exist at all yet._

> **Difficulty scale:** 🟢 Easy (< 1 day) · 🟡 Medium (1–2 days) · 🔴 Hard (several days) · ⚫ Very Hard (week+)

---

### Player Homes + Server Warps (`rpg-admin`) — 🟢 Easy
Conspicuously absent from the suite. Every RPG server needs these.

- `/sethome [name]`, `/home [name]`, `/delhome [name]`, `/homes` — per-player saved locations, configurable max homes per permission group
- `/setwarp <name>`, `/warp <name>`, `/delwarp <name>`, `/warps` — admin-defined server-wide teleport points
- Homes + warps persist via `DataStore`
- Teleport delay configurable (cancel on damage or movement)
- Goes in `rpg-admin` alongside the existing fly/gmc/heal/tp commands

---

### Achievement System (`rpg-core` or new `rpg-achievements`) — 🔴 Hard
No achievement tracking exists anywhere in the suite.

**Achievement YAML** (`plugins/rpg-core/achievements/<file>.yml`):
```yaml
first_kill:
  DisplayName: "&cFirst Blood"
  Description: "Kill your first custom mob."
  Category: combat
  Hidden: false          # true = doesn't show until unlocked (secret achievement)
  Rewards:
    Currency: 500
    SkillXp: { combat: 100 }
    Items: [{ Item: strength_potion, Amount: 1 }]
  Trigger:
    Type: kill_count
    Mob: any
    Count: 1
```

**Trigger types:**
| Type | Fields | Fires when |
|---|---|---|
| `kill_count` | `Mob: <id or any>`, `Count: N` | Player kills N of that mob type |
| `skill_level` | `Skill: <id>`, `Level: N` | Skill reaches level N |
| `money_earned` | `Amount: N` | Lifetime currency earned reaches N |
| `quest_complete` | `Quest: <id>` | Specific quest is completed |
| `item_obtain` | `Item: <id>` | Player picks up that item |
| `stat_reach` | `Stat: <id>`, `Value: N` | Player's effective stat reaches N |
| `manual` | — | Only unlockable via `/achievement give <id> <player>` |

- Per-player progress for count-based triggers tracked in `DataStore`
- On unlock: title toast + configurable broadcast (`&6<player> unlocked &e<achievement>!`), broadcast toggleable in config
- `/achievements` — opens a 54-slot GUI browser with category tabs (locked achievements shown as gray locked items if not hidden; hidden ones invisible until unlocked)
- Progress-based achievements show a progress bar in lore (e.g., `47/100 kills`)
- Goes in `rpg-core` to avoid a new plugin dependency chain; can be split to `rpg-achievements` later

---

### Boss Bar System (`rpg-core`) — 🟡 Medium
No boss bar support exists. Needed by dungeons, world events, and world boss mobs.

**API surface** (`BossBarService` component on the Game object):
- `show(player, barId, text, progress, color, style)` — shows or updates a named bar for that player
- `hide(player, barId)` — removes the bar
- `update(player, barId, text, progress)` — update text/progress without recreating
- `hideAll(player)` — removes all RPG-managed bars for that player on disconnect/death/exit

**barId** is a string key (e.g., `"dungeon:instance-uuid"`, `"worldboss:kraken"`). Players can have multiple bars simultaneously (e.g., a dungeon timer bar + a world boss HP bar at the same time).

**Usage:**
- **Dungeons:** `"Mobs Remaining: X / Y"` bar shown to all players in an instance, updated on each mob kill. Removed on completion or player exit.
- **World boss:** shared HP bar visible to all players in the event area. Title includes boss name + HP %.
- **Timed events:** countdown bar (progress counts down from 1.0 to 0.0 over the event duration)

**Reconnect behaviour:** boss bars are client-side and lost on reconnect. `BossBarService` must store the current active bar state per player and re-send them on `PlayerJoinEvent` / `PlayerRespawnEvent`.

---

### World Events + World Boss (`rpg-core` or new `rpg-events`) — ⚫ Very Hard
Periodic server-wide events add communal engagement that solo play can't. Planned:

- Admin-configurable event schedule (cron-like interval or manual `/event start <id>`)
- Event types: world boss spawn (named mob at a configured location, shared HP, loot pool per-player), resource rush (higher drop rates for N minutes), invasion (wave of strong mobs at a location)
- Boss bar + broadcast announce when event starts/ends
- Per-player loot rolls on completion

---

### Salvaging System (`rpg-enchanting`) — 🟡 Medium
Players break down unwanted RPG items into materials at a salvage station (custom block type with `StationType: salvage`). Lives in `rpg-enchanting` since it's part of the item-modification workflow alongside enchanting and reforging.

**GUI (27 slots):**
- Slot 11: input slot — place the RPG item here
- Slot 13: preview slot (read-only) — shows what materials will be returned. Updates live as the input changes.
- Slot 15: Salvage button — green when valid, red/gray when not. Shows item name + rarity being salvaged.
- Remaining slots: GUI background panes

**Yield logic:**
- Default yield by rarity: `Common → 1 common material`, `Uncommon → 2`, `Rare → 1 rare + 1 common`, etc. Configurable defaults in `config.yml`
- Per-item override: `Salvage: [{ Item: iron_chunk, Amount: 3 }]` in item YAML
- Non-RPG items (vanilla items without PDC tag) can optionally be salvaged if `allow-vanilla-salvage: true` in config (drops nothing by default)
- Items marked `Salvageable: false` in YAML show an error on the button: `§cThis item cannot be salvaged`
- Quest items (`Type: QUEST`) are always non-salvageable regardless of the YAML field

**Admin commands:** `/salvage reload` to reload yield config.

---

### Starter Kit System (`rpg-admin`) — 🟢 Easy
New players joining for the first time should receive a configured starting set of items.

- Admin defines kit contents in `config.yml` (list of item ids + amounts)
- Given automatically on first join (tracked per UUID in `DataStore` so it's only given once)
- `/kit` command to claim manually if they missed it (once per UUID)
- Optional: multiple kits gated by permission (`rpg.kit.starter`, `rpg.kit.vip`, etc.)

---

### Item Set Bonuses (`rpg-core`) — 🟡 Medium
Wearing multiple pieces of the same named set grants additional stat bonuses. Very standard RPG feature.

- Items in a set share a `Set` YAML field (e.g., `Set: mages_robes`)
- Set definition file in `sets/` folder: display name, bonus tiers (`2-piece: { intelligence: 30 }`, `4-piece: { ability_damage: 50, cooldown_reduction: 10 }`)
- Bonuses apply on top of individual item stats during `recalculateStats()`
- Active set bonuses shown at the bottom of the item lore (e.g., `&5Mage's Robes (2/4): Intelligence +30`)
- `/stats` GUI shows active set bonuses in a dedicated section

---

### Leaderboards (`rpg-core`) — 🟡 Medium
No `/top` or leaderboard command exists anywhere in the suite.

- `/top [category]` — categories: `money`, `level` (overall), and one per skill (`combat`, `mining`, etc.)
- `/top` with no args opens a 54-slot GUI with category tabs across the top row
- Each entry shows: rank number, player head, player name, value (formatted with Money.Format for currency, or level number for skills)

**Performance note:** scanning all player records in `DataStore` on demand is O(n) over all players and will be slow on large servers. Use a **cached snapshot** approach:
- A background task re-builds top-N lists on a configurable interval (`leaderboard-refresh-seconds: 300` in config)
- The cache stores the top 10 (or configurable) entries per category
- `/top` reads from the cache — always fast, potentially slightly stale
- Cache is rebuilt on plugin enable and on schedule
- New/offline players are included in snapshots since they're in `DataStore` regardless of online status

---

### Elite / Champion Mob Variants (`rpg-core`) — 🟡 Medium
Randomly enhanced mob spawns that are rarer, stronger, and drop better loot. Standard RPG engagement mechanic.

- Configurable chance per-mob-spawn that it becomes an "elite" variant (e.g., 5%)
- Elite mobs get a configurable stat multiplier (HP × 3, damage × 2, etc.)
- Distinct display name prefix (e.g., `&6⚜ &r` prefix before the mob name)
- Separate loot table or loot multiplier defined on the mob YAML (`EliteLootTable`, `EliteLootMultiplier`)
- Particle effect on elite spawn (configurable)
- Optional: champion tier above elite with even bigger multipliers

---

### Extract Smelting → `rpg-smelting` Plugin (`rpg-core` / new `rpg-smelting`) — 🟢 Easy
Smelting recipe loading (`SmeltingLoader.java`) currently lives in `rpg-core`. It should be its own addon plugin so servers that don't need custom smelting don't load it, and so it can be expanded independently later.

- Create a new blank `rpg-smelting` module (same pattern as `rpg-cooking` / `rpg-alchemy`)
- Move `SmeltingLoader` and any smelting-specific YAML content out of `rpg-core` into the new plugin
- The `smelting: true` vanilla-suppression flag stays in `rpg-core/config.yml` — it's a world-toggle, not addon-specific
- `rpg-core` soft-depends on `rpg-smelting`; if the plugin isn't loaded, smelting suppression still applies but no custom recipes are registered
- Stub out a `config.yml` and `recipes/example.yml` the same way cooking does; flesh out further functionality later

### Extract Crafting → `rpg-crafting` Plugin (`rpg-core` / new `rpg-crafting`) — 🟢 Easy
Same rationale as smelting. `RecipeLoader.java` (shaped/shapeless crafting) currently lives in `rpg-core`.

- Create a new blank `rpg-crafting` module
- Move `RecipeLoader` and crafting YAML content out of `rpg-core` into the new plugin
- The `crafting: true` vanilla-suppression flag stays in `rpg-core/config.yml`
- Stub out `config.yml` and `recipes/example.yml`; flesh out further later

---

### Sign-Entry Number Input (`rpg-core`) — 🟡 Medium
**Required before:** Auction House, Bazaar, Guild Bank GUI, any other GUI that takes a currency or quantity input from the player.

Needed everywhere a player types a numeric value (currency amount, quantity, price) inside a GUI. Build once as a shared `SignEntryService` in `rpg-core` so every addon can call it — don't re-implement per-plugin.

- Open a virtual sign via `PacketPlayOutOpenSignEditor`
- Line 1 = prompt label (e.g., `Enter Price:`)
- Player types the number on the sign and confirms
- Parse `PacketPlayInUpdateSign`; on invalid input, re-open the sign with an error hint on line 2
- Callback-based API: `SignEntryService.open(player, prompt, onResult)`

**Reference implementation:** SurvivalCore repo.

---

### PlaceholderAPI Support (`rpg-hud` / `rpg-core`) — 🟡 Medium
Allow PlaceholderAPI placeholders (e.g., `%player_name%`, `%vault_balance%`) anywhere RPGCORE reads a template string — scoreboard lines, tablist header/footer, nametag format, action bar format, etc.

**Integration (two directions):**

1. **PAPI → RPGCORE** (consume external placeholders):
   - Add `softdepend: [PlaceholderAPI]` to `rpg-hud/plugin.yml` and `rpg-core/plugin.yml`
   - In `PlaceholderResolver.resolve()`, if PAPI is loaded, run `PlaceholderAPI.setPlaceholders(player, template)` as a second pass after RPGCORE's own `{...}` substitution (so both syntaxes work in the same template line)

2. **RPGCORE → PAPI** (expose RPG stats to other plugins):
   - Register a `PlaceholderExpansion` class in `rpg-core` (e.g., `RpgCorePlaceholders`) with identifier `rpgcore`
   - Exposes: `%rpgcore_stat_<id>%`, `%rpgcore_skill_<id>_level%`, `%rpgcore_skill_<id>_xp%`, `%rpgcore_balance%`, `%rpgcore_health%`, `%rpgcore_mana%`, `%rpgcore_guild%`, `%rpgcore_party_size%`
   - Register it on enable only if PAPI is present (soft-dep pattern: `Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")`)

---

### Offline Mail / Inbox System (`rpg-core`) — 🔴 Hard
Several future systems need to deliver items or messages to players who are offline — most notably Auction House sale proceeds, achievement rewards, and admin broadcasts. Without a mail system, these are either silently lost or require the player to be online.

- Per-player inbox stored in `DataStore` (list of mail entries: type, sender, message, attached items, timestamp)
- Mail types: `item_delivery` (AH sale return / expired listing), `currency_delivery` (AH sold proceeds), `system_message` (server announcement), `achievement_reward` (if player was offline when triggered)
- On join: notify player if they have unread mail (`&eYou have N unread mail items!`)
- `/mail` or `/inbox` command — opens a GUI showing all messages, click to claim items, mark-as-read on view
- Admin command: `/mail send <player> <message>` for system messages
- Mail entries expire after a configurable duration (default 7 days) to prevent DataStore bloat
- Items in mail slots are stored serialized (Base64 ItemStack) so they survive server restarts

---

### Resource Pack Auto-Delivery (`rpg-core`) — 🟢 Easy
Server resource packs need to be sent to players on join for custom model data and fonts to work. Currently players must manually apply a resource pack or be sent one through server.properties (which applies to all players with no server-side control).

- Add `resource-pack.url` and `resource-pack.hash` to `rpg-core/config.yml`
- On `PlayerJoinEvent`, call `player.setResourcePack(url, hash, true, Component)` (the last boolean = required)
- Configurable prompt message shown to players
- If `resource-pack.enabled: false`, skip the send (for servers that manage the pack through `server.properties` instead)

---

### Custom Enchantment Effects — Ability Triggers (`rpg-enchanting`) — 🔴 Hard
Currently enchantments only add stat modifiers (e.g., `+5 Strength`). A natural extension is ability-trigger enchantments — enchants that fire an ability effect when specific conditions are met.

**Enchant YAML extension:**
```yaml
vampiric_edge:
  DisplayName: "&4Vampiric Edge"
  Description: "Drain life from every strike."
  Rarity: RARE
  Triggers:
    - Event: on_hit          # on_hit | on_kill | on_hurt | on_use
      Chance: 25.0           # % chance per event
      Ability: drain         # ability effect id from rpg-core
      Level: 3
```

- Trigger events: `on_hit` (melee attack lands), `on_kill` (killing blow), `on_hurt` (player takes damage), `on_use` (right-click with item)
- Chance per trigger is configurable per-enchant
- The ability effect fires the same pipeline as regular ability effects — uses the same `DamageEffect`, `HealEffect`, etc.
- Multiple triggers allowed per enchant (e.g., on_hit fires drain + on_kill fires a death nova)
- Needs an `EnchantTriggerListener` in `rpg-enchanting` that intercepts the relevant events and checks equipped item enchants

---

### Pets System (`rpg-pets`) — ⚫ Very Hard
Referenced in several other systems (`pet_luck` stat, `pet slot` in Stats GUI, companion slot in profile GUI) but no plugin exists yet. This is a large standalone feature — placeholder entry to track it.

**High-level planned scope:**
- Egg/capture system: pets obtained via loot drops or special items
- Pet entity that follows the player (either a real entity or a passenger entity on the player)
- Pets level up separately from the player; gain XP from kills the player makes
- Pet abilities: configurable list of abilities that fire on timers or combat events
- Pet stats: contribute `pet_luck`, passive HP/damage bonuses to the owner
- Pet storage: `/pets` GUI to view, equip, and release pets
- Multiple pets owned per player, one active at a time
- Persisted via `DataStore` (pet species, level, XP, name, equipped status)

This is a large plugin — build in phases. Phase 1: pet item + summon + follow AI. Phase 2: levelling + stats. Phase 3: abilities. Phase 4: GUI.

---

### Auction House (`rpg-auction-house`) — ⚫ Very Hard
**New plugin — nothing exists yet.**

- Player-posted item listings with custom price (uses sign-entry for price input)
- Browse GUI: filterable by name, category, or seller
- `/ah` command: main browser, my listings, create listing, expired returns
- Listing expiry — unsold listings return the item after a configurable duration (delivered via mail system)
- Configurable listing fee (% of sale), max listings per player
- Admin commands: `/ah list <player>`, `/ah remove <id>`, `/ah wipe`
- Non-tradeable items blocked from listing
- See `docs/planned/auction-house.md` for full layout spec

---

### Bazaar (`rpg-bazaar`) — 🔴 Hard
**New plugin — nothing exists yet.**

- Admin-defined fixed-price buy/sell listings organized in categories
- Infinite or limited stock with configurable restock intervals
- Browse GUI with category tabs, stock counts
- `/bazaar` command
- Admin commands: `/bazaar reload`, `/bazaar add`, `/bazaar remove`, `/bazaar stock <item> <amount>`
- Non-tradeable items blocked
- See `docs/planned/bazaar.md` for full layout spec

---
