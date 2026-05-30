# Parties (`rpg-parties`)

> **Status:** In progress — Session-only parties working. `/party create|invite|accept|kick|promote|demote|leave|disband|list` all wired. Owner/moderator/member ranks enforced. Invites time out (default 60s). On player quit, the leaver is removed; on owner quit, ownership transfers to the highest-rank remaining member (unless `disband-on-owner-leave: true`). `PartyService` API exposed via `RpgServices.parties()`. XP sharing live — `SkillXpAwardEvent` distributes bonus XP to in-range party members per the configurable `split-formula`.

Session-only player groups. No persistence — parties dissolve on owner disconnect (configurable). For persistent groups, see [guilds](guilds.md).

## Config

`plugins/rpg-parties/config.yml`:

```yaml
max-size: 5
disband-on-owner-leave: false
invite-timeout-seconds: 60

xp-sharing:
  enabled: false                 # opt-in
  scope: all-skills              # all-skills | combat-only | list
  skills: []                     # used when scope: list
  range-blocks: 64               # 0 = unlimited (same world only)
  split-formula: "amount / party_size"
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

When `xp-sharing.enabled`, each XP award fires a bonus to every in-range party member via `SkillXpAwardEvent`. The earner keeps their full XP; other members receive the `split-formula` result as a bonus. A `ThreadLocal` guard prevents recursive re-entry.

**Formula variables:**

- `amount` — original XP amount
- `party_size` — total in-range members including the earner

**Examples:**

| Formula | Meaning |
|---|---|
| `amount / party_size` | Each other member gets `1/n` of the XP (default — scales down with party size) |
| `amount * 0.5` | Each other member always gets 50% bonus, regardless of party size |
| `amount` | Full XP to everyone (no penalty) |

**Scope options** (`xp-sharing.scope`):

- `all-skills` — every skill shares (default)
- `combat-only` — only the `combat` skill shares
- `list` — only the skill IDs listed under `skills:` share

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
