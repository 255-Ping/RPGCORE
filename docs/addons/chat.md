# Chat (`rpg-chat`)

> **Status:** In Progress — Chat format with `{prefix}{name}{suffix}{message}` placeholders (LuckPerms prefix/suffix via core's `NameFormatter`), `/msg` + `/reply` direct messaging, `/mutechat`, `/clearchat`, **and channel routing**. `/chat global|party|guild` switches your active channel; party/guild messages are filtered to only their respective members. Configurable per-channel prefixes (`channel-prefix-party`, etc.). Staff channel + custom server-defined channels come later.

Replaces vanilla chat with a configurable format, channels, and moderation. Wraps `rpg-core`'s `NameFormatter` for LuckPerms prefix/suffix everywhere.

## Config

`plugins/rpg-chat/config.yml`:

```yaml
chat-format: "{prefix}{name}{suffix} &7» &f{message}"
message-format: "&d{sender} &7-> &d{target}&7: &f{message}"

clearchat-lines: 100
mutechat-default: false

# Per-channel prefix prepended to the rendered chat line. Empty = no prefix.
channel-prefix-global: ""
channel-prefix-party: "&8[&dParty&8] "
channel-prefix-guild: "&8[&aGuild&8] "
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
| `/chat <global\|party\|guild>` | `rpg.chat.use.<channel>` |
| `/msg <player> <message>` | `rpg.chat.msg` (default true; aliases: `/tell`, `/w`, `/whisper`) |
| `/reply <message>` | `rpg.chat.reply` (default true; alias: `/r`) |
| `/clearchat` | `rpg.chat.clearchat` (op; alias: `/cc`) |
| `/mutechat` | `rpg.chat.mutechat` (op; alias: `/mc`) |
| `/chat reload` | `rpg.chat.admin.reload` (op) |

### Additional permissions

| Permission | Default | Notes |
|---|---|---|
| `rpg.chat.mute.bypass` | op | Allows chatting even when mutechat is active |
| `rpg.chat.use.global` | true | Access global channel |
| `rpg.chat.use.party` | true | Access party channel |
| `rpg.chat.use.guild` | true | Access guild channel |

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
