# Accessories (`rpg-accessories`)

> **Status:** In Progress — Per-player accessory bag (27 slots default, configurable rows 1-6). `/accessories` (alias `/bag`) opens it. Only `ACCESSORY`-type items can be placed inside (cursor + shift-click both validated). Stats from accessories in the bag aggregate into `CoreRpgPlayer.recalculateStats` via the new `AccessoryService` API. Persistence via DataStore — bag contents save on close/quit, load on first open/join. Tier upgrades + Family-based stacking rules deferred to a follow-up.

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
  title: "&5&lAccessory Bag"

# Each tier defines a row count (1–6 rows = 9–54 slots).
# cost is in primary currency; the player upgrades from tier N to N+1
# by paying the cost on tier N+1.
tiers:
  - { tier: 1, rows: 1, cost: 0 }
  - { tier: 2, rows: 2, cost: 1000 }
  - { tier: 3, rows: 3, cost: 5000 }
  - { tier: 4, rows: 4, cost: 25000 }
  - { tier: 5, rows: 5, cost: 100000 }
  - { tier: 6, rows: 6, cost: 500000 }
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
| `/accessories reload` | `rpg.accessories.admin.reload` (op) |

## Persistence

Bag contents and current tier persist via `DataStore` per player.

## Related

- [Items (ACCESSORY type)](../content/items.md#accessory)
- [Stats reference](../stats.md)
