# End-to-End Walkthrough: Your First Custom Mob

> A complete example touching six systems: ability → item → mob → spawner → loot → quest. No prior knowledge required. Each step is copy-pasteable.

---

## What we're building

A **Forest Goblin** that:
- Poisons players on hit
- Carries a custom weapon
- Drops a crafting material
- Is spawned by a world spawner
- Is tracked by a quest: "Kill 10 Forest Goblins"

---

## Step 1 — Create the ability

The goblin's on-hit poison. File: `plugins/rpg-core/abilities/goblin.yml`

```yaml
goblin_poison_strike:
  Name: "Poison Strike"
  Description:
  - "&7A venomous bite."
  AbilitySequence:
  - apply_status{id=poison, level=1, duration=100, target=target}
```

This ability applies the built-in `poison` status effect to whatever the goblin just hit. No cooldown needed — mob abilities gate themselves via their trigger interval.

Reload: `/rpg reload`

---

## Step 2 — Create the item (optional — gives the goblin a weapon)

File: `plugins/rpg-core/items/goblin.yml`

```yaml
goblin_dagger:
  MinecraftItem: stone_sword
  Type: SWORD
  DisplayName: "&2Goblin Dagger"
  Rarity: "&f&lCOMMON"
  Lore:
  - "&7Crude but sharp."
  Stats:
    damage: 12
    strength: 5
```

This item shows in the goblin's hand and contributes its `damage` stat to attacks.

Reload: `/rpg reload`

---

## Step 3 — Create the mob

File: `plugins/rpg-core/mobs/forest.yml`

```yaml
forest_goblin:
  MinecraftMob: zombie
  DisplayName: "&2Forest Goblin"
  Health: 150
  Damage: 10
  Armor: 0
  AI:
    profile: aggressive
    aggression-range: 14
    attack-range: 2
    leash-range: 30
    leash-action: return
  Equipment:
  - HAND goblin_dagger
  Abilities:
  - goblin_poison_strike{} ~onHit
  LootTable:
    attribution: weighted-by-damage
    roll-mode: per-player
    coin-drop: { min: 5, max: 20 }
    rolls:
    - { item: goblin_ear, chance: 40.0, min: 1, max: 1 }
    - { item: goblin_dagger, chance: 4.0, min: 1, max: 1 }
```

Also add the drop item to your items file:

```yaml
goblin_ear:
  MinecraftItem: rabbit_foot
  Type: MATERIAL
  DisplayName: "&2Goblin Ear"
  Rarity: "&f&lCOMMON"
  Lore:
  - "&7Proof of a goblin kill."
```

Reload: `/rpg reload`

Test spawn it: `/rpg mob spawn forest_goblin`

You should see a zombie named "Forest Goblin" that poisons you when it hits you, and drops ears + coins on death.

---

## Step 4 — Create a spawner

Place a spawner block in-world (vanilla spawner or any block you designate), then register it:

```
/spawner create goblin_spawner
/spawner set goblin_spawner mob forest_goblin
/spawner set goblin_spawner radius 16
/spawner set goblin_spawner max-alive 5
/spawner set goblin_spawner cooldown 200
/spawner set goblin_spawner mode continuous
```

Now goblins spawn automatically near that block. See [Spawning](spawning.md) for full options.

---

## Step 5 — Create the quest

File: `plugins/rpg-quests/quests/starter.yml`

```yaml
goblin_slayer:
  DisplayName: "&2Goblin Slayer"
  Description: "The forest goblins are getting bold. Thin their numbers."
  Category: combat
  Repeatable: false
  Objectives:
  - type: kill_mob
    mob: forest_goblin
    count: 10
    display: "Kill Forest Goblins"
  Rewards:
    Currency: 500
    SkillXp:
      combat: 200
    Items:
    - { item: healing_potion, amount: 3 }
```

Reload: `/rpg reload`

Players accept the quest via `/quests` (or an NPC with `Quest: goblin_slayer` in its config). Every forest_goblin kill increments their objective counter. At 10 kills, the quest auto-completes and grants the reward.

---

## How the systems connect

```
goblin_poison_strike (ability)
        ↓
    on ~onHit trigger (ability system)
        ↓
    apply_status{id=poison} (status effect system)
        ↓
    victim takes periodic poison damage (damage pipeline)

forest_goblin (mob)
        ↓
    killed by player
        ↓
    LootTable rolls (loot system) → goblin_ear drop + coins
        ↓
    kill_mob objective increments (quest system)
        ↓
    at 10 kills → quest reward granted (economy + XP)
```

---

## Next steps

- Add more mob abilities (see [Abilities](abilities.md) for all effect types)
- Gate a weapon drop behind a rarer roll (`chance: 1.0`)
- Make the quest require a specific region ([Regions](../addons/regions.md))
- Give the goblin a boss variant with higher health and a ground slam ability
- See the full [Cookbook](cookbook.md) for more content examples

## Related

- [Items schema](items.md)
- [Mobs schema](mobs.md)
- [Abilities reference](abilities.md)
- [Spawning](spawning.md)
- [Quests](../addons/quests.md)
- [Status effects](../core/status-effects.md)
- [Cookbook](cookbook.md)
