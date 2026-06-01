# 🐛 Confirmed Bugs

_These are broken in live testing. Fix these before working on new features._

> **Difficulty scale:** 🟢 Easy (< 1 day) · 🟡 Medium (1–2 days) · 🔴 Hard (several days) · ⚫ Very Hard (week+)

---

### Dungeon Enter Does Nothing (`rpg-dungeons`) — 🟡 Medium
`/dungeon enter <id>` sends "Preparing dungeon..." in chat but the player is never teleported and nothing else happens. Tested on a freshly created dungeon with `setentrance / setexit / setspawn` all set correctly — same result.

**Likely cause:** The async paste callback in `DungeonManager.enter()` is silently dropped. Possible reasons: the template world lookup (`Bukkit.getWorld(def.templateWorld())`) returns null, or `TemplatePaster.pasteAsync` never calls its callback due to an unhandled exception.

**Fix approach:** Add null/error logging around the template world lookup and inside the paste callback. Confirm the instance world (`rpg_dungeon_instances`) is being created. Check `TemplatePaster` for swallowed exceptions.

---

### Potions Disappear + Drink Has No Effect (`rpg-core` / `rpg-alchemy`) — 🟡 Medium
Two related issues:
1. **Right-clicking the ground with a potion** — the potion item disappears from the player's inventory with no effect applied.
2. **Right-clicking the air to drink a potion** — the animation plays and the item is consumed, but `/effects` shows no new entry; the status effect is never applied.

Both paths should apply the potion's configured effects and show them in `/effects`.

---

### ~~Mining: Vanilla Break Still Visuals (`rpg-mining`)~~ ✅ Fixed in `rpg-mining 0.2.1`
Mining fatigue amplifier bumped from `1` (Fatigue II) to `255` — vanilla block breaking fully suppressed while holding an RPG gathering tool. Configurable via `mining-fatigue.amplifier` in `plugins/rpg-mining/config.yml`.

> **Still open:** Miners Pickaxe cannot mine a Red Gem Block — `BREAKING_POWER` gate check or tool-type check mismatch in `BlockBreakHandler`. Needs separate investigation.

---

### ~~Beam Wand: No Damage on Hit or Explosion (`rpg-core` / `rpg-combat`)~~ ✅ Fixed in `rpg-core 1.0.7`
Adding `damage: 30` to `beam_wand` (so `carriedDamage` is non-zero) combined with the `ExplodeEffect` + `DamageEffect` `PostDamageEvent` wiring fixed both the direct hit and AoE explosion paths.

---

### ~~Iron Shortsword: Attack Cooldown Stuck at Infinite (`rpg-core`)~~ ✅ Fixed in `rpg-core 1.0.3`
`CoreRpgItem.toItemStack()` now removes vanilla attribute modifiers (`ATTACK_SPEED`, `ATTACK_DAMAGE`, `ARMOR`, etc.) from every custom item's `ItemMeta`. Previously `HIDE_ATTRIBUTES` only hid them from the tooltip — the modifiers still applied, causing `setBaseValue(2.0) + vanilla(-2.4) = -0.4` (bar never filled).

---

### Arrows: Weird Visual Behaviour on Hit (`rpg-core` / `rpg-combat`) — 🟡 Medium
Arrows **do** deal damage (confirmed in testing), but the hit visuals are wrong — the arrow appears to pass through or be cancelled visually before the hit registers. Investigate whether the arrow entity is being removed too early or whether the hit event is firing out of order with the damage pipeline.

**Also:** Bows, swords, and wands should all apply knockback. All example weapons and wands in the default item YAML files are missing a `knockback` stat entry — add one to every example item so the behaviour is demonstrated out of the box.

---

### ~~NPC Click Does Nothing — All Behaviors Broken (`rpg-npcs`)~~ ✅ Fixed in `rpg-npcs 0.5.1`

Three causes fixed:
1. **Orphan sweep**: `NpcManager.loadAll()` now sweeps all loaded worlds for `rpg_npc_id`-tagged entities before despawning and re-spawning. Persistent entities from previous sessions no longer stack up on each reload.
2. **Entity type default**: changed from `VILLAGER` → `ZOMBIE`. Paper's villager trade GUI can fire even when `PlayerInteractEntityEvent` is cancelled.
3. **Handler priority**: changed from `LOW / ignoreCancelled = true` → `NORMAL / ignoreCancelled = false`. NPC clicks now work even if a third-party listener pre-cancelled the event.

`rpg.npcs.use` was already `default: true` — no change needed.

---

### Stats Shown in Lore That Do Nothing (`rpg-core` / `rpg-combat`) — 🔴 Hard
Several `BuiltinStat` entries appear on example items and show up in lore, but are never read by any system. Players see the stat and get nothing from it:

| Stat | Defined | Implemented | Notes |
|---|---|---|---|
| `speed` | ✅ | ✅ `rpg-core 1.0.8` | Sets `generic.movement_speed` in `EquipmentListener`. Formula: `0.1 × (1 + speed × speedPerPoint / 100)`. |
| `ferocity` | ✅ | ✅ `rpg-core 1.0.8` | Extra melee swings in `DamagePipelineListener`. Each 100 ferocity = 1 guaranteed extra hit; remainder = % chance. |
| `swing_range` | ✅ | ✅ `rpg-core 1.0.8` | Sets `entity_interaction_range` in `EquipmentListener`. Formula: `3.0 + swingRange × blocksPerPoint`. |
| `pristine` | ✅ | ❌ | Intended to improve item quality rolls. No quality roll system exists. |
| `enchanting_luck` | ✅ | ❓ | Verify whether `StationGui` actually reads it during enchant application. |
| `pet_luck` | ✅ | ❌ | Irrelevant until `rpg-pets` exists. |
| `magic_find` | ✅ | ❓ | Referenced in loot pool spec as `MagicFindAffected: true` — verify whether any loot roll reads it. |

Remaining: `pristine`, `pet_luck` (pending system), `enchanting_luck`, `magic_find` (needs audit).

---

### Mob Ability Deals No Damage (`rpg-core`) — 🟡 Medium
The ability configured on `testmob` (and likely other mobs) fires and runs its animation/effects, but no damage is applied to the target player. The `DamageEffect` inside the ability pipeline is either not executing or resolving to 0. Check whether `AbilityContext` is correctly carrying the caster entity and whether `DamageEffect` falls back to a null or zero stat value when cast from a mob rather than a player.

---

### ~~Coin Drops Not Depositing to Player Economy (`rpg-core`)~~ ✅ Fixed in `rpg-core 1.0.3`
Added `currency-rolls:` section to the mob loot table schema. `CoreLootTable.rollCurrency()` returns per-player coin amounts; `MobLootListener` deposits them via `RpgServices.economy()` immediately on mob death instead of spawning item entities. Requires `rpg-economy`; silently no-ops if not loaded. Schema: `currency-rolls: [{ chance: 80.0, min: 50, max: 150 }]`.

---
