# Built-in Ability Effects — Reference

> **Status:** Working — all effects listed here are implemented. Most built-in effects shipped in `rpg-core 1.7.0`; `spawn_mob` added in `1.8.0`. Current core version: `1.10.1`.

Full parameter reference for every built-in effect. All effects accept their parameters via the DSL: `effectName{key=value, key2=value2}`.

---

## damage

Deals damage to `ctx.target`. Applies any pending [mark](#mark) multiplier automatically.

| Param | Default | Description |
|---|---|---|
| `amount` | `0` | Flat damage (stacks with carriedDamage) |
| `damage_multiplier` | `1.0` | Multiplier applied to `carriedDamage` |
| `type` | `physical` | `physical` or `true` (true damage bypasses defense) |

```yaml
- "beam{range=10} damage{amount=20, damage_multiplier=1.5}"
```

---

## heal

Restores HP to the caster or target.

| Param | Default | Description |
|---|---|---|
| `amount` | `0` | HP to restore |
| `target` | `caster` | `caster` or `target` |

---

## drain

Deals damage to `ctx.target` and heals the caster for a fraction of that damage. Unlike chaining `damage{} heal{}`, `drain` ties the heal amount directly to the damage number, making lifesteal feel consistent.

| Param | Default | Description |
|---|---|---|
| `amount` | `10.0` | Flat drain damage |
| `damage_multiplier` | `0.0` | Additional multiplier on `carriedDamage` (stacks with `amount`) |
| `leech` | `1.0` | Fraction of damage converted to healing (0.5 = 50%) |

```yaml
- "~on_hit drain{amount=8, leech=0.5}"
- "beam{range=10} drain{amount=0, damage_multiplier=1.5, leech=1.0}"
```

---

## beam

Ray-casts from the caster's eye in the look direction. Sets `ctx.target` on hit and `ctx.point` to the endpoint. Does not deal damage — follow with `damage{}` or `explode{}`.

| Param | Default | Description |
|---|---|---|
| `range` | `5` | Max distance in blocks |
| `damage_multiplier` | `1.0` | Scales `carriedDamage` on hit |
| `particle` | `CRIT` | Particle type for the beam trail |

---

## explode / aoe

AoE damage centered at `ctx.point` (or caster location). Both names are identical aliases.

| Param | Default | Description |
|---|---|---|
| `radius` | `3` | Blast radius in blocks |
| `damage_multiplier` | `1.0` | Multiplier on `carriedDamage` |
| `damage` | `0` | Additional flat damage |
| `particle` | `EXPLOSION_EMITTER` | Particle type for the blast |

---

## knockback

Pushes a target entity away from (or toward) the caster, or straight up.

| Param | Default | Description |
|---|---|---|
| `force` | `1.0` | Horizontal velocity magnitude (blocks/tick) |
| `up` | `0.2` | Upward Y component added to the push |
| `target` | `target` | `target` or `caster` |
| `direction` | `away` | `away` (from caster), `toward` (to caster), or `up` (pure vertical) |

```yaml
- "~on_hit knockback{force=1.2, up=0.3}"
- "~on_hurt knockback{force=0.8, direction=away}"
```

---

## launch

Launches an entity upward, with an optional horizontal forward boost.

| Param | Default | Description |
|---|---|---|
| `force` | `1.5` | Upward Y velocity (1.5 ≈ very high jump) |
| `horizontal` | `0.0` | Forward velocity in caster's look direction |
| `target` | `caster` | `caster` or `target` |

```yaml
- "mana_cost{amount=30} launch{force=2.0, horizontal=1.0}"
- "~on_hurt launch{force=1.0, target=target}"
```

---

## blink

Teleports the caster forward in their look direction, stopping before the first solid block. Sets `ctx.point` to the landing location so following effects act at the destination.

| Param | Default | Description |
|---|---|---|
| `range` | `12.0` | Max distance in blocks |
| `safe` | `true` | Snap to a safe standing spot; `false` = raw endpoint |
| `particles` | `true` | PORTAL particle trail along blink path |

```yaml
- "~shift_right_click mana_cost{amount=40} blink{range=16.0} explode{radius=3.0, damage=20}"
```

---

## chain

Bounces `carriedDamage * damage_multiplier` from `ctx.target` (or `ctx.point`) to the N nearest surrounding entities within the given radius. Draws enchantment-particle arcs between hops.

Chain does not change `ctx.target` — it applies independent damage to each found entity.

| Param | Default | Description |
|---|---|---|
| `count` | `3` | Max bounce targets |
| `range` | `8.0` | Search radius from the origin |
| `damage_multiplier` | `0.7` | Fraction of `carriedDamage` dealt per bounce |

```yaml
- "beam{range=16} damage{} chain{count=4, range=8, damage_multiplier=0.6}"
```

---

## zone

Creates a persistent ground zone that damages and/or applies status effects at regular intervals. The zone expires after `duration` ticks. The zone outline redraws itself every second with configurable particles.

By default, zones drop at the caster's feet. Use `use_point=true` to place the zone at `ctx.point` (e.g. after a `beam{}`).

Server-wide cap: `abilities.zone.max-active` in `config.yml` (default 50). Zones for disconnected players are removed immediately.

| Param | Default | Description |
|---|---|---|
| `radius` | `4.0` | Zone radius in blocks |
| `duration` | `100` | Lifetime in ticks |
| `interval` | `20` | Ticks between damage/effect pulses |
| `damage` | `5.0` | RPG HP per pulse (0 = no damage) |
| `status` | _(empty)_ | Status effect ID applied each pulse |
| `status_level` | `1` | Level of the applied status effect |
| `status_duration` | `60` | Ticks the applied status lasts |
| `particle` | `FLAME` | Particle enum name for outline ring and pulse burst |
| `use_point` | `false` | `true` = place at `ctx.point`; `false` = caster's feet |

```yaml
# Fire zone at caster's feet
- "zone{radius=4, duration=200, interval=20, damage=10, particle=FLAME}"

# Poison cloud at beam endpoint
- "beam{range=20} zone{radius=5, damage=5, status=poison, status_duration=100, particle=SPORE_BLOSSOM, use_point=true}"
```

---

## shield

Grants the caster (or target) a damage-absorbing shield that intercepts all damage sources at the `HealthService` level. Multiple casts stack additively and expire independently.

| Param | Default | Description |
|---|---|---|
| `amount` | `50` | HP the shield can absorb |
| `duration` | `100` | Ticks before it expires even if not depleted |
| `target` | `caster` | `caster` or `target` |

```yaml
- "~shift_right_click mana_cost{amount=60} shield{amount=200, duration=300}"
- "~passive shield{amount=10, duration=40}"   # small ticking shield refresh on armor
```

---

## mark

Tags `ctx.target` with a mark. The next `damage{}` hit against the marked entity applies a damage multiplier and (if `consume=true`) removes the mark.

Only `damage{}` detonates marks. `drain{}`, `chain{}`, and zone pulses do not.

| Param | Default | Description |
|---|---|---|
| `bonus` | `2.0` | Damage multiplier on the detonating `damage{}` hit |
| `duration` | `200` | Ticks before mark expires if not consumed |
| `consume` | `true` | `true` = one-shot; `false` = bonus applies to all `damage{}` hits in window |
| `target` | `target` | `target` or `caster` |

```yaml
# Two-step combo
- "mana_cost{amount=20} beam{range=12} mark{bonus=2.5, duration=300}"
- "~left_click mana_cost{amount=30} beam{range=12, damage_multiplier=1.8} damage{}"
```

---

## freeze

Applies extreme vanilla SLOWNESS to the target for the given duration. No custom status-effect YAML required. Accompanied by a snowflake particle burst.

| Param | Default | Description |
|---|---|---|
| `duration` | `60` | Ticks (60 = 3 seconds) |
| `amplifier` | `4` | Slowness amplifier (0 = Slowness I … 4 = Slowness V) |
| `target` | `target` | `target` or `caster` |

```yaml
- "beam{range=12} freeze{duration=80}"
- "~on_hurt freeze{duration=40, target=target}"
```

---

## restore_mana

Restores mana to a player. No-op on non-player entities.

| Param | Default | Description |
|---|---|---|
| `amount` | `25` | Mana to restore |
| `target` | `caster` | `caster` or `target` |

```yaml
- "~passive restore_mana{amount=5}"        # ticking mana regen on armor
- "restore_mana{amount=75}"               # right-click mana crystal
```

---

## apply_status

Applies a custom RPG status effect (defined in `status-effects/*.yml`).

| Param | Default | Description |
|---|---|---|
| `id` | _(required)_ | Status effect ID |
| `level` | `1` | Effect level |
| `duration` | `100` | Ticks |
| `target` | `target` | `target` or `caster` |

---

## mana_cost

Deducts mana from the caster. Chain aborts with an action bar message if mana is insufficient. **Always put this first** in active ability sequences.

| Param | Default | Description |
|---|---|---|
| `amount` | `0` | Mana to consume |

---

## cooldown

Records a per-ability cooldown, reducible by the `cooldown_reduction` stat.

| Param | Default | Description |
|---|---|---|
| `ticks` | `0` | Soft-floor cooldown duration |

---

## particles

Spawns a particle burst at `ctx.point` (or caster location).

| Param | Default | Description |
|---|---|---|
| `type` | `CRIT` | Particle enum name |
| `count` | `10` | Particle count |
| `spread` | `0.5` | XZ spread radius |
| `height` | `1.0` | Y offset from the spawn point |

---

## sound

Plays a sound at the caster's location.

| Param | Default | Description |
|---|---|---|
| `key` | _(required)_ | Bukkit sound key |
| `volume` | `1.0` | Volume |
| `pitch` | `1.0` | Pitch |

---

## delay

Pauses the ability chain for N ticks without blocking the server thread. Context (`target`, `point`, `carriedDamage`) is preserved across the delay.

| Param | Default | Description |
|---|---|---|
| `ticks` | `0` | Pause duration |

```yaml
- "particles{type=ENCHANT} delay{ticks=10} explode{radius=5}"
```

---

---

## spawn_mob

_Added in rpg-core 1.8.0._ Spawns one or more registered custom mobs at or near the caster, target, or beam point. The anchor is evaluated at cast time; if the chosen anchor is null the caster location is used as a fallback.

| Param | Default | Description |
|---|---|---|
| `id` | _(required)_ | Mob registry ID |
| `count` | `1` | Number of mobs to spawn |
| `at` | `caster` | Spawn anchor: `caster`, `target`, or `point` (ctx.point from a preceding `beam{}`) |
| `radius` | `0.0` | Random XZ spread radius around the anchor (0 = exact location) |
| `offset_y` | `0.0` | Y offset added to the anchor location |
| `owned` | `false` | If `true`: tags the mob with the caster's UUID, counts toward the per-caster cap (`abilities.spawn-mob.max-per-caster` in config, default 10), and despawns all owned mobs when the caster logs out |

```yaml
# Phase-2 trigger: spawn adds once when below 25% HP
- "if_health_below{percent=25} if_not_flag{name=phase2} set_flag{name=phase2} spawn_mob{id=golem_shard,count=2,radius=5.0} shield{amount=500} ~onTimer:20"

# On-death split: mob spawns two weaker versions of itself
- "spawn_mob{id=shadow_shard,count=2,at=caster,radius=2.0} ~onDeath"

# Summoner item: player spawns a temporary ally
- "mana_cost{amount=50} cooldown{ticks=400} spawn_mob{id=summoned_skeleton,count=1,at=caster,radius=2.0,owned=true} right_click"
```

---

## Target selection effects

_Added in rpg-core 1.7.0._ These effects set `ctx.target` before the effects that follow them. Place them at the start of a sequence (or after a trigger gate) to pick who the next `damage{}`, `heal{}`, `mark{}`, etc. acts on.

### nearest_enemy

Sets `ctx.target` to the nearest hostile entity within range.

| Param | Default | Description |
|---|---|---|
| `range` | `12.0` | Search radius in blocks |

```yaml
- "nearest_enemy{range=12} damage{amount=30}"
```

---

### farthest_enemy

Sets `ctx.target` to the farthest hostile entity within range (useful for long-range snipe abilities).

| Param | Default | Description |
|---|---|---|
| `range` | `12.0` | Search radius in blocks |

---

### nearest_ally

Sets `ctx.target` to the nearest friendly player within range. The optional `priority` controls what counts as "nearest" when multiple allies are present.

| Param | Default | Description |
|---|---|---|
| `range` | `12.0` | Search radius in blocks |
| `priority` | `nearest` | `nearest` (closest), `lowest_health` (ally with the least HP%), or `lowest_mana` (player ally with the least mana%) |

```yaml
- "nearest_ally{range=20, priority=lowest_health} heal{amount=50, target=target}"
```

---

### random_enemy

Sets `ctx.target` to a randomly chosen hostile entity within range.

| Param | Default | Description |
|---|---|---|
| `range` | `12.0` | Search radius in blocks |

---

### self

Sets `ctx.target` to the caster. Useful for making a sequence explicitly self-target after a previous effect may have changed the target.

_(No parameters.)_

---

## Conditional gates

_Added in rpg-core 1.7.0._ These effects abort the ability chain if their condition is not met. They are zero-side-effect tests — place them before costly effects to gate execution.

### if_health_below / if_health_above

Continues the chain only if the caster's HP (as a percent of max) is below or above the threshold.

| Param | Default | Description |
|---|---|---|
| `percent` | _(required)_ | HP threshold as a whole number (e.g. `50` = 50%) |

```yaml
# Execute only when below half HP
- "if_health_below{percent=50} heal{amount=80}"
```

---

### if_mana_below / if_mana_above

Same as `if_health_*` but tests the caster's current mana.

| Param | Default | Description |
|---|---|---|
| `percent` | _(required)_ | Mana threshold as a whole number |

---

### if_marked

Continues only if `ctx.target` currently has a mark applied (see [`mark{}`](#mark)).

_(No parameters.)_

```yaml
# Big finisher that only fires if the target is already marked
- "if_marked{} beam{range=12} damage{amount=80}"
```

---

### if_target_has_status

Continues only if `ctx.target` currently has the specified status effect active.

| Param | Default | Description |
|---|---|---|
| `id` | _(required)_ | Status effect ID to check |

```yaml
- "if_target_has_status{id=poison} damage{amount=20}"
```

---

### if_flag / if_not_flag

Tests a named boolean flag on the caster. `if_flag` continues if the flag is set; `if_not_flag` continues if it is **not** set (or was never set). Flags are stored in entity `PersistentDataContainer` under the key `rpg_flag_<name>`. For mobs they clear on death. For players they persist across sessions (PDC is saved with the player data).

| Param | Default | Description |
|---|---|---|
| `name` | _(required)_ | Flag name (any string key) |

```yaml
# Combo: first hit sets a flag, second hit checks and clears it for a bonus
- "~on_hit if_not_flag{name=combo_ready} set_flag{name=combo_ready}"
- "~on_hit if_flag{name=combo_ready} clear_flag{name=combo_ready} damage{amount=60}"
```

---

### set_flag / clear_flag

Sets or clears a named boolean flag on the caster. These are side-effecting and intended to be used alongside `if_flag` / `if_not_flag` to build stateful combo sequences.

| Param | Default | Description |
|---|---|---|
| `name` | _(required)_ | Flag name to set or clear |

_(See `if_flag` example above.)_

---

## Related

- [Abilities overview + trigger system](abilities.md)
- [Armor Sets](../core/armor-sets.md)
- [Status effects](../core/status-effects.md)
- [Stats reference](../stats.md)
