# Suite 21 — In Progress

← [Back to changelog index](../changelog.md)

_Suite 21 opened with the addition of `rpg-crafting` and `rpg-smelting`. Highlights will be compiled here when the suite closes._

---

## Notable changes so far

### rpg-core 1.7.0 — Ability DSL expansion + stats GUI

- **15 new ability effects** added to the built-in effect library:
  - Target selection: `nearest_enemy{}`, `farthest_enemy{}`, `nearest_ally{priority=}`, `random_enemy{}`, `self{}`
  - Conditional gates: `if_health_below{}`, `if_health_above{}`, `if_mana_below{}`, `if_mana_above{}`, `if_marked{}`, `if_target_has_status{id=}`, `if_flag{name=}`, `if_not_flag{name=}`
  - Flag mutation: `set_flag{name=}`, `clear_flag{name=}`
- **`/stats` command** now opens a 54-slot inventory GUI instead of printing to chat. Shows gear column, per-category stat breakdowns, and a Trade button when viewing another player.

### rpg-core 1.6.1 — Beacon suppression

- Beacon status effects are now suppressed alongside other vanilla status effects when vanilla suppression is active.

### rpg-regions 0.6.0 — New flags + API

- New region flags: `enter-message`, `leave-message`, `no-mob-spawn`, `no-damage`, `fly`, `no-item-drop`, `keep-inventory`
- `flagString()` added to `RegionService` API for reading flag values programmatically.

### rpg-enchanting 0.6.0 — Telekinesis

- New `telekinesis` enchant/reforge/upgrade (`auto_loot: 1`): drops from broken blocks and slain mobs go directly to the player's inventory.

### rpg-economy 0.2.0 — Vault bridge

- Vault provider registered, enabling economy integration with third-party plugins that target the Vault API.

### Permission additions

- `rpg.core.particle` — controls ability particle rendering permission.
- `rpg.regions.admin.global` — grants access to the global (server-wide fallback) region flags.
