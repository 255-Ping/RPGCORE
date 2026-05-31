# 🔴 New Features — Not Started

_Full features or systems that don't exist at all yet._

---

### Player Homes + Server Warps (`rpg-admin`)
Conspicuously absent from the suite. Every RPG server needs these.

- `/sethome [name]`, `/home [name]`, `/delhome [name]`, `/homes` — per-player saved locations, configurable max homes per permission group
- `/setwarp <name>`, `/warp <name>`, `/delwarp <name>`, `/warps` — admin-defined server-wide teleport points
- Homes + warps persist via `DataStore`
- Teleport delay configurable (cancel on damage or movement)
- Goes in `rpg-admin` alongside the existing fly/gmc/heal/tp commands

---

### Achievement System (`rpg-core` or new `rpg-achievements`)
No achievement tracking exists anywhere in the suite. RPG servers rely on achievements heavily for progression milestones and player retention.

- Admin-defined achievements in YAML: id, display name, description, hidden (secret) flag, rewards (currency, skill XP, items)
- Trigger types: stat threshold (`money >= 1000000`), skill level (`mining >= 25`), kill count (`mob: goblin >= 100`), quest completion, item obtained, etc.
- Per-player progress tracked via `DataStore`
- Achievement unlock toast notification (title or action bar)
- `/achievements` command — opens a GUI browser (locked/unlocked, categories)
- Optional: link to `Bukkit.Achievements` or custom only

---

### Boss Bar System (`rpg-core`)
No boss bar support exists. Needed for dungeons (progress/timer), world events, and large mobs.

- API: `BossBarService.show(player, text, progress, color, style)` / `hide(player, barId)`
- Dungeon integration: show a "Mobs Remaining: X/Y" bar for all players in the instance
- World boss integration (see World Events below)
- Configurable per use-case; bars auto-hide on death / dungeon exit

---

### World Events + World Boss (`rpg-core` or new `rpg-events`)
Periodic server-wide events add communal engagement that solo play can't. Planned:

- Admin-configurable event schedule (cron-like interval or manual `/event start <id>`)
- Event types: world boss spawn (named mob at a configured location, shared HP, loot pool per-player), resource rush (higher drop rates for N minutes), invasion (wave of strong mobs at a location)
- Boss bar + broadcast announce when event starts/ends
- Per-player loot rolls on completion

---

### Salvaging System (`rpg-core` or `rpg-enchanting`)
Players break down unwanted RPG items into materials at a salvage station (custom block type).

- Right-clicking a salvage-station block opens a 1-slot GUI
- Place an RPG item → shows preview of what you'll get
- Confirm button → item consumed, materials given
- Salvage yield configurable per rarity or per item YAML (`Salvage: [list]`)
- Admins can mark items as non-salvageable (`Salvageable: false`)

---

### Starter Kit System (`rpg-admin`)
New players joining for the first time should receive a configured starting set of items.

- Admin defines kit contents in `config.yml` (list of item ids + amounts)
- Given automatically on first join (tracked per UUID in `DataStore` so it's only given once)
- `/kit` command to claim manually if they missed it (once per UUID)
- Optional: multiple kits gated by permission (`rpg.kit.starter`, `rpg.kit.vip`, etc.)

---

### Item Set Bonuses (`rpg-core`)
Wearing multiple pieces of the same named set grants additional stat bonuses. Very standard RPG feature.

- Items in a set share a `Set` YAML field (e.g., `Set: mages_robes`)
- Set definition file in `sets/` folder: display name, bonus tiers (`2-piece: { intelligence: 30 }`, `4-piece: { ability_damage: 50, cooldown_reduction: 10 }`)
- Bonuses apply on top of individual item stats during `recalculateStats()`
- Active set bonuses shown at the bottom of the item lore (e.g., `&5Mage's Robes (2/4): Intelligence +30`)
- `/stats` GUI shows active set bonuses in a dedicated section

---

### Leaderboards (`rpg-core`)
No `/top` or leaderboard command exists anywhere in the suite.

- `/top [skill|money|level]` — shows top N players for the chosen category
- Reads from `DataStore` — either a live scan or a cached snapshot updated on configurable interval
- Configurable number of entries shown, update interval
- `/top` with no args opens a GUI with category tabs

---

### Elite / Champion Mob Variants (`rpg-core`)
Randomly enhanced mob spawns that are rarer, stronger, and drop better loot. Standard RPG engagement mechanic.

- Configurable chance per-mob-spawn that it becomes an "elite" variant (e.g., 5%)
- Elite mobs get a configurable stat multiplier (HP × 3, damage × 2, etc.)
- Distinct display name prefix (e.g., `&6⚜ &r` prefix before the mob name)
- Separate loot table or loot multiplier defined on the mob YAML (`EliteLootTable`, `EliteLootMultiplier`)
- Particle effect on elite spawn (configurable)
- Optional: champion tier above elite with even bigger multipliers

---

### Extract Smelting → `rpg-smelting` Plugin (`rpg-core` / new `rpg-smelting`)
Smelting recipe loading (`SmeltingLoader.java`) currently lives in `rpg-core`. It should be its own addon plugin so servers that don't need custom smelting don't load it, and so it can be expanded independently later.

- Create a new blank `rpg-smelting` module (same pattern as `rpg-cooking` / `rpg-alchemy`)
- Move `SmeltingLoader` and any smelting-specific YAML content out of `rpg-core` into the new plugin
- The `smelting: true` vanilla-suppression flag stays in `rpg-core/config.yml` — it's a world-toggle, not addon-specific
- `rpg-core` soft-depends on `rpg-smelting`; if the plugin isn't loaded, smelting suppression still applies but no custom recipes are registered
- Stub out a `config.yml` and `recipes/example.yml` the same way cooking does; flesh out further functionality later

### Extract Crafting → `rpg-crafting` Plugin (`rpg-core` / new `rpg-crafting`)
Same rationale as smelting. `RecipeLoader.java` (shaped/shapeless crafting) currently lives in `rpg-core`.

- Create a new blank `rpg-crafting` module
- Move `RecipeLoader` and crafting YAML content out of `rpg-core` into the new plugin
- The `crafting: true` vanilla-suppression flag stays in `rpg-core/config.yml`
- Stub out `config.yml` and `recipes/example.yml`; flesh out further later

---

### Sign-Entry Number Input (`rpg-core`)
**Required before:** Auction House, Bazaar, Guild Bank GUI, any other GUI that takes a currency or quantity input from the player.

Needed everywhere a player types a numeric value (currency amount, quantity, price) inside a GUI. Build once as a shared `SignEntryService` in `rpg-core` so every addon can call it — don't re-implement per-plugin.

- Open a virtual sign via `PacketPlayOutOpenSignEditor`
- Line 1 = prompt label (e.g., `Enter Price:`)
- Player types the number on the sign and confirms
- Parse `PacketPlayInUpdateSign`; on invalid input, re-open the sign with an error hint on line 2
- Callback-based API: `SignEntryService.open(player, prompt, onResult)`

**Reference implementation:** SurvivalCore repo.

---

### PlaceholderAPI Support (`rpg-hud` / `rpg-core`)
Allow PlaceholderAPI placeholders (e.g., `%player_name%`, `%vault_balance%`) to be used anywhere RPGCORE reads a template string — scoreboard lines, tablist header/footer, nametag format, action bar format, etc.

- Soft-depend on PlaceholderAPI in `rpg-hud` and `rpg-core`
- In `PlaceholderResolver.resolve()`, if PAPI is present, run `PlaceholderAPI.setPlaceholders(player, template)` before or after RPGCORE's own `{placeholder}` pass
- Also register RPGCORE's own stats/skills/balance as a PAPI expansion so other plugins can read them

---

### Auction House (`rpg-auction-house`)
**New plugin — nothing exists yet.**

- Player-posted item listings with custom price (uses sign-entry for price input)
- Browse GUI: filterable by name, category, or seller
- `/ah` command: main browser, my listings, create listing, expired returns
- Listing expiry — unsold listings return the item after a configurable duration
- Configurable listing fee (% of sale), max listings per player
- Admin commands: `/ah list <player>`, `/ah remove <id>`, `/ah wipe`
- Non-tradeable items blocked from listing
- See `docs/planned/auction-house.md` for full layout spec

---

### Bazaar (`rpg-bazaar`)
**New plugin — nothing exists yet.**

- Admin-defined fixed-price buy/sell listings organized in categories
- Infinite or limited stock with configurable restock intervals
- Browse GUI with category tabs, stock counts
- `/bazaar` command
- Admin commands: `/bazaar reload`, `/bazaar add`, `/bazaar remove`, `/bazaar stock <item> <amount>`
- Non-tradeable items blocked
- See `docs/planned/bazaar.md` for full layout spec

---
