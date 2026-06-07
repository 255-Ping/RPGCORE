# Suite 21 — In Progress

← [Back to changelog index](../changelog.md)

_Suite 21 opened with the addition of `rpg-crafting` and `rpg-smelting`. The suite is still open; entries below are compiled as work lands._

---

## Notable changes

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
