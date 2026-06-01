![banner-status-effects](../assets/banners/banner-status-effects.png)

# Status effects

> **Status:** Working — YAML-loaded effect content, stacking strategies, stat modifiers (flat + percent), `damage`/`heal` tick primitives, and `on-apply`/`on-expire` sound + particle hooks all active. Full ability-sequence tick actions (running an arbitrary `AbilitySequence` on each tick) are not yet wired — currently only `damage` and `heal` primitives are supported in the `tick` block. Default sample effects (`poison`, `regen`, `strength_boost`, `slow`) ship in `plugins/rpg-core/status-effects/example.yml`.

A fully custom (de)buff framework. Vanilla `PotionEffect`s on entities are cancelled (per [vanilla suppression](vanilla-suppression.md)) — only our `StatusEffect` system applies.

## Design intent

Status effects exist as a separate system from abilities because they model things abilities can't do cleanly: **duration**, **stacking rules** (refresh vs stack-power vs independent), and **persistent stat modifiers** that remain active across multiple damage events. An ability fires once and resolves; a status effect sits on the entity, modifies its stats, and ticks independently until it expires. Using abilities to replicate "burning for 5 seconds" would require a chain of delayed effects with shared state — this system handles all of that declaratively in YAML.

## What a status effect is

Each effect is defined in YAML (planned location: `plugins/rpg-core/status-effects/<file>.yml`, or shipped with addons like `rpg-alchemy`). An effect has:

- ID (`poison`, `strength`, `slow`, `regen`, `stun`, `vulnerability`, etc.)
- Display name + icon (CustomModelData)
- Stat modifiers (additive or percent, applied while active)
- Periodic tick action (e.g., "deal 1 true damage every 20 ticks")
- Stacking strategy
- Hidden flag (e.g., for boss invisibility)

## Schema

```yaml
poison:
  display: "&2Poison"
  category: debuff               # buff | debuff | neutral
  stacking: refresh              # refresh | stack-power | take-max | independent
  hidden: false
  stat-modifiers:                # while active
    defense:
      kind: percent
      value: -10
  tick:
    interval-ticks: 20
    action: damage               # damage | heal
    amount: 2
    source: poison               # shown in damage indicators
  on-apply:
    sound:
      key: entity.spider.hurt
      volume: 1.0
      pitch: 1.5
    particles:
      type: DRIPPING_WATER       # Bukkit Particle enum name (case-insensitive)
      count: 8
      spread: 0.3                # shorthand — all three axes; override per-axis with spread-x/y/z
  on-expire:
    particles:
      type: SMOKE_NORMAL
      count: 4
```

## Stacking strategies

| Strategy | Behavior |
|---|---|
| `refresh` | If reapplied with `new.level >= current.level`, replace duration; else ignore |
| `stack-power` | Sum levels; sum durations (capped) |
| `take-max` | Take the higher of `(currentLevel, newLevel)`; longer duration wins |
| `independent` | Multiple copies run in parallel with independent durations and levels (e.g., from different sources) |

## Application

- Item right-click (`CONSUMABLE` items)
- Ability effect (`apply_status{id=, level=, duration=, target=}`)
- Mob hit (mob's ability uses `apply_status`)
- Admin command (`/rpg status apply <effect> [player] [duration]`)
- Region flag (e.g., a "haste zone" applies a `haste` effect while inside)

## Lifecycle

1. **Apply** — `StatusEffectService.apply(target, effectId, level, durationTicks, sourceId)`. Stacking rules resolved.
2. **Tick** — on every interval tick, the effect runs its `tick.action` (an ability invocation in DSL form).
3. **Modify stats** — while active, modifiers are folded into the target's `StatHolder` aggregation.
4. **Expire** — when duration reaches 0 (or `StatusEffectService.clear` is called), `on-expire` runs and modifiers are removed.

## Built-in effect catalog

These effects ship in `plugins/rpg-core/status-effects/example.yml` on first run. They're ready to use in abilities, consumables, and region flags without any authoring.

| ID | Category | What it does |
|---|---|---|
| `poison` | debuff | Deals 2 true damage every 20 ticks; −10% defense while active |
| `regen` | buff | Heals 3 HP every 20 ticks |
| `strength_boost` | buff | +20 flat `strength` while active |
| `slow` | debuff | −20% `speed` while active |

Define your own in `plugins/rpg-core/status-effects/<file>.yml` following the schema below. Addons (e.g., `rpg-alchemy`) can ship their own files alongside.

## API surface

```java
StatusEffectService svc = RpgServices.statusEffects();
svc.apply(entity, "poison", 2, 200, "source-id-for-stacking");
svc.clear(entity, "poison");
List<ActiveEffect> active = svc.active(entity);
```

## Effects display

Players see their active effects via:

- `/effects` command — opens a GUI listing active effects with time remaining and icon
- Optional HUD line in scoreboard / action bar (configured in `rpg-hud`)
- Damage indicators show a small tag when a status-effect tick deals damage (configurable)

`hidden: true` effects don't appear in the GUI or HUD (used for boss invisibility, internal flags).

## Related

- [Damage pipeline](damage.md)
- [Abilities](../content/abilities.md)
- [Items (CONSUMABLE type)](../content/items.md)
- [rpg-alchemy](../addons/skills.md#alchemy)
- [HUD addon](../addons/hud.md)
