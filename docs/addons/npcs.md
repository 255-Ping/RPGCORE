# NPCs (`rpg-npcs`)

> **Status:** Planned

Custom shop / dialogue / quest NPCs. Replaces vanilla villager trading.

## NPC YAML

Files under `plugins/rpg-npcs/npcs/<id>.yml`. Persistent location + behavior.

```yaml
village_shopkeeper:
  DisplayName: "&eShopkeeper"
  Skin: "MHF_Villager"           # texture name, base64 value, or URL
  EntityType: PLAYER             # PLAYER | VILLAGER | ANY (any LivingEntity type)
  LookAtPlayer: true
  Location: { world: world, x: 100, y: 64, z: -50, yaw: 0, pitch: 0 }
  Interactions:
    type: SHOP                   # SHOP | DIALOGUE | QUEST | BANKER | COMPOSITE (v2)
    shop:
      trades:
      - { buy: { item: super_diamond, amount: 1 }, price: 1000 }
      - { sell: { item: zombie_meat, amount: 1 }, price: 50, max-per-day: 10 }
      gui-title: "&eShop"
```

## Interaction types

### SHOP

A simple buy/sell GUI. `buy` = player gives currency, receives item. `sell` = player gives item, receives currency. `max-per-day` is per-player.

### DIALOGUE

```yaml
Interactions:
  type: DIALOGUE
  dialogue:
    start: greet
    nodes:
      greet:
        lines:
        - "&7Hello, &e{name}&7! What can I do for you?"
        choices:
        - { text: "Quest", goto: quest_hand, condition: "!has_quest(intro_quest)" }
        - { text: "Goodbye", goto: end }
      quest_hand:
        give-quest: intro_quest
        lines:
        - "&7Take this map..."
        goto: end
      end:
        lines: []
```

Conditions use a small expression language: `has_quest(id)`, `skill_level(id) >= n`, `has_item(id)`, `has_perm(node)`, `&&`, `||`, `!`.

### QUEST

Shorthand for a dialogue NPC that just hands off / completes a quest. Hooks into `rpg-quests` once that addon ships.

### BANKER

Opens a currency-only deposit/withdraw GUI (works with `rpg-economy`). Useful for a "bank vault" NPC.

### COMPOSITE (deferred)

Multi-interaction NPC. Player picks from a menu. Planned for v2.

## Entity type

- **`PLAYER`** — packet-level fake player with a skin (Hypixel-style). Looks best.
- **`VILLAGER`** — real villager entity, vanilla trade UI cancelled.
- **`ANY`** — any LivingEntity (e.g., piglin, blaze) for themed NPCs.

For v1, PLAYER is preferred; falls back to VILLAGER if packet path fails.

## Commands

| Command | Permission |
|---|---|
| `/npc create <id>` | `rpg.npcs.admin.create` |
| `/npc edit <id>` | `rpg.npcs.admin.edit` |
| `/npc delete <id>` | `rpg.npcs.admin.delete` |
| `/npc list [world]` | `rpg.npcs.admin.list` |
| `/npc tp <id>` | `rpg.npcs.admin.tp` |
| `/npc move <id>` | `rpg.npcs.admin.move` |

`/npc create <id>` creates an NPC at the admin's location.

## GUI editor

`/npc edit <id>` opens an editor GUI for non-YAML admins to author NPCs. The GUI writes back to the NPC's YAML file.

## Persistence

Each NPC has its own YAML file. Stored via `DataStore` for atomic writes.

## Related

- [Economy](economy.md)
- [Quests](quests.md)
- [Vanilla suppression (villager trading)](../core/vanilla-suppression.md)
- [Holograms](holograms.md) — NPCs can have floating names above them; the NameFormatter applies
