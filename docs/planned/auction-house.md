# Planned: Auction House

**Status:** Planned — not yet implemented.

## Overview

A player-driven marketplace where players can list items for sale at custom prices.
Buyers browse active listings and purchase with in-game currency.

## Planned Features

- List any item stack with a custom price (configured min/max price bounds per item or globally)
- Browse listings filterable by item name, category, or seller
- Coin amount input via **sign UI** (Minecraft sign entry) — player clicks "Set Price" and a sign popup appears with "Price:" on line 1; player types the amount
- Listing expiry — unsold listings expire after a configurable duration and items are returned
- Configurable listing fee (flat or percent of price)
- `/ah` command opens the main GUI
- Admin commands: `/ah list <player>`, `/ah remove <listing-id>`, `/ah wipe`

## GUI Layout (planned)

```
[ Item Grid — active listings ]    [ Filter by: Name / Category ]
[ Prev Page ]  [ Page N/M ]  [ Next Page ]
[ My Listings ]    [ Create Listing ]    [ Expired Returns ]
```

## Config Shape (planned)

```yaml
auction-house:
  listing-fee-percent: 5
  max-listing-duration-game-hours: 72
  max-listings-per-player: 20
  price-bounds:
    global-min: 1
    global-max: 1000000000
```

## Sign Entry

When entering a coin amount (price, bid, buyout) the server opens a sign for input.
The sign shows `Price:` on line 1 and a placeholder on line 2.
On sign submission, the value is parsed and validated; invalid input re-opens the sign with an error hint.

Reference implementation: `SurvivalCore` auction house sign handler.
