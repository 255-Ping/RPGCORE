![banner-farming](../assets/banners/banner-farming.png)

# Farming (`rpg-farming`)

> **Status:** In Progress — Farming XP from harvesting fully grown crops is working. `farming_fortune` stat is wired. Full custom-crop redesign (custom growth stages, not-breakable-until-grown, configurable growth time) is planned — see [todo-improvements](../planned/todo-improvements.md).

Hooks into `PlayerHarvestBlockEvent` and `BlockBreakEvent` for fully-grown vanilla crops. Awards farming skill XP and applies `farming_fortune` to drop rolls.

## Current behavior

- Breaking a **fully-grown** vanilla crop (`WHEAT`, `CARROTS`, `POTATOES`, `BEETROOTS`, `NETHER_WART`, etc.) awards farming XP.
- The `farming_fortune` stat multiplies drop quantity using the same formula as mining fortune: `floor(fortune/100)` guaranteed extra drops + fractional probabilistic extra.
- Immature crops are unaffected — no XP, no fortune bonus, vanilla drops only.

## Planned redesign

The farming redesign will replace vanilla crop tracking with a custom-block approach similar to mining:
- Admins assign custom farming block types to world blocks via a command (like `/rpg block convert`)
- Custom crops progress through configurable visual growth stages
- Not breakable until fully grown (enforced break-cancel)
- Growth time configurable per crop type in `config.yml`
- Breaking a fully grown crop drops from a configured loot table and restarts the growth cycle

Until the redesign ships, only vanilla crops award XP and fortune.

## Stats

| Stat | Effect |
|---|---|
| `farming_fortune` | Multiplies drop quantity from harvested crops. `farming_fortune: 100` = double drops on average. |
| `farming_wisdom` | % bonus farming XP per harvest. |

## Example farming tool

```yaml
enchanted_hoe:
  MinecraftItem: diamond_hoe
  Type: MATERIAL
  DisplayName: "&aEnchanted Hoe"
  Rarity: "&a&lUNCOMMON"
  Stats:
    farming_fortune: 75
    farming_wisdom: 20
```

## Config

`plugins/rpg-farming/config.yml`:

```yaml
# Base XP per successful harvest (scales with farming_wisdom)
xp-per-harvest: 5

# Vanilla crop materials that grant XP when broken at full growth.
# Remove any you want to handle manually.
tracked-crops:
  - WHEAT
  - CARROTS
  - POTATOES
  - BEETROOTS
  - NETHER_WART
  - COCOA
  - SWEET_BERRY_BUSH
```

## Commands

| Command | Permission |
|---|---|
| `/farming reload` | `rpg.farming.admin.reload` |

## Related

- [Stats reference](../stats.md) — `farming_fortune`, `farming_wisdom`
- [Skills framework](../core/skills.md)
- [Blocks](../content/blocks.md) — will be used by the future farming redesign
