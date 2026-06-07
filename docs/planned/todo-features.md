# 🔴 New Features — Not Started

_Full features or systems that don't exist at all yet._

> **Difficulty scale:** 🟢 Easy (< 1 day) · 🟡 Medium (1–2 days) · 🔴 Hard (several days) · ⚫ Very Hard (week+)

---

### Salvaging System (`rpg-salvaging`) — 🔴 Hard

Players feed unwanted items into a **Salvager block** to recover coins, XP levels, and occasionally the reforges/upgrades that were applied to those items. Yield scales with item rarity and the player's Salvaging skill level.

---

#### Block

A new custom RPG block type (`salvager`, defined in `blocks/`) that opens the Salvage GUI on right-click. Placed by admins via `/rpg item give salvager_block`. Hooks into the existing `BlockInteractListener` dispatch.

```yaml
# blocks/salvager.yml
id: salvager
DisplayName: "&6Salvager"
Material: GRINDSTONE          # or a resource-pack custom-model-data override
Interactable: true
# no Toughness / RequiredPower — can't be broken by players in protected areas
```

---

#### GUI — 54-slot inventory

```
[ 0  1  2  3  4  5  6  7  8 ]   ← row 1 \
[ 9 10 11 12 13 14 15 16 17 ]   ← row 2  |
[18 19 20 21 22 23 24 25 26 ]   ← row 3  | 36 input slots (rows 1–4)
[27 28 29 30 31 32 33 34 35 ]   ← row 4 /
[36 37 38 39 40 41 42 43 44 ]   ← row 5 (border separator)
[45 46 47 48 49 50 51 52 53 ]   ← row 6 (action bar)
                  ↑        ↑
              close(49)  scrap(52)
         yield-preview(46)
```

**Row 5** — all border panes (visual separator between input grid and action bar).

**Row 6 slots:**
| Slot | Item | Behaviour |
|---|---|---|
| 45, 47, 48, 50, 51, 53 | Border pane | Static filler |
| 46 | **Yield Preview** | Updates live as items are added/removed. Shows estimated coin yield, XP levels, and recovery chances in lore. Icon = `GOLD_NUGGET`; turns `EMERALD` if the grid is non-empty. |
| 49 | **Close** (standard nav bar) | Returns all items in the grid to the player's inventory (same as closing the GUI normally). |
| 52 | **Scrap All** | Executes salvage of every item in the grid; credits rewards; closes the GUI. `LIME_DYE` icon, turns `GRAY_DYE` (with lore "Add items first") when grid is empty. |

**On any close** (whether via ✕, Close button, or server/disconnect): every `ItemStack` in slots 0–35 that is non-null is returned to the player's inventory; overflow drops at their feet.

---

#### What can be salvaged

An item is salvageable unless any of the following is true:
- Its YAML definition includes `Salvageable: false` (per-item opt-out).
- Its material is in `salvaging.blocked-materials` config list.
- Its RPG item ID is in `salvaging.blocked-items` config list.
- It is a vanilla item (no RPG stats) AND `salvaging.block-vanilla-items: true` (default true).
- It has the PDC key `rpg_no_salvage` set to `1` (runtime flag, for quest items etc.).

Non-salvageable items placed in the grid are immediately returned with an action-bar warning: `"&c<item name> cannot be salvaged."` They are never held in the grid.

---

#### Yield calculation

**Coins:**
```
base_value      = item's ShopValue (defined on RpgItem YAML), or 0 if absent
rarity_mult     = config rarity-multipliers[item.Rarity]   (see config below)
skill_bonus     = 1.0 + (salvagingLevel × config.yield-per-level)
variance        = random in [1.0 - yield-variance, 1.0 + yield-variance]
coin_yield      = base_value × rarity_mult × skill_bonus × variance
```
Minimum coin yield is 1 (never zero if the item was salvageable).

**XP levels** (only if the item has at least one vanilla enchantment):
```
raw_xp = sum of (enchant.level × config.enchant-xp-per-level) for each enchantment
xp_yield = ceil(raw_xp × skill_bonus)
```
Granted via `player.giveExpLevels(xpYield)`.

**Reforge recovery** (only if item has a reforge applied):
```
roll < config.reforge-recovery-chance + (salvagingLevel × config.recovery-per-level)
→ give the matching reforge stone ItemStack to the player
```

**Upgrade recovery** (for each upgrade scroll applied to the item):
```
roll < config.upgrade-recovery-chance + (salvagingLevel × config.recovery-per-level)
→ give one matching upgrade scroll ItemStack to the player
```

Recovery items go directly to the player's inventory (overflow drops at feet). Each reforge/upgrade is rolled independently.

---

#### Yield Preview item

The preview icon in slot 46 recalculates every time the contents of slots 0–35 change (`InventoryClickEvent` / `InventoryDragEvent`). Lore shows the summed estimate across all items currently in the grid:

```
§6Estimated yield
§e  Coins:     ~$1,240 – $1,560
§b  XP Levels: ~4
§a  Reforges:  2 items (15% chance each)
§a  Upgrades:  3 items (10% chance each)
§7
§7Salvaging skill bonus: +12%
```

Ranges are shown using the variance bounds so the player understands it's not a fixed number.

---

#### Config (`plugins/rpg-salvaging/config.yml`)

```yaml
salvaging:
  # Fraction of ShopValue returned as coins, by rarity.
  rarity-multipliers:
    COMMON:    0.10
    UNCOMMON:  0.15
    RARE:      0.25
    EPIC:      0.40
    LEGENDARY: 0.60
    MYTHIC:    1.00

  # Vanilla XP levels granted per enchantment level on the salvaged item.
  enchant-xp-per-level: 1

  # Base probability (0.0–1.0) of recovering a reforge stone.
  reforge-recovery-chance: 0.15

  # Base probability (0.0–1.0) of recovering each applied upgrade scroll.
  upgrade-recovery-chance: 0.10

  # Additive yield bonus per Salvaging skill level (e.g. 0.005 = +0.5%/level).
  yield-per-level: 0.005

  # Additive recovery-chance bonus per Salvaging skill level.
  recovery-per-level: 0.005

  # Yield variance: actual coins = calculated ± this fraction.
  yield-variance: 0.15

  # Prevent vanilla (non-RPG) items from being salvaged.
  block-vanilla-items: true

  # RPG item IDs that cannot be salvaged.
  blocked-items: []

  # Vanilla materials that cannot be salvaged.
  blocked-materials: []

  # Salvaging XP formula: 1 skill-XP per this many coins of yield.
  xp-per-coin: 10.0
```

---

#### Salvaging skill

New `BuiltinSkill.SALVAGING` entry in `rpg-api`. XP is awarded at the moment of scrapping, proportional to total coin yield (`yield / config.xp-per-coin`).

Milestone bonuses (applied passively via `SkillsService.snapshot()`; stored in stat sheet as `salvaging_yield_bonus` and `salvaging_recovery_bonus` virtual stats):

| Level | Bonus |
|---|---|
| 1–49 | +0.5% yield and +0.5% recovery per level (from `yield-per-level` / `recovery-per-level` config) |
| 50 | **Expert Salvager** — reforge recovery floor raised to 50%; upgrade recovery floor raised to 25% |
| 100 | **Master Salvager** — guaranteed at least one reforge or upgrade returned per salvage (if any were applied) |

Milestone bonuses at 50 and 100 are additive on top of the base formula, not replacing it.

---

#### Implementation notes

- New Gradle module `rpg-salvaging`. Depends on `rpg-api` and `rpg-core`.
- `SalvageGui` — 54-slot inventory; `Map<UUID, SalvageSession>` tracks open sessions.
- `SalvageSession` — holds the `Inventory`, `Player`, and the current calculated yield (recalculated on every grid change).
- Grid change detection: `InventoryClickEvent` + `InventoryDragEvent` filtered to the salvage inventory instance; both re-run `recalculate()` after the event resolves (scheduled 1-tick-later so item moves have settled).
- Close handling: `InventoryCloseEvent` — if session was closed without scrapping, return all items and remove session.
- `SalvageBlockListener` — `PlayerInteractEvent` on the salvager custom block → open GUI.
- Reforge/upgrade item lookup: need a way to turn the stored reforge ID / upgrade ID back into an ItemStack. Add `EnchantingService.reforgeItem(id)` and `upgradeItem(id)` to `rpg-api` (or look up from `ItemRegistry` if reforge stones are registered RPG items).
- `BuiltinSkill.SALVAGING` — add to `rpg-api`; `CoreSkillRegistry` picks it up automatically.
- Add `Salvageable: true` field to `RpgItem` YAML (default `true`); loaded in `ItemLoader`, stored on `CoreRpgItem`, checked by `SalvageGui`.

---

### ✅ Main Menu Item (`rpg-core`) — shipped in 1.10.0

A persistent hotbar item that acts as a hub into all major player-facing GUIs. Every player always has it; it cannot be removed, dropped, or moved.

**Item behaviour:**
- Placed in **hotbar slot 8** (last slot) on join and on respawn
- If missing from slot 8 for any reason (edgecases, plugin restart): restored silently on the next inventory interaction
- `/menu` command re-gives it if somehow lost (no permission required)
- The item itself is configurable in `config.yml` — material, name, custom model data (for a resource-pack icon)

**Protected interactions — all cancelled silently:**
- Drop (Q key) → cancelled, item stays in slot 8
- Move to offhand (F key / swap key) → cancelled
- Drag out of hotbar via inventory screen → cancelled, item snaps back
- Death → item is not added to the death drop list; restored to slot 8 on respawn

**Right-click** → opens the [Main Menu GUI](#main-menu-gui-rpg-core----medium) (see [GUI Redesigns](todo-gui.md) for the full layout spec)

Implementation: `PlayerInteractEvent` for right-click, `InventoryClickEvent` + `InventoryDragEvent` for click-cancel, `PlayerDropItemEvent` for Q-key cancel, `PlayerDeathEvent` to strip it from drops, `PlayerRespawnEvent` to restore. Tag the item with PDC key `rpg_menu_item` so it can be identified reliably regardless of name/material changes.

---

### Item Browser GUI (`rpg-core`) — 🟡 Medium

A `/rpg items` command (alias `/items`) that opens a paginated GUI showing every item registered across all loaded RPG plugins. Primarily an admin-facing tool, but can be made player-visible via permission.

**Command:**
- `/rpg items` — opens the browser
- `/rpg items <search>` — opens with the search term pre-applied
- Permission: `rpg.items.browse` (default: op) to open; `rpg.items.give` (default: op) to click-give items

**GUI layout (54 slots):**
- Row 1: filter toggle buttons (one slot per configured filter; active filter highlighted with enchant glint). Last slot = clear-all-filters button.
- Row 2: search button (opens sign-entry prompt; current search shown in button lore), result count, prev/next page arrows
- Rows 3–6 (36 slots): item display grid — actual `ItemStack` of each matching RPG item, built via `ItemRegistry`
- Clicking an item with `rpg.items.give`: gives one copy to the clicker's inventory (or opens a quantity prompt if shift-clicked)

**Search:** case-insensitive substring match against the item's display name and its YAML id. Combine-able with active filters.

**Configurable filters** — defined in `plugins/rpg-core/item-browser.yml`. Each filter entry specifies a display name, GUI icon, and one or more match criteria. Admins create, remove, or rename filters in this file; `/rpg reload` picks up changes without a restart.

```yaml
filters:
  weapons:
    DisplayName: "&cWeapons"
    Icon: IRON_SWORD
    Match:
      Type: [SWORD, BOW, CROSSBOW, WAND]

  armor:
    DisplayName: "&9Armor"
    Icon: IRON_CHESTPLATE
    Match:
      Type: [HELMET, CHESTPLATE, LEGGINGS, BOOTS, ACCESSORY]

  potions:
    DisplayName: "&dPotions"
    Icon: POTION
    Match:
      Type: [POTION, FOOD]

  tools:
    DisplayName: "&eMining Tools"
    Icon: IRON_PICKAXE
    Match:
      Type: [PICKAXE, AXE, SHOVEL]

  # Tag-based filter — matches items with `Tag: rare` in their YAML
  rare:
    DisplayName: "&6Rare+"
    Icon: NETHER_STAR
    Match:
      Tag: rare
```

**Match criteria (all optional, ANDed together within one filter):**
- `Type: [...]` — matches items whose `Type:` field is in the list
- `Tag: <value>` — matches items with a `Tag:` field equal to this value (or one of a list)
- `Stat: { id: DAMAGE, min: 50 }` — matches items with at least N of a given stat (useful for "high damage" filter)
- `Plugin: <plugin-name>` — matches items registered by a specific addon (e.g., `rpg-alchemy` only)

Multiple filters can be active simultaneously — results must satisfy **all** active filters (AND logic). Search is applied on top of whatever filters are active.

**Pagination:** 36 items per page; prev/next arrows hidden when not applicable. Page resets to 1 on any filter or search change.

**Implementation notes:**
- Source of truth is `RpgServices.items()` (`ItemRegistry` API, backed by `CoreItemRegistry`). All plugins register their items on enable. No file-scanning at GUI open time — just iterate the already-loaded registry.
- Filter config loaded by a new `ItemBrowserConfig` class; reloaded on `/rpg reload`.
- Sign-entry for search requires the `SignEntryService` (see above — build that first if it doesn't exist, or use a book-and-quill fallback in the interim).
- Filter slots in row 1 are built dynamically from the YAML; if more than 8 filters are defined, overflow filters are hidden (log a warning on load).

---

### ✅ Player Homes + Server Warps (`rpg-homes`) — shipped in 0.1.0
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

### ✅ Boss Bar System (`rpg-bossbar`) — shipped in 0.1.0
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

### ✅ Starter Kit System (`rpg-kits`) — shipped in 0.1.0
New players joining for the first time should receive a configured starting set of items.

- Admin defines kit contents in `config.yml` (list of item ids + amounts)
- Given automatically on first join (tracked per UUID in `DataStore` so it's only given once)
- `/kit` command to claim manually if they missed it (once per UUID)
- Optional: multiple kits gated by permission (`rpg.kit.starter`, `rpg.kit.vip`, etc.)

---

### ✅ Item Set Bonuses (`rpg-core`) — already shipped
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

### ✅ Extract Smelting → `rpg-smelting` Plugin — shipped in 0.1.0
Smelting recipe loading (`SmeltingLoader.java`) currently lives in `rpg-core`. It should be its own addon plugin so servers that don't need custom smelting don't load it, and so it can be expanded independently later.

- Create a new blank `rpg-smelting` module (same pattern as `rpg-cooking` / `rpg-alchemy`)
- Move `SmeltingLoader` and any smelting-specific YAML content out of `rpg-core` into the new plugin
- The `smelting: true` vanilla-suppression flag stays in `rpg-core/config.yml` — it's a world-toggle, not addon-specific
- `rpg-core` soft-depends on `rpg-smelting`; if the plugin isn't loaded, smelting suppression still applies but no custom recipes are registered
- Stub out a `config.yml` and `recipes/example.yml` the same way cooking does
- **Build timed crafting in from the start** — `rpg-smelting` should support a `CraftTime` field on recipes (same model as cooking/alchemy). See [Timed Smelting](todo-improvements.md) in Improvements for the full spec. Don't ship the plugin without it — retrofitting timed crafting later is messier than including it upfront.

### ✅ Extract Crafting → `rpg-crafting` Plugin — shipped in 0.1.0
Same rationale as smelting. `RecipeLoader.java` (shaped/shapeless crafting) currently lives in `rpg-core`.

- Create a new blank `rpg-crafting` module
- Move `RecipeLoader` and crafting YAML content out of `rpg-core` into the new plugin
- The `crafting: true` vanilla-suppression flag stays in `rpg-core/config.yml`
- Stub out `config.yml` and `recipes/example.yml`; flesh out further later

---

### ✅ Sign-Entry Number Input (`rpg-core`) — shipped in 1.8.0
**Required before:** Auction House, Bazaar, Guild Bank GUI, any other GUI that takes a currency or quantity input from the player.

Needed everywhere a player types a numeric value (currency amount, quantity, price) inside a GUI. Build once as a shared `SignEntryService` in `rpg-core` so every addon can call it — don't re-implement per-plugin.

- Open a virtual sign via `PacketPlayOutOpenSignEditor`
- Line 1 = prompt label (e.g., `Enter Price:`)
- Player types the number on the sign and confirms
- Parse `PacketPlayInUpdateSign`; on invalid input, re-open the sign with an error hint on line 2
- Callback-based API: `SignEntryService.open(player, prompt, onResult)`

**Reference implementation:** SurvivalCore repo.

---

### ✅ PlaceholderAPI Support (`rpg-hud`) — shipped in 0.4.1
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

### ✅ Resource Pack Auto-Delivery (`rpg-core`) — shipped in 1.5.2
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

### Waypoints System (`rpg-core`) — 🟡 Medium

A player-facing fast-travel / spawn-point system using a **custom placeable block** (not admin warps — those stay in `/warp`). Waypoints are physical blocks placed by admins in the world; players interact with them to bind their respawn point.

**Why not use warps:** `/warp` destinations are admin-only lists. Waypoints are world objects players discover and interact with — exploration-flavoured spawn binding rather than a menu of teleport commands.

---

#### Block

New custom RPG block type (`waypoint`, defined in `blocks/`):

```yaml
# blocks/waypoint.yml
id: waypoint
DisplayName: "&b✦ Waypoint"
Material: BEACON        # or any material; resource-pack can replace it
Interactable: true
```

The block is placed by admins via `/rpg item give waypoint_block` and cannot be broken by non-creative players (standard custom-block protection from `BlockBreakHandler`).

---

#### Visual effects

Both configurable in the block's YAML definition or in a `waypoints.yml` config:

**Particles:**
```yaml
waypoint:
  particles:
    enabled: true
    type: END_ROD       # any Particle enum name
    count: 5
    offset-x: 0.3
    offset-y: 0.5
    offset-z: 0.3
    speed: 0.02
    interval-ticks: 10  # how often particles spawn (10 = twice/sec)
```

**Hologram:**
```yaml
waypoint:
  hologram:
    enabled: true
    lines:
      - "&b✦ Waypoint"
      - "&7Right-click to set spawn"
    height-offset: 1.5  # blocks above the block's top face
```

The hologram line can include the waypoint's `Name` field (set by the admin via `/waypoint setname <id> <name>`), so different waypoints display different names: `"&b✦ Starting Village"` vs `"&b✦ The Ruins"`.

---

#### Player interaction

**Non-creative players — right-click:**
1. Check cost (see below). If the player can't afford it, send action-bar message and cancel.
2. Deduct cost.
3. Set the player's spawn point to the waypoint block's location + Y+1 (so they respawn standing on it).
4. Send confirmation message + play a sound (`BLOCK_BEACON_ACTIVATE` or configurable).
5. Grant an achievement trigger `waypoint_set` (for discovery-based achievements).

**Creative players (or players with `rpg.waypoint.free` permission):** no cost charged.

**Setting your spawn at the same waypoint twice** is free (idempotent — no re-charge).

---

#### Cost configuration

Per-waypoint cost override OR a global default in `waypoints.yml`:

```yaml
waypoints:
  default-cost:
    type: none            # none | coins | experience | item
    amount: 0             # coins / XP levels; ignored for "item" type
    item: null            # item id for type:item, e.g. "teleport_crystal"
    item-amount: 1
    # "experience" = Minecraft vanilla XP levels (player.getLevel())

  # Per-waypoint overrides (keyed by waypoint block UUID or admin-assigned ID):
  overrides:
    "uuid-or-id-here":
      type: coins
      amount: 500
```

The cost is charged every time a player sets their spawn, not on each respawn. "Item" type consumes one (or more) of the specified RPG item from the player's inventory.

---

#### Admin commands

| Command | Description |
|---|---|
| `/waypoint list` | List all placed waypoints (UUID, location, name, cost) |
| `/waypoint setname <id> <name>` | Give a waypoint a display name (shown in hologram) |
| `/waypoint setcost <id> <type> <amount>` | Set the interaction cost for a specific waypoint |
| `/waypoint reload` | Reload `waypoints.yml` config |

Waypoints are identified by the block UUID (stored in `BlockPersistence`) — no separate data file needed.

---

#### Implementation notes

- Hook into `BlockInteractListener`'s existing dispatch — when a player right-clicks a block with PDC id `waypoint`, fire `WaypointInteractHandler`.
- `WaypointInteractHandler` reads cost from `WaypointsConfig`, deducts it via `RpgServices.economy()` (coins) or `player.setLevel/getLevel()` (XP), then calls `player.setBedSpawnLocation(block.getLocation(), true)`.
- Particle emission: a repeating `BukkitTask` per placed waypoint. Spawned in `WaypointBlockListener` on `BlockPlaceEvent` for the custom block; cancelled on `BlockBreakEvent`. Tasks stored in `Map<Location, BukkitTask>` in `WaypointParticleManager`. On reload, all tasks are cancelled and restarted.
- Holograms: use `BlockHologramService` (already exists in `rpg-core`) — each waypoint block registers a hologram on place, unregisters on break. Or build atop `rpg-holograms` if loaded (soft-dep).
- Achievement trigger: `WaypointInteractHandler` calls `RpgServices.achievements().grant(player, "waypoint_set")` for a manual-type achievement.

---

### Vault / Storage System (`rpg-core`) — 🔴 Hard

Per-player persistent storage vaults — extra inventory pages players unlock over time, similar to Hypixel's storage upgrades. Admins configure how many vaults exist, their sizes, and what each one requires to unlock.

---

#### Player experience

- `/vault [n]` — opens vault number `n` (defaults to 1). Tab-completes to the player's unlocked vault numbers.
- Players can see all vaults via a **vault selector GUI** (opened with `/vault` or `/vault list`) — a 54-slot overview showing each vault slot as a chest icon, with lock status and unlock requirements in lore.
- Items in vaults persist between sessions (stored in `DataStore`).
- Vaults are **per-player** — not shared, not accessible by other players (admins can use `/vault <player> [n]` to inspect).

---

#### Admin configuration (`plugins/rpg-core/vaults.yml`)

```yaml
vaults:
  # Default row count for a vault if not overridden per-vault (1-6 rows → 9-54 slots).
  default-rows: 6

  # Vault definitions — add as many entries as you want, numbered 1 upward.
  # The vault count is derived from however many entries you define here; there is no cap.
  # Vault 1 is always unlocked (starter vault). All others require the configured unlock.
  unlocks:
    1:
      name: "Vault I"
      rows: 3            # starter vault is smaller
      requires: none     # always unlocked

    2:
      name: "Vault II"
      rows: 6
      requires:
        type: coins
        amount: 10000

    3:
      name: "Vault III"
      rows: 6
      requires:
        type: experience  # Minecraft XP levels
        amount: 30

    4:
      name: "Vault IV"
      rows: 6
      requires:
        type: item
        item: vault_key   # RPG item id
        amount: 1         # consumes this many on unlock

    5:
      name: "Vault V"
      rows: 6
      requires:
        type: permission
        permission: rpg.vault.5  # granted by rank/LuckPerms; no cost deducted

    # Add more entries to create more vaults — no limit.
    # 6:
    #   name: "Vault VI"
    #   rows: 6
    #   requires:
    #     type: coins
    #     amount: 500000
```

**Unlock types:**

| Type | Behaviour |
|---|---|
| `none` | Always unlocked |
| `coins` | Deducts via `RpgServices.economy()`. One-time payment. |
| `experience` | Deducts Minecraft XP levels (`player.setLevel`). One-time payment. |
| `item` | Consumes N of the specified RPG item from inventory. One-time. |
| `permission` | Checks `player.hasPermission(...)`. No deduction — grants access while permission holds. If permission is removed, vault is re-locked but **contents are preserved** (player can't access them until permission is re-granted or they unlock another way). |

---

#### Vault Selector GUI

54-slot paginated overview. Each vault is represented by a chest (or custom icon). Content area is slots 0–44 (rows 1–5, 45 slots per page). Row 6 is the nav bar with Previous / Next page arrows when there are more than 45 vaults.

```
Slot 0 → Vault 1    Slot 1 → Vault 2    Slot 2 → Vault 3 ...
```

Since the vault count is unlimited, pagination is required for large configs. Most servers will have ≤ 10 vaults and never hit the second page.

**Unlocked vault icon:**
- Material: `CHEST`
- Name: `"&6Vault I"` (or configured name)
- Lore: `"&7Slots: 54"`, `"&aUnlocked"`, `"&7Click to open"`

**Locked vault icon:**
- Material: `TRAPPED_CHEST` (or BARRIER)
- Name: `"&7Vault II"` (grayed)
- Lore: `"&cLocked"`, `"&7Requires: &e$10,000"` (using currency formatting)
- Left-click → shows requirements in chat and plays a deny sound; does NOT auto-deduct

**Unlock button:** when a player clicks a locked vault they can afford/qualify for, a confirm prompt appears — either a second click on the same item ("Click again to unlock") or a confirmation slot that pops up. On confirm, cost is deducted and vault is unlocked.

---

#### Implementation notes

- `VaultConfig` — loads `vaults.yml`. Holds a `Map<Integer, VaultDef>` (`VaultDef`: name, rows, `UnlockRequirement`) keyed by vault number. The map can have any number of entries — no upper limit. `maxVault()` returns the highest defined key. Reloaded via `/rpg reload`.
- `VaultService` — interface in `rpg-api`. Methods: `isUnlocked(Player, int vault)`, `unlock(Player, int vault)`, `openVault(Player, int vault)`, `openSelector(Player)`.
- `CoreVaultService` — implementation. Stores per-player unlock state in `DataStore` under key `"vaults"` (map of vault number → `true`). Vault 1 always marked unlocked on first access. Item contents stored under `"vault_contents_<n>"` (serialized `ItemStack[]`).
- `VaultGui` — standard 54-slot (or configured rows × 9) inventory. Nav bar on last row. Items shift up if rows < 6 so the nav bar is always on the visible bottom row.
- `VaultSelectorGui` — paginated selector (45 vaults per page, nav bar on row 6). Clicking a locked vault shows requirements; clicking an unlocked vault opens it nested (Back → selector).
- `VaultCommand` — `/vault [n]`, tab-completes to all defined vault numbers (so players can inspect locked ones' requirements).
- Vault contents are saved on `InventoryCloseEvent` and on `PlayerQuitEvent` as serialized `ItemStack[]`. Loaded lazily on first open.
- **Contents on permission-revoke:** vault contents are never wiped on lock. If a permission-gated vault locks again, contents stay in `DataStore` until access is re-granted.
- Add `vault` command to `plugin.yml` with permission `rpg.core.vault` (default: true) and `rpg.core.vault.other` (default: op).

---
