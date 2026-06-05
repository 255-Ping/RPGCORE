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

### ~~Arrows: Weird Visual Behaviour on Hit (`rpg-core` / `rpg-combat`)~~ ✅ Fixed

Arrow hit visuals corrected; knockback stat added to example weapons and wands.

---

### ~~NPC Click Does Nothing — All Behaviors Broken (`rpg-npcs`)~~ ✅ Fixed in `rpg-npcs 0.5.1`

Three causes fixed:
1. **Orphan sweep**: `NpcManager.loadAll()` now sweeps all loaded worlds for `rpg_npc_id`-tagged entities before despawning and re-spawning. Persistent entities from previous sessions no longer stack up on each reload.
2. **Entity type default**: changed from `VILLAGER` → `ZOMBIE`. Paper's villager trade GUI can fire even when `PlayerInteractEntityEvent` is cancelled.
3. **Handler priority**: changed from `LOW / ignoreCancelled = true` → `NORMAL / ignoreCancelled = false`. NPC clicks now work even if a third-party listener pre-cancelled the event.

`rpg.npcs.use` was already `default: true` — no change needed.

---

### ~~Stats Shown in Lore That Do Nothing (`rpg-core` / `rpg-combat`)~~ ✅ Resolved

`speed`, `ferocity`, `swing_range` all implemented (`rpg-core 1.1.0`). `magic_find` wired into loot rolls. `pristine`, `pet_luck`, `enchanting_luck` deferred until their systems are built.

---

### ~~Mob Ability Deals No Damage (`rpg-core`)~~ ✅ Fixed as a side effect of earlier core work
The ability configured on `testmob` (and likely other mobs) fires and runs its animation/effects, but no damage is applied to the target player. The `DamageEffect` inside the ability pipeline is either not executing or resolving to 0. Check whether `AbilityContext` is correctly carrying the caster entity and whether `DamageEffect` falls back to a null or zero stat value when cast from a mob rather than a player.

---

### ~~Mob Abilities Damage Players in Creative Mode (`rpg-core`)~~ ✅ Fixed in `rpg-core 1.1.1`
Added a `GameMode.CREATIVE` guard in `DamageEffect.apply()` before the `RpgServices.health().damage()` call. Mob abilities (and player abilities) no longer damage creative-mode players.

---

### ~~Beam Wand: Damage Applied 3× + Health Display Doesn't Refresh (`rpg-core`)~~ ✅ Fixed

Triple-damage and hologram refresh issues both resolved.

---

### ~~Coin Drops Not Depositing to Player Economy (`rpg-core`)~~ ✅ Fixed in `rpg-core 1.0.3`
Added `currency-rolls:` section to the mob loot table schema. `CoreLootTable.rollCurrency()` returns per-player coin amounts; `MobLootListener` deposits them via `RpgServices.economy()` immediately on mob death instead of spawning item entities. Requires `rpg-economy`; silently no-ops if not loaded. Schema: `currency-rolls: [{ chance: 80.0, min: 50, max: 150 }]`.

---
