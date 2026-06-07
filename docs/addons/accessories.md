# Accessories (`rpg-accessories`)

> **Status:** Working — Per-player accessory bag with tier upgrades, in-bag upgrade button, and family-based stacking rules.

Adds the `ACCESSORY` item type and the **accessory bag** — a separate inventory GUI carrying accessories. Accessories contribute stats to the player while sitting in the bag.

## Adding accessories

Define them as normal items with `Type: ACCESSORY`. The optional `Accessory:` subsection sets family stacking behaviour:

```yaml
# plugins/rpg-core/items/rings.yml
bronze_ring:
  MinecraftItem: gold_ingot
  Type: ACCESSORY
  DisplayName: "&6Bronze Ring"
  Rarity: "&7&lCOMMON"
  Stats:
    max_health: 20
    strength: 5
  Accessory:
    Family: ring        # see "Stacking" below
    Stacking: highest   # highest | sum | independent

silver_ring:
  MinecraftItem: iron_ingot
  Type: ACCESSORY
  DisplayName: "&fSilver Ring"
  Rarity: "&a&lUNCOMMON"
  Stats:
    max_health: 35
    strength: 10
    crit_chance: 3
  Accessory:
    Family: ring
    Stacking: highest
```

Items without `Accessory.Family` always stack independently (every copy in the bag adds stats).

## The bag

A per-player inventory tab opened via `/accessories` (alias `/bag`). The **last slot is always the upgrade button** — it shows the cost to advance to the next tier and can be clicked to upgrade in-place.

## Tiers

```yaml
# plugins/rpg-accessories/config.yml
bag:
  title: "&5&lAccessory Bag"

# Each tier defines a row count (1–6 rows = 9–54 total slots).
# The last slot of each tier is reserved for the upgrade button.
# Usable accessory slots = rows × 9 − 1.
tiers:
  - { tier: 1, rows: 1, cost: 0 }          # 8 accessory slots — starting tier, free
  - { tier: 2, rows: 2, cost: 1000 }       # 17 slots
  - { tier: 3, rows: 3, cost: 5000 }       # 26 slots
  - { tier: 4, rows: 4, cost: 25000 }      # 35 slots
  - { tier: 5, rows: 5, cost: 100000 }     # 44 slots
  - { tier: 6, rows: 6, cost: 500000 }     # 53 slots — max
```

Upgrading can be done by:
- Clicking the **NETHER_STAR upgrade button** inside the bag (closes + reopens at new size)
- Running `/accessories upgrade`

## Stacking

The `Accessory.Stacking` field on each item controls how duplicates within the same `Family` behave:

| Mode | Behaviour |
|---|---|
| `highest` (default) | Only the copy with the greatest combined stat magnitude counts. Prevents bag-stuffing with duplicates. |
| `sum` | Every copy adds its stats to the total. |
| `independent` | Like `sum`, and each copy also fires its own passive ability hooks independently. |

Items with **no `Family`** are always counted in full regardless of duplicates.

### Example

```yaml
# Bag contains: bronze_ring, silver_ring, silver_ring  (Family: ring, Stacking: highest)
# Result: only the BEST silver_ring contributes — bronze_ring and the duplicate are ignored.

# Bag contains: fortune_talisman, fortune_talisman  (no Family declared)
# Result: both copies contribute (40 + 40 = 80 magic_find total).
```

## Aggregation

While in the bag, accessories feed their stats into the player's `StatHolder` alongside armor and base stats. The aggregation runs via `AccessoryService.aggregateStats` during `CoreRpgPlayer.recalculateStats`.

If an accessory is removed from the bag (placed in normal inventory, dropped, or deposited), its stats stop counting immediately on the next recalculation.

## Commands

| Command | Permission |
|---|---|
| `/accessories` (`/bag`) | `rpg.accessories.open` |
| `/accessories upgrade` | `rpg.accessories.upgrade` |
| `/accessories reload` | `rpg.accessories.admin.reload` |

## Persistence

Bag contents and current tier persist via `DataStore` per player. The upgrade button slot is never saved — it is always regenerated when the bag opens.

## Related

- [Items (ACCESSORY type)](../content/items.md#accessory)
- [Stats reference](../stats.md)
