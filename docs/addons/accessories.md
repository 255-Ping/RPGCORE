# Accessories (`rpg-accessories`)

> **Status:** Planned

Adds the `ACCESSORY` item type and the **accessory bag** — a separate inventory GUI carrying accessories. Accessories contribute stats to the player while sitting in the bag.

## Adding accessories

Define them as normal items with `Type: ACCESSORY`. See [items.md](../content/items.md#accessory) for the schema.

```yaml
zombie_talisman:
  MinecraftItem: zombie_head
  Type: ACCESSORY
  DisplayName: "&aZombie Talisman"
  Rarity: "&a&lUNCOMMON"
  Stats:
    max_health: 10
  Accessory:
    Family: zombie_talisman      # see "Stacking"
```

## The bag

A per-player inventory tab opened via `/accessories` (alias `/bag`). Default size 27 slots (3 rows); upgradeable.

## Tiers

```yaml
# plugins/rpg-accessories/config.yml
bag:
  tiers:
    1: { slots: 27, upgrade-cost: { coins: 0 } }
    2: { slots: 36, upgrade-cost: { coins: 50000, items: { small_storage: 1 } } }
    3: { slots: 45, upgrade-cost: { coins: 250000 } }
  upgrade-permission: rpg.accessories.upgrade
```

`/accessories upgrade` opens the upgrade GUI.

## Stacking

Accessories in the same `Family` only count once (highest-rarity wins) by default. Per-accessory override:

```yaml
Accessory:
  Family: zombie_talisman
  Stacking: highest              # highest | sum | independent
```

- `highest` (default) — only the highest-stat copy in that family counts. Prevents bag-stuffing.
- `sum` — every copy adds its stats.
- `independent` — every copy adds its stats *and* runs its own status-effect hooks independently.

## Aggregation

While in the bag (regardless of stacking), accessories feed their stats into the player's `StatHolder` along with armor and base stats. They're not "equipped" in vanilla slots — they're an aggregation source.

If an accessory item is removed from the bag (placed in normal inventory, dropped, deposited in a chest), its stats stop counting.

## Commands

| Command | Permission |
|---|---|
| `/accessories` (`/bag`) | `rpg.accessories.open` |
| `/accessories upgrade` | `rpg.accessories.upgrade` |

## Persistence

Bag contents and current tier persist via `DataStore` per player.

## Related

- [Items (ACCESSORY type)](../content/items.md#accessory)
- [Stats reference](../stats.md)
