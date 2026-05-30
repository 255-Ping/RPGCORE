# Damage pipeline

> **Status:** In progress — pipeline working; stat aggregation from equipment/accessories/effects/milestones/guild perks is wired for players. Mob attackers/victims return 0 for all stats until mob stat-holder lands. Combat XP awards and damage indicators are live. Per-source damage tuning and ferocity are planned.

All damage flows through `rpg-core`. Vanilla `EntityDamageEvent` is cancelled at `LOWEST` priority and replaced with our pipeline.

## Flow

1. **Event captured** — `EntityDamageEvent` (any cause). Cancelled.
2. **Source identified** — attacker entity if direct, owner if projectile, environmental tag (`fall`, `lava`, `void`, `suffocation`, etc.) otherwise.
3. **`DamageContext` built** — `attacker, victim, baseDamage, source`.
4. **`PreDamageEvent` fired** — cancellable; addons can read/modify the context, apply on-hit effects, set crit, mark `trueDamage`, change `source`.
5. **Stat math applied** — see [Damage math](#damage-math).
6. **HP applied** — victim's RPG HP reduced; bukkit HP resynced.
7. **`PostDamageEvent` fired** — read-only, with final dealt damage. Combat-XP listeners, damage-indicator listeners, etc. hook here.
8. **Damage indicators spawned** (if rpg-holograms loaded) at hit location.

## Damage math

Default formulas (all configurable in `plugins/rpg-core/config.yml` under `damage.formula`):

```yaml
damage:
  formula:
    melee:                       # used when source is a direct melee hit
      raw: "(weapon_damage + strength_bonus_flat) * (1 + strength / 100)"
    ability:                     # ability-effect damage
      raw: "ability_base * (1 + ability_damage / 100) * (1 + intelligence / 1000)"
    crit:
      chance-cap: 100            # crit_chance caps here
      multiplier: "1 + crit_damage / 100"
    defense:
      formula: "damage * (1 - defense / (defense + 100))"
    true-defense:
      formula: "damage * (1 - true_defense / (true_defense + 100))"
    lifesteal:
      cap-percent: 50            # max lifesteal-healed-per-hit as % of max HP
    ferocity:
      extra-hit-window-ticks: 4  # ticks between primary hit and ferocity-induced extra hits
    pvp-damage-multiplier: 1.0
    sources:                     # per-source damage tuning
      void: { multiplier: 1.0 }
      lava: { multiplier: 1.0 }
      fall: { multiplier: 1.0 }
      suffocation: { multiplier: 1.0 }
      fire: { multiplier: 1.0 }
      drowning: { multiplier: 1.0 }
```

Formulas use the in-house expression evaluator (see [skills.md](skills.md#level-curves)).

## True damage

When `DamageContext#trueDamage` is true, the `defense` stat is bypassed and `true_defense` applies instead. Mark a hit as true by:

- An ability effect setting it (`damage{type=true}`)
- A weapon attribute (`Stats: { is_true_damage: 1 }`) — TBD
- A status effect (e.g., `vulnerability`) — TBD

## Crits

On each hit, roll `crit_chance` (clamped to `chance-cap`). If success, multiply by `crit-multiplier`.

## Ferocity

After a successful hit, if `ferocity > 0`, roll for extra hits (`ferocity / 100` chance, repeatable for ferocity > 100 — first 100% guarantees one, remaining rolls for second, etc.). Each extra hit fires the full pipeline (including its own crit roll) after a configurable delay.

## Death rules

Permission-tier list, ordered. First permission a player holds wins.

```yaml
death-rules:
  groups:
  - permission: rpg.death.keep-all
    drop-items-percent: 0
    drop-coins: 0
    drop-coins-pickup: false
    keep-xp: true
  - permission: rpg.death.partial
    drop-items-percent: 60
    drop-coins-amount: percent     # number | "all" | "percent"
    drop-coins-percent: 50
    drop-coins-max: 10000          # cap on dropped amount
    drop-coins-pickup: true
    keep-xp: true
  - permission: rpg.death.full
    drop-items-percent: 100
    drop-coins-amount: all
    drop-coins-pickup: true
    keep-xp: false
  default-permission: rpg.death.partial
```

Inventory containers (shulker boxes, bundles) preserve their contents when dropped.

Coin drops physically spawn as pickup-able items; on pickup, the picker is credited (cross-player or self).

## Combat tagging

Player is "in combat" for N seconds after dealing or taking damage. `HEALTH_REGEN` is paused (or reduced) while in-combat.

```yaml
combat-tag:
  duration-seconds: 8
  vitality-reduction-percent-per-point: 1
  vitality-reduction-cap-percent: 50
```

## API surface

```java
PreDamageEvent — Cancellable, mutable context
PostDamageEvent — read-only, with finalDealt
DamageContext  — { attacker, victim, baseDamage, critMultiplier, trueDamage, source }
```

## Actual v1 formula (implemented in `DamageMath.java`)

The configurable formula strings in the YAML above are the design target. Current hard-coded math:

```
after_strength = base * (1 + strength / 100)
if crit:  after_strength *= (1 + crit_damage / 100)
defense_factor = 1 − defense / (defense + 100)      # trueDamage → uses true_defense instead
final = max(0, after_strength × defense_factor)
```

Crit is rolled as `uniform[0, 100) < min(crit_chance, 100)`.

### Formula variable reference

| Variable | Stat source | Notes |
|---|---|---|
| `base` | `DamageContext.baseDamage()` | Weapon `damage` stat, or ability `carriedDamage` |
| `strength` | Attacker `STRENGTH` | 0 if attacker is not a player (mob stat-holder pending) |
| `crit_chance` | Attacker `CRIT_CHANCE` | Clamped to 100 |
| `crit_damage` | Attacker `CRIT_DAMAGE` | Applied only on a successful crit roll |
| `defense` | Victim `DEFENSE` | Ignored when `trueDamage = true` |
| `true_defense` | Victim `TRUE_DEFENSE` | Used instead of defense when `trueDamage = true` |

## Related

- [Status effects](status-effects.md)
- [Health display](health-display.md)
- [Vanilla suppression](vanilla-suppression.md)
- [Stats reference](../stats.md)
- [Holograms (damage indicators)](../addons/holograms.md)
