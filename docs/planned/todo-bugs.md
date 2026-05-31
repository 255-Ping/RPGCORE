# 🐛 Confirmed Bugs

_These are broken in live testing. Fix these before working on new features._

---

### Dungeon Enter Does Nothing (`rpg-dungeons`)
`/dungeon enter <id>` sends "Preparing dungeon..." in chat but the player is never teleported and nothing else happens. Tested on a freshly created dungeon with `setentrance / setexit / setspawn` all set correctly — same result.

**Likely cause:** The async paste callback in `DungeonManager.enter()` is silently dropped. Possible reasons: the template world lookup (`Bukkit.getWorld(def.templateWorld())`) returns null, or `TemplatePaster.pasteAsync` never calls its callback due to an unhandled exception.

**Fix approach:** Add null/error logging around the template world lookup and inside the paste callback. Confirm the instance world (`rpg_dungeon_instances`) is being created. Check `TemplatePaster` for swallowed exceptions.

---

### Potions Disappear + Drink Has No Effect (`rpg-core` / `rpg-alchemy`)
Two related issues:
1. **Right-clicking the ground with a potion** — the potion item disappears from the player's inventory with no effect applied.
2. **Right-clicking the air to drink a potion** — the animation plays and the item is consumed, but `/effects` shows no new entry; the status effect is never applied.

Both paths should apply the potion's configured effects and show them in `/effects`.

---

### Mining: Custom Block Can't Be Mined + Vanilla Break Still Visuals (`rpg-mining` / `rpg-core`)
Two related issues:
1. **Miners Pickaxe cannot mine a Red Gem Block** — the pickaxe has the correct breaking power but the block doesn't break. Likely a `BREAKING_POWER` gate check or tool-type check mismatch in `BlockBreakHandler`.
2. **Visual vanilla "Minecraft" mining still plays** — the block shows crack animation and appears to break visually, but doesn't actually drop. Mining Fatigue level being applied is probably not high enough to fully suppress vanilla break time. Needs a higher amplifier (e.g., level 255) to make vanilla break time effectively infinite.

---

### Beam Wand: No Damage on Hit or Explosion (`rpg-core` / `rpg-combat`)
The beam ability visually stops and "explodes" when it contacts a mob, but:
- The beam itself deals no damage while travelling through the mob
- The explosion on impact deals no damage either

The collision detection is working (it stops correctly), so the issue is in the damage application step — either the `DamageEffect` inside the beam's hit/explode pipeline is not firing, or it's firing against the wrong target entity.

---

### Iron Shortsword: Attack Cooldown Stuck at Infinite (`rpg-core` / `rpg-combat`)
Equipping the Iron Shortsword puts the attack cooldown indicator permanently at 0 — it never fills and the player can never land a registered RPG attack. The `generic.attack_speed` attribute is likely being set to an extremely low or negative value, or the `AttackCooldown` field in the item YAML is being parsed/applied incorrectly.

---

### Arrows: No Damage, No Knockback, Cancelled on Hit (`rpg-core` / `rpg-combat`)
When an arrow hits a mob:
- No RPG damage is applied
- No knockback occurs
- It looks like the arrow hit is cancelled before Minecraft's own damage gets a chance to run

**Also:** Bows, swords, and wands should all apply knockback. All example weapons and wands in the default item YAML files are missing a `knockback` stat entry — add one to every example item so the behaviour is demonstrated out of the box.

---
