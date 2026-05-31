# Trade (`rpg-trade`)

> **Status:** Working — Player-to-player GUI trade with coin transfers and non-tradeable item blocking.

Adds `/trade` for safe player-to-player item and currency exchange. Both parties confirm before anything transfers. Non-tradeable items (defined with `Tradeable: false` in item YAML) are blocked from the trade window.

## How it works

1. Player A runs `/trade PlayerB` — sends a trade request.
2. Player B sees a chat prompt to accept. They run `/trade accept` (or `/trade deny` to refuse).
3. A 54-slot GUI opens for both players simultaneously:
   - **Left side** — Player A's offered items and coin amount
   - **Right side** — Player B's offered items and coin amount
   - **Confirm button** — each player must click it to lock in their side
4. Once both players have confirmed, the trade executes atomically — items swap, coins transfer.
5. Either player can click **Cancel** at any time to abort. `/trade cancel` also works from chat.

## Non-tradeable items

Items defined with `Tradeable: false` in item YAML cannot be placed in the trade window. Attempting to do so shows an error message and the item bounces back. See [Items](../content/items.md) for the `Tradeable` field.

## Commands

| Command | Permission | Notes |
|---|---|---|
| `/trade <player>` | `rpg.trade.use` | Sends a trade request |
| `/trade accept` | `rpg.trade.use` | Accepts a pending incoming request |
| `/trade deny` | `rpg.trade.use` | Declines a pending incoming request |
| `/trade cancel` | `rpg.trade.use` | Cancels your active trade or pending request |
| `/trade reload` | `rpg.trade.admin.reload` | Reloads rpg-trade config |

`rpg.trade.use` defaults to `true` (all players).

## Config

`plugins/rpg-trade/config.yml`:

```yaml
request-timeout-seconds: 30     # how long before an unanswered request expires
confirm-required: true          # whether both players must click Confirm before transfer
```

## Related

- [Items (Tradeable flag)](../content/items.md)
- [Economy](economy.md)
