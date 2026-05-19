# Chat (`rpg-chat`)

> **Status:** In progress — Chat format with `{prefix}{name}{suffix}{message}` placeholders (LuckPerms prefix/suffix via core's `NameFormatter`), `/msg` + `/reply` direct messaging, `/mutechat` (with `rpg.chat.mute.bypass` perm), `/clearchat`. Per-channel routing (global/staff/party/guild) is deferred to a polish slice — for now everything is single-channel global.

Replaces vanilla chat with a configurable format, channels, and moderation. Wraps `rpg-core`'s `NameFormatter` for LuckPerms prefix/suffix everywhere.

## Config

`plugins/rpg-chat/config.yml`:

```yaml
chat-format: "{prefix}{name}{suffix} &7» &f{message}"

channels:
  global:
    prefix: "&7[G]"
    permission: "rpg.chat.use.global"
    default: true
  staff:
    prefix: "&c[Staff]"
    permission: "rpg.chat.use.staff"
    default: false
  # party and guild channels are registered automatically when those addons load

message-format: "&d{sender} &7-> &d{target}&7: &f{message}"
socialspy-format: "&8[SPY] {sender} -> {target}: {message}"

moderation:
  clearchat-lines: 100
  mutechat-bypass-permission: "rpg.chat.mute.bypass"
slowmode-default-seconds: 0

name-format:                     # passed to core NameFormatter
  use-luckperms-prefix: true
  use-luckperms-suffix: true
  fallback-format: "{name}"
```

## Placeholders

| Placeholder | Resolved by |
|---|---|
| `{name}` | Player's display name |
| `{prefix}` | LuckPerms prefix (empty if no LP) |
| `{suffix}` | LuckPerms suffix |
| `{message}` | Their message |
| `{sender}` / `{target}` | DM placeholders |
| `{world}` | World name |
| `{health}`, `{mana}`, `{coins}`, `{skill:<id>:level}` | Stat/skill snapshots |

## Commands

| Command | Permission |
|---|---|
| `/chat <channel>` | `rpg.chat.use.<channel>` |
| `/msg <player> <msg>` | `rpg.chat.msg` |
| `/reply <msg>` | `rpg.chat.reply` |
| `/clearchat` | `rpg.chat.clearchat` |
| `/mutechat [on\|off]` | `rpg.chat.mutechat` |
| `/mute <player> [duration] [reason]` | `rpg.chat.mute` |
| `/unmute <player>` | `rpg.chat.unmute` |
| `/slowmode <seconds>` | `rpg.chat.slowmode` |
| `/socialspy` | `rpg.chat.socialspy` |

## Channels

Channels work like Hypixel: each player has an *active* channel, and their messages route there.

- `global` — visible to all players with `rpg.chat.use.global`
- `staff` — visible only to staff
- `party` — registered by `rpg-parties` if loaded; visible to party members
- `guild` — registered by `rpg-guilds` if loaded; visible to guild members

Other addons can register their own channels via the chat addon's API.

## Persistence

Mute records (player, expires-at, reason) persist via `DataStore`.

Soft-depend on LuckPerms; falls back to bare names without it.

## Related

- [NameFormatter (core)](../core/README.md)
- [Parties](parties.md), [Guilds](guilds.md)
- [Master command reference](../commands.md#chat-rpg-chat)
