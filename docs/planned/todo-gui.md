# 🔵 GUI Redesigns & New Screens

_Layout changes, pagination, and brand new inventory-based screens._

> **Difficulty scale:** 🟢 Easy (< 1 day) · 🟡 Medium (1–2 days) · 🔴 Hard (several days) · ⚫ Very Hard (week+)

---

## Redesigns — Existing GUIs

### Brewing Station GUI Redesign (`rpg-alchemy`) — 🟡 Medium
Current layout puts ingredients inline with recipes which gets cluttered. New layout:
- **Ingredient slots** move to the **top middle** of the GUI (row 1, centred)
- **Recipes** start at the **far left of row 2** and fill left-to-right, row by row
- **Pagination** — if recipes exceed one page, add Previous / Next buttons (bottom corners or bottom centre). Page indicator in the middle of the bottom row.
- Same ingredient-slot position as the cooking station redesign below (consistent across both stations)

### Cooking Station GUI Redesign (`rpg-cooking`) — 🟡 Medium
Current ingredient slots need to move one slot to the left. New layout:
- **Ingredient slots** shifted one column to the left from their current position
- **Ingredient slot position** should match the brewing station (both stations feel consistent)
- **Pagination** — if recipes exceed one page, add Previous / Next buttons. Same style as brewing.

### Enchanting Table GUI: Pagination (`rpg-enchanting`) — 🟢 Easy
The enchant-selection screen (ENCHANTING mode in `StationGui`) currently shows all applicable enchants as a fixed grid. If an item has many applicable enchants the grid fills and there's no overflow. Add:
- **Pagination** — Previous / Next buttons when enchants exceed one page (14 slots per page matching the current grid size)
- Page indicator in a fixed slot

---

## New GUI Screens (conversions from command-only)

These replace or supplement existing command interfaces. All are in `docs/planned/gui-overhaul.md` for detailed layout specs.

**Input primitives needed:**
- **Chat-entry** — close GUI → prompt in chat → `AsyncChatEvent` capture → reopen. For player names, search terms.
- **Sign-entry** — virtual sign for numeric input (prices, quantities). See [New Features](todo-features.md) — must be built first.

| GUI | Plugin | Current state | Difficulty |
|---|---|---|---|
| Party GUI (`/party`) | `rpg-parties` | All commands work; no GUI | 🟡 Medium |
| Guild GUI (`/guild`) | `rpg-guilds` | All commands work; no GUI | 🔴 Hard |
| Quest log GUI (`/quests`) | `rpg-quests` | Chat-list only | 🔴 Hard |
| Admin Spawner GUI (`/spawner`) | `rpg-core` | Fields set via `/spawner set`; GUI planned | 🟡 Medium |
| Hologram Editor GUI (`/holograms`) | `rpg-holograms` | Commands work; GUI editor deferred | 🟡 Medium |
| Display Entity Editor GUI (`/de edit`) | `rpg-holograms` | Not built yet — DEE-style physical editor + fine-detail GUI | 🔴 Hard |
| NPC Editor GUI (`/npc`) | `rpg-npcs` | All commands work; GUI editor deferred | 🔴 Hard |
| Achievements GUI (`/achievements`) | `rpg-core` | Not built yet — needed alongside achievement system | 🟡 Medium |
| Leaderboard GUI (`/top`) | `rpg-core` | Not built yet — needed alongside leaderboard feature | 🟡 Medium |
| Inbox / Mail GUI (`/inbox`) | `rpg-core` | Not built yet — needed alongside mail system | 🟡 Medium |

### Party GUI (`/party`) — 🟡 Medium
- Member list with roles, online/offline status, combat status
- Invite: chat-entry for player name → invite sent → target sees clickable accept/deny in chat
- Kick / Leave: confirmation dialog
- Promote / Demote: click role indicator

### Guild GUI (`/guild`) — 🔴 Hard
- Tabs: Members, Info, Bank (when bank is built), Settings (officer+)
- Invite: chat-entry for player name
- Kick / Promote / Demote: click member → action menu
- Edit description / name: chat-entry (officer+)

### Quest Log GUI (`/quests`) — 🔴 Hard
- Available / Active / Completed tabs
- Click quest → detail view: objectives with progress bars, rewards, accept / abandon button

### Admin Spawner GUI (`/spawner`) — 🟡 Medium
- All spawner fields shown as named items (max-alive, cooldown, radius, continuous)
- Click field → sign-entry (numeric) or chat-entry (string ID)
- Changes saved on close

### Hologram Editor GUI (`/holograms`) — 🟡 Medium
Opened via `/holograms edit <id>`. Two-tab layout within the same 54-slot inventory:

**Tab bar (row 1):**
- Slot 0: ✏️ `Lines` tab (active = lime glass, inactive = gray glass)
- Slot 1: ⚙️ `Settings` tab
- Slot 8: ✖ Close (save + close on click)

---

**Lines tab (54 slots):**
- Slots 10–43: line entries (up to 34 lines). Each slot shows a written book named `Line N: <text preview>`. Lore shows the full line.
  - Left-click → chat-entry to edit that line's text (supports `&` color codes)
  - Right-click → delete that line (with a confirm prompt on the same click)
  - Shift-left-click → move line up; Shift-right-click → move line down (swap with neighbour)
- Slot 45: ➕ `Add Line` — appends a new empty line, immediately opens chat-entry for it
- Slot 53: 🔄 `Reload Preview` — despawns and respawns the TextDisplay entity with current settings so admin can see changes in-world without closing

---

**Settings tab (54 slots):**
Each property is a named item. Left-click cycles the value (for enums/booleans) or opens sign-entry / chat-entry (for numerics/colors). Current value always shown in the item name or lore.

| Slot | Icon | Property | Interaction |
|---|---|---|---|
| 10 | PAPER | Billboard | Left-click cycles: CENTER → FIXED → VERTICAL → HORIZONTAL |
| 11 | GLASS | Background | Left-click cycles: `transparent → default → custom`; custom opens sign-entry for `r,g,b,a` |
| 12 | TORCH | Shadowed | Left-click toggles true/false |
| 13 | ENDER_EYE | See-Through | Left-click toggles true/false |
| 14 | INK_SAC | Text Opacity | Left-click opens sign-entry (0–255) |
| 15 | OAK_SIGN | Alignment | Left-click cycles: CENTER → LEFT → RIGHT |
| 16 | STRING | Line Width | Left-click opens sign-entry (pixels, default 200) |
| 19 | SPYGLASS | View Range | Left-click opens sign-entry (float multiplier) |
| 20 | GLOWSTONE_DUST | Brightness | Left-click cycles: `world lighting → always lit (15/15) → custom`; custom opens sign-entry for block/sky |
| 21 | DIAMOND | Scale | Left-click opens sign-entry for `x y z` floats |
| 22 | COMPASS | Offset | Left-click opens sign-entry for `x y z` floats (sub-block translation) |
| 23 | GRAY_DYE | Shadow Radius | Left-click opens sign-entry (float) |
| 24 | BLACK_DYE | Shadow Strength | Left-click opens sign-entry (0.0–1.0) |
| 28 | GLOWING_ITEM_FRAME | Glowing | Left-click toggles true/false |
| 29 | MAGENTA_DYE | Glow Color | Left-click opens sign-entry for `r g b`; grayed out if Glowing = false |
| 31 | CLOCK | Interpolation | Left-click opens sign-entry for `delay_ticks duration_ticks` |
| 32 | MINECART | Teleport Duration | Left-click opens sign-entry (ticks) |
| 37 | RECOVERY_COMPASS | Animated | Left-click toggles true/false (see Animated Holograms) |
| 38 | REPEATER | Frame Interval | Left-click opens sign-entry (ticks); grayed out if not animated |
| 53 | LIME_CONCRETE | Apply & Preview | Saves all settings + respawns the display entity in-world |

### NPC Editor GUI (`/npc`) — 🔴 Hard
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

### Achievements GUI (`/achievements`) — 🟡 Medium
_(Companion to the Achievement System feature — build together)_
- 54-slot grid, category tabs in the top row (Combat, Gathering, Economy, Exploration, Social, Secret)
- Locked achievements shown as gray barriers with "???" name if `Hidden: false`, invisible if `Hidden: true`
- Unlocked achievements shown with their display item + colored name
- Lore on each: description, reward summary, unlock date
- Progress-based achievements show a progress bar in lore

### Inbox / Mail GUI (`/inbox`) — 🟡 Medium
_(Companion to the Mail/Inbox System feature — build together)_
- 54-slot list: each slot is a mail entry item
- Item name = sender + type icon (📦 item, 💰 currency, 📜 message)
- Item lore = preview of message + attached items/amount + expiry date
- Click = claim items into inventory (or prompt if inventory full), mark as read
- Red border on unread mail; gray on read
- Delete button (bottom right) to clear read messages

---

### Display Entity Editor — Fine-Detail GUI (`/de edit`) — 🔴 Hard
Opened from editor mode via the `Open GUI` book item (hotbar slot 6). A 54-slot multi-page GUI for editing exact numeric values of every display entity property. Complements the physical editor (which is good for rough positioning) — this is for precise values.

The GUI has **three pages** accessible from a fixed navigation row at the bottom (row 6). Current page shown in the navigation button name.

---

**Page 1 — Transform**

Controls the `Transformation` object (scale, translation, left/right rotation). All values are exact — clicking opens sign-entry for the relevant float(s).

```
[ Scale X  ] [ Scale Y  ] [ Scale Z  ] [  ----  ] [ Trans X  ] [ Trans Y  ] [ Trans Z  ] [  ----  ] [ Reset All ]
[ L.Rot X  ] [ L.Rot Y  ] [ L.Rot Z  ] [ L.Rot W] [ R.Rot X  ] [ R.Rot Y  ] [ R.Rot Z  ] [ R.Rot W] [  ----    ]
[ Euler L→ ] [  ----   ] [  ----   ] [  ----  ] [ Euler R→ ] [  ----   ] [  ----   ] [  ----  ] [  ----    ]
[ BG pane  ] [ BG pane  ] [ BG pane  ] [ BG pane] [ BG pane  ] [ BG pane  ] [ BG pane  ] [ BG pane] [ BG pane  ]
[ ◀ Prev  ] [ BG pane  ] [ BG pane  ] [ Page 1 ] [ BG pane  ] [ BG pane  ] [ BG pane  ] [ BG pane] [ Next ▶   ]
```

| Slot group | Items | Notes |
|---|---|---|
| Scale X/Y/Z | RED/GREEN/BLUE_DYE | Show current value in name. Click → sign-entry for float. Shift-click scale X/Y/Z simultaneously (uniform scale). |
| Translation X/Y/Z | RED/GREEN/BLUE_WOOL | Sub-block offset. Click → sign-entry. |
| Left Rotation X/Y/Z/W | ORANGE items | Raw quaternion components. 99% of the time admins won't touch this directly — see Euler buttons below. |
| Right Rotation X/Y/Z/W | PURPLE items | Raw quaternion. |
| Euler L→ | WRITABLE_BOOK | Converts current left rotation to Euler angles (pitch/yaw/roll in degrees), opens a sign-entry for three space-separated degree values, then converts back to quaternion. Much more intuitive than raw XYZW. |
| Euler R→ | WRITABLE_BOOK | Same for right rotation. |
| Reset All | BARRIER | Resets entire Transformation to identity (scale 1/1/1, no translation, no rotation). Requires confirmation (click once = glow red + tooltip "Click again to confirm"; click again = apply). |

---

**Page 2 — Display Properties**

Controls all the `Display` class properties that apply to all entity types.

```
[ Billboard ] [ Brightness] [ ViewRange ] [ Disp.W  ] [ Disp.H  ] [  ----   ] [  ----   ] [  ----   ] [  ----   ]
[ Shadow R  ] [ Shadow St ] [  ----     ] [  ----   ] [  ----   ] [  ----   ] [  ----   ] [  ----   ] [  ----   ]
[ Interp.D  ] [ Interp.Dur] [ Tele.Dur  ] [  ----   ] [  ----   ] [  ----   ] [  ----   ] [  ----   ] [  ----   ]
[ Glowing   ] [ GlowColor ] [  ----     ] [  ----   ] [  ----   ] [  ----   ] [  ----   ] [  ----   ] [  ----   ]
[ ◀ Prev   ] [ BG pane   ] [ BG pane   ] [ Page 2  ] [ BG pane ] [ BG pane ] [ BG pane ] [ BG pane ] [ Next ▶  ]
```

| Item | Property | Interaction |
|---|---|---|
| PAPER — Billboard | `BillboardConstraint` | Cycle: CENTER → FIXED → VERTICAL → HORIZONTAL |
| GLOWSTONE — Brightness | Block + sky light override | Click → sign-entry `<block 0-15> <sky 0-15>`. Shows `World` if no override. |
| SPYGLASS — View Range | Render multiplier | Click → sign-entry float. |
| GRAY_CONCRETE — Display W/H | Culling box | Click → sign-entry `<width> <height>`. Shows `0×0 (no culling)` if unset. |
| GRAY_DYE — Shadow Radius | Ground shadow size | Click → sign-entry float. 0 = off. |
| BLACK_DYE — Shadow Strength | Shadow opacity | Click → sign-entry 0.0–1.0 |
| CLOCK — Interp. Delay | Ticks before tween starts | Click → sign-entry int. |
| REPEATER — Interp. Duration | Tween duration ticks | Click → sign-entry int. 0 = instant. |
| MINECART — Teleport Duration | Smooth movement ticks | Click → sign-entry int. |
| GLOWING_ITEM_FRAME — Glowing | Outline glow | Toggle true/false. |
| MAGENTA_DYE — Glow Color | Glow RGB | Click → sign-entry `r g b`. Grayed out if Glowing = false. |

---

**Page 3 — Entity-Type Properties**

Contents change depending on the entity type being edited.

**If TextDisplay:**
Mirrors the existing [Hologram Editor GUI](#hologram-editor-gui-holograms----medium) Settings tab — all the same slots and interactions. The two GUIs share a single implementation; opening from `/de edit` and from `/holograms edit` both open the same class, passing the entity.

**If ItemDisplay:**

```
[ Item     ] [ Transform] [  ----   ] [  ----   ] [  ----   ] [  ----   ] [  ----   ] [  ----   ] [  ----   ]
[ BG pane  ] ...
```

| Item | Property | Interaction |
|---|---|---|
| Shows actual item — Item | Which item to display | Click → chat-entry for RPG item id or `vanilla:<MATERIAL>`. Preview updates live. |
| COMPASS — Display Transform | `ItemDisplayTransform` preset | Cycle: FIXED → GUI → GROUND → HEAD → FIRSTPERSON_RIGHTHAND → FIRSTPERSON_LEFTHAND → THIRDPERSON_RIGHTHAND → THIRDPERSON_LEFTHAND → NONE |

**If BlockDisplay:**

| Item | Property | Interaction |
|---|---|---|
| Shows actual block — Block | Which block to display | Click → chat-entry for block type string (e.g., `oak_stairs[facing=north]`). Preview updates live. |

---

**General GUI behaviour:**
- All sign-entry values validate on input; out-of-range values reopen the sign with an error hint on line 2
- Every change applies to the live entity **immediately** on confirm — admin sees the effect in-world without closing the GUI
- The GUI is not a separate mode — the physical editor items are still in the hotbar while the GUI is open on a chest (player can close GUI to get back to click-mode, or click Done from the GUI navigation)
- Closing the GUI via ESC does **not** exit editor mode — the player still holds their editor items. Only `/de done` or the Done button exits.

---
