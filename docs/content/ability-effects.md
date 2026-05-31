# Built-In Effects Reference

Every `AbilityEffect` registered by `rpg-core` is documented here. For each effect:

- **Parameters** — DSL arguments in the `{...}` block
- **Reads / Writes** — what fields the effect reads from `AbilityContext` and what it leaves behind
- **Example** — a typical sequence fragment

Effects execute in order. Later effects in a chain see every mutation made by earlier ones. See [Abilities → AbilityContext](abilities.md#abilitycontext) for field definitions.

---

## `beam`

Traces a ray from the caster along their look direction. Marks the first entity hit as `target` and the ray endpoint as `point`.

| Param | Default | Description |
|---|---|---|
| `range` | `5.0` | Max distance in blocks |
| `damage_multiplier` | `1.0` | Scales `carriedDamage` on hit before writing it back |
| `particle` | `CRIT` | Particle drawn along the beam each tick |
| `pierce` | `0` | Extra entities to pass through after the first hit |

| | Field | Detail |
|---|---|---|
| **Reads** | `caster` | Ray origin + look direction |
| **Reads** | `carriedDamage` | Multiplied on hit; result stored back into `carriedDamage` |
| **Writes** | `target` | First entity the ray struck; `null` if nothing in range |
| **Writes** | `point` | Endpoint — the block-face it stopped at or the max-range location |

```yaml
# Snipe: long beam, pierce one extra target, then deal damage to the first hit
- beam{range=20.0, pierce=1}
- damage{}
```

---

## `projectile`

Launches a moving hitbox. The ability chain **suspends here** until the projectile hits an entity or expires — effects after `projectile` run at impact time, not at cast time.

| Param | Default | Description |
|---|---|---|
| `speed` | `1.5` | Blocks per tick |
| `gravity` | `0.05` | Downward acceleration per tick; `0.0` = perfectly straight |
| `damage_multiplier` | `1.0` | Scales `carriedDamage` on impact |
| `lifetime` | `60` | Ticks before auto-despawn if no hit |
| `particle` | `FLAME` | Trail particle |

| | Field | Detail |
|---|---|---|
| **Reads** | `caster` | Launch origin + direction |
| **Reads** | `carriedDamage` | Multiplied on impact |
| **Writes** | `target` | Entity hit (if any; `null` if it expired without a hit) |
| **Writes** | `point` | Impact location |

> If the projectile expires without a hit, `target` stays `null`. Any `damage{}` or `apply_status{}` after it will silently no-op or fall back to caster.

```yaml
# Fireball: straight shot, on hit deal damage then apply burn
- projectile{speed=2.0, gravity=0.0, particle=FLAME}
- damage{}
- apply_status{id=burn, duration=80}
```

---

## `explode`

AoE blast centered on `point`. Falls back to the caster's location if `point` is not set. Does **not** set `target` — if you need to act on individual hit entities, use `aoe` or chain from a prior targeting effect.

| Param | Default | Description |
|---|---|---|
| `radius` | `3.0` | Blast radius in blocks |
| `damage_multiplier` | `1.0` | Scales `carriedDamage` per entity hit |
| `particle` | `EXPLOSION_NORMAL` | Burst particle |
| `falloff` | `none` | `linear` = damage reduces from center to edge; `none` = flat |

| | Field | Detail |
|---|---|---|
| **Reads** | `point` | Blast center (falls back to `caster` location if null) |
| **Reads** | `carriedDamage` | Base damage before multiplier |
| **Writes** | _(nothing)_ | |

```yaml
# Rocket: projectile flies to target, then detonates with falloff
- projectile{speed=1.5, lifetime=100}
- explode{radius=5.0, falloff=linear, damage_multiplier=1.5}
```

---

## `aoe`

Damages all entities within radius of the **caster's current location**.

| Param | Default | Description |
|---|---|---|
| `radius` | `4.0` | Radius in blocks |
| `damage_multiplier` | `1.0` | Scales `carriedDamage` per entity hit |

| | Field | Detail |
|---|---|---|
| **Reads** | `caster` location | Center of the AoE hit-scan |
| **Reads** | `carriedDamage` | Base damage before multiplier |
| **Writes** | _(nothing)_ | |

```yaml
# Ground slam: sound cue, then hit everything nearby
- sound{key=entity.generic.explode, pitch=0.7}
- aoe{radius=5.0, damage_multiplier=1.2}
```

---

## `damage`

Deals damage to `target` through the full damage pipeline (strength scaling, crit, defense reduction, `PreDamageEvent`, `PostDamageEvent`).

| Param | Default | Description |
|---|---|---|
| `amount` | `carriedDamage` | Explicit base damage; omit to use the carried value |
| `type` | `physical` | `magic` skips strength scaling; `true` bypasses defense entirely |

| | Field | Detail |
|---|---|---|
| **Reads** | `target` | Recipient — must be set by a prior effect; no-ops silently if null |
| **Reads** | `carriedDamage` | Used as `amount` when no explicit value is given |
| **Writes** | _(nothing)_ | |

```yaml
# Beam snipe dealing true (unmitigated) damage
- beam{range=15.0}
- damage{type=true}
```

---

## `heal`

Restores HP. Targets the caster by default.

| Param | Default | Description |
|---|---|---|
| `amount` | _(required)_ | HP to restore |
| `target` | `caster` | `caster` or `target` (context target) |

| | Field | Detail |
|---|---|---|
| **Reads** | `caster` | Healed when `target=caster` (default) |
| **Reads** | `target` | Healed when `target=target`; no-ops silently if null |
| **Writes** | _(nothing)_ | |

```yaml
# Vampiric strike: beam hits an enemy, drains HP back to caster
- beam{range=8.0, damage_multiplier=1.2}
- damage{}
- heal{amount=15, target=caster}
```

---

## `apply_status`

Applies a registered status effect to an entity.

| Param | Default | Description |
|---|---|---|
| `id` | _(required)_ | Status effect ID — must exist in the registry |
| `level` | `1` | Effect level |
| `duration` | `200` | Duration in ticks |
| `target` | `target` | `target` (context target) or `caster` |

| | Field | Detail |
|---|---|---|
| **Reads** | `target` | Default recipient; falls back to `caster` if `target` is null |
| **Reads** | `caster` | Recipient when `target=caster` |
| **Writes** | _(nothing)_ | |

```yaml
# Cursed arrow: on hit, apply 5 seconds of poison level 2
- projectile{speed=3.0, gravity=0.1}
- apply_status{id=poison, level=2, duration=100}
```

---

## `teleport`

Moves the caster. Updates the caster's in-world position; effects after `teleport` use the new location.

| Param | Default | Description |
|---|---|---|
| `distance` | `8.0` | Distance in blocks |
| `mode` | `eyeline` | `eyeline` = forward along look; `random` = random XZ direction; `back` = behind caster |

| | Field | Detail |
|---|---|---|
| **Reads** | `caster` | Current position and look direction |
| **Writes** | Caster world position | The caster moves; `caster.getLocation()` reflects the new spot for later effects |

```yaml
# Blink: particles at origin, dash forward, particles at destination
- particles{type=PORTAL, count=20}
- teleport{distance=12.0, mode=eyeline}
- particles{type=PORTAL, count=20}
```

---

## `summon`

Spawns a custom mob from the mob registry at the caster's location.

| Param | Default | Description |
|---|---|---|
| `mob` | _(required)_ | Mob ID — must exist in the mob registry |
| `count` | `1` | Number of mobs to spawn |
| `lifetime` | `600` | Ticks until auto-despawn; `0` = permanent |

| | Field | Detail |
|---|---|---|
| **Reads** | `caster` location | Spawn point |
| **Writes** | _(nothing)_ | |

```yaml
# Mob ability: every 5 seconds summon 2 skeleton minions (live 30 seconds)
- summon{mob=skeleton_minion, count=2, lifetime=600}
```

---

## `particles`

Spawns a particle burst. Visual only — no damage or targeting side effects.

| Param | Default | Description |
|---|---|---|
| `type` | `CRIT` | Bukkit `Particle` name |
| `count` | `10` | Number of particles |
| `spread` | `0.3` | XYZ scatter radius |
| `spread_x` / `spread_y` / `spread_z` | `spread` | Per-axis override |

| | Field | Detail |
|---|---|---|
| **Reads** | `point` | Burst origin; falls back to `caster` location if null |
| **Writes** | _(nothing)_ | |

---

## `sound`

Plays a sound at the caster's current location. Volume controls attenuation radius.

| Param | Default | Description |
|---|---|---|
| `key` | _(required)_ | Bukkit sound key, e.g. `entity.generic.explode` |
| `volume` | `1.0` | Attenuation range |
| `pitch` | `1.0` | `0.5`–`2.0` |

| | Field | Detail |
|---|---|---|
| **Reads** | `caster` location | Where the sound is anchored |
| **Writes** | _(nothing)_ | |

---

## `delay`

Pauses the ability chain for N ticks on a scheduler thread without blocking the server. Context passes through unchanged — `target`, `point`, and `carriedDamage` are preserved across the wait.

| Param | Default | Description |
|---|---|---|
| `ticks` | `20` | Server ticks to wait (20 ticks = 1 second) |

| | Field | Detail |
|---|---|---|
| **Reads** | _(nothing)_ | |
| **Writes** | _(nothing)_ | |

```yaml
# Delayed mine: mark the spot with a sound, wait 2 seconds, then explode there
- sound{key=block.note_block.hat, pitch=1.5}
- particles{type=FLAME, count=5}
- delay{ticks=40}
- explode{radius=3.5}
```

---

## `mana_cost`

Deducts mana from the caster. If the caster does not have enough mana, the **entire chain stops** — no further effects (including those listed after `mana_cost`) run. Place this **first** to avoid running visual effects before hitting the mana gate.

| Param | Default | Description |
|---|---|---|
| `amount` | _(required)_ | Mana to deduct |

| | Field | Detail |
|---|---|---|
| **Reads** | `caster` mana | Compared to `amount` |
| **Writes** | `caster` mana | Deducted if sufficient; chain aborted if insufficient |

---

## `cooldown`

Starts a cooldown for this ability keyed to `(caster, abilityId)`. Soft cooldown — reducible by the `cooldown_reduction` stat, but cannot go below the ability YAML's hard-floor `Cooldown:` value.

| Param | Default | Description |
|---|---|---|
| `ticks` | _(required)_ | Soft cooldown duration |

| | Field | Detail |
|---|---|---|
| **Reads** | Ability ID (from invocation source) | Key used in `CooldownService` |
| **Writes** | `CooldownService` state | Starts the timer |

---

## Addon effects

Skill addons can register additional effects via `RpgServices.abilityRegistry().register(effect)`. Two are currently bundled:

| Effect | Addon | Description |
|---|---|---|
| `fishing_lure` | `rpg-fishing` | Applies a lure marker to the player's active bobber |
| `mining_charge` | `rpg-mining` | Triggers an area-break on the block the caster is looking at |

To write your own, implement `AbilityEffect` from `rpg-api` — see [Developer Guide → Extension Points](../development.md#extension-points).
