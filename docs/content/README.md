![banner-content](../assets/banners/banner-content.png)

# Content authoring overview

> **Status:** In Progress — items, mobs, abilities, blocks, loot tables, admin spawners, and natural spawning all working. Recipes (crafting/cooking/brewing) are planned — station blocks open GUIs but recipe matching is not yet wired.

This section covers everything an admin authors as YAML to add content to the server: items, mobs, abilities, blocks, recipes, loot tables, NPCs, holograms, spawners, and natural-spawn rules.

## Rules of thumb

- Files can be **named anything** and split however an admin prefers — `items/swords.yml`, `items/armor.yml`, `items/everything.yml`, or one file per item, all valid.
- Each top-level YAML key in a file is the **content ID** (e.g., `red_gem`, `cave_zombie`, `super_diamond_sword`). IDs must be globally unique within their content type.
- IDs are lowercase, snake_case by convention. The loader is case-sensitive.
- Reload changes with `/<module> reload` or `/rpg reloadall`. No restart needed.
- Malformed entries log a clear error and are **skipped** — one bad item doesn't crash the plugin.

## Pages

| Page | Folder | What you author |
|---|---|---|
| [Items](items.md) | `items/` | Swords, armor, materials, consumables, upgrades, accessories |
| [Mobs](mobs.md) | `mobs/` | Custom mobs with stats, equipment, abilities, AI |
| [Abilities](abilities.md) | `abilities/` | Custom ability sequences (DSL + per-effect params) |
| [Blocks](blocks.md) | `blocks/` | Custom blocks tagged onto vanilla bases |
| [Recipes](recipes.md) | `recipes/crafting/`, `recipes/cooking/`, `recipes/brewing/` | Crafting / cooking / brewing recipes |
| [Loot tables](loot-tables.md) | `loot-tables/` | Drop tables (inlineable in mobs or external) |
| [Spawning](spawning.md) | `spawners/`, `natural-spawning/` | Admin spawners + natural spawn rules |

Other content types live in their owning addons:

- **NPCs** — in `rpg-npcs`, see [addons/npcs.md](../addons/npcs.md)
- **Holograms** — in `rpg-holograms`, see [addons/holograms.md](../addons/holograms.md)
- **Enchants / reforges / upgrades** — in `rpg-enchanting`, see [addons/enchanting.md](../addons/enchanting.md)
- **Regions** — in `rpg-regions`, see [addons/regions.md](../addons/regions.md)
- **Dungeons** — in `rpg-dungeons`, see [addons/dungeons.md](../addons/dungeons.md)
- **Quests** — in `rpg-quests`, see [addons/quests.md](../addons/quests.md)

## Lookup precedence

When an ID is referenced (in a recipe, a loot table, a drop list, etc.):

1. **Custom items / blocks / mobs / abilities** are looked up in their respective registry first.
2. If not found, the loader falls back to a vanilla `Material` / `EntityType` by the same key.
3. If neither matches, the entry is rejected with a clear error.

This lets admins reference `oak_planks` or `stone` directly in recipes without having to redefine vanilla items.
