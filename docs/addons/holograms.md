# Holograms (`rpg-holograms`)

> **Status:** Planned

Two related features in one addon:

1. **Holograms** — floating text displays using Minecraft DisplayEntities, with multi-line content, click interactions, and per-player visibility.
2. **Damage indicators** — floating number text spawned at hit locations, fading out.

Inspired by the DisplayEntityEditor plugin's feature set.

## Hologram YAML

Files under `plugins/rpg-holograms/holograms/<id>.yml`:

```yaml
welcome_sign:
  Location: { world: world, x: 0, y: 80, z: 0 }
  Lines:
  - "&6&lWelcome to the Server"
  - "&7Visit &espawn.example.com"
  Background: { color: "#000000", opacity: 0.4 }
  Scale: 1.0
  Rotation: 0
  Visibility:
    mode: all                    # all | permission | quest-progress | proximity
    permission: ""               # used when mode=permission
    quest: ""                    # used when mode=quest-progress
    quest-state: ""              # required quest state for quest-progress mode
    radius: 0                    # used when mode=proximity (0 = unlimited)
  Click:
    enabled: false
    action: ""                   # command | give-item | open-shop:<npc> | start-quest:<id>
    args: {}
  Persistent: true
```

## Hologram features

- **Multi-line**: any number of lines, MiniMessage or legacy color codes
- **Background**: color and opacity per the DisplayEntity API
- **Scale + rotation**: per the DisplayEntity API
- **Click actions**: right-click runs a configured action
- **Per-player visibility**: filter by permission, quest progress, or proximity
- **Persistent across restart**: yes by default

## Click actions

| Action | Args |
|---|---|
| `command` | `{ command: "give @p iron_sword 1" }` — runs as console |
| `give-item` | `{ item: super_sword, amount: 1 }` — gives a registered item |
| `open-shop` | `{ npc: village_shopkeeper }` — opens that NPC's shop GUI |
| `start-quest` | `{ quest: intro_quest }` — gives the player a quest |

More actions can be added by other addons.

## Commands

| Command | Permission |
|---|---|
| `/hologram create <id>` | `rpg.holograms.admin.create` |
| `/hologram edit <id>` | `rpg.holograms.admin.edit` |
| `/hologram addline <id> <text>` | `rpg.holograms.admin.edit` |
| `/hologram delete <id>` | `rpg.holograms.admin.delete` |
| `/hologram list [near\|world]` | `rpg.holograms.admin.list` |
| `/hologram tp <id>` | `rpg.holograms.admin.tp` |

`/hologram create <id>` uses the selection wand's `hologram` mode for the anchor location.

## Damage indicators

`plugins/rpg-holograms/config.yml`:

```yaml
damage-indicators:
  enabled: true
  duration-ticks: 30
  rise-blocks: 1.0               # how far the number floats upward
  random-offset: 0.3             # XZ scatter to avoid stacking
  formats:
    normal:    "&f{amount}"
    crit:      "&e&l✧ {amount} ✧"
    true:      "&f&l⚡ {amount} ⚡"
    lifesteal: "&c+{amount}"
    healed:    "&a+{amount} ❤"
    miss:      "&7MISS"
    immune:    "&7IMMUNE"
  show-to:
    attacker: true
    victim-if-player: true
    bystanders: false
```

Damage indicators are short-lived DisplayEntities, pooled and reused to avoid GC churn at high-frequency combat.

## Implementation note

All visuals use **DisplayEntity (text display)** — modern, performant, supports backgrounds and scaling. We don't use armor stands.

## Related

- [Damage pipeline](../core/damage.md)
- [Selection wand](../core/selection-wand.md)
- [NPCs](npcs.md)
- [Quests](quests.md)
