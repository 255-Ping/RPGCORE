# Kits (`rpg-kits`)

> **Status:** Working — YAML-driven starter and reward kits with one-time or cooldown claim modes, RPG/vanilla item support, and DataStore-backed claim state.

## Kit YAML

Files under `plugins/rpg-kits/kits/<file>.yml`. Each top-level key is a kit:

```yaml
starter:
  DisplayName: "&aStarter Kit"
  Description:
  - "&7Everything a new adventurer needs."
  - "&8One-time only."
  Items:
  - { Item: iron_sword,   Amount: 1 }
  - { Item: bread,        Amount: 16 }
  OneTime: true
  Cooldown: 0         # ignored when OneTime: true

daily:
  DisplayName: "&6Daily Rations"
  Description:
  - "&7A small daily food supply."
  Items:
  - { Item: cooked_beef,  Amount: 8 }
  - { Item: golden_apple, Amount: 1 }
  OneTime: false
  Cooldown: 86400     # 24 hours in seconds
  Permission: "rpg.kit.daily"   # optional; anyone with rpg.kits.use if omitted
```

**Item IDs:** can be any RPG custom item ID (looked up via `RpgServices.items()`) or a vanilla Minecraft material name (e.g. `diamond_sword`, `oak_log`). Unresolvable IDs are skipped with a warning.

## Claim modes

| Field | Behaviour |
|---|---|
| `OneTime: true` | Player may claim once, ever. Resets require `/kitreset`. |
| `OneTime: false` + `Cooldown: N` | Player may claim every N seconds. Tracked per-player. |
| `OneTime: false` + `Cooldown: 0` | Claimable any number of times with no limit. |

## Commands

| Command | Permission | Description |
|---|---|---|
| `/kit` | `rpg.kits.use` | List available kits (shows claim status / cooldown) |
| `/kit <name>` | `rpg.kits.use` | Claim a kit |
| `/givenkit <player> <kit>` | `rpg.kits.admin` | Force-give a kit, bypassing claim checks |
| `/kitreset <player> [kit]` | `rpg.kits.admin` | Clear claim history for one kit or all kits |

## Permissions

| Node | Default | Description |
|---|---|---|
| `rpg.kits.use` | `true` | Claim kits via `/kit` |
| `rpg.kits.admin` | `op` | `/givenkit`, `/kitreset` |
| `rpg.kits.reload` | `op` | Reload kit files |
| *(kit-specific)* | — | Optional per-kit permission node set via `Permission:` in YAML |

## Storage

Claim state is stored in DataStore repository `kit_claims`, keyed by player UUID. Each entry holds:
- `claimed` — list of one-time claimed kit IDs.
- `cooldowns` — map of kit ID → next-available epoch-ms (for cooldown kits).

## Related

- [Items](../content/items.md)
- [Persistence](../core/persistence.md)
