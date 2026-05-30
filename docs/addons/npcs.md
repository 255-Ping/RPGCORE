# NPCs (`rpg-npcs`)

> **Status:** v0.2.0 — shipped. Entity-based clickable NPCs with four behaviors (dialogue / shop / quest / banker). Entity style is per-NPC: `ENTITY` (any vanilla mob type, default VILLAGER) or `PLAYER` (fake player with skin). Invulnerability enforced by `NpcProtectionListener` — damage and mob targeting are cancelled at `HIGHEST` priority; death respawns the NPC after one tick. Fake players are spawned via NMS and are NOT shown in the tab list.

Custom NPCs for shops, dialogue, quest hand-off, and banking.

## NPC YAML

Files under `plugins/rpg-npcs/npcs/<file>.yml`. All NPCs in the folder load on startup; `all.yml` is written on any change when `autosave: true`.

```yaml
village_shopkeeper:
  DisplayName: "&eShopkeeper"
  World: world
  X: 100.5
  Y: 64.0
  Z: -50.5
  Yaw: 90.0
  Pitch: 0.0
  EntityStyle: entity         # entity (any mob) | player (fake player with skin)
  Behavior:
    Type: shop
    Items:
    - { Item: strength_potion, Buy: 500, Sell: 250 }
    - { Item: zombie_meat, Buy: 100, Sell: 50 }

my_banker:
  DisplayName: "&6Bank Teller"
  World: world
  X: 110.0
  Y: 64.0
  Z: -50.0
  Yaw: 180.0
  Pitch: 0.0
  EntityStyle: player
  Skin:
    Name: Notch               # fetches skin from this player name on first load (cached)
    # OR provide raw textures to skip the API fetch:
    # Value: <base64-texture-value>
    # Signature: <base64-texture-signature>
  Behavior:
    Type: banker
    BankName: "&6First National Bank"
    DailyInterestPercent: 0.5

quest_giver:
  DisplayName: "&aGuard Captain"
  World: world
  X: 120.0
  Y: 64.0
  Z: -55.0
  Yaw: 0.0
  Pitch: 0.0
  EntityStyle: entity
  Behavior:
    Type: quest
    Quest: guard_intro

dialogue_npc:
  DisplayName: "&7Town Elder"
  World: world
  X: 130.0
  Y: 64.0
  Z: -60.0
  Yaw: 45.0
  Pitch: 0.0
  EntityStyle: entity
  Behavior:
    Type: dialogue
    Lines:
    - "&7Welcome, traveler."
    - "&7These lands grow dangerous."
```

## Entity styles

### `entity` (default)
Spawns any vanilla `EntityType`. The type used is controlled by `display.body-entity` in `config.yml` (default: `VILLAGER`). AI, collision, and sound are disabled; entity is set invulnerable.

### `player`
Spawns an NMS `ServerPlayer` entity (fake player). Renders with a full player model and skin. The entity is **not** added to the server's PlayerList and is **not** shown in the tab list. A brief tab-list ADD packet is sent to each client so the skin texture loads, then removed after 2 ticks.

Skin source (evaluated in order):
1. `Skin.Value` + `Skin.Signature` — raw Mojang texture data, no API call needed
2. `Skin.Name` — player name; fetched async from Mojang API on first load, cached in memory and saved back to YAML

## Interaction types

### `dialogue`
Sends each line in `Lines` to the player as a chat message when right-clicked.

### `shop`
Opens a 27-slot buy/sell GUI. Items listed under `Behavior.Items`; each entry has `Item` (RPG item id or vanilla material), `Buy` (price to buy), `Sell` (price received when shift-clicking back). Requires `rpg-economy` soft-depend.

### `quest`
Delegates to `rpg-quests` via `QuestHandoffBridge` (Bukkit ServicesManager). Set `Behavior.Quest` to the quest id.

### `banker`
Opens a 27-slot bank GUI (balance display + preset deposit/withdraw amounts). Bank balance stored per-player per-NPC via `DataStore`. Daily interest accrues at `DailyInterestPercent` once per `banker.interest-interval-hours` (real-time). Requires `rpg-economy` soft-depend.

## Config

`plugins/rpg-npcs/config.yml`:

```yaml
display:
  use-text-display-for-name: true   # floating TextDisplay name tag above the body
  body-entity: VILLAGER             # vanilla EntityType for EntityStyle: entity NPCs
  click-range-blocks: 4.5

autosave: true

banker:
  default-daily-interest-percent: 0.5   # applied to all banker NPCs unless overridden per-NPC
  interest-interval-hours: 24           # real-time hours between interest accruals
```

## Commands

| Command | Permission |
|---|---|
| `/npc create <id> [name]` | `rpg.npcs.admin.create` |
| `/npc delete <id>` | `rpg.npcs.admin.delete` |
| `/npc move <id>` | `rpg.npcs.admin.move` |
| `/npc list` | `rpg.npcs.admin.list` |
| `/npc reload` | `rpg.npcs.admin.reload` |
| `/npc setbehavior <id> <dialogue\|shop\|quest\|banker> [args]` | `rpg.npcs.admin.setbehavior` |

`/npc create` places the NPC at the admin's current location with default DIALOGUE behavior. Edit `npcs/all.yml` to set `EntityStyle: player` and add a `Skin` section.

GUI editor (`/npc edit`) is deferred to the GUI overhaul pass.

## Persistence

All NPCs serialize to `plugins/rpg-npcs/npcs/all.yml` on change (when `autosave: true`) and on server shutdown. Bank balances persist via `DataStore` repository `npc_bank` with keys in `<npcId>:<playerUuid>` format.

## Related

- [Economy](economy.md)
- [Quests](quests.md)
- [Vanilla suppression](../core/vanilla-suppression.md)
