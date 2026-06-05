![banner-addons](../assets/banners/banner-addons.png)

# Crafting (`rpg-crafting`)

> **Status:** Shipped — custom shaped and shapeless crafting-table recipes.

`rpg-crafting` registers admin-defined recipes with Bukkit so they show up in-game via the vanilla crafting table. Vanilla recipes are suppressed by rpg-core's `vanilla-suppression.crafting` flag; non-minecraft namespaced recipes (including all `rpg-crafting` ones) are always allowed through.

## Recipe YAML

Recipes live in `plugins/rpg-crafting/recipes/*.yml`. Each top-level key is the recipe ID.

### Shaped recipe

```yaml
diamond_sword_plus:
  Shape:
  - "DDD"
  - " S "
  - " S "
  Ingredients:
    D: { item: super_diamond, amount: 1 }
    S: { item: stick, amount: 1 }
  Output: { item: diamond_sword_plus, amount: 1 }
```

- `Shape` — 1–3 rows; each row is exactly 3 characters. Space = empty slot.
- `Ingredients` — maps each character to `{ item: <id>, amount: N }`. Items can be custom RPG item IDs or vanilla `Material` names.
- `Output` — `{ item: <id>, amount: N }`.

### Shapeless recipe

```yaml
torch_bundle:
  Shapeless: true
  Ingredients:
  - { item: torch, amount: 4 }
  - { item: stick, amount: 1 }
  Output: { item: torch_bundle, amount: 1 }
```

## Commands

| Command | Permission | Description |
|---|---|---|
| `/crafting reload` | `rpg.crafting.admin.reload` | Reloads all recipe files |
| `/crafting list` | `rpg.crafting.admin.list` | Shows how many recipes are loaded |

## Notes

- The recipes' `NamespacedKey` namespace is `rpg-crafting`. The vanilla crafting suppression listener in rpg-core only clears results with the `minecraft` namespace, so custom recipes are unaffected.
- On `/crafting reload`, all existing `rpg-crafting` recipes are unregistered (including a defensive sweep) before reloading from disk.
