# Quests (`rpg-quests`)

> **Status:** Planned (full design deferred)

Quest system. Skeleton plugin exists; full design is deferred until earlier systems are implemented. Documented here as a placeholder so the rest of the docs can link to it.

## Intended shape

- Quest definitions in YAML under `plugins/rpg-quests/quests/<file>.yml`
- Objective tree: kill X, collect Y, reach Z, interact with NPC, dialogue choice
- Rewards: items (from item registry), currency (from `rpg-economy`), skill XP, status effects, region unlocks
- Per-player progress persisted via `DataStore`
- Quest log GUI

## Commands

| Command | Permission |
|---|---|
| `/quest [id]` | `rpg.quests.open` |
| `/quest give <id> [player]` | `rpg.quests.admin.give` |
| `/quest reset <id> [player]` | `rpg.quests.admin.reset` |
| `/quest complete <id> [player]` | `rpg.quests.admin.complete` |

## NPC integration

[NPC](npcs.md) `QUEST` and `DIALOGUE` interaction types hook into this addon. NPCs are how players receive quests.

## Tentative quest YAML shape

```yaml
intro_quest:
  display: "&aA New Beginning"
  description:
  - "&7Speak with the village elder."
  - "&7Then defeat 5 cave zombies."
  objectives:
  - { type: dialogue, npc: village_elder, dialogue-node: intro_done }
  - { type: kill_mob, mob: cave_zombie, count: 5 }
  rewards:
    coins: 500
    skill-xp: { combat: 100 }
    items:
    - { id: iron_sword, amount: 1 }
  requirements:
    skills: { combat: 1 }
  repeatable: false
```

## Related

- [NPCs](npcs.md)
- [Economy](economy.md)
- [Skills framework](../core/skills.md)
