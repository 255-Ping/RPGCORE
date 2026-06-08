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

### ✅ Brewing Station GUI Redesign (`rpg-alchemy`) — shipped in 0.3.2
- **Ingredient slots** moved to row 1, centred (slots 12–14), matching cooking station
- **Recipe tiles** start at row 2 (slot 18); 27 recipes per page (rows 2–4)
- **GUI expanded** from 36 → 54 slots; nav bar in row 5
- **Pagination** — PREV at slot 45 / Close at 49 / NEXT at 53; page-aware refresh + tryCook

### ✅ Cooking Station GUI Redesign (`rpg-cooking`) — shipped in 0.3.1
- **Ingredient slots** shifted to slots 12–14 (row 1, centred) — matches brewing station
- **Recipe tiles** start at slot 18 (row 2); 27 recipes per page (rows 2–4)
- **GUI expanded** from 36 → 54 slots; nav bar in row 5
- **Pagination** — PREV at slot 45 / Close at 49 / NEXT at 53; same style as brewing

### ✅ Enchanting Table GUI: Pagination (`rpg-enchanting`) — shipped in 0.4.1
- **GUI expanded** from 45 → 54 slots; nav bar added to both ENCHANTING and ANVIL modes
- **ENCHANTING pagination** — per-player page state; PREV at slot 45 / page indicator at 47 / Close at 49 / NEXT at 53; `refreshEnchanting` page-aware; `tryApplyEnchant` uses page offset to resolve correct enchant
- **ANVIL** — Close button added at slot 49 (no pagination needed)

---

## New GUI Screens (conversions from command-only)

These replace or supplement existing command interfaces. All are in `docs/planned/gui-overhaul.md` for detailed layout specs.

**Input primitives needed:**
- **Chat-entry** — close GUI → prompt in chat → `AsyncChatEvent` capture → reopen. For player names, search terms.
- **Sign-entry** — virtual sign for numeric input (prices, quantities). See [New Features](todo-features.md) — must be built first.

| GUI | Plugin | Current state | Difficulty |
|---|---|---|---|
| ✅ Main Menu GUI (menu item right-click) | `rpg-core` | **Done** — `MainMenuGui`, `MainMenuListener`, `MenuCommand` in coreVersion 1.10.0. **Pending redesign** — main menu item being removed; navigation replaced by inventory crafting-slot buttons; see redesign spec below. | 🟡 Needs update |
| ✅ Party GUI (`/party`) | `rpg-parties` | **Done** — `PartyGui` in partiesVersion 0.4.0. 54-slot GUI: member cards with PLAYER_HEAD skulls, role colours, online/offline status, HP%; sign-entry invite flow; promote/demote on left-click; kick/leave/disband with confirmation overlay. | ✅ Done |
| Guild GUI (`/guild`) | `rpg-guilds` | All commands work; no GUI | 🔴 Hard |
| Quest log GUI (`/quests`) | `rpg-quests` | Chat-list only | 🔴 Hard |
| Admin Spawner GUI (`/spawner`) | `rpg-core` | Fields set via `/spawner set`; GUI planned | 🟡 Medium |
| Hologram Editor GUI (`/holograms`) | `rpg-holograms` | Commands work; GUI editor deferred | 🟡 Medium |
| Display Entity Editor GUI (`/de edit`) | `rpg-holograms` | Not built yet — DEE-style physical editor + fine-detail GUI | 🔴 Hard |
| NPC Editor GUI (`/npc`) | `rpg-npcs` | All commands work; GUI editor deferred | 🔴 Hard |
| ✅ Achievements GUI (`/achievements`) | `rpg-core` | **Done** — `AchievementGui` in coreVersion 1.10.0. Locked = GRAY_DYE, unlocked = LIME_DYE. | ✅ Done |
| Leaderboard GUI (`/top`) | `rpg-core` | Not built yet — needed alongside leaderboard feature | 🟡 Medium |
| Inbox / Mail GUI (`/inbox`) | `rpg-core` | Not built yet — needed alongside mail system | 🟡 Medium |
| Inventory Crafting-Slot Nav Buttons | `rpg-core` | New — replaces main menu item with 5 phantom buttons in player inventory crafting grid | 🟡 Medium |
| Social GUI (`/social`) | `rpg-core` | New — hub for Friends / Party / Guild / Mail | 🟡 Medium |
| Friends GUI | `rpg-core` | New — full GUI-based friends system; commands may be added alongside | 🔴 Hard |
| ✅ Adventure GUI | `rpg-core` | **Done** — `AdventureGui` + `AdventureCommand` in `rpg-core 1.10.16`. Slot 12 Quests (placeholder), slot 13 Economy (live → WalletGui), slot 14 Achievements (live → AchievementGui). `/adventure` command + `rpg.adventure.view` permission. | ✅ Done |
| Settings GUI | `rpg-core` | New — player-facing settings toggles | 🟡 Medium |
| Crafting Station GUI | `rpg-crafting` | New — custom 3×3 GUI at a plugin block; multi-item slots; quick-craft sidebar | 🔴 Hard |

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

### ✅ Party GUI (`/party`) — shipped in 0.4.0
- 54-slot GUI: Party Info header (slot 4), Invite button (slot 8), 21 member card slots across rows 2–4 (PLAYER_HEAD skulls with role colour, online/offline, HP%)
- Invite via sign-entry (one-tick delay + pendingInvite guard so GUI reopens cleanly after sign)
- Left-click member card (owner only): promote ↔ demote toggle
- Right-click member card (owner/mod, online non-owner): confirmation overlay → kick
- Slot 53: Leave (member/mod) or Disband (owner), both with confirmation overlay
- No-party mode: Create Party button at slot 22

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

## Existing GUI Refinements

### ~~Stats GUI — Polish Pass (`rpg-core`)~~ ✅ Done in `rpg-core 1.10.16`

1. Added `STAT_DESC` map in `StatsGui` — every stat entry in category lore now shows a grey description line below its value.
2. Removed the Auction House placeholder (moved to Profile GUI).
3. Removed the Trade button (trade shortcut lives only in Profile GUI).

---

### ~~Profile GUI — Polish Pass (`rpg-core`)~~ ✅ Done in `rpg-core 1.10.16`

1. Balance now formats via `Currency.format()` from the primary `CurrencyRegistry` entry — falls back to `$N,NNN` if economy not loaded.
2. Skill average icon (`EXPERIENCE_BOTTLE`) added at slot 7 — shows average level with per-skill breakdown in lore.
3. Auction House placeholder (`GOLD_BLOCK`) added at slot 42 — "Coming soon." until AH is built.
4. Trade button confirmed as Profile GUI only (Stats GUI no longer has it).

---

### Main Menu GUI — Redesign (`rpg-core`) — 🟡 Medium

The main menu **item** (the compass/trigger item in the player's hotbar) is being removed entirely. Navigation is replaced by the Inventory Crafting-Slot Buttons (see below). The Main Menu GUI itself is retained as an optional overview screen but its layout and buttons need updating.

**Changes to the existing `MainMenuGui`:**

1. **Profile button at slot 4** (top center, row 1) — `PLAYER_HEAD` skull of the viewer; label `"&e⚑ Profile"`. Replaces the decorative title item. Clicking opens the viewer's own Profile GUI (nested, Back → Main Menu).

2. **Remove Stats button** — slot 10 previously opened Stats/Profile. Remove it (replace with decorative glass or leave empty). Stats are now accessed via the Profile GUI.

3. **Updated button table:**

| Slot | Icon | Label | Opens | Notes |
|---|---|---|---|---|
| 4 | PLAYER_HEAD (viewer) | `⚑ Profile` | Profile GUI | Replaces old decorative title item |
| 11 | DIAMOND_SWORD | `✦ Skills` | Skills GUI | Unchanged |
| 13 | DIAMOND | `🏆 Achievements` | Achievements GUI | Fully live (was grayed out) |
| 14 | SHIELD | `🛡 Social` | Social GUI | Replaces old Party button; Social GUI contains party + guild + friends + mail |
| 19 | NETHER_STAR | `✦ Waypoints` | Waypoints GUI | Grayed out placeholder until Waypoints ships |
| 21 | EMERALD | `💰 Adventure` | Adventure GUI | Replaces old Economy button; Adventure GUI contains quests + economy + achievements |
| 22 | CHEST | `⚙ Settings` | Settings GUI | Replaces old Vault placeholder |
| 20 | PAPER | `✉ Mail` | Inbox GUI | Unchanged; shows unread count if > 0 |

4. **How the Main Menu is now accessed** — it is no longer triggered by a hotbar item. It may be linked from the Profile GUI or Social GUI as a back-navigation target, or kept as a `/menu` command fallback.

---

## Inventory Crafting-Slot Nav Buttons (`rpg-core`) — 🟡 Medium

Replaces the old main menu item. The player's 2×2 crafting grid (accessible from their survival inventory screen) is populated with 5 phantom GUI-shortcut buttons. These are **not real items** — they are placed into the view server-side via `InventoryOpenEvent` (or a scheduled task one tick after open) and removed before `InventoryCloseEvent` propagates, so they never end up in the player's actual inventory or the crafting result slot.

**Slot assignment (Bukkit `InventoryView` slot indices for `InventoryType.CRAFTING`):**

| Crafting grid position | Bukkit slot | Opens |
|---|---|---|
| Top-left | 1 | Profile GUI |
| Top-right | 2 | Skills GUI |
| Bottom-left | 3 | Social GUI |
| Bottom-right | 4 | Adventure GUI |
| Output slot | 0 | Settings GUI |

**Button items:**

| Slot | Material | Display name | Lore |
|---|---|---|---|
| 1 (Profile) | PLAYER_HEAD (viewer skull) | `&e⚑ Profile` | `&7View your stats, skills, and profile` |
| 2 (Skills) | DIAMOND_SWORD | `&a✦ Skills` | `&7View your skill levels and progress` |
| 3 (Social) | IRON_SWORD | `&b⚑ Social` | `&7Friends, party, guild, and mail` |
| 4 (Adventure) | WRITABLE_BOOK | `&6📜 Adventure` | `&7Quests, economy, and achievements` |
| 0 (Settings) | COMPARATOR | `&7⚙ Settings` | `&7Adjust your player settings` |

**Implementation notes:**
- Listen on `InventoryOpenEvent` where `event.getInventory().getType() == InventoryType.CRAFTING`.
- Place button items into slots 0–4 of the view one tick later (use `runTask`) so the client's inventory is fully open.
- Listen on `InventoryClickEvent` — if the slot is one of the 5 button slots, cancel the event and open the target GUI.
- Listen on `InventoryCloseEvent` — remove the 5 button items before close so the crafting inventory stays clean (no phantom items persisting to the real `PlayerInventory`).
- If the player has a real item in one of the crafting slots before opening, restore it after close. Track per-player "saved crafting contents" in a `Map<UUID, ItemStack[]>`.
- All 5 buttons must have `HIDE_ATTRIBUTES` and `HIDE_ADDITIONAL_TOOLTIP` ItemFlags.

---

## Social GUI (`rpg-core`) — 🟡 Medium

Top-level hub opened from the inventory crafting bottom-left button or from Main Menu slot 14. Contains quick-access buttons to Friends, Party, Guild, and Mail. No Back button (top-level); Close at slot 49.

**Layout (54 slots):**

```
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Title ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
[ Glass ] [ Glass ] [Friends] [ Glass ] [ Party ] [ Glass ] [ Guild ] [ Glass ] [ Glass ]
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [  Mail ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Close ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
```

| Slot | Material | Label | Opens |
|---|---|---|---|
| 4 | NETHER_STAR | `&b✦ Social` | — (decorative title) |
| 11 | SKELETON_SKULL | `&e👥 Friends` | Friends GUI |
| 13 | IRON_SWORD | `&a⚑ Party` | Party GUI (existing `PartyGui`) |
| 15 | SHIELD | `&6🛡 Guild` | Guild GUI (grayed out until built) |
| 22 | PAPER | `&e✉ Mail` | Inbox GUI (grayed out until built) |
| 49 | BARRIER | `&c❌ Close` | Closes GUI |

All unbuilt buttons use the standard grayed-out placeholder style (`GRAY_DYE`, `&7<Label>`, lore `&8[Not yet available]`).

---

## Friends GUI (`rpg-core`) — 🔴 Hard

A new player social feature. All friend management is done through this GUI (no chat commands required, though `/friend add <name>` etc. may be provided as aliases).

**Concepts:**
- A player can have up to a config-defined number of friends (default 50, configurable via `friends.max-friends`).
- Friend requests are sent by name; the recipient gets a notification and can accept/deny.
- Friends list shows online/offline status, last-seen time for offline players.

**Friends GUI layout (54 slots):**

```
[ Glass ] [ Glass ] [ Glass ] [AddFrnd] [ Title ] [PndgReq] [ Glass ] [ Glass ] [ Glass ]
[  F1   ] [  F2   ] [  F3   ] [  F4   ] [  F5   ] [  F6   ] [  F7   ] [  F8   ] [  F9  ]
[  F10  ] [  F11  ] [  F12  ] [  F13  ] [  F14  ] [  F15  ] [  F16  ] [  F17  ] [  F18 ]
[  F19  ] [  F20  ] [  F21  ] [  F22  ] [  F23  ] [  F24  ] [  F25  ] [  F26  ] [  F27 ]
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
[ PREV  ] [ Glass ] [ Glass ] [ Glass ] [ Close ] [ Glass ] [ Glass ] [ Glass ] [ NEXT  ]
```

- **Title** (slot 4): `NETHER_STAR`, `&e👥 Friends (&f<count>&e)`.
- **Add Friend** (slot 3): `LIME_DYE`, `&a➕ Add Friend`. Click → pendingInvite guard → closeInventory → runTask → `signInput().ask("Friend name:")` → send request → reopen GUI.
- **Pending Requests** (slot 5): `YELLOW_DYE`, `&eⓘ Pending Requests (&f<count>&e)`. Click → opens Pending Requests sub-GUI.
- **Friend slots** (slots 9–35, 27 per page): PLAYER_HEAD with SkullMeta. Name: `&e<playerName>` (green if online, gray if offline). Lore: `&7Status: &aOnline / &7Last seen: &f<time-ago>`. Left-click: opens Friend Actions sub-GUI (Visit / Trade / Message / Remove).
- **Pagination**: PREV at slot 45, NEXT at slot 53, Close at slot 49.

**Pending Requests sub-GUI (nested, Back → Friends):**
- Shows incoming requests as PLAYER_HEAD items. Left-click = Accept, Right-click = Deny. Confirmation overlay on accept.

**Friend Actions sub-GUI (nested, Back → Friends):**
- Slot 20: `ENDER_PEARL` `&eTeleport` (if online; grayed out if offline or `friends.allow-teleport: false`)
- Slot 22: `GOLD_INGOT` `&6Trade` — sends a trade request (same as `/trade <name>`)
- Slot 24: `BARRIER` `&cRemove Friend` — confirmation overlay before removing

---

## ~~Adventure GUI (`rpg-core`)~~ ✅ Done in `rpg-core 1.10.16`

Top-level hub opened via `/adventure` (or from the inventory crafting bottom-right button once nav buttons are built). Contains quick-access to Quests, Economy, and Achievements.

**Layout (54 slots):**

```
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Title ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
[ Glass ] [ Glass ] [ Glass ] [Quests ] [Economy] [Achieve] [ Glass ] [ Glass ] [ Glass ]
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Close ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
```

| Slot | Material | Label | Opens |
|---|---|---|---|
| 4 | WRITABLE_BOOK | `&6📜 Adventure` | — (decorative title) |
| 12 | WRITABLE_BOOK | `&e📜 Quests` | Quest Log GUI (grayed out until built) |
| 13 | EMERALD | `&a💰 Economy` | Economy / wallet summary GUI |
| 14 | DIAMOND | `&b🏆 Achievements` | Achievements GUI (existing `AchievementGui`) |
| 49 | BARRIER | `&c❌ Close` | Closes GUI |

As features are built, swap grayed-out placeholders for live buttons.

---

## Settings GUI (`rpg-core`) — 🟡 Medium

Top-level player preferences screen. Opened from the crafting output slot button or from Main Menu slot 22. Contains toggles and settings for player-facing plugin options. **Not** admin settings — these are personal preferences visible and adjustable by each player.

**Layout (54 slots):**

```
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Title ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
[ Glass ] [HudTgl ] [SndTgl ] [MsgTgl ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
...
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Close ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
```

Each setting is a toggle or cycle item. Current value always shown in the item name or lore. Clicking applies the change immediately (no save button needed — changes persist via existing player data save).

**Initial settings to expose:**

| Slot | Material | Setting | Type | Options |
|---|---|---|---|---|
| 10 | GOLDEN_APPLE | `Party HUD` | Toggle | On / Off — controls the action-bar HP display from rpg-parties |
| 11 | NOTE_BLOCK | `Sound Effects` | Toggle | On / Off — mutes RPG ability/UI sounds for this player |
| 12 | PAPER | `Damage Numbers` | Toggle | On / Off — shows/hides floating damage hologram numbers |
| 13 | COMPASS | `Show on Leaderboard` | Toggle | Visible / Hidden — opt out of public leaderboards |

Expand with more player-facing settings as they become available. Each new setting needs a corresponding per-player field in `PlayerState` (or config file) with save/load wired in.

**Bottom bar:** Close at slot 49, all others glass.

---

## Station GUI Improvements — Offline Timers + Output Slot (`rpg-alchemy`, `rpg-cooking`, `rpg-smelting`) — 🟡 Medium

All three crafting stations (brewing, cooking, smelting) need two shared improvements:

### 1. Timers run while GUI is closed

Currently processing timers may pause or reset when the player closes the GUI. Timers must run on **game time** (`GameTimeSeconds`), not session time. When the player opens the GUI after being away, it should show how much time has elapsed and reflect completed items.

- Store the timer start timestamp as a `long` game-time snapshot in the station's persistent data (PDC on the block entity, or in a plugin data map keyed by block location).
- On GUI open: compute elapsed time = `currentGameTime − savedStartTime`; advance processing state accordingly (may have completed multiple batches while away).
- If multiple queued items finish while the player is away, all should be ready in the output slot (or queue up if the output slot only holds one stack at a time).

### 2. Output slot — items go there, not directly to inventory

Add a dedicated **output slot** to each station GUI. When processing completes, the result goes into the output slot rather than auto-depositing into the player's inventory. The player manually clicks to collect it.

- If the output slot is occupied and a new result is ready, the station pauses until the player collects (display a "Output full — collect your item!" indicator in the GUI).
- The output slot should be visually distinct from input slots (border panes around it, or placed in a separate area of the GUI).

### 3. Pagination — confirm consistent with enchanting standard

Confirm that all three GUIs use the same pagination style as `rpg-enchanting`'s `StationGui`: PREV at slot 45, page indicator at slot 47, Close at slot 49, NEXT at slot 53. If `rpg-smelting` doesn't have pagination yet, add it using the same pattern.

---

## Crafting Station GUI (`rpg-crafting`) — 🔴 Hard

Replace the current `rpg-crafting` command-based interface with a full GUI at a custom plugin block.

### Custom Block

A new plugin block (`crafting_table` or similar ID in `blocks/`) that opens the Crafting Station GUI on right-click. Placed by admins via `/rpg item give crafting_table_block` (or similar). Uses the existing `BlockInteractListener` dispatch.

### GUI Layout (54 slots)

```
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [Quick1 ] [Quick2 ] [Quick3 ]
[ Glass ] [ In1   ] [ In2   ] [ In3   ] [ Glass ] [Output ] [Quick4 ] [Quick5 ] [Quick6 ]
[ Glass ] [ In4   ] [ In5   ] [ In6   ] [ Glass ] [ Glass ] [Quick7 ] [Quick8 ] [Quick9 ]
[ Glass ] [ In7   ] [ In8   ] [ In9   ] [ Glass ] [ Glass ] [Quick10] [Quick11] [Quick12]
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
[ Glass ] [ Glass ] [ Glass ] [ Glass ] [ Close ] [ Glass ] [ Glass ] [ Glass ] [ Glass ]
```

- **Slots 10, 11, 12, 19, 20, 21, 28, 29, 30** — 3×3 crafting input grid.
- **Slot 23** — Output slot. Shows the crafted result (grayed-out GRAY_DYE if no valid recipe). Click to collect the result (removes inputs, gives output).
- **Slots 15, 16, 17, 24, 25, 26, 33, 34, 35, 42, 43, 44** — Quick-Craft sidebar (12 slots, see below).
- **Slot 49** — Close.

### Multi-Item Slots

A recipe can require more than 1 item per grid slot (e.g. 64 dirt in every slot). The input slot tooltip shows the required quantity. If the slot contains fewer than required, it renders the item in red (or with a `&c` lore indicator `"&cNeed <N>, have <M>"`). The output slot only activates when all slot requirements are met.

Recipe YAML shape addition:
```yaml
my_recipe:
  Type: SHAPED
  Grid:
    - "DDD"
    - "DDD"
    - "DDD"
  Ingredients:
    D:
      item: dirt
      amount: 64       # <-- new field; defaults to 1 if omitted
  Result:
    item: mega_dirt
    amount: 1
```

### Quick-Craft Sidebar

The 12 right-hand slots are auto-populated on GUI open (and refreshed after each craft) with items the player **can currently craft** given their inventory contents.

- Scan all registered recipes; for each recipe where the player has sufficient materials, add it to the list.
- Show up to 12 results. If more than 12 are available, add PREV/NEXT buttons at slots 33/35 to page through them (or add a scroll mechanic using shift-click).
- Each Quick-Craft slot: material = the recipe's output item; name = `"&a<output name> &7(×<amount>)"`. Lore: ingredient list (`"&7- <amount>× <ingredient name>"`), then `"&eClick to craft"`.
- **Clicking a Quick-Craft slot:** validates the player still has materials (re-check), removes the ingredients from their inventory, gives the output. Does **not** place anything in the 3×3 grid — it is a direct "one-click craft" action.
- Quick-Craft slots update every time the player's inventory changes while the GUI is open (listen on `InventoryClickEvent` in the player's own inventory while the station GUI is open, and refresh after each action).

### Implementation Notes

- The 3×3 grid is manual (the player arranges items themselves for shaped recipes, or just fills slots for shapeless).
- When the grid contents change, re-evaluate all recipes in the registry and update the output slot.
- For shaped recipes, only exact grid arrangement triggers a match (same as vanilla). Shapeless recipes match regardless of arrangement.
- A `CraftingStationGui` class in `rpg-crafting` handles all of this. The existing `CraftingManager` / recipe registry remains unchanged — the GUI consults it.

---
