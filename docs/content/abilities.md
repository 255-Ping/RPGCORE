![banner-abilities](../assets/banners/banner-abilities.png)

# Abilities

> **Status:** Working — Built-in effects library, custom ability YAML, full player trigger system (active click, passive proc, ticking passive), armor set ability bonuses, and mob ability triggers all implemented. Items and mobs share the same ability registry.

## Design intent

A sequence model is used instead of a monolithic "ability" type because **composability** lets you build complex behaviors from simple primitives without writing Java. A fireball is `projectile{} → explode{}`; a poison lance is `beam{} → apply_status{id=poison}`; a charge-up burst is `particles{} → delay{} → explode{}`. Each primitive does one thing and passes a shared context forward.

The ability system has two layers:

1. **Built-in effects** — primitive operations like `damage`, `explode`, `beam`, `heal`. Registered by `rpg-core` and addons. Invoked from item / mob YAML via the DSL.
2. **Custom abilities** — admin-authored YAML chains of built-in effects, defined under `plugins/rpg-core/abilities/`. Custom abilities can be invoked by their ID anywhere a built-in can.

---

## Triggers

### Player item triggers

Every entry in an item's `Abilities:` list can be bound to a specific **trigger** using the `~trigger ` prefix. Lines without a prefix default to `right_click` (backwards compatible with pre-trigger YAML).

| Trigger | Prefix | When it fires |
|---|---|---|
| `right_click` | _(default, no prefix)_ | Player right-clicks (air or block) |
| `left_click` | `~left_click ` | Player left-clicks |
| `shift_right_click` | `~shift_right_click ` | Player sneaks + right-clicks |
| `shift_left_click` | `~shift_left_click ` | Player sneaks + left-clicks |
| `on_hit` | `~on_hit ` | Player deals melee or projectile damage |
| `on_hurt` | `~on_hurt ` | Player receives damage (any source) |
| `on_jump` | `~on_jump ` | Player jumps |
| `passive` | `~passive ` | Ticking — fires every `abilities.passive-interval-ticks` while item is held/equipped |

**Active vs passive:**
- **Active** triggers (`right_click`, `left_click`, `shift_*`) require the player to act and typically use `mana_cost{}`.  
- **Passive / proc** triggers (`on_hit`, `on_hurt`, `on_jump`, `passive`) fire automatically. **No mana cost is applied unless you add `mana_cost{}` explicitly.**

```yaml
phantom_blade:
  Type: SWORD
  Stats:
    damage: 45
    intelligence: 40
    max_mana: 60
  Abilities:
  - "mana_cost{amount=25} beam{range=12.0}"            # right_click (default)
  - "~shift_right_click mana_cost{amount=60} explode{radius=5.0}"
  - "~on_hit heal{amount=5,target=caster}"             # free lifesteal on every hit
  - "~passive particles{type=FLAME,count=2}"           # visual ticker while held
```

Passive triggers scan all equipped items (armor + main hand) and active set bonuses, so a chestplate with `~on_hurt` fires for every hit the player takes regardless of what weapon they're holding.

### Mob triggers

Mobs use the `~trigger` suffix on their `Abilities:` entries (appended after the effect sequence):

```yaml
testmob:
  Abilities:
  - "explode{radius=3.0} ~onTimer:100"   # every 5 seconds
  - "beam{range=10.0} ~onHit"            # when mob attacks
  - "aoe{radius=4.0} ~onDeath"           # on death burst
```

| Mob trigger | When it fires |
|---|---|
| `~onTimer:N` | Every N ticks while the mob is alive |
| `~onHit` | When the mob deals damage |
| `~onHurt` | When the mob takes damage |
| `~onSpawn` | Once on spawn |
| `~onDeath` | Once on death |

See [Mobs → Ability triggers](mobs.md#ability-triggers) for full mob documentation.

---

## The DSL

Effect invocations use this format:

```
effectName{key1=value1, key2=value2} nextEffect{key=value}
```

Multiple invocations on one line are chained in sequence. Each builds an `AbilityEffect` that executes in order; later effects read the same `AbilityContext` produced by earlier ones (carried damage, target, point, etc.).

Prefix the whole line with `~trigger ` to route it to a non-default trigger:

```yaml
Abilities:
- "beam{range=5.0} damage{}"                      # right_click chain
- "~shift_right_click mana_cost{amount=100} aoe{radius=6.0}"
- "~on_hit particles{type=CRIT,count=5}"
```

---

## Custom ability YAML

Define reusable ability chains in `plugins/rpg-core/abilities/`:

```yaml
testability:
  Name: "Beam Burst"
  Description:
  - "&7Fires a beam, then detonates at the endpoint"
  AbilitySequence:
  - mana_cost{amount=50}
  - beam{range=5.0, damage_multiplier=1.0, particle=CRIT}
  - explode{radius=3.0, damage_multiplier=0.5}
  Cooldown: 20          # hard-floor ticks — cannot be reduced by cooldown_reduction stat
```

> **Mana cost goes in the sequence, not as a YAML key.** `- mana_cost{amount=N}` as the first effect gates the ability on mana. A top-level `ManaCost:` field is not supported.

Custom abilities are referenced by ID on items and mobs:

```yaml
voidblade:
  Type: SWORD
  Abilities:
  - testability          # calls the ability by ID on right-click
  - "~shift_right_click another_ability"
```

---

## Built-in effects — quick reference

| Effect | Purpose |
|---|---|
| `damage` | Deals damage to context target; detonates [marks](#mark) |
| `heal` | Restores HP |
| `drain` | Deals damage and heals caster for a fraction (vampiric) |
| `beam` | Ray-cast to first entity; sets target + point |
| `explode` / `aoe` | AoE damage at point / caster location |
| `knockback` | Push/pull a target or send them upward |
| `launch` | Vertical (+ optional forward) velocity on caster or target |
| `blink` | Teleport caster forward up to N blocks |
| `chain` | Bounce damage to N nearest surrounding entities |
| `zone` | Persistent area that deals damage/status every interval |
| `shield` | Damage-absorbing buffer; intercepts all damage sources |
| `mark` | Tag a target for a bonus-damage detonate on next `damage{}` |
| `freeze` | Apply extreme Slowness for a duration |
| `restore_mana` | Restore mana to caster or a player target |
| `apply_status` | Applies a custom status effect |
| `particles` | Visual burst at point |
| `sound` | Plays a sound at caster |
| `delay` | Pauses chain N ticks without blocking server |
| `mana_cost` | Deducts mana; aborts chain if insufficient |
| `cooldown` | Starts a soft cooldown for this ability |

Full parameter tables: **[Effects Reference →](ability-effects.md)**

---

## Armor set abilities

Abilities can also be granted by **armor sets**. When a player wears enough pieces of a set, the set's passive and proc bindings become active. These follow the same trigger system, with one restriction: **only passive/proc triggers are valid on set bonuses** (`on_hit`, `on_hurt`, `on_jump`, `passive`). Active click triggers are silently ignored.

Set bonus abilities fire once per event regardless of how many pieces of the set are worn — wearing a 4/4 set with an `on_hit` bonus fires the ability once per hit, not four times.

See **[Armor Sets →](../core/armor-sets.md)** for YAML schema and examples.

---

## AbilityContext

Every ability cast creates one `AbilityContext` that flows through the entire effect chain. Effects read from it and mutate it; later effects see every change made by earlier ones.

| Field | Type | Description |
|---|---|---|
| `caster` | `LivingEntity` | The casting entity. Never null, never changes. |
| `target` | `LivingEntity` | Current entity target. **Starts null.** Set by `beam`/`projectile` on hit. |
| `point` | `Location` | Current world position. **Starts null.** Set by `beam`/`projectile`/`teleport`. |
| `carriedDamage` | `double` | Running damage. Initialized from item `damage` stat. Consumed by `damage{}` when no `amount` param. |
| `pierceRemaining` | `int` | Entities the beam/projectile can still pass through. |
| `bag` | `Map<String,Object>` | Open key-value store for addon-defined cross-effect state. |

Context flows through `delay` and `projectile` suspension — `target`, `point`, and `carriedDamage` are still set when the chain resumes after a wait.

---

## Cooldowns

Two sources:

1. **Item cooldown** (`ItemCooldown: N` on the item YAML) — ticks between any use of that item. Per-item, not per-ability. Reducible by the `cooldown_reduction` stat.
2. **Ability cooldown** (`cooldown{ticks=N}` in the sequence, or `Cooldown: N` in ability YAML) — per-ability keyed cooldown. Hard-floor set by `Cooldown:` in ability YAML; `cooldown{}` effect provides the soft default, reducible by `cooldown_reduction`.

Cooldowns are tracked by `CooldownService`, keyed per `(player, abilityId)`.

---

## Lore rendering

Items show ability bindings in lore automatically. Active bindings display as:

```
§5Ability: §dBeam Burst §8(Right-click)
  §7Fires a beam, then detonates at the endpoint
```

Passive/proc bindings display as:

```
§2Passive: §aLifesteal §8(On Hit)
```

Pipeline-only effects (`mana_cost`, `cooldown`, `particles`, `sound`, `delay`) are hidden from lore unless they have a `Description:` in their ability YAML.

---

## Related

- [Items](items.md)
- [Mobs](mobs.md)
- [Armor Sets](../core/armor-sets.md)
- [Effects Reference](ability-effects.md)
- [Status effects](../core/status-effects.md)
- [Stats reference](../stats.md)
