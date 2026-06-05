![banner-addons](../assets/banners/banner-addons.png)

# Smelting (`rpg-smelting`)

> **Status:** Shipped — timed smelting station with progress bar + optional vanilla furnace recipe registration.

`rpg-smelting` provides a custom block station that opens a GUI-driven smelting workflow, mirroring the cooking and alchemy station pattern. It also optionally registers vanilla `FurnaceRecipe` entries so the same items smelt in a regular furnace.

## Station block

On first enable, `rpg-smelting` installs `rpg-smelting-stations.yml` into `plugins/rpg-core/blocks/` and triggers a reload. The default block is `BLAST_FURNACE`. You can edit the file to use any block type with `StationType: smelting`.

## Recipe YAML

Recipes live in `plugins/rpg-smelting/recipes/*.yml`.

```yaml
iron_ingot_from_ore:
  Input:  { Item: iron_ore, Amount: 1 }
  Output: { Item: iron_ingot, Amount: 1 }
  SmeltTicks: 200      # 200 ticks = 10 seconds. 0 = instant.
  RequiredLevel: 1     # Minimum Mining skill level
  VanillaXP: 0.7       # XP orbs if vanilla furnace registration is enabled
```

| Field | Default | Description |
|---|---|---|
| `Input` | required | `{ Item: <id>, Amount: N }` — single ingredient |
| `Output` | required | `{ Item: <id>, Amount: N }` — result item |
| `SmeltTicks` | `default-smelt-ticks` config | Ticks to smelt. 0 = instant (no progress bar). |
| `RequiredLevel` | `1` | Minimum Mining skill level to use the recipe |
| `VanillaXP` | `0.1` | XP orbs awarded by the vanilla furnace on completion |

Item IDs can be custom RPG item IDs or vanilla `Material` names (e.g. `IRON_ORE`).

## GUI layout

```
Row 0: [progress bar — orange panes + furnace info item]
Row 1: [bg] [bg] [bg] [bg] [INPUT] [bg] [bg] [bg] [bg]
Row 2–4: recipe tiles (27 per page)
Row 5: ← PREV | bg | bg | bg | ✖ CLOSE | bg | bg | bg | NEXT →
```

### Timed smelting

When `SmeltTicks > 0`:
1. Clicking a recipe tile consumes the input immediately.
2. Orange panes in row 0 fill left-to-right over the smelt duration.
3. Closing mid-smelt saves state to DataStore; reopening any smelting station resumes.
4. On completion: output delivered, Mining XP awarded, furnace crackle sound played.

### Instant smelting

When `SmeltTicks = 0`, output is delivered immediately on click with no progress bar.

## Vanilla furnace recipes

With `features.vanilla-furnace-recipes: true` (default), each recipe is also registered as a `FurnaceRecipe` so it works in a regular furnace. rpg-core's `VanillaSuppressionListener` now allows any non-`minecraft` namespace through smelting, so these recipes work even with vanilla smelting suppressed.

## config.yml

```yaml
features:
  smelting: true
  vanilla-furnace-recipes: true
default-smelt-ticks: 200
xp:
  per-smelt: 20     # Mining XP per successful smelt
```

## Commands

| Command | Permission | Description |
|---|---|---|
| `/smelting reload` | `rpg.smelting.admin.reload` | Reloads recipes and re-registers furnace recipes |
| `/smelting list` | `rpg.smelting.admin.list` | Shows recipe count |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `rpg.smelting.use` | `true` | Use the smelting station |
| `rpg.smelting.admin.reload` | op | Reload config + recipes |
| `rpg.smelting.admin.list` | op | List loaded recipes |

## Storage

In-progress smelt state is stored in the `smelting_craft` DataStore repository, keyed by player UUID. The record holds `recipe_id` and `elapsed_ticks`. On reconnect / reopen, the saved state is restored automatically.
