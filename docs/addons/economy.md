# Economy (`rpg-economy`)

> **Status:** In Progress — `rpg-economy` module ships. Single primary currency configurable in `config.yml` (id, prefix, suffix, decimals, starting balance, max balance). Commands: `/balance`, `/pay`, `/eco set|add|remove|reset`, `/baltop` (all with documented permissions). Balances persist via core's `DataStore` as one record per UUID under `balances/`. Vault provider integration not yet shipped — non-suite plugins can use the `Economy` API from `rpg-api` directly.

Currency, balances, payments. Replaces the previously-considered `CURRENCY` ItemType.

## Currency model

- `Currency` is a registry entry. Single primary currency out of the box; the system supports multiple currencies via the registry (e.g., dungeon-coins from `rpg-dungeons`).
- Balances are stored as `BigDecimal` for precision.
- Display precision is configurable (default 0 decimals).

## Config

`plugins/rpg-economy/config.yml`:

```yaml
currency:
  id: coins                      # internal id
  display-singular: "coin"
  display-plural: "coins"
  prefix: ""                     # e.g. "$" or "✦"
  suffix: " coins"
  decimals: 0
  starting-balance: 100
  max-balance: 1000000000

pay:
  allow-self: false
  min-amount: 1
  cooldown-seconds: 5

baltop:
  page-size: 10
```

## Commands

| Command | Permission |
|---|---|
| `/balance [player]` | `rpg.economy.balance` / `rpg.economy.balance.other` |
| `/pay <player> <amount>` | `rpg.economy.pay` |
| `/eco set <player> <amount>` | `rpg.economy.admin.set` |
| `/eco add <player> <amount>` | `rpg.economy.admin.add` |
| `/eco remove <player> <amount>` | `rpg.economy.admin.remove` |
| `/eco reset <player>` | `rpg.economy.admin.reset` |
| `/baltop [page]` | `rpg.economy.baltop` |

## Coin drops on death

When a player dies under a death tier that drops coins, the amount is removed from their balance and spawned as one or more pickup-able **coin pile** items in the world. Picking up a coin pile credits the picker's balance.

Config (in core, under `death-rules`):

- `drop-coins-amount: number | "all" | "percent"`
- `drop-coins-percent: <0..100>`
- `drop-coins-max: <int>`
- `drop-coins-pickup: true | false`

See [damage pipeline](../core/damage.md#death-rules).

## Vault compatibility (planned)

`rpg-economy` registers itself as a Vault economy provider so non-suite plugins (shops, perks) can read/write balances. Soft-depend on Vault; works fine without it.

## API

```java
Economy eco = RpgServices.economy();
BigDecimal bal = eco.balance(player, "coins");
eco.deposit(player, "coins", BigDecimal.valueOf(100));
eco.withdraw(player, "coins", BigDecimal.valueOf(50));
boolean ok = eco.transfer(from, to, "coins", BigDecimal.valueOf(10));
```

## Related

- [Damage / death rules](../core/damage.md#death-rules)
- [Guild bank](guilds.md#bank)
- [Stats](../stats.md)
