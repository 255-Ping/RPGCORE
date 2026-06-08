# Suite 21 — In Progress

← [Back to changelog index](../changelog.md)

_Suite 21 opened with the addition of `rpg-crafting` and `rpg-smelting`. The suite is still open; entries below are compiled as work lands._

---

## Notable changes

### rpg-parties 0.4.0 — Party GUI

- **`/party` (no args)** now opens a 54-slot inventory GUI instead of showing a text usage hint.
- **In-party layout:** Party Info at slot 4 (member count, party ID), Invite button at slot 8, up to 21 member cards in rows 2–4, Leave/Disband at slot 53, Close at slot 49.
- **Member cards:** PLAYER_HEAD skulls via `SkullMeta.setOwningPlayer`; name shows `[Owner]`/`[Mod]`/`[Member]` prefix in colour + online/offline status + HP percentage.
- **Invite flow:** click Invite → inventory closes → one-tick scheduler delay → sign-entry for player name → invite sent → GUI reopens. A `pendingInvite` guard prevents `InventoryCloseEvent` from tearing down GUI state during the sign-entry sequence.
- **Promote / Demote:** left-click a member card (owner only) — toggles mod ↔ member with a chat confirmation.
- **Kick:** right-click a member card (owner or mod, online non-owner) → confirmation overlay at slots 20/22/24 replaces all GUI content.
- **Leave / Disband:** slot 53 shows Leave (non-owner) or Disband (owner); both trigger the same confirmation overlay before acting.
- **No-party mode:** single Create Party button at slot 22; clicking calls `PartyManager.create`.
- `onQuit` handler cleans all per-player state (`openInvs`, `slotToMember`, `pendingConfirm`, `pendingInvite`) to prevent memory leaks on disconnect.

### rpg-core 1.10.14 — Player Profile Command

- **`/profile [player]`** — opens a 54-slot profile GUI. No argument = your own profile; with argument = another player's (requires `rpg.profile.view.others`).
- **GUI layout:** player skull (with party membership), 8 skill levels (Combat / Mining / Foraging / Fishing / Farming / Cooking / Alchemy / Enchanting), balance (if rpg-economy is loaded), 3 most-recently-unlocked achievements (definition order), a "View Stats →" button that drills into StatsGui with a Back callback, and a "Trade →" button when viewing another player.
- Economy, achievement, and party sections degrade gracefully (show "unavailable") if those services aren't loaded.
- **New permissions:** `rpg.profile.view` (default: true), `rpg.profile.view.others` (default: op).

### rpg-parties 0.3.0 — Party HP Action-Bar HUD

- Each online party member with at least one other online teammate now receives a repeating action-bar showing teammates' current HP percentages: `❤ Alice 85%  |  ❤ Bob 23%`.
- Color coding: **green** > 70%, **yellow** 30–70%, **red** < 30%.
- Controlled by two new config keys in rpg-parties `config.yml`:
  - `party-hud.enabled: true` — toggle the HUD on/off.
  - `party-hud.interval-ticks: 20` — refresh rate (20 ticks = 1 second).
- Solo members (party of 1) see nothing; the action bar is only sent when there is at least one other online teammate to display.

### rpg-core 1.10.13 — Ability DSL: Numeric Variables (Tier 2)

- **7 new numeric-variable ability effects**, stored as doubles in entity PDC under `rpg_var_<name>`. Auto-cleared on entity removal (death / logout). Available for both mob and item abilities.
  - `set_var{name,value}` — set variable to an exact value
  - `increment{name,amount=1,max=}` — add `amount`; clamp to `max` if specified
  - `decrement{name,amount=1,min=0}` — subtract `amount`; clamp to `min`
  - `reset{name}` — set variable to 0
  - `if_var_gte{name,value}` — gate: chain continues only if var ≥ value
  - `if_var_lte{name,value}` — gate: chain continues only if var ≤ value
  - `if_var_eq{name,value}` — gate: chain continues only if var ≈ value (within 1e-9)
- Gate effects respect the existing `ctx.isBlocked()` mechanism (same as `chance{}`, `if_health_below{}`).
- Static `VarEffect.read(entity, name)` lets other effects query a variable value programmatically.
- **4 showcase abilities** added to `abilities/example.yml`: `combo_strike` (3-hit combo), `enrage_stacks` (mob enrage cap), `enrage_hit` (companion hit effect), `escalating_beam` (charge-up wand).

### rpg-quests 0.1.0 — Quest Chains + Repeatable Quests

- **`Requires:` field** — scalar string or YAML list of quest IDs. A quest cannot be accepted until every listed prerequisite quest is in the player's completed list. Enables multi-stage quest chains.
- **`Repeatable: true` + `CooldownSeconds: N`** — after completion a repeatable quest can be re-accepted once the cooldown elapses. Remaining time is formatted as a human-readable string (`1d 3h`, `45m`, `30s`). Repeatable quests stay in the `completed` list between runs so they continue to satisfy chain prerequisites on other quests.
- **3 new messages** in `messages.yml`: `quest.already-completed`, `quest.requires-quest`, `quest.cooldown`.
- **Example quests updated**: `forest_intro → goblin_menace → goblin_hunt_ii` three-quest chain; `daily_ore_run` 24-hour repeatable quest.
- **Bug fix**: `complete()` now guards against adding duplicate IDs to the `completed` list (was possible if a quest was force-completed via command while already present).

### rpg-core 1.10.12 — Mob Factions + AI Goals

- **`Faction:` field on mob YAML.** Any mob can now declare `Faction: <string>` — a plain label that identifies which group it belongs to. `"player"` is reserved and matches all players. No registry needed; factions are purely string comparisons.
- **`AiGoals:` ordered goal list.** Replaces the single `profile:` kind for mobs that declare it. Goals are evaluated top-to-bottom each AI tick; the first one that acts wins. Mobs without `AiGoals:` continue using `MobAiProfile.Kind` unchanged.
- **Goals implemented:** `attack_player`, `attack_faction{faction,range}`, `defend_faction{faction,radius}`, `assist_faction{faction,radius}`, `flee_from{faction,range,healthThreshold}`, `call_for_help{faction,radius}`, `guard_radius{radius}`, `idle`.
- **`call_for_help` is event-driven** — fires immediately on hurt (no tick lag), alerting idle nearby allies without interrupting already-engaged mobs.
- **`guard_radius` tracks spawn location** in memory on first AI tick; clears target when mob drifts outside the leash radius.
- **Hysteresis on target switching** — valid targets already within `range × 1.5` are kept to prevent jitter when mobs dance near range boundaries.
- **`flee_from`** clears the combat target and applies a velocity push away from the nearest threat each tick. Flee only activates at or below `healthThreshold` HP%.
- **`FactionAlertMap`** — shared `ConcurrentHashMap<victimUUID, attacker>` populated by `MobAbilityEventListener` on every RPG hurt event; stale entries pruned lazily by liveness check and explicitly on mob death.
- **Three showcase mobs** added to `mobs/example.yml`: `forest_guard` (guards faction — hunts undead, assists allies, guard_radius leash), `undead_minion` (undead faction — attacks players, calls for help), `cowardly_witch` (flees players when ≤40% HP).

### rpg-core — Remove vanilla XP bar config stub; add status effect examples

- **`vanilla-xp-bar` config key removed.** The setting was never implemented in Java (no `player.setExp()` call existed) — it was documentation-only, making it a misleading stub. The vanilla XP bar is now left untouched so `rpg-enchanting`'s level cost display works correctly. `docs/core/vanilla-suppression.md` updated: XP row in the "Repurposed vanilla bars" table now notes the bar is intentionally unmodified.
- **Six new status effects in `status-effects/example.yml`:** `weakness` (−30% damage, −20 strength), `vulnerability` (−35% defense, −10 true_defense), `mana_drain` (−40% max_mana, −80% mana_regen), `blindness` (−60% crit_chance, −40% magic_find), `speed_boost` (+40% speed), `thorns` (+20 defense, +5 true_defense — reflect mechanic needs a code hook to complete).

### rpg-crafting — Fill out example recipes

- Replaced the two commented-out stubs in `recipes/example.yml` with four live recipes that cover all four recipe patterns:
  - `shaped_iron_shortsword` — shaped, vanilla ingredients → custom RPG item
  - `shaped_crude_iron_ingot` — shaped, vanilla → custom (9 nuggets)
  - `shapeless_antidote_flask` — shapeless, custom + vanilla → custom
  - `shaped_red_gems` — shaped, multi-amount output (3× `red_gem`)

### rpg-core 1.10.11 — Loot tables consolidated into loot pools

- The separate `loot-tables/` folder, `LootTableRegistry`, `CoreLootTableRegistry`, and `LootTableLoader` have been removed. There was only one loot system from this point on: **loot pools** (`loot-pools/*.yml`, `LootPoolRegistry`).
- `LootTable: <id>` (plain-string form in mob YAML) is now **deprecated** — it is still accepted but logs a warning and is treated as an alias for `LootPool: <id>`. Update to `LootPool: <id>` at your convenience.
- Inline `LootTable:` (block form inside a mob YAML) is unchanged and continues to work as before.
- `LootChestRegistry` and `/rpg loot-chest define` tab completion now resolve against `lootPools()` instead of the removed `lootTables()`.
- Docs: `loot-tables.md` replaced with a migration guide; content index updated to point to `loot-pools.md`.

### rpg-accessories 0.1.1 — Family stacking + in-bag upgrade button

- **Family stacking:** `ACCESSORY` items now support an `Accessory: { Family:, Stacking: }` block. Three stacking modes: `highest` (default — only the best copy of a family counts), `sum` (every copy adds stats), `independent` (sum + each copy fires its own passive ability hooks). Items without a family always stack independently.
- **Upgrade button:** the last slot of the bag is now a permanent UI button — a NETHER_STAR showing the current/next tier and upgrade cost. Clicking it charges the player and reopens the bag at the new size. The button slot is never saved or treated as an accessory slot. Usable slots per tier: 8 / 17 / 26 / 35 / 44 / 53.
- `/accessories upgrade` also reopens the bag automatically after a successful upgrade.
- `AccessoryService.aggregateStats` is now family-aware; the stat-skip for the button slot is baked in.

### rpg-core 1.10.10 — Cooldown gate blocks the ability chain with action-bar feedback

- `cooldown{}` now acts as a **gate**: if the ability key is on cooldown when the effect fires, the chain is blocked (`ctx.setBlocked(true)`) and the caster sees an action-bar message — `Ability on cooldown — X.Xs remaining` (red + yellow, proper Adventure Component). Previously the effect only SET the cooldown and never checked it.
- Remaining time is formatted as a whole number when it falls on an exact second (`3s`) and with one decimal otherwise (`3.5s`).

### rpg-holograms 0.0.5 — Animated holograms, `info`/`set` subcommands, `line list` op

- **`Animated: true` + `FrameInterval: N`** YAML fields on hologram definitions. When `Animated: true`, all entries in `Lines` become **animation frames**: a single `TextDisplay` entity cycles through them at the configured tick interval. A single global 1-tick `BukkitTask` drives all animated holograms via per-hologram `frameTicks`/`frameIndices` maps — no per-hologram task overhead.
- **`/holograms info <id>`** subcommand: prints id, world + coordinates, animated status, frame interval, line/frame count, and all line content.
- **`/holograms line list <id>`** op: lists all lines with `[index]` prefix.
- **`/holograms set <id> animated <true|false>`**: toggle animation on/off and respawn the hologram.
- **`/holograms set <id> frameinterval <ticks>`**: adjust frame rate without respawn.
- **Tab completions** updated: `info` and `set` now appear in subcommand completions; `set` arg-3 completes with property names (`animated`, `frameinterval`); `set animated` arg-4 completes with `true`/`false`.
- `config.yml`: `animation-interval: 20` default added.

### rpg-cooking 0.4.1 — Dedicated output slot + offline timer advancement

- **Output slot** (GUI slot 16): a PDC-tagged gray-dye placeholder occupies the slot until the craft completes. The player must click the slot to collect the finished item.
- **Auto-collect on close**: if the output slot holds a finished item when the GUI closes, it is automatically moved to the player's inventory.
- **New craft blocked**: clicking a recipe tile is rejected if the output slot already holds a finished item; `cook.collect-output` message sent.
- **Offline advancement**: `timestamp_ms` (real epoch ms) saved alongside `elapsed_ticks` on GUI close. On reopen, `offlineTicks = (now - savedMs) / 50` is added to elapsed — cooking continues while the player is offline or the server is restarted.
- **Arrow** (slot 15): orange-dye decorative indicator between ingredients and output.
- New message key `cook.collect-output` in `messages.yml`.

### rpg-alchemy 0.4.2 — Dedicated output slot + offline timer advancement

- Same pattern as rpg-cooking 0.4.1: output slot at GUI slot 16, arrow at slot 15 (purple dye), offline `timestamp_ms` advancement, auto-collect on close, new-brew blocked while slot occupied.
- New message key `brew.collect-output` in `messages.yml`.

### rpg-smelting 0.1.1 — Dedicated output slot + offline timer advancement

- Same pattern: output slot at GUI slot 15, arrow at slot 14 (orange dye), offline `timestamp_ms` advancement, auto-collect on close, new-smelt blocked while slot occupied.
- DataStore record now includes `timestamp_ms` alongside `recipe_id` and `elapsed_ticks`.
- New message key `smelt.collect-output` in `messages.yml`.

### rpg-core 1.10.18 — Admin Spawner GUI (`/spawner edit`)

- **`SpawnerGui`** — new 54-slot inventory GUI for editing all spawner fields. Opened via `/spawner edit <id>` (requires `rpg.spawners.admin.edit`).
- **Slot layout**: slot 4 SPAWNER title (id + mob), slot 10 max-alive (COMPARATOR), slot 12 cooldown-ticks (CLOCK), slot 13 spawn-radius (COMPASS), slot 16 continuous toggle (LIME/GRAY_DYE), slot 21 min-level (LIME_DYE), slot 23 max-level (RED_DYE), slot 24 location read-only (GRASS_BLOCK), slot 49 close (BARRIER).
- **Numeric fields**: click opens a sign-entry prompt via `SignInputService`; validated against min/max bounds; saved immediately via `SpawnerManager.saveOne`.
- **Continuous toggle**: click flips the boolean and updates the item in-place; no sign entry needed.
- **`/spawner set <id> <field> <value>`** command also documented (was previously undocumented): direct CLI editing of the same fields without opening the GUI.
- Tab completion: `edit` added to subcommand list; spawner IDs complete for `edit`.

### rpg-holograms 0.0.4 — Tab completions for `/holograms`

- All `/holograms` subcommands now offer tab completions.
- Subcommand name at arg 1; hologram IDs (from the live registry) at arg 2 for `delete`, `tp`, `move`; line ops (`add`/`set`/`remove`) at arg 2 for `line`; hologram IDs at arg 3 for line ops; existing line indices at arg 4 for `line set`/`line remove`.
- All completions are case-insensitive prefix-filtered against what the player has typed so far.

### rpg-alchemy 0.4.1 — Configurable return item after drinking a potion

- New global config key `drink-return.item` in `config.yml`. Accepts `none` (default — glass bottle is already suppressed), `glass_bottle`, any vanilla Material name, or any rpg-core item id.
- New per-potion `ReturnItem:` field in `potions/*.yml`. When present it overrides the global default for that specific potion. Absent means fall through to global config.
- Resolution order: RPG item registry → vanilla Material → warning + nothing. Leftover items that don't fit the inventory are dropped naturally.

### rpg-core 1.10.9 — Cooldown time in ability lore

- `cooldown{ticks=N}` bindings now display the duration in the ability's lore hint: `(Right-click | 5s cd)`. Whole-second values show as `5s`; fractional as `3.5s`. Bindings without a cooldown are unchanged.

### rpg-core 1.10.8 — Vanilla XP split to damagers

- On RPG mob death, vanilla XP (from `XP:`, inline `exp:`, and pool `exp:` fields) is no longer dropped as orb entities. Instead it is split directly to each damager proportional to damage dealt via `player.giveExp()`. Players offline at kill time receive nothing.

### rpg-core 1.10.6 — Item type label + configurable stat order

- **Item type label in lore:** the item's type (e.g. `Sword`, `Armor`, `Wand`) now renders as dark-gray italic on the first lore line, followed by a blank line before stats.
- **`stat-order.yml`:** controls which stats appear first in lore per item type. Shipped with sensible defaults for all `BuiltinItemType` values. Edit the file and run `/rpg reload` — no restart needed. Stats not listed appear after configured ones, alphabetically.

### rpg-core 1.10.5 — Item Browser GUI

- `/rpg items` opens a 54-slot paginated inventory GUI listing every registered item.
- Filters: cycle item Type, cycle Rarity, sign-entry Search.
- Click a card to receive ×1; shift-click to receive ×64 (requires `rpg.core.item.give`).
- Inventory updates in-place (no close/reopen flicker).
- New permissions: `rpg.core.items.browse` (browse), `rpg.core.item.give` (give from browser).

### rpg-core 1.10.4 — `~on_login` trigger + beam pierce cap

- **`~on_login`** (`rpg-api 0.5.5`): item ability trigger that fires once when the holding player joins. Useful for login buffs, stat refreshes, or conditional unlocks.
- **`pierce_cap` on `beam{}`:** controls how many entities the beam hits before stopping. Default `1` (original behaviour). Set `0` for unlimited. Knockback applies to all hits; `ctx.target` is the first hit.

### rpg-fishing 0.1.0 — Fishing content slice

- Custom catch tables loaded from `catch-tables/*.yml`.
- `FISHING_FORTUNE` boosts fortune-affected entry chances; `FISHING_SPEED` shortens bobber wait time; `FISHING_WISDOM` scales XP per catch.
- Catch message sent to player on land.
- `/fishing reload` command.
- Bundled `default.yml` with fish, junk, and treasure entries.

### rpg-bossbar 0.1.0 — Boss bar system

- `BossBar: {Color, Style, Range}` field in mob YAML activates a proximity boss bar.
- Per-player show/hide task (range-based); HP fraction updates via `PostDamageEvent`.
- `RpgServices.bossBar()` API. `ancient_golem` example mob added.

### rpg-core 1.8.0 — Sign-entry utility + spawn_mob effect

- **`SignInputService`** (`rpg-api 0.5.2`): `RpgServices.signInput().ask(player, label, callback)` opens a virtual sign editor and delivers text on submit (60 s timeout; `null` on cancel / disconnect).
- **`spawn_mob{}`** ability effect: spawns a registered mob at the caster, target, or beam point. `owned=true` tags the mob to the caster (no friendly fire; despawns on logout; per-caster cap configurable).

### rpg-core 1.7.0 — Ability DSL expansion + stats GUI

- **15 new built-in ability effects:**
  - Target selection: `nearest_enemy{}`, `farthest_enemy{}`, `nearest_ally{priority=}`, `random_enemy{}`, `self{}`
  - Conditional gates: `if_health_below{}`, `if_health_above{}`, `if_mana_below{}`, `if_mana_above{}`, `if_marked{}`, `if_target_has_status{id=}`, `if_flag{name=}`, `if_not_flag{name=}`
  - Flag mutation: `set_flag{name=}`, `clear_flag{name=}`
- **`/stats` command** now opens a 54-slot inventory GUI: gear column, per-category stat breakdowns, Trade button when viewing another player.

### rpg-regions 0.6.0 — New flags + enter/exit messages

- New flags: `enter-message`, `leave-message` (title or `[actionbar]` prefix; `{player}`/`{region}` placeholders), `no-mob-spawn`, `no-damage`, `fly`, `no-item-drop`, `keep-inventory`.

### rpg-alchemy 0.3.2 / rpg-cooking 0.3.1 / rpg-enchanting 0.4.1 — GUI station overhaul

All three station GUIs were expanded from 36/45 → 54 slots and given a consistent layout pass.

**rpg-alchemy 0.3.2 (Brewing station):**
- Ingredient slots moved to row 1, centred (slots 12–14), matching cooking station
- Recipe tiles start at row 2 (slot 18); 27 recipes per page (rows 2–4)
- Pagination: PREV at slot 45 / Close at 49 / NEXT at 53

**rpg-cooking 0.3.1 (Cooking station):**
- Ingredient slots shifted to slots 12–14 (row 1, centred) — identical position to brewing
- Recipe tiles start at slot 18 (row 2); 27 per page
- Same PREV / Close / NEXT pagination nav as brewing

**rpg-enchanting 0.4.1 (Enchanting table):**
- ENCHANTING mode: per-player page state; PREV at 45 / page indicator at 47 / Close at 49 / NEXT at 53; `refreshEnchanting` is page-aware; `tryApplyEnchant` uses page offset to resolve the correct enchant
- ANVIL mode: Close button added at slot 49 (no pagination needed)

### rpg-enchanting 0.6.0 — Telekinesis

- New `telekinesis` enchant / `Telekinetic Edge` reforge stone / `Telekinesis Scroll` upgrade book. All tag the item with `auto_loot: 1` — drops from broken blocks and slain mobs go directly to inventory. Requires `rpg-enchanting`.

### rpg-economy 0.2.0 — Vault bridge

- Vault provider registered at `ServicePriority.Normal`, enabling economy integration with third-party plugins that target the Vault API.

### rpg-core 1.6.1 — Beacon suppression

- Beacon status effects now suppressed alongside other vanilla effects when `vanilla-suppression.beacons: true` is set.

### rpg-core 1.6.0 — Mob death animation

- `DeathParticle`, `DeathParticleCount`, `DeathParticleSpread`, `DeathSound` YAML fields on mobs. `MobDeathAnimListener` plays particle burst + sound and zeroes knockback velocity at death.

### Permission additions (various)

- `rpg.core.particle` — controls ability particle rendering.
- `rpg.core.items.browse` — browse item GUI (`/rpg items`).
- `rpg.core.fix` — fix orphaned movement-speed modifiers (`/rpg fix`).
- `rpg.regions.admin.global` — grants access to global region flags.

### Bug fixes (suite 21)

- **Mob stuck Slowness** (`rpg-core 1.8.1`): `EquipmentListener` scrubs orphaned `MOVEMENT_SPEED` modifiers on recalc; `/rpg fix [player]` command for manual recovery.
- **Frost Golem permanent Slowness** (`rpg-core 1.3.1`): zone pulses now use `null` attacker to prevent `~onHit` cascade.
- **Item Browser NPE on search** (`rpg-core 1.10.7`): null-safe guards on `displayName()` and `rarity()` in search/filter predicates.
