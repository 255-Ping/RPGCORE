# Common Patterns

Named recipes for common RPG mechanics built from the RPGCORE ability/stat primitives. One-line setup — for fuller examples see the [Cookbook](cookbook.md).

---

## Fireball

Projectile that explodes on impact.

```
projectile{speed=1.5, gravity=0.05} explode{radius=3.0, damage_multiplier=1.2}
particles{type=EXPLOSION_LARGE, count=1}
```

---

## Poison on hit (item or mob)

Apply a status effect whenever the attack lands.

**Item ability trigger:**
```
apply_status{id=poison, duration=100, level=1} ~onHit
```

**Mob ability trigger:**
```
apply_status{id=poison, duration=100, level=1} ~onHit
```

The same DSL works on both. `duration` is in ticks (100 = 5 seconds). Requires a `poison` effect defined in `plugins/rpg-core/status-effects/`.

---

## Life steal

Deal damage, then heal the caster for a percentage.

```
damage{amount=50} heal{target=caster, amount=15}
```

Or via stat: add `lifesteal: 10` on the item (10 = 10% of damage dealt returned as HP). The stat approach applies to all attacks automatically; the ability approach applies only when the ability is cast.

---

## AoE slam

Ground pound that hits everything in a radius with particles.

```yaml
ground_slam:
  AbilitySequence:
    - aoe{radius=5.0, damage_multiplier=1.0}
    - particles{type=EXPLOSION_NORMAL, count=12, spread=1.0}
    - sound{sound=ENTITY_GENERIC_EXPLODE, volume=0.8, pitch=0.8}
  Cooldown: 60
  ManaCost: 30
```

---

## Blink / dash

Teleport the caster forward along their eye line.

```yaml
phase_step:
  AbilitySequence:
    - teleport{distance=8, mode=eyeline}
    - particles{type=PORTAL, count=20, spread=0.5}
  Cooldown: 200
  ManaCost: 20
```

---

## Beam (melee range pierce)

Fire a short beam that hits all entities in the line.

```yaml
lance_strike:
  AbilitySequence:
    - beam{range=6.0, damage_multiplier=1.5, particle=crit}
  Cooldown: 40
```

---

## Summon minions

Spawn supporting mobs around the caster.

```yaml
call_wolves:
  AbilitySequence:
    - summon{mob=tamed_wolf, count=3, lifetime=600}
    - particles{type=HEART, count=6, spread=1.0}
  Cooldown: 400
  ManaCost: 50
```

Requires `tamed_wolf` defined in `plugins/rpg-core/mobs/`.

---

## Delayed combo

Two effects with a tick gap between them (e.g., charge-up then burst).

```yaml
charged_strike:
  AbilitySequence:
    - particles{type=FLAME, count=20, spread=0.3}
    - delay{ticks=20}                              # 1 second pause
    - explode{radius=4.0, damage_multiplier=2.0}
  Cooldown: 120
```

---

## Boss phase change (planned — not yet wired)

Trigger a new ability sequence when the mob drops below a health threshold. The `~onHurt` trigger fires on every damage event; filtering by health percentage requires the `health_threshold` condition, which is on the ability effects roadmap. Design for it now but expect a code update before it works.

```yaml
# Intended design (not yet functional):
phase_two_rage:
  AbilitySequence:
    - aoe{radius=8.0, damage_multiplier=1.5}
    - apply_status{id=enraged, duration=600, level=1, target=caster}
```

---

## Per-pattern stat reference

| Pattern | Key stats / effects used | Notes |
|---|---|---|
| Fireball | `projectile`, `explode` | Adjust `radius` for splash vs pinpoint |
| Poison on hit | `apply_status` + status YAML | Define the status effect first |
| Life steal | `heal{target=caster}` or `lifesteal` stat | Stat scales with all damage; ability only on cast |
| AoE slam | `aoe`, `particles`, `sound` | Stack `damage_multiplier` carefully — hits everything |
| Blink | `teleport{mode=eyeline}` | `distance` in blocks; clips to solid block if obstructed |
| Beam | `beam{range=N}` | `particle` param controls trail visual |
| Summon | `summon{mob=id, lifetime=N}` | Mob must exist in registry |
| Delayed combo | `delay{ticks=N}` | Subsequent effects fire on the server thread after N ticks |

> **See also:** [Abilities reference](abilities.md), [Status effects](../core/status-effects.md), [Cookbook](cookbook.md)
