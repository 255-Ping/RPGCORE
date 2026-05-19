# HUD (`rpg-hud`)

> **Status:** Planned

Configurable scoreboard, tablist, action bar, and player nametags. All formats are templates with placeholders resolved by core's `MessageFormatter`.

## Config

`plugins/rpg-hud/config.yml`:

```yaml
scoreboard:
  enabled: true
  title: "&6&lRPG"
  lines:
  - "&7Health: &c{health}/{max_health}"
  - "&7Mana: &b{mana}/{max_mana}"
  - "&7Combat: &c{skill:combat:level}"
  - ""
  - "&e{coins} coins"
  update-ticks: 10

tablist:
  enabled: true
  header:
  - "&6&lRPG SERVER"
  - "&7Players online: {online}"
  footer:
  - "&7play.example.com"
  name-format: "{prefix}{name}{suffix}"

nametags:
  enabled: true
  format: "{prefix}{name}{suffix}"
  show-health-bar: true          # tiny health bar under the name
  show-status-icons: true        # active status effects show as icons

action-bar:
  idle-format: "&c❤ {health}/{max_health}  &b✦ {mana}/{max_mana}  &a✤ {defense}"
  show-on-xp-gain: true
  xp-gain-format: "&a+{amount} {skill} XP &7(&e{progress}/{required}&7)"
  priority-order: [combat-feedback, xp-gain, idle]
  message-duration-ticks: 40
```

## Placeholders

Same set as [chat](chat.md#placeholders) plus:

| Placeholder | Resolves to |
|---|---|
| `{max_health}` | Max RPG HP |
| `{defense}`, `{strength}`, etc. | Live stat values |
| `{online}` | Online player count |
| `{ping}` | Player ping |
| `{world}` | World name |
| `{skill:<id>:level}` | Skill level |
| `{skill:<id>:progress}` | XP toward next level |
| `{skill:<id>:required}` | XP required to next level |

## Commands

| Command | Permission |
|---|---|
| `/hud toggle <scoreboard\|tablist\|actionbar>` | `rpg.hud.toggle` |
| `/hud reload` | `rpg.hud.reload` |

## Action bar priorities

The action bar shows one message at a time. When multiple sources want it (combat feedback, XP gain, idle stats), they're prioritized:

- **combat-feedback** — damage indicators not handled by holograms, mana-cost notices, ability cast info
- **xp-gain** — XP gain messages (optionally suppressed)
- **idle** — stat summary when nothing else to show

`priority-order` is configurable. Each transient message displays for `message-duration-ticks` before idle resumes.

## Pinned XP bar

The vanilla XP bar (above hotbar) is repurposed (per [vanilla suppression](../core/vanilla-suppression.md)):

```yaml
vanilla-xp-bar: most-recent      # most-recent | pinned | hidden
```

Most-recent shows the skill you most recently gained XP in. `/skill pin <skill>` (core command) overrides to a specific skill.

## Player join

On join, `HudService` builds the scoreboard, tablist, nametags. Updates tick at the configured rate.

## Related

- [Chat](chat.md) — `NameFormatter` shared
- [Vanilla suppression](../core/vanilla-suppression.md)
- [Stats reference](../stats.md)
