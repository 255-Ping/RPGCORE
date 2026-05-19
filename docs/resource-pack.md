# Resource pack & CustomModelData ranges

> **Status:** Planned

The plugin suite does not ship a resource pack. Admins author their own. To prevent CMD collisions across addons, the following integer ranges are reserved per-addon. Admin custom content should use `10000+`.

| Range | Owning addon |
|---|---|
| `1000-1999` | `rpg-mining` |
| `2000-2999` | `rpg-foraging` |
| `3000-3999` | `rpg-farming` |
| `4000-4999` | `rpg-fishing` |
| `5000-5999` | `rpg-cooking` |
| `6000-6999` | `rpg-alchemy` |
| `7000-7999` | `rpg-enchanting` |
| `8000-8999` | `rpg-accessories` |
| `9000-9999` | `rpg-economy` (currency icons, coin pile drops, etc.) |
| `10000+` | Admin-defined custom content |

## How items reference CMDs

Item YAML:

```yaml
red_gem:
  MinecraftItem: emerald
  Type: MATERIAL
  CustomModelData: 1042
  DisplayName: "&cRed Gem"
  ...
```

Paper 26.1.2 supports the modern `minecraft:custom_model_data` component (int/float/string/flag lists). For v1 the loader uses the integer form via `ItemMeta.setCustomModelData(int)`.

## What we use CMDs for

- Distinguishing custom items from vanilla ones in the resource pack
- Per-addon "themed" model overrides (e.g., custom pickaxe variants in the mining range)
- Distinguishing custom blocks visually if the admin pairs them with a textured base material plus custom item icons (note: world blocks themselves use vanilla textures; CMDs apply to the *item form* of the block)

## What we don't use

- No `nbt` lore-encoded magic strings — we use Persistent Data Containers (PDC) for all custom-item identification.
- No spaces in CMD ranges that conflict with common public packs — admins should avoid using `10000-19999` if they want compatibility with widely-used SkyBlock packs.

## Related

- [Items](content/items.md)
- [Blocks](content/blocks.md)
