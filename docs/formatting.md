# Formatting guide

Standards for every player-facing UI element in the suite. **All GUIs and messages must follow this guide.** When in doubt, look at Hypixel Skyblock for GUI inspiration — we follow the same visual language with the constraints listed below.

> All pane materials and system message prefixes are configurable. See the relevant config sections below. Never hardcode pane materials or color codes that belong in config.

---

## GUI standards

### Layout philosophy

Follow Hypixel Skyblock GUI conventions:
- Clean, organized layout — every slot has a purpose.
- Items displayed in logical grids with clear visual groupings.
- No raw empty slots — fill unused slots with background panes.
- Descriptive, well-formatted lore on every interactive item.
- Action/navigation buttons near the **bottom** of the inventory.

### Pane materials

| Role | Default material | Config key (`rpg-core/config.yml`) |
|---|---|---|
| Background (empty slots) | `GRAY_STAINED_GLASS_PANE` | `gui.background-material` |
| Bottom border (extra space) | `BLACK_STAINED_GLASS_PANE` | `gui.border-material` |

Always read both via `RpgServices.guiConfig()`. Never hardcode the material.

```java
GuiConfig gui = RpgServices.guiConfig();
// 1. Fill everything with the gray background first:
gui.fillAll(inv);
// 2. Place real content on top.
inv.setItem(20, someItem);
// 3. If the GUI has a row of extra vertical space at the bottom, add the black border:
gui.fillBottomRow(inv);
```

### Standard inventory sizes and slot maps

**27 slots (3 rows) — simple menus:**
```
Row 0 [0–8]:   content
Row 1 [9–17]:  content
Row 2 [18–26]: action buttons (gray background behind them)
```

**45 slots (5 rows) — standard menus:**
```
Row 0 [0–8]:   header / category panes or content
Row 1 [9–17]:  content  (gray background)
Row 2 [18–26]: content  (gray background)
Row 3 [27–35]: action buttons + gray fill
Row 4 [36–44]: BLACK border row (only add this row if the content doesn't fill row 3 completely)
```

**54 slots (6 rows) — large menus (skills, guild, quest list):**
```
Row 0 [0–8]:   header / navigation panes
Row 1 [9–17]:  content  (gray background)
Row 2 [18–26]: content  (gray background)
Row 3 [27–35]: content  (gray background)
Row 4 [36–44]: action buttons + gray fill
Row 5 [45–53]: BLACK border row
```

**Rule:** The black border row is added **only when there is extra vertical space** below the content — i.e., when placing all content leaves an entire bottom row empty. If the content fills the inventory, use gray background on the last row (no black border needed).

### Button placement

- Buttons always go in the **last row that has content**, not the black border row.
- Center buttons or right-align them (slots 3–5 or 6–8 in a row of 9).
- Leave at least one background pane on each side of a button cluster as visual padding.
- A "close" or "back" button conventionally goes in the **bottom-left or bottom-right** corner of the button row (slots 0/8 of that row, or the equivalent in a 27/45/54 slot GUI).

**Example button row (row 4 of 54, slots 36–44):**
```
[GRAY][GRAY][GRAY][PREV][GRAY][NEXT][GRAY][GRAY][BACK]
  36    37    38    39    40    41    42    43    44
```

### Section label panes

Use named panes (gray background material, custom display name) as visual dividers or section headers. Color: `&7` for labels, `&6` or `&e` for titles.

```java
ItemStack label = gui.backgroundItem();
ItemMeta meta = label.getItemMeta();
meta.displayName(legacy("&6&lSkills").decoration(ITALIC, false));
label.setItemMeta(meta);
inv.setItem(4, label);  // center of top row
```

### Item lore inside GUIs

Interactive items (buttons, selectable entries) follow the same lore structure as world items (see [Item lore](#item-lore) below), with two additions:

- **Action hint** at the bottom of lore, before rarity if any:
  `&8▶ &7Left click to <action>`
  `&8▶ &7Right click to <action>`
- **State indicators** just below the description:
  - Selected / active: `&a✔ Active`
  - Locked / unavailable: `&c✘ Requires level {n}`
  - Purchased / owned: `&6✦ Owned`

### GUI title format

```
"&<theme-color>&l<Title>"
```

| GUI type | Color |
|---|---|
| Skills, stats | `&b` (aqua) |
| Inventory, equipment | `&6` (gold) |
| Shop, economy | `&a` (green) |
| Enchanting, upgrades | `&5` (dark purple) |
| Brewing, alchemy | `&d` (light purple) |
| Quests | `&e` (yellow) |
| Dungeons | `&c` (red) |
| Guild, parties | `&a` (green) |
| Generic admin/info | `&7` (gray) |

---

## Text & chat formatting

### System message prefix

All plugin-generated chat messages use the prefix `&8[&6RPG&8] &r`, read from `rpg-core/messages.yml` key `prefix`. Access it via `RpgServices.messageFormatter()`.

```java
// ✅ Right — goes through messageFormatter so the prefix is configurable
player.sendMessage(RpgServices.messageFormatter().component("skill.level-up",
    Map.of("skill", "Mining", "level", "5")));

// ❌ Wrong — hardcoded inline string
player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&8[&6RPG&8] &r..."));
```

All messages belong in the plugin's `messages.yml`. No player-visible string should be a Java string literal.

### Color palette

| Color | `&` code | Use case |
|---|---|---|
| Dark gray | `&8` | Metadata, parentheticals, dividers, bracket framing |
| Gray | `&7` | Descriptive body text, labels, secondary info |
| White | `&f` | Player-entered values, item names in context |
| Gold | `&6` | Currency, headings, titles |
| Yellow | `&e` | Warnings, notable highlights, quantity numbers |
| Green | `&a` | Success, positive stat values, "on" states |
| Red | `&c` | Errors, negative values, "off" states, damage |
| Aqua | `&b` | Mana, magical stats, water-themed content |
| Dark aqua | `&3` | Secondary magical labels |
| Light purple | `&d` | Abilities, ability names, special items |
| Dark purple | `&5` | Enchantments, arcane/enchant headers |
| Blue | `&9` | Quest-related text |
| Dark red | `&4` | Critical errors, void damage |
| Bold | `&l` | Headers, rarity labels, titles only — never on body text |
| Strikethrough | `&m` | Divider lines only |
| Italic | — | Suppressed on all item lore via `TextDecoration.ITALIC, false` |

### Message types

| Type | Format | Example |
|---|---|---|
| Success | `&aMessage` | `&aPurchased Iron Pickaxe.` |
| Error | `&cMessage` | `&cNot enough currency.` |
| Warning | `&eMessage` | `&eThis cannot be undone.` |
| Info | `&7Message &fvalue` | `&7Balance: &f1,500 coins` |
| Title heading | `&6&lHeading` | `&6&lRPG` (scoreboard) |
| Divider | `&8&m` + 20 spaces | section separator |

### Section dividers

Use `&8&m                    ` (strikethrough + trailing spaces) as a visual separator between sections in lore, list outputs, or HUD scoreboard lines.

---

## Item lore

### Standard section order

```
&7Description line 1.
&7Description line 2.

&7 ——— Stats ———
<stat lines>

&5Ability: &d<Name> &8(<Trigger>)
&7Description of what it does.
&8Mana Cost: &b<cost>  &8Cooldown: &a<n>s

<rarity line>
```

### Stats section

Each stat on its own line, using the stat's registered color code:

```
&<stat_color><Stat Display Name>: <value><unit>
```

- Positive flat values: prefixed `+` (e.g., `+50`)
- Negative flat values: no prefix needed, color signals it
- Percent stats: value suffixed `%` (e.g., `+5.0%`)
- Decimal values: one decimal place (`%.1f`)
- Stats section header (optional for long stat lists): `&8 ——— Stats ———`

### Ability section

```
&5Ability: &d<Name> &8(<Trigger>)
&7<One-line description.>
&8Mana Cost: &b<mana>  &8Cooldown: &a<ticks / 20>s
```

- Trigger labels: `Right Click`, `Left Click`, `On Hit`, `On Hurt`, `Passive`, `Timer`
- If no mana cost: omit the mana line
- If no cooldown: omit the cooldown suffix

### Rarity line

Always the **last line** of item lore. Bold, uppercase, color matches rarity:

| Rarity | Format |
|---|---|
| Common | `&7&lCOMMON` |
| Uncommon | `&a&lUNCOMMON` |
| Rare | `&9&lRARE` |
| Epic | `&5&lEPIC` |
| Legendary | `&6&lLEGENDARY` |
| Mythic | `&d&lMYTHIC` |
| Special | `&e&lSPECIAL` |

---

## Action bar

Default format (configurable in `rpg-hud/config.yml` key `action-bar.idle-format`):

```
&c❤ {health}/{max_health}  &b✦ {mana}/{max_mana}  &a✤ {defense}
```

Color convention: health = `&c`, mana = `&b`, defense = `&a`. Use the same colors for these stats everywhere — scoreboard, GUI lore, HUD.

---

## Damage indicators

Configurable in `rpg-holograms/config.yml` under `damage-indicators.formats`:

| Type | Default | Notes |
|---|---|---|
| Normal | `&f{amount}` | White number |
| Crit | `&e&l✧ {amount} ✧` | Gold bold with sparkles |
| True damage | `&f&l⚡ {amount} ⚡` | White bold with lightning |
| Lifesteal | `&c+{amount}` | Red (taking back HP) |
| Heal | `&a+{amount} ❤` | Green with heart |

---

## Scoreboard

Configurable in `rpg-hud/config.yml`. Color convention:
- Title: `&6&l` (gold bold)
- Section labels: `&7` (gray)
- Stat values: match the stat's color (health `&c`, mana `&b`, defense `&a`, skill levels `&e`)
- Divider lines: `&8&m` strikethrough

---

## Titles & subtitles

| Element | Format |
|---|---|
| Main title | `&<theme>&l<Text>` — color matches the context (level-up = `&6`, death = `&c`, etc.) |
| Subtitle | `&7<supporting info>` |

---

## Serializer

The entire suite uses `LegacyComponentSerializer.legacyAmpersand()`. Rules:
- **Always** use `&` codes in content YAML and messages.yml.
- **Never** use `§` codes in content (only acceptable deep inside Bukkit API calls that require raw section strings, like scoreboard entry dedup).
- **Never** use MiniMessage syntax — gradient/click/hover events are not supported by the current formatting stack.
- Italic must always be explicitly suppressed on item display names and lore:
  ```java
  meta.displayName(component.decoration(TextDecoration.ITALIC, false));
  ```

---

## Related

- [Configuration overview](configuration.md)
- [Stats reference](stats.md)
- [Items](content/items.md)
- [HUD](addons/hud.md)
