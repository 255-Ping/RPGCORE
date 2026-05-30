# Recipes

> **Status:** Working — Cooking and brewing recipe matching fully implemented. Station right-click dispatch routes via `StationType` on the block definition (no per-addon config needed). Crafting recipes (shaped/shapeless) register with Bukkit. Custom items are matched by PDC item-id; vanilla materials by `Material` name.

Three recipe systems share a common shape: **crafting**, **cooking**, and **brewing**. All vanilla recipes are cancelled by default (per [vanilla suppression](../core/vanilla-suppression.md)) — every recipe in the game is authored by admins.

## Folders

- `plugins/rpg-core/recipes/crafting/` — 3×3 crafting bench recipes
- `plugins/rpg-core/recipes/cooking/` — cooking station recipes
- `plugins/rpg-core/recipes/brewing/` — brewing station recipes

## Crafting recipes

A custom block with `StationType: crafting` opens a 3×3 GUI. Alternatively `/craft` works anywhere (perm `rpg.core.craft`, default true).

### Shaped

```yaml
super_diamond_sword:
  Shape:
  - "DDD"
  - "DDD"
  - " S "                        # space = empty slot
  Ingredients:
    D: { item: super_diamond, amount: 1 }
    S: { item: oak_stick, amount: 1 }   # vanilla materials valid as item IDs
  Output: { item: super_diamond_sword, amount: 1 }
  Requirements:                  # optional
    permission: rpg.crafting.super_diamond_sword
    skills: { enchanting: 5 }
```

### Shapeless

```yaml
enchanted_book:
  Shapeless: true
  Ingredients:
  - { item: book, amount: 1 }
  - { item: experience_bottle, amount: 16 }
  Output: { item: enchanted_book, amount: 1 }
```

### Ingredient quantities > 1

`amount: 16` in a shaped slot requires that slot to be filled with a stack of 16 (Hypixel SkyBlock pattern). For shapeless, the total amount across all slots must meet the requirement.

## Cooking recipes

A custom block with `StationType: cooking` opens the cooking GUI (recipe-pick list, not a 3-input layout). Outputs are typically `CONSUMABLE` items applying status effects or stat buffs.

```yaml
roasted_meat:
  Inputs:
  - { Item: raw_meat, Amount: 1 }
  CookTicks: 200
  Output: { Item: roasted_meat, Amount: 1 }
  RequiredLevel: 5
```

## Brewing recipes

A custom block with `StationType: brewing` opens the brewing GUI. Vanilla brewing stand mechanics are cancelled — outputs are admin-defined items.

```yaml
strength_potion_t1:
  Inputs:
  - { Item: strength_essence, Amount: 1 }
  - { Item: glass_bottle, Amount: 1 }
  BrewTicks: 600
  Output: { Item: strength_potion_t1, Amount: 1 }
  RequiredLevel: 5
```

The output potion is itself an item with a `Consumable:` block in its YAML that applies the status effects on right-click.

## Output behavior

- Crafting: instant on click in the crafting GUI.
- Cooking / brewing: queue-and-walk-away. Block shows particles + a progress indicator. Player can leave and come back.
- Queue is persisted with the block — surviving server restart for blocks whose `RespawnTicks: 0` (i.e., the station itself doesn't despawn). If the station is broken mid-brew, the queue is lost (configurable: refund inputs vs lose them).

## Lookup

Recipes are matched at runtime against the GUI's input grid (crafting) or input list (cooking, brewing). Custom items are matched by `ItemRegistry.from(stack).id()`; vanilla materials by `Material`. PDC-tagged custom items never match vanilla material recipes — they're strictly distinguished.

## Related

- [Items](items.md)
- [Blocks](blocks.md) — custom station blocks
- [rpg-cooking](../addons/skills.md#cooking)
- [rpg-alchemy](../addons/skills.md#alchemy)
- [Vanilla suppression](../core/vanilla-suppression.md)
