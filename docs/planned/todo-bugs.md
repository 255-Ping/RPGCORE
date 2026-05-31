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

### Beam Wand: No Damage on Hit or Explosion (`rpg-core` / `rpg-combat`) — 🟡 Medium
The beam ability visually stops and "explodes" when it contacts a mob, but:
- The beam itself deals no damage while travelling through the mob
- The explosion on impact deals no damage either

The collision detection is working (it stops correctly), so the issue is in the damage application step — either the `DamageEffect` inside the beam's hit/explode pipeline is not firing, or it's firing against the wrong target entity.

---

### ~~Iron Shortsword: Attack Cooldown Stuck at Infinite (`rpg-core`)~~ ✅ Fixed in `rpg-core 1.0.3`
`CoreRpgItem.toItemStack()` now removes vanilla attribute modifiers (`ATTACK_SPEED`, `ATTACK_DAMAGE`, `ARMOR`, etc.) from every custom item's `ItemMeta`. Previously `HIDE_ATTRIBUTES` only hid them from the tooltip — the modifiers still applied, causing `setBaseValue(2.0) + vanilla(-2.4) = -0.4` (bar never filled).

---

### Arrows: Weird Visual Behaviour on Hit (`rpg-core` / `rpg-combat`) — 🟡 Medium
Arrows **do** deal damage (confirmed in testing), but the hit visuals are wrong — the arrow appears to pass through or be cancelled visually before the hit registers. Investigate whether the arrow entity is being removed too early or whether the hit event is firing out of order with the damage pipeline.

**Also:** Bows, swords, and wands should all apply knockback. All example weapons and wands in the default item YAML files are missing a `knockback` stat entry — add one to every example item so the behaviour is demonstrated out of the box.

---

### NPC Click Does Nothing — All Behaviors Broken (`rpg-npcs`) — 🟡 Medium
Right-clicking any NPC (dialogue, shop, quest, banker) does nothing. Two likely causes, both need investigating:

1. **`rpg.npcs.use` permission** — `NpcInteractListener.onInteract` cancels the vanilla interact then immediately returns if the player lacks `rpg.npcs.use`. If this permission defaults to op-only (check `plugin.yml`), regular players get silently blocked. Fix: set `rpg.npcs.use` to `default: true` in `plugin.yml`.

2. **Villager entity type interference** — the default entity type is `VILLAGER`. Paper may be pre-processing villager trades before `PlayerInteractEntityEvent` reaches our `LOW`-priority handler. Since the handler uses `ignoreCancelled = true`, anything that pre-cancels or pre-handles the event will silently skip our code. Fix: change the default entity type to something that doesn't have special right-click behaviour (e.g., `ARMOR_STAND` or `ZOMBIE`) and/or change handler priority to `NORMAL`/`HIGH`.

3. **Double-spawn on reload** — NPC entities are spawned with `setPersistent(true)`. On reload, `despawnAll()` runs against an empty `entityToId` map (not populated yet), so old persistent entities stay in the world. Then `spawnAll()` adds new ones on top. Two overlapping entities exist at every NPC location; clicks may resolve to the wrong one. Fix: on `loadAll`, scan the world for entities with the `rpg_npc_id` PDC key and remove them before spawning fresh ones.

---

### Stats Shown in Lore That Do Nothing (`rpg-core` / `rpg-combat`) — 🔴 Hard
Several `BuiltinStat` entries appear on example items and show up in lore, but are never read by any system. Players see the stat and get nothing from it:

| Stat | Defined | Used | Notes |
|---|---|---|---|
| `speed` | ✅ | ❌ | `EquipmentListener` only sets `generic.attack_speed`, never touches `generic.movement_speed`. Windwalker Boots shows `+12 Speed` — does nothing. |
| `ferocity` | ✅ | ❌ | Intended as "% chance for an extra swing." Voidblade shows `+60 Ferocity` — does nothing. No extra-swing logic exists in the damage pipeline. |
| `swing_range` | ✅ | ❌ | Intended to expand melee reach. Voidblade shows `+2 Swing Range` — the player's hit box is never modified. |
| `pristine` | ✅ | ❌ | Intended to improve item quality rolls. Pristine Talisman shows `+25 Pristine` — no quality roll system exists. |
| `enchanting_luck` | ✅ | ❓ | Shown on several items. Verify whether `StationGui` actually reads it during enchant application or just ignores it. |
| `pet_luck` | ✅ | ❌ | Irrelevant until `rpg-pets` exists, but shows on lore. Consider hiding it until the system is built. |
| `magic_find` | ✅ | ❓ | Referenced in loot pool spec as `MagicFindAffected: true` — verify whether any loot roll actually reads this stat from the player. |

Fix approach: either implement the missing behaviour for each stat, or suppress it from lore display until the system is ready (add a `hidden: true` flag or a dedicated "not yet active" lore note).

---

### Mob Ability Deals No Damage (`rpg-core`) — 🟡 Medium
The ability configured on `testmob` (and likely other mobs) fires and runs its animation/effects, but no damage is applied to the target player. The `DamageEffect` inside the ability pipeline is either not executing or resolving to 0. Check whether `AbilityContext` is correctly carrying the caster entity and whether `DamageEffect` falls back to a null or zero stat value when cast from a mob rather than a player.

---

### ~~Coin Drops Not Depositing to Player Economy (`rpg-core`)~~ ✅ Fixed in `rpg-core 1.0.3`
Added `currency-rolls:` section to the mob loot table schema. `CoreLootTable.rollCurrency()` returns per-player coin amounts; `MobLootListener` deposits them via `RpgServices.economy()` immediately on mob death instead of spawning item entities. Requires `rpg-economy`; silently no-ops if not loaded. Schema: `currency-rolls: [{ chance: 80.0, min: 50, max: 150 }]`.

---
