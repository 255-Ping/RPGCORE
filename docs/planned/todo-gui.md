# 🔵 GUI Redesigns & New Screens

_Layout changes, pagination, and brand new inventory-based screens._

---

## Redesigns — Existing GUIs

### Brewing Station GUI Redesign (`rpg-alchemy`)
Current layout puts ingredients inline with recipes which gets cluttered. New layout:
- **Ingredient slots** move to the **top middle** of the GUI (row 1, centred)
- **Recipes** start at the **far left of row 2** and fill left-to-right, row by row
- **Pagination** — if recipes exceed one page, add Previous / Next buttons (bottom corners or bottom centre). Page indicator in the middle of the bottom row.
- Same ingredient-slot position as the cooking station redesign below (consistent across both stations)

### Cooking Station GUI Redesign (`rpg-cooking`)
Current ingredient slots need to move one slot to the left. New layout:
- **Ingredient slots** shifted one column to the left from their current position
- **Ingredient slot position** should match the brewing station (both stations feel consistent)
- **Pagination** — if recipes exceed one page, add Previous / Next buttons. Same style as brewing.

### Enchanting Table GUI: Pagination (`rpg-enchanting`)
The enchant-selection screen (ENCHANTING mode in `StationGui`) currently shows all applicable enchants as a fixed grid. If an item has many applicable enchants the grid fills and there's no overflow. Add:
- **Pagination** — Previous / Next buttons when enchants exceed one page (14 slots per page matching the current grid size)
- Page indicator in a fixed slot

---

## New GUI Screens (conversions from command-only)

These replace or supplement existing command interfaces. All are in `docs/planned/gui-overhaul.md` for detailed layout specs.

**Input primitives needed:**
- **Chat-entry** — close GUI → prompt in chat → `AsyncChatEvent` capture → reopen. For player names, search terms.
- **Sign-entry** — virtual sign for numeric input (prices, quantities). See [New Features](todo-features.md) — must be built first.

| GUI | Plugin | Current state |
|---|---|---|
| Party GUI (`/party`) | `rpg-parties` | All commands work; no GUI |
| Guild GUI (`/guild`) | `rpg-guilds` | All commands work; no GUI |
| Quest log GUI (`/quests`) | `rpg-quests` | Chat-list only |
| Admin Spawner GUI (`/spawner`) | `rpg-core` | Fields set via `/spawner set`; GUI planned |
| Hologram Editor GUI (`/holograms`) | `rpg-holograms` | Commands work; GUI editor deferred |
| NPC Editor GUI (`/npc`) | `rpg-npcs` | All commands work; GUI editor deferred |

### Party GUI (`/party`)
- Member list with roles, online/offline status, combat status
- Invite: chat-entry for player name → invite sent → target sees clickable accept/deny in chat
- Kick / Leave: confirmation dialog
- Promote / Demote: click role indicator

### Guild GUI (`/guild`)
- Tabs: Members, Info, Bank (when bank is built), Settings (officer+)
- Invite: chat-entry for player name
- Kick / Promote / Demote: click member → action menu
- Edit description / name: chat-entry (officer+)

### Quest Log GUI (`/quests`)
- Available / Active / Completed tabs
- Click quest → detail view: objectives with progress bars, rewards, accept / abandon button

### Admin Spawner GUI (`/spawner`)
- All spawner fields shown as named items (max-alive, cooldown, radius, continuous)
- Click field → sign-entry (numeric) or chat-entry (string ID)
- Changes saved on close

### Hologram Editor GUI (`/holograms`)
- Line slots: click line slot → chat-entry for line text
- Add / remove / reorder lines in GUI
- Click-action support on lines (run command, open shop)

### NPC Editor GUI (`/npc`)
Long-form alternative to the command-based editing for non-technical admins:
- Open with `/npc edit <id>`
- Shows current settings: entity style, entity type, skin name, behavior type, look-at-players toggle
- Click style/type slots → cycle through options directly
- Click skin slot → chat-entry for player name (triggers `SkinFetcher`)
- Click behaviour slot → opens a sub-GUI:
  - **Dialogue:** lists all current lines; click a line slot → chat-entry to edit; Add Line / Clear All buttons
  - **Shop:** lists shop entries with buy/sell prices; Add Item (chat-entry for item id + sign-entry for prices); Remove buttons
  - **Quest:** shows current quest id; click → chat-entry for quest id
  - **Banker:** shows bank name and interest %; click each → sign-entry or chat-entry
- Confirm / Discard buttons; changes saved immediately on confirm

---
