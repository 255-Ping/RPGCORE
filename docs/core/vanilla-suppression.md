# Vanilla suppression

> **Status:** Wired for `mob-spawning`, `hunger`, `player-regen`, `xp-orbs`, `raids`, `enchanting-table`, `anvil`, `brewing-stand`, `crafting`, `fishing`. Remaining flags accepted in config but not yet enforced.

The project's design goal is **total vanilla replacement**. Mana, defense, damage, crits, status effects, XP, levels, brewing, food saturation, regeneration, mob spawning, mob loot, mob AI, fishing, enchanting, anvils, smelting, crafting recipes, villager trading, raids, pillager patrols, beacons — all custom-coded.

The vanilla **player health bar** is the only thing we let MC drive directly, and even then only as a *display* of our HEALTH stat. See [health-display](health-display.md).

## The master config

Lives in `plugins/rpg-core/config.yml` under `vanilla-suppression`. Defaults reflect the project intent; dial back per-server as needed.

```yaml
vanilla-suppression:
  mob-spawning: true             # cancel all natural mob spawns; admin spawners + natural-spawning rules are the only source
  mob-loot: true                 # cancel vanilla drops; loot comes from mob YAML loot tables
  mob-ai: true                   # disable vanilla AI on tagged mobs; our AI profiles drive behavior
  damage: true                   # cancel vanilla damage, route through our damage pipeline
  player-regen: true             # cancel vanilla HP regen; HEALTH_REGEN drives ours
  hunger: true                   # cancel vanilla hunger drain; consumables apply custom buffs
  xp-orbs: true                  # cancel vanilla XP orb pickup
  enchanting-table: true         # cancel vanilla enchant table
  anvil: true                    # cancel vanilla anvil
  brewing-stand: true            # cancel vanilla brewing
  potions: true                  # cancel vanilla potion effects on entities (we apply our StatusEffects)
  smelting: true                 # cancel vanilla furnace recipes; admin defines smelting in YAML
  crafting: true                 # cancel vanilla crafting recipes
  fishing: true                  # cancel vanilla fishing loot
  villager-trading: true         # cancel vanilla trades; use NPCs instead
  raids: true                    # disable pillager raids
  pillager-patrols: true         # disable patrol spawning
  beacons: true                  # cancel beacon effects
  death-drops: true              # cancel vanilla drops; rpg-core death rules govern
  block-explosion-damage: true   # cancel block damage from TNT/creepers; region flag can re-enable
  block-break-tagged: true       # cancel vanilla break for PDC-tagged custom blocks
```

## Creative-mode bypass

Default: creative-mode players bypass our break-time math, damage pipeline, and economy deductions (so admins can build freely). Configurable per system:

```yaml
creative-mode-bypass:
  break-time: true
  damage: true
  economy: true
  region-flags: true             # creative bypasses region restrictions
```

## Repurposed vanilla bars

| Bar | What we do |
|---|---|
| Health | Heart-as-percent display ([health-display](health-display.md)) |
| Hunger | **Hidden** (frozen at max) |
| Armor | **Hidden** |
| XP | Shows most-recently-active skill's XP progress; pinnable via `/skill pin <skill>` |

```yaml
vanilla-xp-bar: most-recent      # most-recent | pinned | hidden
hunger-bar: hidden               # hidden | frozen-visible
armor-bar: hidden                # hidden | visible
```

## What if I want vanilla X back?

Set the corresponding flag to `false`. For example, to allow vanilla mob spawning in addition to our spawners:

```yaml
vanilla-suppression:
  mob-spawning: false
```

This is mainly useful for *peaceful* worlds where you want vanilla animals to spawn naturally but custom hostile mobs from our spawners.

## Related

- [Health display](health-display.md)
- [Damage pipeline](damage.md)
- [Status effects](status-effects.md)
- [Spawning](../content/spawning.md)
- [Recipes](../content/recipes.md)
