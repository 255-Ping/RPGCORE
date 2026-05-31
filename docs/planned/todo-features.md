# 🔴 New Features — Not Started

_Full features or systems that don't exist at all yet._

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
