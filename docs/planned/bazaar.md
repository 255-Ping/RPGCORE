# Planned: Bazaar

**Status:** Planned — not yet implemented.

Reference implementation: https://github.com/255-Ping/SurvivalCore (Bazaar module)

## Overview

A server-controlled shop where administrators configure fixed-price buy/sell listings
for items. Unlike the auction house, prices are set by the server, not players.
Players can instantly buy or sell configured items at any time.

## Planned Features

- Admin-configurable item listings (item id, buy price, sell price, stock limit or unlimited)
- Players `/bazaar` to open the GUI browser
- Items organized in configurable categories with icons
- Each listing shows buy price, sell price, and current stock (if limited)
- Stock replenishment on configurable interval (or unlimited)
- Per-item toggles: buy-only, sell-only, or both
- Coin amounts entered via sign UI (same mechanism as auction house)
- Admin commands: `/bazaar reload`, `/bazaar stock <item> <amount>`, `/bazaar add`, `/bazaar remove`

## Config Shape (planned)

```yaml
bazaar:
  categories:
    - id: consumables
      display-name: "&aConsumables"
      icon: POTION
      items:
        - item: health_potion
          buy-price: 500
          sell-price: 250
          stock: -1        # -1 = unlimited
        - item: mana_potion
          buy-price: 400
          sell-price: 200
          stock: -1
    - id: materials
      display-name: "&6Materials"
      icon: IRON_INGOT
      items:
        - item: iron_ore
          buy-price: 50
          sell-price: 20
          stock: 1000
          restock-interval-game-hours: 24
          restock-amount: 500
```

## GUI Layout (planned)

```
[ Category tabs across top ]
[ Item Grid — listings for selected category ]
[ Prev Page ] [ Page N/M ] [ Next Page ]
[ Balance: $X ]
```

Each listing slot shows:
- Item icon
- Display name
- Buy: $X / Sell: $Y
- Stock: N remaining (or ∞)

Clicking a listing opens a confirm dialog or sign entry for quantity/amount.
