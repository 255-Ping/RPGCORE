# Content Creator Quick-Start

> **Goal:** get a custom weapon, a custom mob that drops it, and a quest to kill that mob — all in under 30 minutes. No Java required.

This guide skips theory. Follow the steps top-to-bottom and you'll have something playable by the end. Deeper references are linked at each step.

---

## Prerequisites

- `rpg-core` (and optionally `rpg-quests`) installed and loaded — see [Installation](../installation.md)
- You're in a running Paper server with op
- A text editor open for YAML files

---

## Step 1 — Create a custom item

Open (or create) `plugins/rpg-core/items/my_items.yml` and add:

```yaml
iron_slayer:
  MinecraftItem: iron_sword
  Type: SWORD
  DisplayName: "&fIron Slayer"
  Rarity: "&7&lCOMMON"
  Stats:
    damage: 40
    strength: 10
    crit_chance: 5
```

Save the file and run `/rpg reload`. Give it to yourself:

```
/rpg item give iron_slayer
```

You should see a sword in your inventory with custom lore. If you see an error in console, the YAML is malformed — check indentation.

> **Reference:** [Items](items.md) — full field list, all `Type` values, stat IDs.

---

## Step 2 — Create a custom mob

Open `plugins/rpg-core/mobs/my_mobs.yml`:

```yaml
iron_goblin:
  EntityType: ZOMBIE
  DisplayName: "&aIron Goblin"
  Level: 5
  MaxHealth: 120
  Stats:
    damage: 20
    defense: 15
  LootTable:
    - { Item: iron_slayer, Chance: 0.15, Min: 1, Max: 1 }
    - { Item: IRON_INGOT,  Chance: 0.60, Min: 1, Max: 3 }
```

Save and run `/rpg reload`. Spawn one at your feet:

```
/rpg mob spawn iron_goblin
```

Kill it — 15% of the time you'll get your `iron_slayer`. The rest of the time you get vanilla iron ingots (you can use any vanilla `Material` name as an `Item` value).

> **Reference:** [Mobs](mobs.md), [Loot Tables](loot-tables.md)

---

## Step 3 — Give the mob an ability

Create an ability in `plugins/rpg-core/abilities/my_abilities.yml`:

```yaml
goblin_smash:
  AbilitySequence:
    - aoe{radius=3.0, damage_multiplier=0.8}
    - particles{type=EXPLOSION_NORMAL, count=8, spread=0.5}
  Cooldown: 100      # ticks (100 = 5 seconds)
```

Then add it to the mob:

```yaml
iron_goblin:
  # ... same as before ...
  Abilities:
    - "goblin_smash ~onTimer:80"   # fires every 80 ticks (4 seconds)
```

Run `/rpg reload` and spawn the mob. It will occasionally smash the area around it with particles.

> **Reference:** [Abilities](abilities.md), [Mob ability triggers](mobs.md#ability-triggers)

---

## Step 4 — Place a spawner

Use the admin spawner commands to make the mob appear in the world automatically:

```
/spawner create iron_goblin 1 20   # mob ID, min delay ticks, max delay ticks
```

Stand where you want the spawner and run the command. A new spawner appears at your location and begins spawning `iron_goblin` on a random 1–20 tick timer.

> **Reference:** [Spawning](spawning.md)

---

## Step 5 — Create a quest (requires `rpg-quests`)

Open `plugins/rpg-quests/quests/my_quests.yml`:

```yaml
goblin_hunter:
  DisplayName: "&aGoblin Hunter"
  Description:
    - "&7The Iron Goblins are causing trouble."
    - "&7Slay 5 of them to earn a reward."
  RequiredLevel: 1
  Objectives:
    - { Type: kill_mob, Target: iron_goblin, Count: 5 }
  Rewards:
    Xp:
      combat: 200
    Currency: 500
    Items:
      - { Item: iron_slayer, Amount: 1 }
```

Run `/quest reload`. Accept it in-game:

```
/quest accept goblin_hunter
```

Kill 5 Iron Goblins. The action bar shows your progress. When the last one dies the quest completes and you receive the rewards.

> **Reference:** [Quests](../addons/quests.md)

---

## What's next

| Goal | Where to look |
|---|---|
| Fancier abilities (projectiles, beams, status effects) | [Abilities](abilities.md), [Patterns](patterns.md) |
| Custom armor set | [Items](items.md) — `Type: HELMET/CHEST/LEGS/BOOTS` |
| Full example library (every content type) | [Cookbook](cookbook.md) |
| How stat numbers translate to game feel | [Progression Guide](progression-guide.md) |
| Multiple linked quests (quest chains) | [Quests](../addons/quests.md) — chaining with `RequiredQuests` |
| Dungeons | [Dungeons](../addons/dungeons.md) |
