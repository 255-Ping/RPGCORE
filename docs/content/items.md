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

A right-clickable item that applies status effects and is consumed on use.

```yaml
healing_apple:
  MinecraftItem: apple
  Type: CONSUMABLE
  DisplayName: "&dHealing Apple"
  OnConsume:
    Effects:                     # status effects applied on right-click
    - { effect: regen, level: 2, duration: 200 }
    - { effect: strength_boost, level: 1, duration: 100 }
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

### `SWORD`

```yaml
iron_slayer:
  MinecraftItem: iron_sword
  Type: SWORD
  DisplayName: "&fIron Slayer"
  Rarity: "&f&lCOMMON"
  Stats:
    damage: 40
    strength: 10
    crit_chance: 5
  Abilities:
  - cleave{}                     # inline DSL, or reference a custom ability by ID
```

![A COMMON sword with Strength, Crit Chance, and Damage stats rendered in lore](../assets/screenshots/item_recruit_sword.PNG){ .screenshot }

### `BOW`

Bows consume an ammo item from the player's inventory on each shot. **`AmmoType` must reference a valid item ID** — without it the bow fires for free and any ammo item you defined is never consumed.

```yaml
forest_bow:
  MinecraftItem: bow
  Type: BOW
  DisplayName: "&aForest Bow"
  Rarity: "&a&lUNCOMMON"
  Stats:
    damage: 35
    crit_chance: 8
  AmmoType: iron_arrow           # item ID of the required ammo — must also be defined
  InfiniteAmmo: false            # true = fires without consuming ammo (quiver, ability effect, etc.)
  Abilities:
  - arrow_shot{}                 # ability fires on bow release; projectile{} + damage{} is the typical pattern

iron_arrow:
  MinecraftItem: arrow
  Type: MATERIAL
  DisplayName: "&7Iron Arrow"
  Rarity: "&f&lCOMMON"
```

> The `AmmoType` / ammo item relationship is a two-part wiring: define the ammo item **and** reference it with `AmmoType:` on the bow. Missing either half leaves one end dead.

### `WAND`

Wands scale ability damage with `intelligence` (configurable). The `max_mana` stat (not `mana`) sets the caster's mana pool.

```yaml
apprentice_wand:
  MinecraftItem: blaze_rod
  Type: WAND
  DisplayName: "&aApprentice Wand"
  Rarity: "&a&lUNCOMMON"
  Stats:
    damage: 30
    intelligence: 25
    max_mana: 50                 # ← correct ID; "mana: 50" is silently dropped
  Abilities:
  - arcane_bolt{}
```

Mana cost is enforced by the `mana_cost{}` effect at the top of the ability sequence — **not** by a `ManaCost:` field on the item or ability YAML. See [Abilities](abilities.md).

![An Apprentice Wand showing Mana, Intelligence, and Damage stats with the ability description in lore](../assets/screenshots/item_apprentice_wand.PNG){ .screenshot }

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
