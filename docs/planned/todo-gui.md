# 🔵 GUI Redesigns & New Screens

_Layout changes, pagination, and brand new inventory-based screens._

> **Difficulty scale:** 🟢 Easy (< 1 day) · 🟡 Medium (1–2 days) · 🔴 Hard (several days) · ⚫ Very Hard (week+)

---

## GUI Navigation Standard

**This rule applies to every GUI in the suite — existing and future.**

Every inventory GUI must have a navigation row along the bottom (row 6, slots 45–53). What goes there depends on whether the GUI is nested (opened from inside another GUI) or top-level (opened directly from a command or the menu item).

| GUI type | Back button | Close button |
|---|---|---|
| **Top-level** (main menu, or opened directly by command/item) | — | Slot 49 (centre bottom), red barrier or dye, `❌ Close` |
| **Nested** (opened from a button inside another GUI) | Slot 45 (far left), arrow or compass, `← Back` — returns to the immediately previous GUI | Slot 53 (far right), red barrier or dye, `❌ Close` — closes everything entirely |

**Back context** — when opening a nested GUI, pass the parent GUI instance so the Back button can reopen it. A simple `@Nullable JavaPlugin openedFrom` or a `GUIContext` record is enough. Do not rely on re-constructing the parent from scratch; reopen the exact instance so state is preserved (e.g. the page number the player was on in the parent).

**Retrofit rule** — when touching any existing GUI for any other reason, add the bottom-bar buttons as part of that same change. Don't leave a GUI without them after editing it.

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
| ✅ Main Menu GUI (menu item right-click) | `rpg-core` | **Done** — `MainMenuGui`, `MainMenuListener`, `MenuCommand` in coreVersion 1.10.0. Configurability pass still pending (see below). | ✅ Done |
| Party GUI (`/party`) | `rpg-parties` | All commands work; no GUI | 🟡 Medium |
| Guild GUI (`/guild`) | `rpg-guilds` | All commands work; no GUI | 🔴 Hard |
| Quest log GUI (`/quests`) | `rpg-quests` | Chat-list only | 🔴 Hard |
| Admin Spawner GUI (`/spawner`) | `rpg-core` | Fields set via `/spawner set`; GUI planned | 🟡 Medium |
| Hologram Editor GUI (`/holograms`) | `rpg-holograms` | Commands work; GUI editor deferred | 🟡 Medium |
| Display Entity Editor GUI (`/de edit`) | `rpg-holograms` | Not built yet — DEE-style physical editor + fine-detail GUI | 🔴 Hard |
| NPC Editor GUI (`/npc`) | `rpg-npcs` | All commands work; GUI editor deferred | 🔴 Hard |
| ✅ Achievements GUI (`/achievements`) | `rpg-core` | **Done** — `AchievementGui` in coreVersion 1.10.0. Locked = GRAY_DYE, unlocked = LIME_DYE. | ✅ Done |
| Leaderboard GUI (`/top`) | `rpg-core` | Not built yet — needed alongside leaderboard feature | 🟡 Medium |
| Inbox / Mail GUI (`/inbox`) | `rpg-core` | Not built yet — needed alongside mail system | 🟡 Medium |

### Main Menu GUI (`rpg-core`) — 🟡 Medium

Opened by right-clicking the [Main Menu Item](todo-features.md). This is a **top-level GUI** — no Back button, Close button only (slot 49, centre of bottom row).

**Layout (54 slots, 6 rows):**

```
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [  Title ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
[ Glass ] [ Stats ] [Skills ] [Quests ] [ Achieve] [ Party ] [ Guild ] [ Glass ] [ Glass ]
[ Glass ] [ Glass ] [ Mail  ] [Economy] [ Vault  ] [        ] [        ] [ Glass ] [ Glass ]
[ Glass ] [        ] [        ] [        ] [        ] [        ] [        ] [ Glass ] [ Glass ]
[ Glass ] [        ] [        ] [        ] [        ] [        ] [        ] [ Glass ] [ Glass ]
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Close  ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
```

_(Note: Warps removed from player menu — use `/warp` admin command instead. Slot 19 currently empty; will become Waypoints once that system ships. Vault at slot 22 once vault system ships.)_

**Feature buttons** — each opens the relevant GUI as a nested screen (so that GUI gets Back → Main Menu + Close):

| Slot | Icon | Label | Opens | Notes |
|---|---|---|---|---|
| 10 | PLAYER_HEAD (of viewer) | `⚔ Stats` | Stats/Profile GUI | Always available |
| 11 | DIAMOND_SWORD | `✦ Skills` | Skills GUI | Always available |
| 12 | WRITABLE_BOOK | `📜 Quests` | Quest Log GUI | Grayed out with `[Coming Soon]` until quest log GUI is built |
| 13 | DIAMOND | `🏆 Achievements` | Achievements GUI | Grayed out until achievement system is built |
| 14 | IRON_SWORD | `⚑ Party` | Party GUI | Grayed out until party GUI is built |
| 15 | SHIELD | `🛡 Guild` | Guild GUI | Grayed out until guild GUI is built |
| 19 | NETHER_STAR | `✦ Waypoints` | Waypoints GUI (list of discovered spawn points) | Grayed out placeholder until Waypoints system is built; **Warps removed** — admin warps are a separate `/warp` command, not exposed here |
| 20 | PAPER | `✉ Mail` | Inbox GUI | Shows unread count in item name if > 0; grayed out until mail system is built |
| 22 | CHEST | `🗄 Vault` | Vault Selector GUI | Grayed out until Vault/Storage system is built |
| 21 | EMERALD | `💰 Economy` | Economy / wallet summary GUI | Always available |
| 4 | NETHER_STAR | `✦ RPGCORE ✦` | — | Decorative title item, not clickable |

**"Grayed out" placeholder style:** `GRAY_DYE`, name in gray (`&7<Label>`), lore line `&8[Not yet available]`. Clicking does nothing (event cancelled).

**Bottom bar:**
- Slot 49: `❌ Close` — BARRIER, closes the GUI (top-level, no Back button)
- All other bottom-row slots: dark gray stained glass panes (decorative filler)

**Building notes:**
- The PLAYER_HEAD for Stats uses `SkullMeta` with the viewer's own `PlayerProfile` — call `Bukkit.createPlayerProfile(player.getUniqueId())` and `skullMeta.setOwnerProfile(...)`.
- As new GUIs are built, swap their placeholder items for the real buttons — no other changes needed.

---

### Main Menu GUI — Configurability Pass (`rpg-core`) — 🟡 Medium

The existing `MainMenuGui` hardcodes slot positions, icon materials, names, and which buttons are visible. Admins should be able to fully customize the menu from `config.yml`.

**Config shape (`main-menu.buttons.<id>:`):**

```yaml
main-menu:
  enabled: true
  slot: 8
  material: COMPASS
  name: "&6✦ Menu &6✦"
  title: "&6✦ RPGCORE ✦"    # GUI title bar text

  buttons:
    stats:
      slot: 10
      enabled: true
      name: "⚔ Stats"
    skills:
      slot: 11
      enabled: true
      name: "✦ Skills"
    quests:
      slot: 12
      enabled: true
      name: "📜 Quests"
    achievements:
      slot: 13
      enabled: true
      name: "🏆 Achievements"
    party:
      slot: 14
      enabled: true
      name: "⚑ Party"
    guild:
      slot: 15
      enabled: true
      name: "🛡 Guild"
    waypoints:
      slot: 19
      enabled: true        # only visible once waypoints system is built
      name: "✦ Waypoints"
    mail:
      slot: 20
      enabled: true
      name: "✉ Mail"
    economy:
      slot: 21
      enabled: true
      name: "💰 Economy"
    vault:
      slot: 22
      enabled: true        # only visible once vault system is built
      name: "🗄 Vault"
```

**Behaviour:**
- `enabled: false` → button not rendered at all (slot stays background filler).
- `slot` override → button moves to that slot; if two buttons share a slot, the one defined first wins and a warning is logged.
- `name` supports `&` color codes.
- Materials are **not** configurable per-button (icons are semantic and would break UX to change); only name/slot/visibility are exposed.
- A new `MainMenuConfig` class reads the `main-menu.buttons` section and exposes `ButtonDef(String id, int slot, boolean enabled, String name)`. `MainMenuGui` reads from this instead of constants.
- Reload: `MainMenuConfig` re-reads on `/rpg reload`. Already-open menus update on next open.

---

### Skills GUI Redesign + Per-Skill Detail GUI (`rpg-core`) — 🔴 Hard

The current `SkillsGui` lists all skills in a flat grid with a simple progress bar. This redesign makes it significantly more polished and adds a per-skill detail screen inspired by Hypixel SkyBlock's skill pages.

---

#### Skills Overview GUI (improved)

Replace the flat grid with a cleaner layout:

- **Header row (row 1):** total XP summary at slot 4. Slots 0–3 and 5–8: decorative glass.
- **Skill items (rows 2–5):** one item per skill in a centered grid (up to 36 skills). Each item shows:
  - Icon: skill-specific material (existing `SKILL_ICONS` map)
  - Name: `"<color><skill display name>"` (bold, coloured by skill category — combat = red, gathering = green, crafting = yellow, etc.)
  - Lore:
    - `"&7Level: &f<N> &7/ &f<max>"`
    - Progress bar: `"[████████░░] &e80%"` (20-char bar, gold fill `█`, dark gray empty `░`, percentage in gold)
    - `"&7XP to next: &f<formatted>"`
    - `"&7Total XP: &f<formatted>"`
    - `""`
    - `"&e▶ Click for details"`
- **Nav bar (row 6):** standard.

Clicking a skill item opens the Per-Skill Detail GUI for that skill (nested, Back → Overview).

---

#### Per-Skill Detail GUI (snake / path design)

A 54-slot GUI displaying the level progression as a **snake path** — the same visual pattern Hypixel SkyBlock uses for skill pages. Each node on the snake represents one milestone level (not every level — just the levels that grant rewards or notable XP thresholds).

**Snake layout:**

The path zigzags across rows 1–5 (slots 0–44). Nodes are placed in a snake pattern:

```
Row 1 (left→right):  [1] [2] [3] [4] [5] [6] [7] [8] [9]
Row 2 (right→left):  [ ] [ ] [ ] [ ] [ ] [10][11][12][13][14]
Row 3 (left→right):  [15][16][17]...
...
```

Each node is a coloured item:
- **Reached level:** `LIME_DYE` (or skill's accent color) — player has reached this level
- **Current level:** `YELLOW_DYE` + enchant glint — the level the player is at right now
- **Future level:** `GRAY_DYE` — not yet reached

**Node lore:**
```
&6Level <N>
&7Rewards:
  &a+<stat> <display-name>       (per-level stat gains if configured)
  &e+<xp> XP                    (if a milestone bonus)
  &d<special reward text>        (if a milestone ability or item reward)
&7
&7XP required: &f<formatted>
&7Total XP at this level: &f<formatted>
```

Levels without any reward still show the level number and XP required. Levels with milestone rewards (from the skill's `milestones:` config) get a highlighted lore entry.

**Header (slot 4):** skill name, icon, and current level summary. Same as the overview item for this skill but with more XP context.

**Pagination:** if the skill has more than 45 milestone nodes (unlikely for max level 50, but possible for max 100), use Previous/Next nav. Otherwise no pagination needed.

**Nav bar (row 6):** Back at slot 45 (returns to Skills Overview), Close at slot 53.

---

#### Implementation notes

- `SkillDetailGui` — new class, same package as `SkillsGui`.
- Snake path ordering: pre-compute a `List<Integer>` of slot indices in snake order. For a 5-row × 9-col grid, row 0 goes L→R (slots 0–8), row 1 R→L (slots 17–9), row 2 L→R (slots 18–26), etc.
- Level nodes: only milestone levels (those in the skill's `milestones:` map) get reward lore. All other levels get minimal lore (level number + XP required).
- `SkillsService.xpForLevel(skillId, level)` — may need adding to the API if it's not already exposed (currently `xpToNext` gives the delta, but we need the cumulative total for each level). Add `long xpRequired(String skillId, int level)` to `SkillsService` in `rpg-api`.
- Color coding by category: define a `Map<String, NamedTextColor>` in `SkillsGui` — `combat → RED`, `gathering → GREEN`, `crafting → YELLOW`, `utility → AQUA`. Skill IDs not in the map default to `WHITE`.

---

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
