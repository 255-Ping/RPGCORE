# RPGCore Backlog

Items collected from testing and review, sorted by complexity. Pull into a session when ready.

---

## Phase 2 — Medium complexity

### ~~Global region in /region commands~~ ✅ Fixed in rpg-regions 0.5.0
### ~~Config examples everywhere~~ ✅ Done — all 20 config.yml files annotated
### ~~Shift-click in other GUIs~~ ✅ Fixed in rpg-alchemy 0.2.0 (BrewingGui) + rpg-enchanting 0.2.0 (StationGui); BankerGui has no input slots (buttons only, correctly blocks all bottom-inv clicks)

### GUI formatting consistency pass
All GUIs should follow the formatting rules in `docs/formatting.md`. Do a sweep: pane colors, title colors, lore format, button naming conventions.

---

## Phase 3 — Major features

### /trade command
Full player-to-player trade system.
- `/trade <player>` — sends a trade invite
- Target can do `/trade accept` or click the chat message to open the trade GUI
- Trade GUI: each player has a 3×3 item grid + a coin input area (use the sign-entry pattern from SurvivalCore)
- Configurable countdown (e.g. `trade.countdown-seconds: 5`) before items are swapped
- Items can be marked `tradeable: false` in their item definition — affects trade AND future bazaar/AH
- Permission: `rpg.trade.use`

### Stats GUI redesign
The stats screen should show:
- Armor currently worn (4 slots, visual items)
- Tool/weapon in main hand
- Companion/pet slot (placeholder for now)
- Accessories count (and list) from rpg-accessories
- Stats grouped into categories shown on separate items (Combat, Gathering, Economy, etc.)
- "Send trade request" button → triggers trade invite
- "View auctions" button → opens a filtered view of the player's active AH listings (when AH is built)

### Dungeon system flesh-out
Current state: create/setspawn/setexit/setentrance commands work, but `/dungeon enter` teleports the player and then does nothing. The dungeon needs:
- Instance isolation (copy of the region, or schematic paste) so multiple groups can run simultaneously
- Mob spawning within the dungeon boundary
- Completion condition (kill all mobs? reach the exit?) that triggers reward + exit teleport
- Progress tracking (floor, wave, etc.)
- See `docs/addons/dungeons.md` for the full intended design.

### Enchantment/Reforge overhaul
**Enchantments in lore:**
- Remove the `Enchantments:` section header
- Show each enchantment inline between the item stats block and the item flavor lore
- Enchantments modify stats shown in lore: e.g. `Strength: +10 (+5)` where the (+5) in its own color indicates the enchantment's contribution; base stat is +5

**Stat increment display:**
- Upgrades AND reforges should each show their own `(+X)` contribution next to the stat, in a distinct color
- One reforge per item max; shown as a prefix on the item name

**Application method:**
- Reforges and upgrades become physical items placed in the anvil GUI alongside the target item
- Click "Apply" → consumes the upgrade/reforge item, modifies the target
- Some reforges/upgrades should require rare reagents as a cost

### RPG-Farming redesign
New farming model (similar to custom blocks):
- Admins assign world blocks to custom farming block types (like `/rpg block convert`)
- Custom farming blocks respawn through their vanilla growth stages visually
- Not breakable while not fully grown
- Growth time configurable per crop type in `config.yml`
- Breaking a fully-grown crop drops configured loot and restarts the growth cycle

### /trade, Bazaar, and Auction House: non-tradeable items
Items defined with `tradeable: false` should be blocked in:
- Player-to-player `/trade`
- Bazaar listings (when built)
- Auction House listings (when built)

---

## Known bugs deferred (need more investigation)

| Bug | Plugin | Notes |
|-----|--------|-------|
| ~~Potions vanish on block click, no `/effects` entry~~ | ~~rpg-core~~ | ~~Fixed: potions suppression default changed to false~~ |
| ~~Admin custom block placement doesn't register~~ | ~~rpg-core~~ | ~~Fixed: BlockPlaceListener + PDC tag on give items~~ |
