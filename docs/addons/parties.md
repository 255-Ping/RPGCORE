# Parties (`rpg-parties`)

> **Status:** In progress — Session-only parties working. `/party create|invite|accept|kick|promote|demote|leave|disband|list` all wired. Owner/moderator/member ranks enforced. Invites time out (default 60s). On player quit, the leaver is removed; on owner quit, ownership transfers to the highest-rank remaining member (unless `disband-on-owner-leave: true`). New `PartyService` API exposed via `RpgServices.parties()` so chat/dungeons can query party membership. XP sharing config knobs are deferred to a polish slice.

Session-only player groups. No persistence — parties dissolve on owner disconnect (configurable). For persistent groups, see [guilds](guilds.md).

## Config

`plugins/rpg-parties/config.yml`:

```yaml
max-size: 5
xp-sharing:
  enabled: true
  scope: all-skills              # all-skills | combat-only | list: [combat, mining]
  range-blocks: 64               # 0 = unlimited
  split-formula: "amount / party_size"   # configurable expression
disband-on-owner-leave: false
allow-friendly-fire: false
invite-timeout-seconds: 60
```

## Ranks

- **Owner** — created the party. Promote, demote, disband.
- **Moderator** — invite, kick.
- **Member** — leave, list.

## Commands

| Command | Permission | Notes |
|---|---|---|
| `/party create` | `rpg.parties.create` | Creates a one-player party |
| `/party invite <player>` | `rpg.parties.invite` | Owner/mod only |
| `/party accept` | `rpg.parties.accept` | Recipient |
| `/party kick <player>` | `rpg.parties.kick` | Owner/mod only |
| `/party promote <player>` | `rpg.parties.promote` | Owner only |
| `/party demote <player>` | `rpg.parties.demote` | Owner only |
| `/party leave` | `rpg.parties.leave` | Anyone |
| `/party disband` | `rpg.parties.disband` | Owner only |
| `/party list` | `rpg.parties.list` | Anyone in party |
| `/chat party` | `rpg.chat.use.party` | Registered with rpg-chat if loaded |

## XP sharing

When `xp-sharing.enabled`, the XP a member earns is shared across the party. The `split-formula` is an expression with variables:

- `amount` — original XP amount
- `party_size` — total members
- `nearby_count` — members within `range-blocks`

Default `amount / party_size` (equal split). `amount / 1` would mean each member gets full XP independently (no penalty).

## Dungeons integration

Parties are the unit of entry for `rpg-dungeons`. Every member must pass entry requirements or the whole party is refused. On death rules inside dungeons, see [dungeons](dungeons.md#death-handling).

## API

```java
PartyService svc = RpgServices.parties();
Optional<Party> p = svc.partyOf(player);
Collection<Player> members = p.map(Party::members).orElseGet(List::of);
```

## Related

- [Chat](chat.md)
- [Guilds](guilds.md)
- [Dungeons](dungeons.md)
- [Skills](../core/skills.md)
