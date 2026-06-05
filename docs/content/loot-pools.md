![banner-loot](../assets/banners/banner-loot.png)

# Loot Pools

> **Status:** Working — Named loot pools load from `plugins/rpg-core/loot-pools/*.yml`, register in `LootPoolRegistry`, and are referenced by mobs via `LootPool:` / `LootPools:`. Vanilla XP orbs and combat skill XP both fire on kill.

Loot pools are **named, reusable drop tables** defined in their own YAML files. Any number of mobs can reference the same pool by ID. When a mob dies, all referenced pools roll independently and their rewards stack on top of each other and on top of any inline `LootTable:`.

---

## Why pools instead of inline tables?

| Inline `LootTable:` | Named `LootPool:` |
|---|---|
| Defined per-mob, repeated if multiple mobs share the same drops | Defined once, referenced by ID from any mob |
| Changing drops requires editing every mob | Change the pool once — all mobs referencing it update |
| No vanilla XP or skill XP support | Carries `exp:` (vanilla XP) and `combat-exp:` (skill XP) |
| Good for unique one-off mobs | Good for mob archetypes (trash, elite, boss tiers) |

Both coexist: a mob can have an inline `LootTable:` (for unique drops) **and** one or more `LootPool:` references (for shared archetype drops). All roll on death.

---

## Pool YAML schema

Defined in `plugins/rpg-core/loot-pools/*.yml`. Any number of files; any number of pools per file.

```yaml
goblin_drops:
  attribution: last-hit           # last-hit | top-damager | split-equal | weighted-by-damage
  roll-mode: per-player           # per-player | shared
  exp: 15                         # vanilla XP orbs spawned at corpse (default 0)
  combat-exp: 50                  # skill XP awarded to every damager (default 0)
  combat-skill: combat            # skill ID for combat-exp (default "combat")
  rolls:
    - { item: goblin_fang,    chance: 60.0, min: 1, max: 2 }
    - { item: gold_nugget,    chance: 40.0, min: 1, max: 5, magic-find-affected: true }
    - { item: rare_goblin_hat, chance: 1.0, min: 1, max: 1, magic-find-affected: true }
  guaranteed:
    - { item: bone, min: 1, max: 1 }
  currency-rolls:
    - { chance: 100.0, min: 20, max: 50 }
```

### Fields

| Field | Default | Description |
|---|---|---|
| `attribution` | `weighted-by-damage` | Who receives the loot. See [Attribution modes](#attribution-modes). |
| `roll-mode` | `per-player` | `per-player` = each eligible player rolls the full table. `shared` = table rolls once, items distributed round-robin. |
| `exp` | `0` | Vanilla XP orbs spawned at the mob's corpse. Added to the mob's `XP:` field and any other active pools. |
| `combat-exp` | `0` | Skill XP awarded to every player who dealt damage on the kill. |
| `combat-skill` | `combat` | Which skill receives the `combat-exp` award. |
| `rolls` | `[]` | Chance-based item drops. `magic-find-affected: true` multiplies the chance by `(1 + magic_find / 100)`. |
| `guaranteed` | `[]` | Items always dropped (no chance roll). Every eligible player gets a copy. |
| `currency-rolls` | `[]` | Direct currency deposit — no item entity spawned. Each eligible player receives the rolled amount. |

---

## Referencing pools from mobs

```yaml
# Single pool
goblin:
  LootPool: goblin_drops

# Multiple pools — all roll independently on kill
goblin_champion:
  LootPool: goblin_drops          # base drops
  LootPools:                      # additional drops
    - goblin_drops
    - elite_bonus_drops
```

> If both `LootPool:` and `LootPools:` are set, the single `LootPool:` is treated as the first entry and de-duplicated. Inline `LootTable:` also rolls alongside pool references if present.

---

## Attribution modes

| Mode | Who gets loot |
|---|---|
| `last-hit` | The player who landed the killing blow |
| `top-damager` | The player who dealt the most total damage |
| `split-equal` | Every player who dealt any damage |
| `weighted-by-damage` | Every player, but items are distributed proportionally to damage dealt |

---

## XP rewards

Three independent XP sources stack on every kill:

| Source | Field | Where |
|---|---|---|
| Mob base XP | `XP: N` on the mob | `mobs/*.yml` |
| Inline table XP | `exp: N` in `LootTable:` section | Mob's inline table |
| Pool XP | `exp: N` in pool definition | `loot-pools/*.yml` |

All three sum into the single `EntityDeathEvent.setDroppedExp(total)` call — one XP orb burst at the corpse.

**Combat skill XP** (`combat-exp:`) is independent: it calls `RpgServices.skills().awardXp(player, skillId, amount)` for every player who dealt damage, regardless of attribution mode. If both an inline table and a pool specify `combat-exp`, the amounts add together.

---

## Reloading

```
/rpg reloadall
```

Pools reload before mobs — mob YAML always sees the latest pool registry.

---

## Example pools

See `plugins/rpg-core/loot-pools/example.yml` for the three starter pools:
- `basic_mob_drops` — general trash-mob drops with magic-find-affected gems
- `elite_bonus_drops` — stacked on top of another pool for champion variants
- `boss_drops` — rich table for boss-tier kills

And `plugins/rpg-core/mobs/example.yml` for usage examples:
- `testmob` — `LootPool: basic_mob_drops`
- `mini_boss` — `LootPools: [boss_drops, elite_bonus_drops]`
- `shield_golem` — `LootPools: [boss_drops, elite_bonus_drops]`

---

## Related

- [Mobs](mobs.md)
- [Items](items.md)
- [Stats reference → magic_find](../stats.md)
