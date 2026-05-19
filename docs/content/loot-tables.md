# Loot tables

> **Status:** Planned

Loot tables describe what drops from a mob (or a custom block, or a chest). They can be inlined in a mob's YAML or extracted as standalone files for sharing.

## Inline (in `mobs/`)

```yaml
testmob:
  # ... other mob fields ...
  LootTable:
    attribution: weighted-by-damage   # last-hit | top-damager | split-equal | weighted-by-damage
    roll-mode: per-player            # per-player | shared
    coin-drop: { min: 5, max: 20 }
    rolls:
    - { item: red_gem, chance: 5.0, min: 1, max: 1, magic-find-affected: true }
    - { item: zombie_meat, chance: 50.0, min: 1, max: 3 }
    - { item: rare_helmet, chance: 0.5, min: 1, max: 1, magic-find-affected: true }
    guaranteed:
    - { item: experience_token, min: 1, max: 1 }
```

## External (in `loot-tables/<file>.yml`)

```yaml
generic_undead:
  attribution: weighted-by-damage
  roll-mode: per-player
  coin-drop: { min: 5, max: 20 }
  rolls:
  - { item: rotten_flesh, chance: 80.0, min: 1, max: 3 }
  - { item: bone, chance: 30.0, min: 1, max: 2 }
```

Then reference it from a mob:

```yaml
testmob:
  LootTable: generic_undead      # by id, string form
```

## Field reference

| Field | Meaning |
|---|---|
| `attribution` | Who gets to roll the loot |
| `last-hit` | Final-blow player rolls everything |
| `top-damager` | Player who dealt the most damage rolls |
| `split-equal` | Drops split equally among damagers |
| `weighted-by-damage` | Drops split proportional to damage dealt |
| `roll-mode` | `shared` = one roll for the table; `per-player` = each eligible player rolls independently |
| `coin-drop` | Optional currency drop; physical pickup item that credits the picker's balance |
| `rolls[].chance` | Percent chance, `5.0` = 5% |
| `rolls[].magic-find-affected` | If true, player's `MAGIC_FIND` stat multiplies the chance |
| `guaranteed` | Always drops, no chance roll, ignores `attribution` and `roll-mode` (every eligible player gets it) |

## Magic Find scaling

When `magic-find-affected: true`, the effective chance becomes:

```
effective_chance = base_chance * (1 + magic_find / 100)
```

(Formula is configurable in core config.)

## Damager tracking

The damage pipeline records all damage dealt to each mob, attributing each blow to its source player (or the player who "owns" the attacker — e.g., a player's projectile). On death, `attribution` and `roll-mode` consume this record.

## Currency drops

Coin amounts are deducted from no one — they're freshly created as pickup-able "coin pile" items. On pickup, the item is consumed and the picker's balance is credited. Configurable per-tier whether other players can pick up your kill's coins. See [economy addon](../addons/economy.md).

## Related

- [Mobs](mobs.md)
- [Stats reference](../stats.md) — `MAGIC_FIND`, `PRISTINE`
- [Economy addon](../addons/economy.md)
- [Damage pipeline](../core/damage.md)
