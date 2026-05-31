# Suite 19 (current)

← [Back to changelog index](../changelog.md)

---

### rpg-core `1.0.0`
- **Block holograms**: custom block definitions now support an optional `Hologram: "&6Text"` field (and `HologramYOffset: 1.2`). A `TextDisplay` entity is spawned above every placed instance of that block type, centered over the block, at the configured Y offset above its top surface. Holograms despawn when the block is broken and re-spawn after respawn cycles. On plugin enable, stale entities from the previous session are swept before fresh ones are created. Add to any block YAML: `Hologram: "&6⚗ Brewing Station"`.
- **Per-player GUI isolation confirmed**: `CookingGui`, `BrewingGui`, and `StationGui` each create a brand-new `Inventory` object per `open()` call. Two players clicking the same station block simultaneously receive independent inventories — no shared state, no interference. Added clarifying Javadoc to all three GUIs.

### rpg-regions `0.5.0`
- **Global region in `/region` commands**: `/region list` now shows `[global]` as the first entry (with its current flag count). `/region flag __global__ <flag> <value|clear>` is a new alias for `/region global flag` — admins can manage the world-wide region using the same flag command they use for named regions. Tab-complete on `/region flag` now suggests `__global__` as the first option.

### rpg-alchemy `0.2.0`
- **Brewing shift-click fix**: shift-clicking an ingredient from the player's inventory into the Brewing Station GUI now routes it to the first free input slot (same pattern as CookingGui). Previously the click was silently ignored.

### rpg-enchanting `0.2.0`
- **Enchanting/Anvil station shift-click fix**: shift-clicking an item from the player's inventory into the Enchanting Table or Custom Anvil GUI now places it directly into the input slot if it's empty. Previously the click passed through without being routed.

### All plugins (config docs)
- **Config examples everywhere**: added inline YAML comments with descriptions, valid ranges, and examples to every configurable field across all 20 `config.yml` files (rpg-core, rpg-combat, rpg-regions, rpg-economy, rpg-npcs, rpg-cooking, rpg-alchemy, rpg-enchanting, rpg-mining, rpg-foraging, rpg-fishing, rpg-farming, rpg-hud, rpg-chat, rpg-parties, rpg-guilds, rpg-accessories, rpg-holograms, rpg-quests, rpg-dungeons, rpg-admin). No behavior changes — docs only.

---

### rpg-core `0.9.0`
- **Potions fix**: `VanillaSuppressionListener` now defaults `vanilla-suppression.potions` to `false` — vanilla potions work correctly (effects apply, items consumed as expected). Previously the default `true` silently swallowed every potion effect while still consuming the item. Set to `true` in config to re-enable suppression when a proper RPG potion→status-effect converter is added.
- **Custom block auto-registration on place**: `/rpg block give` items now carry a `rpg_block_id` PDC tag. New `BlockPlaceListener` fires on `BlockPlaceEvent` (admin+creative only) and automatically calls `tagLocation()` + saves persistence — no more needing `/rpg block convert` after placing. Action bar confirmation shown on successful registration.
- **Tab completions** — `/rpg`: now completes all 10 subcommands (was only 5); `/rpg wand mode` now completes mode names; `/rpg particle delete/move` completes existing particle IDs; `/rpg loot-chest define` completes loot table IDs; `/rpg effects` completes online player names.

### rpg-regions `0.4.0`
- **Tab completions**: `/region global` now completes `info`/`flag`; `/region global flag` completes known flag names and `true`/`false`/`clear`; `/region info` no longer incorrectly suggests region IDs (it uses player location, not an ID argument); `/region flag` value arg now also suggests `clear`.

### rpg-quests `0.0.3`
- **Tab completions**: `/quest complete <player>` now completes online player names; `/quest complete <player> <questId>` completes quest IDs.

---

### rpg-core `0.8.0`
- **Damage NPE fix**: `DamageMath.statOf()` now null-guards the entity argument — fall damage, campfire, and other non-attacker damage sources no longer throw `NullPointerException`.
- **Damage pipeline priority**: `DamagePipelineListener.onDamage` moved from `LOWEST` to `NORMAL` (`ignoreCancelled = true` unchanged). This lets lower-priority listeners (e.g. NPC protection) cancel the event before the pipeline runs, eliminating damage indicators on protected entities.
- **Custom block break permission**: creative-mode bypass now requires `rpg.admin` in addition to `GameMode.CREATIVE`. Non-admin creative players can no longer break custom blocks; only `rpg.admin` admins can.

### rpg-npcs `0.3.0`
- **NPC damage indicators fixed**: `NpcProtectionListener` damage/targeting handlers moved from `HIGHEST` to `LOWEST` priority — they now cancel the event before `DamagePipelineListener` (at `NORMAL`) runs, preventing hit-indicator animations on NPC entities.
- **Double name tag fixed**: entity-style NPCs no longer have `customName` set on their body entity when the TextDisplay overlay is active. Hovering over the entity no longer shows a second name tooltip.

### rpg-cooking `0.2.0`
- **Slot layout redesign**: ingredient slots moved to row 0 center (slots 4, 5, 6); recipe tiles now start at slot 9 (row 1) and fill forward across the grid.
- **Shift-click fix**: shift-clicking an ingredient from the player's inventory now correctly routes it into the first free input slot instead of silently failing or landing on a pane tile.

---

### rpg-npcs `0.2.0`
- **Damage bug fix**: `NpcProtectionListener` cancels all damage and mob-targeting events for NPC entities at `HIGHEST` priority, and respawns the NPC after one tick if it somehow dies. Belt-and-suspenders on top of `setInvulnerable(true)`.
- **Fake player NPCs**: `EntityStyle: player` spawns a packet-only fake player with a custom `GameProfile`. Skins configured via raw texture `Value`/`Signature` or by `Name` (fetched async from Mojang API, cached). Not shown in tab list — brief ADD packet sent for skin load, removed after 2 ticks. New players on join receive the skin packet for all active fake player NPCs.
- **Banker behavior**: `Behavior.Type: banker` opens a deposit/withdraw GUI backed by `DataStore`. Bank balance persisted per-player per-NPC. Daily interest accrues at `DailyInterestPercent` on a configurable real-time interval. Requires `rpg-economy`.
- **`/npc setbehavior`** now accepts `banker [bankName]` and updates tab-complete to include it.

### rpg-api `0.2.0`
- Added `StationService` interface (`api/station/`) with `register(stationType, handler)` and `open(stationType, player, block)`. Accessible via `RpgServices.stations()`.

### rpg-core `0.7.0`
- **Station dispatch**: new `CoreStationService` implementation + `BlockInteractListener`. Right-clicking any custom block with `Interactable: true` and a non-empty `StationType` now routes through `RpgServices.stations()`. Addons register their GUI handler once in `onEnable`; rpg-core handles event cancellation.

### rpg-cooking `0.1.0`
- Station interaction now wired through `RpgServices.stations().register("cooking", ...)` — no longer needs a per-block-id config key. Removed `cooking-block` config key and `CookingStationInteractListener`.

### rpg-alchemy `0.1.0`
- Station interaction now wired through `RpgServices.stations().register("brewing", ...)`. Removed `brewing-block` config key and `BrewingStationInteractListener`.
