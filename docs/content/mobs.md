# Mobs

> **Status:** Planned

Custom mobs are defined in YAML under `plugins/rpg-core/mobs/`. Any number of files; many mobs per file.

## Schema

```yaml
testmob: #mobid
  MinecraftMob: zombie           # base EntityType
  DisplayName: "&cTest Zombie"   # optional
  Health: 200
  Damage: 5
  Armor: 1
  Boss: false                    # if true, persists across restart with current HP (configurable)
  Immunities: []                 # damage source IDs immune to (e.g., fire, magic, true)
  Stats:                         # optional bonus stat block aggregated like a StatHolder
    crit_chance: 10
    crit_damage: 50
  Equipment:
  - HELMET leather_helmet
  - CHEST leather_tunic
  - LEGS leather_leggings
  - BOOTS leather_boots
  - HAND stone_sword
  Abilities:
  - explode{radius=5,damage_multiplier=0.5} ~onTimer:100  # ability DSL + trigger suffix
  - poison_strike{} ~onHit
  AI:
    profile: aggressive          # aggressive | passive | defensive | ranged_kiter | stationary | boss | swarming | pack-hunter | flying
    aggression-range: 16
    attack-range: 2
    target-priority: nearest     # nearest | lowest-health | highest-threat | random
    leash-range: 32
    leash-action: return         # return | despawn | teleport
    retreat-at-health-percent: 0
    move-speed-multiplier: 1.0
    flees-from: []
    immune-to-knockback: false
  LootTable:                     # inline; alternatively `LootTable: <id>` references loot-tables/
    attribution: weighted-by-damage   # last-hit | top-damager | split-equal | weighted-by-damage
    roll-mode: per-player        # per-player | shared
    coin-drop: { min: 5, max: 20 }
    rolls:
    - { item: red_gem, chance: 5.0, min: 1, max: 1, magic-find-affected: true }
    - { item: zombie_meat, chance: 50.0, min: 1, max: 3 }
    guaranteed:
    - { item: experience_token, min: 1, max: 1 }
  CustomHeadTexture: ""          # optional player-head texture for PLAYER-skinned mobs
```

## Equipment syntax

Each line: `<SLOT> <itemId>`. Slot ∈ `HELMET, CHEST, LEGS, BOOTS, HAND, OFFHAND`. `itemId` resolves via item registry first, vanilla material second.

## Ability triggers

Mob abilities use the same ability DSL as items, with a trailing `~triggerName[:arg]`:

| Trigger | Argument | Fires when |
|---|---|---|
| `~onTimer:<ticks>` | tick interval | Every N ticks while alive |
| `~onHit` | (none) | Mob hits an entity |
| `~onHurt` | (none) | Mob takes damage |
| `~onSpawn` | (none) | Mob spawns |
| `~onDeath` | (none) | Mob dies |
| `~onTargetAcquired` | (none) | Mob picks up a target |

## AI profiles

| Profile | Behavior |
|---|---|
| `aggressive` | Pursues nearest target within aggression range |
| `passive` | Never attacks; flees if hurt |
| `defensive` | Doesn't pursue; counter-attacks if hit |
| `ranged_kiter` | Keeps distance, fires abilities; flees if approached |
| `stationary` | Doesn't move; attacks anything in range |
| `boss` | Use abilities-as-AI (phases driven by HP thresholds — config future) |
| `swarming` | Pursues but doesn't claim a target — multiple swarmers can stack on one player |
| `pack-hunter` | Coordinates with others of the same mob ID (radius config) — they aggro together |
| `flying` | Uses flying movement (no pathfinding to ground) |

Vanilla pathfinding is kept; AI profile only overrides target selection, attack range, leash behavior.

## Spawning

Mobs aren't spawned directly via this YAML. They're spawned by:

- **Admin spawners** ([spawning.md](spawning.md))
- **Natural spawning rules** ([spawning.md](spawning.md))
- **Admin command** `/rpg mob spawn <id>` (perm `rpg.core.mob.spawn`)
- **Custom abilities** that spawn mobs (effect type TBD)

Vanilla mob spawning is cancelled when `vanilla-suppression.mob-spawning: true` in core config.

## Identification

When a custom mob spawns, its ID is written to the entity's PDC. `MobRegistry.from(LivingEntity)` recovers the definition. Vanilla mobs return empty.

## Related

- [Spawning](spawning.md)
- [Loot tables](loot-tables.md)
- [Abilities](abilities.md)
- [Vanilla suppression](../core/vanilla-suppression.md)
