# Suite 19 — Closed

← [Back to changelog index](../changelog.md)

## Highlights

- **Ability trigger system** (`rpg-api 0.4.2`, `rpg-core 1.2.0`) — `~on_hit`, `~on_hurt`, `~on_jump`, `~passive`, plus `~on_attack`, `~on_kill`, `~on_block` (`rpg-core 1.5.0`). Mob triggers extended with `~onAttack`, `~onKill`, `~onJump`.
- **10 new built-in ability effects** (`rpg-core 1.3.0`) — `knockback`, `blink`, `chain`, `zone`, `shield`, `drain`, `mark`, `launch`, `freeze`, `restore_mana`. 5 showcase mobs added.
- **Ability DSL `chance{}` gate** (`rpg-core 1.5.1`, `rpg-api 0.5.1`) — inline probability gate; blocked flag propagated through pipeline; stacking = AND logic.
- **Loot pool system** (`rpg-core 1.4.0`, `rpg-api 0.4.3`) — named reusable loot pools; mobs reference by ID; vanilla XP + combat XP per pool.
- **Enchanting: vanilla XP cost** (`rpg-enchanting 0.5.0`) — `XpCost:` field wired; levels shown in GUI and deducted on apply.
- **NPC command overhaul** (`rpg-npcs 0.6.0`) — per-NPC entity types, in-game dialogue/shop editing, look-at-player, `/npc info`.
- **Timed cooking + brewing** (`rpg-cooking 0.4.0`, `rpg-alchemy 0.4.0`) — `CookTicks`/`BrewTicks` now drive a progress bar + DataStore-persisted mid-craft state.
- **New addon plugins** — `rpg-homes 0.1.0` (player homes + server warps) and `rpg-kits 0.1.0` (starter/reward kits).
- **Damage indicators polish** (`rpg-core 1.5.2`) — sin-arc position + linear scale shrink 1→0.
- **Resource pack auto-delivery** (`rpg-core 1.5.2`) — `resource-pack:` config block; sends pack on join.

---

## Granular dev log (archived)

!!! note "Archived format"
    The entries below are the granular per-change development log maintained during Suite 19.
    New entries are no longer added here.

---

### rpg-core `1.2.0` + rpg-api `0.4.2`
- **Ability trigger system**: item `Abilities:` entries now support a `~trigger ` prefix. Supported player triggers: `right_click` (default, backwards-compatible), `left_click`, `shift_right_click`, `shift_left_click`, `on_hit`, `on_hurt`, `on_jump`, `passive`. Active triggers (`right_click` etc.) gate on mana cost if the sequence includes `mana_cost{}`. Passive/proc triggers (`on_hit`, `on_hurt`, `on_jump`, `passive`) fire freely with no mana gate unless explicitly added.
- **Passive ability infrastructure**: `PassiveAbilityFirer` collects trigger-matched bindings from all equipped items + active set bonuses and fires them. New event listeners: `PlayerHitAbilityListener` (`on_hit`), `PlayerHurtAbilityListener` (`on_hurt`), `PlayerJumpAbilityListener` (`on_jump`). New task: `PlayerPassiveAbilityTask` fires `passive` bindings on a configurable interval (`abilities.passive-interval-ticks` in `config.yml`, default 20).
- **Left-click + shift variants**: `ItemAbilityListener` extended to handle `LEFT_CLICK_AIR`, `LEFT_CLICK_BLOCK`, and sneaking variants of both click types.
- **Armor set system**: sets defined in `plugins/rpg-core/sets/*.yml`. Each set names piece IDs and threshold tiers. `ArmorSetListener` detects piece count changes on armor events and writes to `CoreRpgPlayer.setSetBonusStats()` — a new Layer 2.5 in the stat pipeline (after equipment, before accessories). Set passive ability bindings are tracked per-player and queried by proc listeners.
- **Scale: support on set bonuses**: `Scale: 0.5` on a `SetBonus` multiplies all numeric ability params at load time, deriving weaker tiers automatically without duplicating effect strings.
- **Set lore on items**: `CoreRpgItem.toItemStack()` renders a set membership block showing set name + each tier's stat bonuses and trigger hints, above the rarity line.
- **New `RpgServices` entry**: `armorSets()` / `setArmorSets(ArmorSetRegistry)`.
- **New API types**: `PlayerAbilityTrigger`, `ItemAbilityBinding`, `ArmorSetDef`, `ArmorSetRegistry`, `SetBonus`.
- **`RpgItem` additions**: `triggeredAbilities()` (new canonical method), `setId()`. `abilities()` kept as backwards-compat alias returning right-click invocations.

### rpg-core `1.1.6`
- **GUI nav bar standard** on all GUIs: BankerGui, NPC shop expanded to 54 slots with a close button at slot 49.

### rpg-npcs `0.6.1`
- **GUI nav bar** added to BankerGui and NPC shop GUIs.

### rpg-enchanting `0.4.1`, rpg-cooking `0.3.1`, rpg-alchemy `0.3.2`
- **GUI pagination + redesign**: enchanting station paginated (14 enchants/page, prev/close/next nav bar). Cooking and brewing stations redesigned to 54-slot layout — ingredient slots at row 1 center (12, 13, 14), recipe tiles in rows 2–4, paginated nav bar in row 5.

### rpg-api `0.4.1`, rpg-core `1.1.5`, rpg-npcs `0.6.0`, rpg-trade `0.1.1`
- **GUI nav bar standard**: `GuiConfig.placeNavBar(inv)` / `placeNavBarNested(inv)` API. Close button (BARRIER, slot 49) on all top-level GUIs; back button (ARROW, slot 45) + close button (slot 53) on nested GUIs. PDC-tagged via `rpg:nav_action` key. `TradeGui` cancel button moved to slot 49.

### rpg-npcs `0.6.0`
- **NPC command overhaul**: new subcommands `/npc setentitytype`, `/npc setstyle`, `/npc setskin`, `/npc dialogue`, `/npc shop`, `/npc setlook`, `/npc info`. Full tab-complete including quest IDs (soft-dep reflection). `saveOnly()` for dialogue/shop edits avoids entity respawn flicker.
- **Per-NPC entity type**: `EntityType:` field in npc.yml. `setentitytype` validates against `LivingEntity` subtypes (excludes PLAYER).
- **Look-at-player task**: NPCs with `LookAtPlayers: true` (default via config) tick every `look-at-players.interval-ticks` ticks and rotate toward the nearest player within `LookRadius` blocks. Entity-style NPCs use Paper's `setRotation(yaw, pitch)`. Fake-player NPCs send `ClientboundMoveEntityPacket.Rot` + `ClientboundRotateHeadPacket` via reflection.

---

### rpg-dungeons `0.0.3`
- **Dungeon enter now teleports the player**: `TemplatePaster.run()` was missing exception handling around `dst.setBlockData()`. In Paper 1.21.4, block writes to freshly-generated void-world chunks can throw; the exception escaped the `while` loop, got swallowed by Bukkit's repeating-task runner, and the task retried the same coordinate forever — `onDone` was never called and the player was stuck at "Preparing dungeon...". Fix: wrapped per-block operations in `try/finally { advance(); }` so a failed block is always skipped and the paste completes. Also wrapped `onDone.accept()` in a try-catch so callback exceptions are logged at SEVERE rather than silently dropped.

### rpg-alchemy `0.3.1`
- **Potions fixed — effects now apply on drink**: `PotionDrinkListener` was incorrectly intercepting `PlayerInteractEvent`. In Paper 1.21.4 the `ServerboundUseItemPacket` is processed before that event fires, so cancelling there did not reliably suppress the vanilla drink path — the animation played, the item was consumed, and no RPG effects fired. Handler rewritten to intercept `PlayerItemConsumeEvent` instead: vanilla drives the 1.6-second animation; on completion we cancel the event (suppresses item removal + glass-bottle replacement + vanilla effects) and apply RPG status effects + manually reduce item count.
- **Example potion IDs corrected**: `strength_buff` → `strength_boost`, `heal_over_time` → `regen`, matching the status effect IDs actually defined in `rpg-core`'s `status-effects/example.yml`. Previously `CoreStatusEffectService.apply()` silently no-oped because the IDs were not in the registry.

### rpg-core `1.1.0`
- **`speed` stat wired**: `EquipmentListener` now sets `generic.movement_speed` on every equipment recalc. Formula: `0.1 × (1 + speed × speedPerPoint / 100)`, where `speedPerPoint` comes from `stats.speed-per-point` in `config.yml` (default 1.0). Applied on join and all gear changes.
- **`swing_range` stat wired**: `EquipmentListener` sets `entity_interaction_range` (melee reach) on every recalc. Formula: `3.0 + swingRange × blocksPerPoint` (config: `stats.swing-range-per-point`, default 1.0). `swing_range: 2` = 5.0-block reach.
- **`ferocity` stat wired**: `DamagePipelineListener` now fires extra melee swings after each hit. Each 100 ferocity = 1 guaranteed extra hit; the remainder is a fractional % chance. Extra hits deal the same `finalDamage` (crit already factored), fire `PostDamageEvent` for indicators, but skip knockback and lifesteal. Cap: `ferocity.max-extra-hits` (default 10).
- **Three new config sections**: `stats.speed-per-point`, `stats.swing-range-per-point`, and `ferocity.max-extra-hits` added to `config.yml`.

### rpg-core `1.0.7`
- **`projectile_speed` stat is now a % bonus** (was a direct multiplier). `projectile_speed: 25` now means 25% faster than vanilla, not 25×. Formula: `speedMult = 1.0 + stat / 100.0`. Zero/absent stat = vanilla arrow speed unchanged.

### rpg-core `1.0.6`
- **Player melee damage fix**: `DamagePipelineListener` now reads the player's RPG `DAMAGE` stat as the melee base, the same way mob attackers already did. Previously, `event.getFinalDamage()` was used — but since all vanilla attack-damage attributes are removed from custom items, that returned `1.0`, causing swords to deal ~1 damage regardless of stats.
- **Wand ability damage fix**: `beam_wand` and `glacial_staff` now have a `damage` stat (`30` and `50` respectively). Without it, `carriedDamage = 0` while holding the wand, making all `damage_multiplier`-based ability effects deal zero.
- **Damage indicators for ability hits**: `DamageEffect` and `ExplodeEffect` now fire `PostDamageEvent` after each damage application, so floating damage numbers appear for wand abilities and mob AoE explosions.

### rpg-core `1.0.5`
- **Hurt animation fix**: `CoreHealthService.damage()` now calls `entity.playHurtAnimation(0f)` after reducing HP. Previously, bypassing `EntityDamageEvent` meant vanilla never sent the red-flash status packet — arrows and mob abilities dealt damage silently with no visual feedback.
- **Mob ability exception logging**: `MobAbilityRuntime.cast()` now logs a warning when an ability chain throws instead of silently swallowing the error.
- **Example items — knockback added**: `aspect_of_test`, `iron_shortsword`, `voidblade`, `hunters_bow`, `beam_wand`, and `glacial_staff` now include a `knockback` stat so the mechanic is demonstrated out of the box.

### rpg-core `1.0.4`
- **AbilityLoader unknown-field warning**: loading an ability YAML that contains an unrecognized top-level field (e.g. `ManaCost: 50`, `CombatXpMultiplier: 1.0`) now logs a console warning naming the field and pointing to the correct pattern (`mana_cost{amount=N}` in `AbilitySequence`). Previously these fields were silently ignored — abilities appeared to have a mana cost in config but never deducted any mana at runtime.

### rpg-npcs `0.5.1`
- **NPC click fix — orphan sweep**: `NpcManager.loadAll()` now calls `sweepOrphanedEntities()` before `despawnAll()`, scanning every loaded world for entities carrying the `rpg_npc_id` PDC key and removing them. Previously, NPCs with `setPersistent(true)` survived server restarts and plugin reloads; each reload stacked a new copy on top, causing visual overlap and unreliable interaction resolution.
- **NPC click fix — entity type default**: changed default `display.body-entity` from `VILLAGER` to `ZOMBIE`. Paper's villager trade GUI can interfere with right-click handling even when `PlayerInteractEntityEvent` is cancelled.
- **NPC click fix — handler priority**: `NpcInteractListener.onInteract` changed from `LOW / ignoreCancelled = true` to `NORMAL / ignoreCancelled = false`. NPC interactions now fire even if a third-party plugin pre-cancelled the event.

### rpg-core `1.0.3`
- **Attack cooldown fix**: `CoreRpgItem.toItemStack()` now explicitly removes vanilla attribute modifiers (`ATTACK_DAMAGE`, `ATTACK_SPEED`, `ARMOR`, `ARMOR_TOUGHNESS`, `KNOCKBACK_RESISTANCE`) from every custom item's `ItemMeta`. Previously `HIDE_ATTRIBUTES` hid them from the tooltip but they still applied — holding an iron-based sword applied a `-2.4` ADDITION to `generic.attack_speed`, making our `setBaseValue()` resolve to a negative value and the attack bar never fill.
- **Currency drops from mob loot tables**: `CoreLootTable` now supports a `currency-rolls:` section (`{ chance, min, max }` entries). `MobLootListener` calls `rollCurrency()` after item rolls and deposits the result via `RpgServices.economy()` directly to the player's balance. Requires `rpg-economy`; silently skipped if not loaded. Update mob YAMLs to use `currency-rolls:` for coin drops instead of spawning coin item entities.

### rpg-mining `0.2.1`
- **Mining fatigue suppression fix**: default amplifier changed from `1` (Fatigue II — slows but doesn't prevent mining) to `255` (effectively infinite break time). Vanilla blocks can no longer be mined at all while holding an RPG gathering tool. Configurable in `plugins/rpg-mining/config.yml` under `mining-fatigue.amplifier`.

---

### rpg-trade `0.1.0` *(new plugin)*
- **`/trade <player>`** — sends a trade invite (30s expiry, configurable). Target accepts with `/trade accept`, declines with `/trade deny`. Either party can cancel at any time with `/trade cancel`.
- **Trade GUI** (54-slot): your 3×3 item offer on the left, the other player's items on the right (read-only). Coin amount set by clicking the sunflower button and typing in chat. Both players must click **Confirm Trade**; a configurable countdown (default 5s) then fires before items and coins swap atomically. Either player can click the button again during countdown to cancel.
- **Bait-and-switch prevention**: modifying your offer after clicking Confirm resets your confirmation state.
- **Coin safety**: balances are checked and deducted atomically at swap time; if either player can't afford their offer, all items are returned and the trade fails.
- **Item return on cancel**: all items in the GUI are returned to their owners on close, disconnect, or `/trade cancel`.
- **Permission**: `rpg.trade.use` (default: true).
- **Config**: `trade.countdown-seconds`, `trade.invite-expiry-seconds`, `trade.max-coins`.

### rpg-api `0.3.0`
- **`RpgItem.tradeable()`**: new default interface method returning `true`. Items with `Tradeable: false` in their YAML return `false`; the trade GUI rejects them with a message.

### rpg-core `1.0.1`
- **`Tradeable: false` item flag**: `CoreRpgItem` and `ItemLoader` support the new `Tradeable` field. Non-tradeable items show `&c✘ Not Tradeable` at the bottom of their lore.

---

### rpg-cooking `0.3.0`
- **GUI formatting pass**: title is now bold (`&6&l`). Background panes read from `rpg-core` `gui.background-material` config via `GuiConfig` instead of being hardcoded orange. Recipe tiles now suppress italic on all lore lines and show a `&8▶ &7Left-click to cook` action hint when the recipe is satisfiable.

### rpg-alchemy `0.3.0`
- **GUI formatting pass**: title is now bold (`&d&l`). Background panes read from `GuiConfig` instead of hardcoded purple. Recipe tiles now suppress italic on all lore lines and show a `&8▶ &7Left-click to brew` action hint when satisfied.

### rpg-enchanting `0.3.0`
- **GUI formatting pass**: titles are now bold (`&5&l`). Background panes read from `GuiConfig`. The enchant result button, reforge options, and upgrade options each show a `&8▶ &7Left-click to …` action hint in lore. `simple()` cleaned up to use the shared `LEGACY` field; new `simpleWithHint()` helper added for the hint pattern.

### rpg-npcs `0.4.0`
- **BankerGui formatting pass**: title is now bold (`&6&l`). Hardcoded `CYAN`/`GRAY` `fillGlass` calls replaced with `RpgServices.guiConfig().fillBackground(inv)` — background pane material is now admin-configurable. Deposit and withdraw buttons suppress italic on their display names and show `&8▶ &7Left-click to deposit / withdraw` action hints in lore. Removed unused `fillGlass` helper.

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
