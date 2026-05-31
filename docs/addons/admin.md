# Admin Utilities (`rpg-admin`)

> **Status:** Working — General-purpose admin commands for server management.

A collection of standard admin utility commands. All require `rpg-core` to be loaded (for the permission framework and player data access).

## Commands

### Gamemode

| Command | Permission | Notes |
|---|---|---|
| `/gmc [player]` | `rpg.admin.gamemode` | Set Creative |
| `/gms [player]` | `rpg.admin.gamemode` | Set Survival |
| `/gma [player]` | `rpg.admin.gamemode` | Set Adventure |
| `/gmsp [player]` | `rpg.admin.gamemode` | Set Spectator |

All four share the same `rpg.admin.gamemode` permission. Omit `[player]` to target yourself.

### Movement

| Command | Permission | Notes |
|---|---|---|
| `/fly [player]` | `rpg.admin.fly` | Toggle flight |
| `/tp <player>` | `rpg.admin.tp` | Teleport to a player |
| `/tp <x> <y> <z>` | `rpg.admin.tp` | Teleport to coordinates |
| `/tp <from> <to>` | `rpg.admin.tp` | Teleport one player to another |
| `/tphere <player>` | `rpg.admin.tp` | Pull a player to your location |
| `/speed <value> [player]` | `rpg.admin.speed` | Set walk speed (1–10 scale) |
| `/speed walk\|fly <value> [player]` | `rpg.admin.speed` | Set walk or fly speed explicitly |

### Player state

| Command | Permission | Notes |
|---|---|---|
| `/heal [player]` | `rpg.admin.heal` | Restore to full RPG HP |
| `/heal <player>` | `rpg.admin.heal.others` | Heal another player (requires `.others`) |
| `/feed [player]` | `rpg.admin.feed` | Restore hunger |
| `/feed <player>` | `rpg.admin.feed.others` | Feed another player (requires `.others`) |
| `/god [player]` | `rpg.admin.god` | Toggle invincibility (ignores all damage) |
| `/clear [player]` | `rpg.admin.clear` | Clear own inventory |
| `/clear <player>` | `rpg.admin.clear.others` | Clear another player's inventory |

### Server

| Command | Permission | Notes |
|---|---|---|
| `/broadcast <message>` | `rpg.admin.broadcast` | Broadcast to all players. Alias: `/bc` |
| `/sudo <player> <command>` | `rpg.admin.sudo` | Force a player to run a command as themselves |

## Permission summary

| Permission | Default | What it covers |
|---|---|---|
| `rpg.admin.*` | op | All rpg-admin permissions |
| `rpg.admin.gamemode` | op | `/gmc`, `/gms`, `/gma`, `/gmsp` |
| `rpg.admin.fly` | op | `/fly` |
| `rpg.admin.god` | op | `/god` |
| `rpg.admin.tp` | op | `/tp`, `/tphere` |
| `rpg.admin.heal` | op | `/heal` (self) |
| `rpg.admin.heal.others` | op | `/heal <player>` |
| `rpg.admin.feed` | op | `/feed` (self) |
| `rpg.admin.feed.others` | op | `/feed <player>` |
| `rpg.admin.speed` | op | `/speed` |
| `rpg.admin.clear` | op | `/clear` (self) |
| `rpg.admin.clear.others` | op | `/clear <player>` |
| `rpg.admin.broadcast` | op | `/broadcast` |
| `rpg.admin.sudo` | op | `/sudo` |

## Notes

- `/heal` restores **RPG HP** (not vanilla hearts) — it calls `HealthService.healToFull`.
- `/god` makes the player immune to all damage events in the RPG pipeline. It does not affect vanilla damage sources that bypass the pipeline (void death still teleports to spawn per the server's config).
- All commands that accept `[player]` will target the sender if the argument is omitted.
