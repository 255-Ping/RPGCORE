# Holograms (`rpg-holograms`)

> **Status:** Working — Static and animated multi-line holograms (create, edit lines, move, teleport, delete, persist across restart) plus damage indicators. Animated holograms added in 0.0.5. All management done via commands; GUI editor planned but not yet built.

Two related features in one addon:

1. **Holograms** — floating text displays using Minecraft `TextDisplay` entities. Multi-line for static holograms; single entity cycling through lines as frames for animated holograms.
2. **Damage indicators** — floating number text spawned at hit locations, fading out.

## Hologram YAML

Files under `plugins/rpg-holograms/holograms/<id>.yml`:

```yaml
# Static hologram — one TextDisplay per line, stacked vertically
welcome_sign:
  Location: { world: world, x: 0, y: 80, z: 0 }
  Lines:
  - "&6&lWelcome to the Server"
  - "&7Visit &espawn.example.com"

# Animated hologram — single TextDisplay cycles through Lines as frames
animated_tip:
  Location: { world: world, x: 10, y: 80, z: 0 }
  Animated: true
  FrameInterval: 40    # ticks between frame advances (default 20 = 1 second)
  Lines:
  - "&eTip: Right-click to open your stats!"
  - "&eTip: Join a party with /party create"
  - "&eTip: Upgrade your bag with /accessories upgrade"
```

| Field | Default | Description |
|---|---|---|
| `Location` | required | `{ world, x, y, z }` |
| `Lines` | required | List of text strings (legacy `&` color codes supported) |
| `Animated` | `false` | If `true`, Lines become animation frames on a single entity |
| `FrameInterval` | `20` | Ticks between frame advances (ignored when `Animated: false`) |

## Hologram features

- **Multi-line (static)**: any number of lines, stacked vertically — one `TextDisplay` entity per line
- **Animated**: single entity cycles through `Lines` as frames at `FrameInterval` ticks; driven by a single global 1-tick BukkitTask
- **Legacy color codes**: `&6`, `&l`, etc. — standard Bukkit legacy format
- **Persistent across restart**: all holograms are saved to YAML and respawned on startup

## Commands

All subcommands have alias `/holo`.

| Command | Permission |
|---|---|
| `/holograms create <id> <text>` | `rpg.holograms.admin.create` |
| `/holograms delete <id>` | `rpg.holograms.admin.delete` |
| `/holograms list` | `rpg.holograms.admin.list` |
| `/holograms info <id>` | `rpg.holograms.admin.edit` |
| `/holograms tp <id>` | `rpg.holograms.admin.tp` |
| `/holograms move <id>` | `rpg.holograms.admin.move` |
| `/holograms line add <id> <text>` | `rpg.holograms.admin.edit` |
| `/holograms line set <id> <index> <text>` | `rpg.holograms.admin.edit` |
| `/holograms line remove <id> <index>` | `rpg.holograms.admin.edit` |
| `/holograms line list <id>` | `rpg.holograms.admin.edit` |
| `/holograms set <id> animated <true\|false>` | `rpg.holograms.admin.edit` |
| `/holograms set <id> frameinterval <ticks>` | `rpg.holograms.admin.edit` |
| `/holograms reload` | `rpg.holograms.admin.reload` |

`/holograms create <id> <text>` spawns the hologram at your current location with one initial line. Use `/holograms move <id>` to reposition. `/holograms info <id>` prints all current properties including line count, animated state, and interval.

## Damage indicators

`plugins/rpg-holograms/config.yml`:

```yaml
damage-indicators:
  enabled: true
  duration-ticks: 25
  drop-blocks: 0.8              # how far the number drifts downward (indicators fall, not rise)
  random-offset: 0.4            # XZ scatter to avoid stacking
  start-scale: 1.0              # initial display scale
  min-scale: 0.3                # shrinks to this as it fades
  formats:
    normal:    "&f{amount}"
    crit:      "&e&l✧ {amount} ✧"
    true:      "&f&l⚡ {amount} ⚡"
    lifesteal: "&c+{amount}"
    healed:    "&a+{amount} ❤"
  show-to:
    attacker: true
    victim-if-player: true
    bystanders: false
```

> **Note:** Damage indicators float **downward** (not upward) and shrink as they fade — see `drop-blocks`, `start-scale`, `min-scale`. The `miss` and `immune` format keys do not exist in the current implementation.

Damage indicators are short-lived DisplayEntities, pooled and reused to avoid GC churn at high-frequency combat.

## Implementation note

All visuals use **DisplayEntity (text display)** — modern, performant, supports backgrounds and scaling. We don't use armor stands.

## Related

- [Damage pipeline](../core/damage.md)
- [Selection wand](../core/selection-wand.md)
- [NPCs](npcs.md)
- [Quests](quests.md)
