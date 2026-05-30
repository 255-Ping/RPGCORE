# Planned: GUI Overhaul

**Status:** Planned — not yet implemented.

## Overview

Most player-facing command interfaces will be replaced or supplemented with
inventory-based GUIs. This document outlines the planned GUI conversions
and the input systems that support them.

---

## Input Systems

### Chat Entry
Used when free-text input from the player is needed (names, messages, search queries).

Flow:
1. GUI is temporarily closed (or kept open via packet trickery)
2. Server sends a chat prompt (e.g., `&aType the player name to invite:`)
3. Next chat message from that player is captured by a `AsyncChatEvent` listener
4. Input is validated; on error, the prompt is re-sent with an error hint
5. GUI re-opens with the result applied

Use cases: party invite, guild invite, rename, search/filter fields.

### Sign Entry
Used when a numeric value is needed (prices, amounts, quantities).

Flow:
1. Server opens a virtual sign via the `PacketPlayOutOpenSignEditor` NPC packet
2. Sign shows a label on line 1 (e.g., `Enter Price:`) and a placeholder on line 2
3. Player fills in the value and confirms the sign
4. Server reads `PacketPlayInUpdateSign` and parses the numeric value
5. Invalid values show an error message and re-open the sign

Use cases: auction house listing price, bazaar quantity, trade offer amounts.

Reference implementation: `SurvivalCore` sign editor handler.

---

## Planned GUI Conversions

### Party GUI (`/party`)
- Main panel: member list with roles, status (online/offline), combat status
- Invite: chat entry — player types name, invite is sent, target sees clickable accept/deny chat
- Kick/Leave: confirmation dialog in GUI
- Promote/Demote: click role indicator in member list

### Guild GUI (`/guild`)
- Tabs: Members, Info, Bank (future), Settings (officer+)
- Invite: chat entry for player name
- Kick/Promote/Demote: click member in list → action menu
- Edit description/name: chat entry (officer+)

### Quest GUI (`/quests`)
- Available quests tab, active quests tab, completed tab
- Click quest → detail view: objectives, rewards, accept/abandon button
- Progress bars for each objective

### Hologram Creation GUI (`/holo create`)
- Instead of a long command string, open a GUI:
  - Line editor: click line slot → chat entry for line text
  - Add/remove/reorder lines
  - Preview hologram name
  - Confirm to create at player location

### Admin Spawner GUI (`/spawner`)
- Instead of `/spawner set <id> <field> <value>`, open a GUI showing all fields
- Click a field slot → sign entry (numeric) or chat entry (string)
- Changes saved immediately on close

---

## Non-GUI Inputs That Stay as Commands

Some actions intentionally remain as commands for speed:
- `/heal`, `/fly`, `/gmc` etc. (single-action admin commands)
- `/balance`, `/pay` (simple economy lookups)
- `/skill`, `/stats` (read-only display, may get GUI later)
