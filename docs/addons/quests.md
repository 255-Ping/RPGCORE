# Quests (`rpg-quests`)

> **Status:** In Progress — Four objective types live: `kill_mob`, `mine_block`, `collect_item`, `talk_npc`. Per-player state via core DataStore. Rewards: skill XP, currency, items. Auto-complete on objective finish (configurable). NPC turn-in via `rpg-npcs` bridge. `/quest` commands wired. Quest log GUI deferred.

Quest definitions live in YAML under `plugins/rpg-quests/quests/`. Any number of files, any number of quests per file. An example file (`quests/example.yml`) is generated on first run.

## Config

`plugins/rpg-quests/config.yml`:

```yaml
# Persistence repository name inside core DataStore.
data-repository: quests

# Show "Quest progressed" action bar message on each objective tick.
progress-action-bar: true

# Auto-complete the quest when all objectives finish.
# If false, player must run /quest complete <id> or visit the turn-in NPC.
auto-complete: true
```

## Quest YAML

```yaml
# quests/<file>.yml — each top-level key is the quest ID

goblin_menace:
  DisplayName: "&cThe Goblin Menace"
  Description:
    - "&7Slay the goblins infesting the forest."
    - "&7Bring back 5 hides to the elder."
  RequiredLevel: 1          # minimum Combat skill level to accept (0 = none)
  Objectives:
    - { Type: kill_mob,    Target: goblin,       Count: 10 }
    - { Type: collect_item, Target: goblin_hide,  Count: 5 }
    - { Type: talk_npc,    Target: forest_elder }
  Rewards:
    Xp:
      combat: 500           # skill-id: xp amount
      mining: 50
    Currency: 250           # primary currency coins
    Items:
      - { Item: strength_potion, Amount: 2 }
      - { Item: iron_sword,      Amount: 1 }
```

## Objective types

| Type | Target | Count | Fires when |
|---|---|---|---|
| `kill_mob` | mob id (custom or vanilla entity type name) | kills required | `EntityDeathEvent` for the target |
| `mine_block` | block material or custom block id | blocks required | `BlockBreakEvent` / `RpgBlockBreakEvent` |
| `collect_item` | item id (custom or vanilla material) | items required | `EntityPickupItemEvent` |
| `talk_npc` | NPC id | 1 (always) | Right-clicking the NPC (via npcs bridge) |

`Target: any` on `kill_mob` / `mine_block` / `collect_item` matches any entity/block/item of that type.

Progress toward each objective is displayed in the action bar as `"Kill 3 goblin (3/10)"`.

## Rewards

| Key | Value format | Effect |
|---|---|---|
| `Xp` | `{ skill-id: amount, ... }` | Awards XP via `SkillsService.awardXp` |
| `Currency` | number | Deposits into player's primary balance |
| `Items` | list of `{ Item: id, Amount: n }` | Given to inventory; overflow dropped at feet |

## NPC integration

[NPC](npcs.md) with `type: QUEST` hands off a quest and auto-accepts it on click. `type: DIALOGUE` can include a `give-quest:` node to give one mid-conversation. Both bridge to `QuestManager.accept()` via the `QuestNpcHandoff` service registered at plugin enable.

## Commands

| Command | Permission | Notes |
|---|---|---|
| `/quest` | `rpg.quests.open` | Open quest log (GUI deferred; lists active quests in chat) |
| `/quest <id>` | `rpg.quests.open` | Show details for a specific quest |
| `/quest give <id> [player]` | `rpg.quests.admin.give` | Force-accept a quest for a player |
| `/quest reset <id> [player]` | `rpg.quests.admin.reset` | Clear progress and remove from active |
| `/quest complete <id> [player]` | `rpg.quests.admin.complete` | Force all objectives complete and award rewards |
| `/quest reload` | `rpg.quests.admin.reload` | Reload quest YAML files |

## Persistence

Per-player state: active quests (with per-objective progress counters), completed quest IDs, and last-completion timestamps. Stored via `DataStore` under `quests/<uuid>`. Persists across restarts.

## Related

- [NPCs](npcs.md)
- [Economy](economy.md)
- [Skills framework](../core/skills.md)
- [Items](../content/items.md) — item rewards reference the item registry
