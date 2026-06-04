# 🐛 Confirmed Bugs

_These are broken in live testing. Fix these before working on new features._

> **Difficulty scale:** 🟢 Easy (< 1 day) · 🟡 Medium (1–2 days) · 🔴 Hard (several days) · ⚫ Very Hard (week+)

---

### ~~Dungeon Enter Does Nothing (`rpg-dungeons`)~~ ✅ Fixed in `rpg-dungeons 0.0.3`

`TemplatePaster.run()` lacked exception handling around `dst.setBlockData()`. In Paper 1.21.4, block writes to freshly-generated void-world chunks can throw (unloaded-chunk state). The exception escaped the `while` loop, was swallowed by Bukkit's repeating-task runner, and the task retried the same `(x, y, z)` coordinate on every tick forever — `onDone` was never called, player stuck at "Preparing dungeon...". Fixed by wrapping per-block ops in `try/finally { advance(); }` (failed blocks are skipped) and `onDone.accept()` in a try-catch (callback failures now log at SEVERE).

---

### ~~Potions Disappear + Drink Has No Effect (`rpg-core` / `rpg-alchemy`)~~ ✅ Fixed in `rpg-alchemy 0.3.1`

Two root causes:
1. **Wrong interception point** — `PotionDrinkListener` was handling `PlayerInteractEvent`. In Paper 1.21.4, the `ServerboundUseItemPacket` is already processed server-side before that event fires, so cancelling there did not reliably suppress vanilla. Handler moved to `PlayerItemConsumeEvent`: vanilla drives the animation, we intercept at consumption time.
2. **Effect ID mismatch** — `potions/example.yml` referenced `strength_buff` and `heal_over_time`, neither of which existed in the status-effects registry. `CoreStatusEffectService.apply()` silently no-ops on unknown IDs. Updated to `strength_boost` and `regen`.

---

### ~~Mining: Vanilla Break Still Visuals (`rpg-mining`)~~ ✅ Fixed in `rpg-mining 0.2.1`
Mining fatigue amplifier bumped from `1` (Fatigue II) to `255` — vanilla block breaking fully suppressed while holding an RPG gathering tool. Configurable via `mining-fatigue.amplifier` in `plugins/rpg-mining/config.yml`.

### ~~Miner's Pickaxe Can't Mine Red Gem Block (`rpg-core`)~~ ✅ Fixed in `rpg-core 1.1.1`
Root cause: Mining Fatigue amplifier 255 (applied by rpg-mining to all gathering tools) causes Paper to suppress per-tick `BlockDamageEvent` because the server-side break-progress-per-tick rounds to 0. `BlockBreakHandler` used a 400 ms `lastClickMs` heartbeat fed by `BlockDamageEvent` to detect "still mining" — with no per-tick events arriving, the timeout expired and cleared progress after the initial click. The BREAKING_POWER gate and tool-type check were both correct all along.

Fixed by:
1. Removing the `lastClickMs` field and 400 ms timeout entirely from `BlockBreakProgress` / `tickAll()`. Release detection now relies on `BlockDamageAbortEvent` (fired by the client's `ABORT_DESTROY_BLOCK` packet regardless of Mining Fatigue), `getTargetBlockExact()` look-away check, and existing `PlayerItemHeldEvent` / `PlayerQuitEvent` guards.
2. Adding a `PlayerInteractEvent` LEFT_CLICK_BLOCK handler as a secondary trigger so progress starts even if the initial `BlockDamageEvent` is also suppressed in edge cases.

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
| `speed` | ✅ | ✅ `rpg-core 1.1.0` | Sets `generic.movement_speed` in `EquipmentListener`. Formula: `0.1 × (1 + speed × speedPerPoint / 100)`. |
| `ferocity` | ✅ | ✅ `rpg-core 1.1.0` | Extra melee swings in `DamagePipelineListener`. Each 100 ferocity = 1 guaranteed extra hit; remainder = % chance. |
| `swing_range` | ✅ | ✅ `rpg-core 1.1.0` | Sets `entity_interaction_range` in `EquipmentListener`. Formula: `3.0 + swingRange × blocksPerPoint`. |
| `pristine` | ✅ | ❌ | Intended to improve item quality rolls. No quality roll system exists. |
| `enchanting_luck` | ✅ | ❓ | Verify whether `StationGui` actually reads it during enchant application. |
| `pet_luck` | ✅ | ❌ | Irrelevant until `rpg-pets` exists. |
| `magic_find` | ✅ | ❓ | Referenced in loot pool spec as `MagicFindAffected: true` — verify whether any loot roll reads it. |

Remaining: `pristine`, `pet_luck` (pending system), `enchanting_luck`, `magic_find` (needs audit).

---

### ~~Mob Ability Deals No Damage (`rpg-core`)~~ ✅ Fixed as a side effect of earlier core work
The ability configured on `testmob` (and likely other mobs) fires and runs its animation/effects, but no damage is applied to the target player. The `DamageEffect` inside the ability pipeline is either not executing or resolving to 0. Check whether `AbilityContext` is correctly carrying the caster entity and whether `DamageEffect` falls back to a null or zero stat value when cast from a mob rather than a player.

---

### ~~Mob Abilities Damage Players in Creative Mode (`rpg-core`)~~ ✅ Fixed in `rpg-core 1.1.1`
Added a `GameMode.CREATIVE` guard in `DamageEffect.apply()` before the `RpgServices.health().damage()` call. Mob abilities (and player abilities) no longer damage creative-mode players.

---

### Beam Wand: Damage Applied 3× + Health Display Doesn't Refresh (`rpg-core`) — 🟡 Medium

Three distinct issues confirmed in testing (zombie set to 100 HP):

1. **Damage applied ~3× per cast** — beam wand shows `23.3` on the indicator but the zombie drops from 100 HP to ~30 (≈70 actual damage). `23.3 × 3 ≈ 69.9` — the damage is being applied exactly three times per trigger. Most likely cause: `BeamEffect` fires `entity.damage()` or calls `DamageEffect` once per tick while the beam is active and the beam lingers for 3 ticks, or `DamageEffect` itself is being invoked three times through the ability pipeline. Needs logging around every damage call in the beam path to confirm hit count.

2. **Damage indicator sourcing wrong value** — the `23.3` shown is the RPG-pipeline damage (probably post-mitigation), but the actual HP removed is `~70`. The indicator is not wrong per se — it's showing one application correctly — but it's showing it once while the entity is hit three times. So the indicator fires on the first hit and the remaining two hits land silently.

3. **Health display hologram doesn't update on beam damage** (`rpg-holograms`) — the TextDisplay nameplate showing mob HP doesn't refresh after the beam wand hits. A fist hit immediately after does update it. The relevant listener is in `rpg-holograms` (a `DamageIndicatorListener` or equivalent), not `rpg-core`. It likely watches `EntityDamageEvent` or `PostDamageEvent` to schedule a nameplate refresh; the beam's damage path may be bypassing whichever event it hooks into, or the update fires before the HP value is actually written.

Fix approach: (a) cap beam damage to one application per cast via the per-tick dedup set in `BeamEffect` (see pierce-cap improvement entry); (b) ensure `rpg-holograms`' health display refresh is triggered after `entity.damage()` regardless of the damage source path — may need to fire a custom event or ensure `PostDamageEvent` is always published by the beam path.

---

### ~~Coin Drops Not Depositing to Player Economy (`rpg-core`)~~ ✅ Fixed in `rpg-core 1.0.3`
Added `currency-rolls:` section to the mob loot table schema. `CoreLootTable.rollCurrency()` returns per-player coin amounts; `MobLootListener` deposits them via `RpgServices.economy()` immediately on mob death instead of spawning item entities. Requires `rpg-economy`; silently no-ops if not loaded. Schema: `currency-rolls: [{ chance: 80.0, min: 50, max: 150 }]`.

---
