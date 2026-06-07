# Suite 21 — In Progress

← [Back to changelog index](../changelog.md)

_Suite 21 opened with the addition of `rpg-crafting` and `rpg-smelting`. The suite is still open; entries below are compiled as work lands._

---

## Notable changes

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
