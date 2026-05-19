# Selection wand

> **Status:** Planned

A single admin tool for selecting locations, volumes, and regions across the suite. Modes are switched via `/rpg wand <mode>`.

## Get the wand

`/rpg wand [mode]` — perm `rpg.core.wand` (default op). If `mode` is omitted, gives the wand in its last-used mode.

The item is a marked stick (CMD reserved by `rpg-core`). Drop it or replace it freely — `/rpg wand` always regenerates.

## Modes

| Mode | Use |
|---|---|
| `dungeon` | Two-point 3D box selection for dungeon authoring. Left-click sets corner 1, right-click sets corner 2. Visible particle outline. |
| `spawner` | Marks the next right-click as a spawner location; opens `/spawner create` flow. |
| `entrance` | Marks a TP destination (dungeon entrance / exit, NPC anchor, etc.). |
| `region` | Two-point 3D box selection for region creation. Same UX as dungeon mode. |
| `hologram` | Marks a hologram anchor location. |

More modes added as systems land.

## Switching modes

`/rpg wand <mode>` — switches the current player's wand to a new mode without re-giving it.

`/rpg wand` (no arg) — gives the wand if missing, prints current mode if present.

## Selection state

Selections are per-player, in-memory only. Crossing world / disconnecting clears them. Use `/<system> save <id>` to persist a selection into that system's storage.

## Visual

- 3D box selections: corners marked with a colored beacon-beam-style particle column; edges with dust particles every block.
- Marker modes: single point shown with a rotating particle ring.

## API surface

```java
SelectionWand wand = RpgServices.selectionWand();
Optional<Selection> sel = wand.currentSelection(player);
// Selection ::= { mode: String, points: List<Location>, world: World }
```

Addons that consume wand selections read from this service rather than parsing wand state themselves.

## Related

- [Dungeons addon](../addons/dungeons.md)
- [Regions addon](../addons/regions.md)
- [Spawners](../content/spawning.md)
- [Holograms addon](../addons/holograms.md)
