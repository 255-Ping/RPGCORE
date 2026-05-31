# Guilds (`rpg-guilds`)

> **Status:** In Progress — Persistent guilds via DataStore. v1 ships fixed ranks (`owner` / `officer` / `member`). Guild XP from member skill gains, configurable guild level curve, and per-stat perk formulas are implemented. Commands: `/guild create <name>`, `/guild invite`, `/guild accept`, `/guild kick`, `/guild promote`, `/guild demote`, `/guild leave`, `/guild disband`, `/guild info [name]`, `/guild list`, `/guild deposit <amount>`, `/guild withdraw <amount>`. Officers can invite/kick/withdraw; owner alone can promote/demote/disband. Creation cost via `rpg-economy` (soft-depend; free if economy isn't loaded). Planned: tiered bank, configurable rank slots, per-rank perm flags, custom rank renaming.

Persistent player communities with bank, ranks, XP/level, and perks.

## Config

`plugins/rpg-guilds/config.yml`:

```yaml
# Currency cost to create a guild (via rpg-economy; free if economy not loaded).
creation:
  cost: 10000

max-members: 50

invite-timeout-seconds: 60

# XP sharing — a portion of every member's earned skill XP flows into the guild pool.
xp-sharing:
  enabled: true
  rate-percent: 10              # percent of each member's skill XP that flows to the guild

# XP required to advance the guild from 'level' to 'level+1'.
# Expression variable: level (1-based current level).
guild-curve: "level * level * 1000"

# Highest attainable guild level.
guild-max-level: 25

# Stat bonuses granted to every guild member, scaling continuously with guild level.
# Each key is a stat ID; the value is an expression with 'level' as a variable.
# Remove or comment out any entry to disable that perk.
guild-perks:
  max_health: "level * 5"
  strength: "level * 2"
  defense: "level"
```

> **Planned additions to config:** tiered bank (`bank.tiers`), per-rank permissions (`default-rank-perms`), server-defined rank slots (`ranks`), discrete milestone perks per level (`guild-level-perks`), `allow-friendly-fire`.

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

| Command | Permission | Notes |
|---|---|---|
| `/guild create <name>` | `rpg.guilds.create` | Costs `creation.cost` coins if economy loaded |
| `/guild invite <player>` | `rpg.guilds.invite` | Owner/officer only |
| `/guild accept` | `rpg.guilds.accept` | Accept a pending invite |
| `/guild kick <player>` | `rpg.guilds.kick` | Owner/officer only; can't kick owner |
| `/guild promote <player>` | `rpg.guilds.promote` | Owner only → officer |
| `/guild demote <player>` | `rpg.guilds.demote` | Owner only → member |
| `/guild leave` | `rpg.guilds.leave` | |
| `/guild disband` | `rpg.guilds.disband` | Owner only; broadcasts to members |
| `/guild info [guild]` | `rpg.guilds.info` | No arg = your own guild |
| `/guild list` | `rpg.guilds.list` | |
| `/guild deposit <amount>` | `rpg.guilds.bank` | Any member; deducted from personal balance |
| `/guild withdraw <amount>` | `rpg.guilds.bank` | Officer/owner only |
| `/guild bank upgrade` | `rpg.guilds.bank` | _(planned — not yet implemented)_ |
| `/guild ranks` | _(planned)_ | _(planned — not yet implemented)_ |
| `/guild reload` | `rpg.guilds.admin.reload` | Op only |
| `/chat guild` | `rpg.chat.use.guild` | Registered by rpg-chat if loaded |

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
