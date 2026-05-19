# Abilities

> **Status:** In progress — Built-in effects library (damage, heal, beam, explode, particles, sound, delay, apply_status, mana_cost, cooldown), custom ability YAML loader, and item right-click cast all working. Item with `Abilities: [<id>]` fires on right-click; the chain runs through `AbilityPipeline.cast`. Custom abilities (`abilities/*.yml`) load as composite effects with hard-floor cooldowns. Mob ability triggers (`~onTimer` etc.) parse but don't execute yet — a future polish slice ties them to runtime tasks and damage events.

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

## Built-in effects (v1 target list)

| Effect | Purpose |
|---|---|
| `damage{amount=, type=physical/magic/true}` | Deals damage to context target |
| `heal{amount=}` | Heal caster or target |
| `beam{range=, damage_multiplier=, particle=}` | Particle beam from caster in look direction; sets `point` to endpoint, sets `target` to first entity hit |
| `explode{radius=, damage_multiplier=, particle=}` | AoE damage centered on `point` (or caster if unset) |
| `projectile{speed=, gravity=, damage_multiplier=, lifetime=}` | Fires a moving point; `onHit` continues the chain |
| `aoe{radius=, damage_multiplier=}` | Damage to all entities in radius around caster |
| `teleport{distance=, mode=eyeline/random/back}` | Move caster |
| `particles{type=, count=, offset=}` | Visual only |
| `sound{key=, volume=, pitch=}` | Audio only |
| `delay{ticks=}` | Pause before next effect |
| `apply_status{id=, level=, duration=, target=caster/target}` | Apply a status effect |
| `summon{mob=, count=, lifetime=}` | Spawn a mob (with timer to despawn) |
| `mana_cost{amount=}` | Consume mana; abort if insufficient |
| `cooldown{ticks=}` | Block re-cast until elapsed; soft (overridable by hard-floor custom-ability cooldown) |

More effects added by skill addons (e.g., `mining_charge`, `fishing_lure`).

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
