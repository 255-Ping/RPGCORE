# Items

> **Status:** Working — YAML loader, registry, PDC tagging, lore rendering, and equipment stat aggregation all working. Stats on equipped items (armor slots + main hand) aggregate into the player's effective stat sheet on every gear change via `EquipmentListener`. `/rpg item give <id> [player] [amount]` lights up. Type-specific runtime behavior for `CONSUMABLE`, `UPGRADE`, and `ACCESSORY` items is handled by their respective addons.

Custom items are defined in YAML under `plugins/rpg-core/items/`. Any number of files, any number of items per file.

## Schema

```yaml
testitem: #itemid
  MinecraftItem: stone           # base Material
  Type: MATERIAL                 # SWORD | BOW | WAND | ARMOR | MATERIAL | QUEST | CONSUMABLE | UPGRADE | ACCESSORY
  DisplayName: "&7Test Item"     # optional; defaults to a humanized form of itemid
  Lore:
  - '&7This is some test lore for the first line'
  - '&eThis is some test lore for the second line'
  Rarity: '&7&lCOMMON'           # colored display string
  CustomModelData: 10001         # optional
  Stats:                         # optional; map of stat-id -> number
    damage: 5
    strength: 2
    crit_chance: 5
  Abilities:                     # optional; ability invocations using the ability DSL
  - explode{radius=3,damage_multiplier=1.5}
  CombatXpMultiplier: 1.0        # optional; per-ability override for combat XP from this item
```

## Type-specific blocks

### `CONSUMABLE`

A right-clickable item that applies a status effect / stat buff and is consumed.

```yaml
healing_apple:
  MinecraftItem: apple
  Type: CONSUMABLE
  DisplayName: "&dHealing Apple"
  Consumable:
    Duration: 200                # ticks for the buff (0 = instant)
    Heal: 50                     # instant HP healed
    Effects:                     # status effects applied
    - { id: regen, level: 2, duration: 200 }
    Cooldown: 100                # ticks before player can consume again
```

### `UPGRADE`

An item that, when applied to another item in the anvil GUI, modifies the target.

```yaml
hot_potato_book:
  MinecraftItem: book
  Type: UPGRADE
  DisplayName: "&6Hot Potato Book"
  Upgrade:
    AppliesTo: [SWORD, BOW, ARMOR]
    MaxStacks: 10                # how many can be applied to one item
    Effect:
      stats:
        strength: 2
        max_health: 4
    LoreAdd: "&7+%stacks% Hot Potato Books"
```

### `ACCESSORY`

Provides stats while in the player's accessory bag. See [addons/accessories.md](../addons/accessories.md).

```yaml
zombie_talisman:
  MinecraftItem: zombie_head
  Type: ACCESSORY
  DisplayName: "&aZombie Talisman"
  Rarity: '&a&lUNCOMMON'
  Stats:
    max_health: 10
  Accessory:
    Family: zombie_talisman      # accessories of the same family don't stack (default behavior)
```

### `ARMOR`

Standard item with optional armor-slot binding. Stats apply while equipped.

```yaml
flame_helmet:
  MinecraftItem: golden_helmet
  Type: ARMOR
  ArmorSlot: HELMET              # HELMET | CHEST | LEGS | BOOTS
  Stats:
    defense: 25
    max_health: 30
```

### `SWORD` / `BOW` / `WAND`

Standard items. `Abilities:` is most useful here. `WAND` items may scale ability damage with `intelligence`.

## Lookup & PDC tagging

When `RpgItem.toItemStack()` produces an `ItemStack`, it embeds the item ID in the item's Persistent Data Container under a fixed key. `ItemRegistry.from(stack)` recovers the item by reading that tag. PDC survives chests, dropped item entities, and server restarts.

## Identifying & sourcing

- Admin: `/rpg item give <id> [player] [amount]` (perm `rpg.core.item.give`)
- In code: `RpgServices.items().get("red_gem")`
- In recipes / loot tables: reference by ID

## Related

- [Stats reference](../stats.md)
- [Abilities](abilities.md)
- [Accessories addon](../addons/accessories.md)
- [Resource pack ranges](../resource-pack.md)
