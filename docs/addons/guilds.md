# Guilds (`rpg-guilds`)

> **Status:** In progress — Persistent guilds via DataStore. v1 ships fixed ranks (`owner` / `officer` / `member`). Commands: `/guild create <name>`, `/guild invite`, `/guild accept`, `/guild kick`, `/guild promote`, `/guild demote`, `/guild leave`, `/guild disband`, `/guild info [name]`, `/guild list`, `/guild deposit <amount>`, `/guild withdraw <amount>`. Officers can invite/kick/withdraw; owner alone can promote/demote/disband. Creation cost via `rpg-economy` (soft-depend; free if economy isn't loaded). Guild bank is currency-only for v1 — items bank and tier-upgrades come later. Server-admin-configurable rank slot list, per-rank perm flags, and guild XP from member skill XP arrive in a polish slice.

Persistent player communities with bank, ranks, XP/level, and perks.

## Config

`plugins/rpg-guilds/config.yml`:

```yaml
creation-requirements:
  cost: 10000                    # currency from rpg-economy
  required-skills:
    combat: 10
  required-items:
  - { id: guild_charter, amount: 1, consume: true }

max-members: 50
member-cap-per-level: 2          # +2 max members per guild level (configurable)

# Server-defined rank slots (ordered low → high). Each guild owner can rename
# their guild's instance of these, but cannot add/remove slots.
ranks:
- { id: recruit,  default-name: "Recruit" }
- { id: member,   default-name: "Member" }
- { id: officer,  default-name: "Officer" }
- { id: elite,    default-name: "Elite" }
- { id: owner,    default-name: "Guild Master" }

# Default per-rank permissions inside the guild. Each guild owner overrides via /guild bank perms.
default-rank-perms:
  owner:   { invite, kick, promote, demote, bank.deposit, bank.withdraw, bank.upgrade, ranks.rename, disband }
  elite:   { invite, bank.deposit, bank.withdraw }
  officer: { invite, bank.deposit }
  member:  { bank.deposit }
  recruit: {}

xp-sharing:
  enabled: true
  rate-percent: 5                # 5% of each member's earned XP goes to the guild
guild-curve: "1000 * level ^ 1.5"
guild-max-level: 20
guild-level-perks:
  5:  { stats: { max_health: 5 }, member-cap-bonus: 5 }
  10: { stats: { strength: 5 } }

allow-friendly-fire: false

bank:
  tiers:
    1: { slot-count: 27, currency-cap: 10000, required-guild-level: 0, upgrade-cost: { coins: 0 } }
    2: { slot-count: 36, currency-cap: 100000, required-guild-level: 5, upgrade-cost: { coins: 50000 } }
    3: { slot-count: 54, currency-cap: 1000000, required-guild-level: 10, upgrade-cost: { coins: 500000, items: { guild_safe: 1 } } }
  audit-log-max-entries: 200
```

## Ranks

Server admin defines the **list of rank slots** (in `ranks`) — fixed across all guilds. Guild owner can **rename** their guild's instance of those slots via `/guild ranks`.

```
/guild ranks               # opens the rank rename GUI
```

Per-rank perms are also editable by the owner via `/guild ranks perms` (TBD GUI).

## Bank

A tiered shared inventory + currency pool.

- **Items**: a vault GUI with `slot-count` slots from the current tier
- **Currency**: a numeric pool capped at `currency-cap`
- **Upgrade**: requires `required-guild-level` and pays `upgrade-cost`
- **Access**: per-rank perms control who can deposit / withdraw / upgrade
- **Audit log**: every deposit/withdraw recorded, viewable in the GUI

## Guild XP / level

Members' personal skill XP also flows into the guild (at `rate-percent`). The guild has its own level via `guild-curve`. Guild level unlocks:

- Higher bank tiers (`required-guild-level`)
- More member slots (`member-cap-per-level`)
- Per-level perks (stats granted to all members, custom messages)

## Commands

| Command | Permission |
|---|---|
| `/guild create <name>` | `rpg.guilds.create` |
| `/guild invite <player>` | `rpg.guilds.invite` |
| `/guild kick <player>` | `rpg.guilds.kick` |
| `/guild promote <player>` | `rpg.guilds.promote` |
| `/guild demote <player>` | `rpg.guilds.demote` |
| `/guild leave` | `rpg.guilds.leave` |
| `/guild disband` | `rpg.guilds.disband` |
| `/guild info [guild]` | `rpg.guilds.info` |
| `/guild list` | `rpg.guilds.list` |
| `/guild bank` | `rpg.guilds.bank` |
| `/guild bank upgrade` | `rpg.guilds.bank.upgrade` |
| `/guild ranks` | `rpg.guilds.ranks` |
| `/chat guild` | `rpg.chat.use.guild` |

## Persistence

Stored via `DataStore`:

- Guild metadata (id, name, owner, created-at, level, total-xp)
- Members (uuid, rank-id, joined-at)
- Bank (items + currency, current tier)
- Audit log

## Related

- [Economy](economy.md) — guild bank uses Currency
- [Chat](chat.md) — guild channel
- [Skills](../core/skills.md) — guild XP flows from member skill XP
- [Parties](parties.md) — short-lived alternative
