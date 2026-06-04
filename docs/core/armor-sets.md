# Armor Sets

> **Status:** Working — `ArmorSetDef`, `ArmorSetRegistry`, `ArmorSetLoader`, and `ArmorSetListener` all implemented. Sets load from `plugins/rpg-core/sets/*.yml`. Stat bonuses and passive ability bindings activate/deactivate automatically as players equip or unequip pieces.

Armor sets let you reward players for collecting and wearing matching gear. Each set defines a list of item IDs and a map of **thresholds** — wearing at least `N` pieces of the set activates that tier's bonus. Only the highest satisfied tier is active at any one time.

---

## YAML schema

Sets are defined in `plugins/rpg-core/sets/*.yml`. Any number of files, any number of sets per file.

```yaml
berserker_set:
  Name: "&c&lBerserker's Set"      # display name shown in item lore
  Pieces:                          # item IDs that count as pieces; one slot per piece
    - berserker_helmet
    - berserker_chestplate
    - berserker_leggings
    - berserker_boots
  Bonuses:
    2:                             # threshold: ≥2 pieces → this bonus activates
      Stats:
        ferocity: 25
      Abilities:
      - "~on_hit particles{type=crit}"
    4:                             # full set
      Stats:
        ferocity: 75
        damage: 50
      Abilities:
      - "~on_hit particles{type=heart}"
```

### Fields

| Field | Required | Description |
|---|---|---|
| `Name` | Yes | Colored display name shown in item lore |
| `Pieces` | Yes | List of item IDs. One count per armor slot (duplicates in list don't stack). |
| `Bonuses` | No | Map of `threshold: SetBonus`. Keys are the minimum piece count to activate. |

### SetBonus fields

| Field | Description |
|---|---|
| `Stats` | Flat stat bonuses added on top of equipment stats (same stat IDs as items). |
| `Abilities` | List of ability bindings using `~trigger effect{}` syntax. |
| `Scale` | Optional multiplier applied to **all numeric params** in `Abilities` at load time. See below. |

---

## Tier resolution

When a player's armor changes, the system counts how many pieces of each set they're wearing across all four armor slots. For each set, the **highest threshold that is ≤ piece count** becomes active.

```
berserker_set has thresholds 2, 4.
Player wears 3 pieces → threshold 2 is active (since 2 ≤ 3 < 4).
Player wears 4 pieces → threshold 4 is active.
Player wears 0 pieces → no bonus (threshold 0 → nothing).
```

Only one tier can be active per set per player at a time. Swapping a single piece drops from 4/4 to 3/4, deactivating threshold 4 and falling back to threshold 2.

---

## Stat bonuses

Set stat bonuses are applied as **Layer 2.5** in the stat pipeline — after equipment stats, before accessory stats and status-effect modifiers:

```
Base stats
  + Equipment stats (armor + main hand)
  + Set bonus stats          ← here
  + Accessory stats
  + Status-effect modifiers (flat then %)
= Effective stats
```

Stats stack with equipment stats of the same type using the same flat-addition formula.

---

## Ability bindings

Set bonuses support the same `~trigger effect{}` syntax as item abilities — with one restriction: **only passive/proc triggers are valid** (`on_hit`, `on_hurt`, `on_jump`, `passive`). Active click triggers (`right_click`, `left_click`, etc.) on set bonuses are silently ignored with a console warning.

```yaml
Bonuses:
  4:
    Abilities:
    - "~on_hit drain{amount=10}"        # valid
    - "~passive heal{amount=2}"         # valid
    - "~right_click explode{radius=4}"  # ignored — sets can't have click abilities
```

**Abilities fire once per event**, regardless of how many pieces are worn. Wearing a full 4/4 set with an `on_hit` bonus fires the ability exactly once per hit, not four times.

Set passive abilities are checked alongside item passive abilities by the same listener infrastructure. The proc queue is:
1. Bindings from all equipped armor slots (per item)
2. Bindings from main-hand item
3. Active set bonus bindings (deduplicated — one per set, not one per piece)

---

## Scale: automatic tier derivation

Instead of writing different ability params per tier, you can use `Scale:` to auto-derive a weaker tier from the stronger one. `Scale: 0.5` halves all numeric params in `Abilities` **at load time** — no runtime cost.

```yaml
flame_set:
  Name: "&6&lFlame Walker's Set"
  Pieces: [flame_helmet, flame_chestplate, flame_leggings, flame_boots]
  Bonuses:
    2:
      Stats:
        health_regen: 5
      Scale: 0.5
      Abilities:
      - "~on_hurt heal{amount=10}"    # stored as heal{amount=5} after scaling
    4:
      Stats:
        health_regen: 15
      Abilities:
      - "~on_hurt heal{amount=10}"    # full value — no Scale
      - "~passive heal{amount=2}"
```

**How Scale works:**
- Every numeric param value in every ability binding is multiplied by `Scale`.
- Non-numeric params (e.g. `type=crit`, `target=caster`) pass through unchanged.
- The scale is applied once at load time to the stored `AbilityInvocation` params.
- The original YAML values are not modified on disk.

**When to use Scale vs explicit values:**
- Use `Scale` when tiers share the same ability shape with weaker numbers — one source of truth, less repetition.
- Use explicit values per tier when tiers need fundamentally different effects (e.g. tier 2 procs particles, tier 4 procs drain + particles).
- Both approaches can coexist in the same set.

---

## Item lore

Any item with a `SetId:` field automatically renders a set info block in its lore, just above the rarity line:

```
§6Berserker's Set
  §8(2/4) §f+25 Ferocity §8| §7On Hit
  §8(4/4) §f+75 Ferocity, +50 Damage §8| §7On Hit
```

The `(N/4)` shows the threshold and total piece count. Stat values use the same sign/format as item stats. Ability bindings are summarized as their trigger hint (`On Hit`, `Passive`, `On Hurt`, etc.). Dynamic piece count (how many the player is currently wearing) is not shown in static item lore — this can be added to a HUD plugin.

---

## Wiring a new set

1. **Define the set** in `plugins/rpg-core/sets/myfile.yml`.
2. **Tag the items** — add `SetId: your_set_id` to each piece's item YAML.
3. **Reload** with `/rpg reloadall`.
4. Give the items with `/rpg item give <id>` and equip them.

Sets and items reload independently — you can adjust tier thresholds without touching item YAML, and vice versa.

---

## Limitations

- A single item ID can only belong to one set. Defining the same ID in two sets results in only the last-loaded one counting.
- Pieces are counted from the four armor slots only (helmet, chestplate, leggings, boots). Main-hand, off-hand, and accessory bag slots do not count.
- The `Pieces:` list is for documentation only — the engine counts items by their `SetId` field, not by checking the list. Listing an item in `Pieces:` that doesn't have a matching `SetId:` just makes the lore confusing; listing fewer items than have that `SetId:` under-reports the total in lore.

---

## Related

- [Items → Armor Set Pieces](../content/items.md#armor-set-pieces)
- [Abilities → Armor set abilities](../content/abilities.md#armor-set-abilities)
- [Stats reference](../stats.md)
