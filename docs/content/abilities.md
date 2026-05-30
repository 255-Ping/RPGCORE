# Abilities

> **Status:** In progress — Built-in effects library (damage, heal, beam, explode, particles, sound, delay, apply_status, mana_cost, cooldown), custom ability YAML loader, item right-click cast, **and full mob ability trigger system** (`~onTimer:N`, `~onHit`, `~onHurt`, `~onSpawn`, `~onDeath`) all working. Items and mobs share the same ability registry, so custom abilities defined in `abilities/*.yml` are usable from both.

The ability system has two layers:

1. **Built-in effects** — primitive operations like `damage`, `explode`, `beam`, `heal`. Registered by `rpg-core` and addons. Invoked from item / mob YAML via a DSL.
2. **Custom abilities** — admin-authored YAML chains of built-in effects, defined under `plugins/rpg-core/abilities/`. Custom abilities can be invoked the same way as built-ins (by their ID).

## The DSL

Items and mobs reference abilities like this:

```
effectName{key1=value1, key2=value2} otherEffect{key=value}
```

Multiple invocations separated by whitespace. Each builds an `AbilityEffect` that executes in sequence; later effects read from the same `AbilityContext` produced by earlier ones (carried damage, target, point, etc.).

For mob abilities, append a trigger: `effectName{...} ~onTimer:100`. See [mobs.md](mobs.md#ability-triggers).

## Custom ability YAML

```yaml
testability: #abilityid
  AbilitySequence:
  - beam{range=5.0,damage_multiplier=1.0,particle=crit}
  - explode{radius=3.0,damage_multiplier=0.5}
  Cooldown: 20                   # ticks; HARD FLOOR — can't be lowered by items using this ability
  ManaCost: 50                   # optional
  CombatXpMultiplier: 1.0        # how much combat XP the damage from this ability awards (0 to opt out)
  Name: "Beam Burst"             # optional display name
  Description:                   # optional, used in lore / GUIs
  - "&7Fires a beam, then explodes at the endpoint"
```

`AbilitySequence` is a list of effect invocations in DSL form. The example above fires a beam from the caster in look-direction, then explodes at the beam's endpoint.

## Built-in effects — full parameter reference

### `damage`
Deals damage to the context target using the full damage pipeline.

| Param | Type | Default | Description |
|---|---|---|---|
| `amount` | number | `carriedDamage` | Base damage; if omitted, uses context's carried damage |
| `type` | `physical` / `magic` / `true` | `physical` | `true` bypasses defense; `magic` bypasses strength scaling |

### `heal`
Heals HP. Target defaults to caster.

| Param | Type | Default | Description |
|---|---|---|---|
| `amount` | number | — | HP to restore |
| `target` | `caster` / `target` | `caster` | Who gets healed |

### `beam`
Fires a particle beam from the caster in their look direction. Sets context `point` to the endpoint and `target` to the first entity hit (if any).

| Param | Type | Default | Description |
|---|---|---|---|
| `range` | number | `5.0` | Max distance in blocks |
| `damage_multiplier` | number | `1.0` | Multiplies carried damage on hit |
| `particle` | Bukkit `Particle` name | `CRIT` | Particle drawn along the beam |
| `pierce` | int | `0` | How many entities the beam passes through |

### `explode`
AoE damage centered on context `point` (falls back to caster location if point not set).

| Param | Type | Default | Description |
|---|---|---|---|
| `radius` | number | `3.0` | Blast radius in blocks |
| `damage_multiplier` | number | `1.0` | Multiplies carried damage per target |
| `particle` | Bukkit `Particle` name | `EXPLOSION_NORMAL` | Burst particle |
| `falloff` | `linear` / `none` | `none` | Damage reduction toward the edge |

### `projectile`
Fires a moving hitbox. When it hits an entity, sets context `target` and continues the ability chain at that point.

| Param | Type | Default | Description |
|---|---|---|---|
| `speed` | number | `1.5` | Blocks per tick |
| `gravity` | number | `0.05` | Downward acceleration per tick (0 = straight) |
| `damage_multiplier` | number | `1.0` | Multiplies carried damage on impact |
| `lifetime` | int (ticks) | `60` | Despawn after this many ticks if no hit |
| `particle` | Bukkit `Particle` name | `FLAME` | Trail particle |

### `aoe`
Damages all entities within radius of the caster.

| Param | Type | Default | Description |
|---|---|---|---|
| `radius` | number | `4.0` | Radius in blocks |
| `damage_multiplier` | number | `1.0` | Multiplies carried damage |

### `teleport`
Moves the caster.

| Param | Type | Default | Description |
|---|---|---|---|
| `distance` | number | `8.0` | Distance in blocks |
| `mode` | `eyeline` / `random` / `back` | `eyeline` | `eyeline` = forward along look; `random` = random direction; `back` = behind caster |

### `particles`
Visual only — no damage.

| Param | Type | Default | Description |
|---|---|---|---|
| `type` | Bukkit `Particle` name | `CRIT` | Particle type |
| `count` | int | `10` | Number of particles |
| `spread` | number | `0.3` | XYZ scatter radius (override per-axis with `spread_x`, `spread_y`, `spread_z`) |

### `sound`
Plays a sound at the caster's location.

| Param | Type | Default | Description |
|---|---|---|---|
| `key` | Bukkit sound key | — | e.g., `entity.generic.explode` |
| `volume` | number | `1.0` | |
| `pitch` | number | `1.0` | |

### `delay`
Pauses the ability chain without blocking the server thread.

| Param | Type | Default | Description |
|---|---|---|---|
| `ticks` | int | `20` | Ticks to wait before next effect |

### `apply_status`
Applies a status effect. Target defaults to the context target (or caster if no target).

| Param | Type | Default | Description |
|---|---|---|---|
| `id` | effect id | — | Must exist in the status effect registry |
| `level` | int | `1` | Effect level |
| `duration` | int (ticks) | `200` | Duration |
| `target` | `caster` / `target` | `target` | Who receives the effect |

### `summon`
Spawns a mob at the caster's location. Despawns after `lifetime` ticks if positive.

| Param | Type | Default | Description |
|---|---|---|---|
| `mob` | mob id | — | Must exist in mob registry |
| `count` | int | `1` | How many to spawn |
| `lifetime` | int (ticks) | `600` | 0 = permanent |

### `mana_cost`
Deducts mana. Aborts the entire ability chain if insufficient mana.

| Param | Type | Default | Description |
|---|---|---|---|
| `amount` | number | — | Mana to consume |

### `cooldown`
Starts a cooldown keyed to the ability ID. **Soft** — reducible by `COOLDOWN_REDUCTION`; cannot go below the ability YAML's hard-floor `Cooldown:`.

| Param | Type | Default | Description |
|---|---|---|---|
| `ticks` | int | — | Cooldown duration |

More effects added by skill addons (e.g., `fishing_lure`, `mining_charge`).

## Pipeline & context

An `AbilityContext` carries:

- `caster` — the entity casting (player or mob)
- `target` — current entity target (may be set/changed by effects)
- `point` — current location (may be set/changed by effects)
- `carriedDamage` — current damage value being computed
- `pierceRemaining` — for projectile chains
- `bag` — open `Map<String,Object>` for cross-effect state

Effects return `CompletableFuture<AbilityContext>`. The pipeline chains effects with `thenCompose`, so an effect can defer (e.g., `delay`, `projectile` waiting for impact).

## Cooldowns

Two cooldown sources:

1. **Item-declared cooldown** — set on `Abilities:` in item YAML, soft (reducible by `cooldown_reduction` stat).
2. **Custom-ability declared cooldown** (`Cooldown:` in ability YAML) — **hard floor**, can't be reduced below this value.

Cooldowns are tracked by core's `CooldownService`, keyed per-(player, ability ID).

## Triggers

- **Item-equipped triggers:** abilities on items fire when the holder uses the item (right-click for swords/wands, fire arrows for bows, on-hit for damage callbacks, on-equip for armor). Each ItemType has documented default triggers; abilities can override via `Trigger:` field.
- **Mob triggers:** see [mobs.md](mobs.md#ability-triggers).

## Identification

Custom abilities are referenced by ID anywhere an ability is expected. An item can reference a custom ability by its ID:

```yaml
flame_sword:
  Type: SWORD
  Abilities:
  - testability                  # by ID — resolved to the YAML-defined sequence
  # ...or inline:
  - beam{range=5, damage_multiplier=1.0}
```

## Related

- [Items](items.md)
- [Mobs](mobs.md)
- [Status effects](../core/status-effects.md)
- [Stats reference](../stats.md)
