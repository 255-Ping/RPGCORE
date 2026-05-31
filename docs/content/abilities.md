# Abilities

> **Status:** In Progress — Built-in effects library (damage, heal, beam, explode, particles, sound, delay, apply_status, mana_cost, cooldown), custom ability YAML loader, item right-click cast, **and full mob ability trigger system** (`~onTimer:N`, `~onHit`, `~onHurt`, `~onSpawn`, `~onDeath`) all working. Items and mobs share the same ability registry, so custom abilities defined in `abilities/*.yml` are usable from both.

## Design intent

A sequence model is used instead of a monolithic "ability" type because **composability** lets you build complex behaviors from simple primitives without writing Java. A fireball is `projectile{} → explode{}`; a poison lance is `beam{} → apply_status{id=poison}`; a charge-up burst is `particles{} → delay{} → explode{}`. Each primitive does one thing and passes a shared context forward. Adding a new primitive (one Java class) unlocks a combinatorial space of new abilities without touching existing code.

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
  - mana_cost{amount=50}         # deduct mana first; chain aborts here if insufficient
  - beam{range=5.0,damage_multiplier=1.0,particle=CRIT}
  - explode{radius=3.0,damage_multiplier=0.5}
  Cooldown: 20                   # ticks; HARD FLOOR — can't be lowered by items using this ability
  Name: "Beam Burst"             # optional display name
  Description:                   # optional, used in lore / GUIs
  - "&7Fires a beam, then explodes at the endpoint"
```

> **Mana cost goes in the sequence, not as a YAML key.** Use `- mana_cost{amount=N}` as the first effect to gate the ability on mana. A top-level `ManaCost:` field is not read by the loader.

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

## AbilityContext

Every ability invocation creates one `AbilityContext` instance that flows through the entire effect chain. Effects read from it and mutate it; later effects see every change made by earlier ones. Context is never reset between effects — including across `delay` waits.

### Fields

| Field | Type | Mutable | Description |
|---|---|---|---|
| `caster` | `LivingEntity` | **No** | The casting entity (player or mob). Never null. Set at invocation; no effect can change it. |
| `target` | `LivingEntity` | Yes | Current entity target. **Starts null.** Set by `beam` or `projectile` on hit. `damage` silently no-ops if null; `apply_status` falls back to `caster` if null. |
| `point` | `Location` | Yes | Current world position. **Starts null.** Set by `beam` (endpoint), `projectile` (impact), and `teleport` (new caster position). `explode` and `particles` fall back to caster location if null. |
| `carriedDamage` | `double` | Yes | Running damage value. Initialized from the item's base `damage` stat. Scaled by `damage_multiplier` params on `beam`/`projectile`/`explode`. Consumed by `damage{}` when no explicit `amount` is given. |
| `pierceRemaining` | `int` | Yes | How many more entities the projectile/beam can pass through. Starts at 1; decremented on each hit. Increased by `pierce=` params. |
| `bag` | `Map<String,Object>` | Yes | Open key-value store for cross-effect state. No built-in effects use this — it is for addon-defined effects to share data. |

### How context flows

```
Sequence: projectile{} → damage{} → apply_status{id=poison}

 Before cast:
   caster        = <player>
   target        = null
   point         = null
   carriedDamage = 55.0   ← from item base damage

 After projectile hits <zombie>:
   target        = <zombie>       ← set by projectile
   point         = <impact loc>   ← set by projectile
   carriedDamage = 55.0           ← unchanged (no damage_multiplier)

 After damage{}:
   reads target=<zombie>, carriedDamage=55.0
   → 55 damage dealt to <zombie>

 After apply_status{id=poison}:
   reads target=<zombie>
   → poison applied to <zombie>
```

If the projectile expires without hitting anything, `target` stays null. `damage{}` silently no-ops and `apply_status{}` falls back to applying poison to the `caster` instead.

### Pipeline mechanics

Effects return `CompletableFuture<AbilityContext>`. The pipeline chains them with `thenCompose`, so deferred effects like `delay` and `projectile` suspend the chain without blocking the server thread. The same context instance is passed forward through any wait — `target`, `point`, and `carriedDamage` are all still there when the chain resumes.

### Quick reads/writes summary

| Effect | Reads | Writes |
|---|---|---|
| `beam` | `caster` location + look direction | `target` (first hit entity), `point` (endpoint) |
| `projectile` | `caster` location + look direction | `target`, `point` (on impact) |
| `explode` | `point` (fallback: caster location) | — |
| `aoe` | `caster` location | — |
| `damage` | `target`, `carriedDamage` (if no `amount` param) | — |
| `heal` | `caster` or `target` (see `target=` param) | — |
| `particles` | `point` (fallback: caster location) | — |
| `sound` | `caster` location | — |
| `apply_status` | `target` (fallback: `caster`) | — |
| `teleport` | `caster` | caster's world position |
| `summon` | `caster` location | — |
| `delay` | — | — (pauses chain; context unchanged) |
| `mana_cost` | `caster` mana | Deducts mana; aborts chain if insufficient |
| `cooldown` | ability ID | Starts cooldown in `CooldownService` |

For full parameter tables, null-handling details, and examples per effect see the **[Effects Reference](ability-effects.md)**.

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
