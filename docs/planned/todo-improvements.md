# 🟠 Improvements — In Progress / Major Missing Chunks

_These systems exist and partially work, but have significant gaps._

> **Difficulty scale:** 🟢 Easy (< 1 day) · 🟡 Medium (1–2 days) · 🔴 Hard (several days) · ⚫ Very Hard (week+)

---

### ✅ Expand Example Content — Mobs, Abilities, and Items (`rpg-core`) — shipped in 1.3.0

5 showcase mobs added to `mobs/example.yml` — `frost_golem` (zone denial + freeze on hit), `chain_wraith` (beam+chain+death nova), `blood_shade` (mark+detonate), `shield_golem` (boss: shield+slam+launch), `void_phantom` (blink+drain+death zone). Berserker set cleaned up to pure passive stats (no `~on_hit` procs on stat tiers). Items updated with DualCast Wand demonstrating `beam{}+damage{}` chain. Docs page `mobs.md` updated with ability pattern examples. Armor sets docs updated with passive-stats callout.

---

### ✅ New Built-in Ability Effects (`rpg-core`) — shipped in 1.3.0

10 new effects implemented: `knockback` (push/pull/launch), `blink` (forward dash to first solid), `chain` (bounce damage to N targets), `zone` (persistent AoE ground zone with interval pulses), `shield` (damage-absorb HP buffer), `drain` (damage + lifesteal), `mark` (damage-amplify debuff, consumed by `damage{}`), `launch` (velocity burst), `freeze` (Slowness V + Mining Fatigue), `restore_mana`. All available in mob and item ability chains. Zone pulse null-attacker fix shipped in 1.3.1 (prevented mob `~onHit` cascade). `teleport` deferred to a later slice.

---

### ✅ Ability Trigger: `~onLogin` (`rpg-core`) — shipped in 1.10.4

`PlayerAbilityTrigger.ON_LOGIN` added to rpg-api 0.5.5; `PlayerLoginAbilityListener` (fires on `PlayerJoinEvent`, registered in `RpgCorePlugin.onEnable`) added to rpg-core 1.10.4. Use `~on_login` prefix in item ability YAML. Target is `null` on login; the trigger is not `isActive()` (no mana cost by default).

---

### ✅ Ability Pierce Cap (`rpg-core`) — shipped in 1.10.4

`pierce_cap` int param added to `BeamEffect` (default 1 = single-target, matching previous behaviour; 0 = unlimited). Uses repeated `rayTrace` calls with a `Set<UUID> hitIds` exclusion filter so the beam tunnels through already-hit entities. `ctx.target` = first hit; knockback applied to all hits; beam stops at the pierce cap or on block contact, whichever comes first.

---

### ✅ Ability DSL: Random Chance Gate (`rpg-core`) — already shipped
`ChanceEffect` exists, is registered as `"chance"` in `registerBuiltins()`, and uses `ctx.setBlocked(true)` when the roll fails. Verified complete during session audit.

<!--
A `chance{}` effect that acts as an inline probability gate. When the ability chain reaches it, a random roll is made — if it fails the rest of the chain is silently skipped.

```yaml
# 25% chance to apply poison on hit
- "chance{percent=25} apply_status{id=poison,duration=100,amplifier=1} ~onHit"

# Boss ability: 40% chance to chain-lightning after a beam hit
- "beam{range=12.0,damage_multiplier=1.0} damage{} chance{percent=40} chain{count=2,range=8.0,damage_multiplier=0.5} ~onTimer:40"
```

**Behaviour:**
- `chance{percent=N}` — rolls `random(0,100) < N`. If the roll fails, all effects *after* `chance{}` in the same chain are skipped. Effects *before* it have already executed.
- `percent` is a double (supports `percent=12.5`)
- Stacking two `chance{}` calls is AND logic: `chance{percent=50} chance{percent=50}` = ~25% net
- No `seed` — each invocation rolls independently

**Implementation:** `ChanceEffect` sets a `boolean ChanceEffect.BLOCKED_KEY` flag on the `AbilityContext` if the roll fails. Subsequent effects check this flag at the start of `apply()` and no-op if set. The flag is cleared after the full chain finishes (reset in the chain runner, not by individual effects). This is cleaner than throwing an exception or returning an enum — context already has a carried-state bag.
-->

---

### ✅ Ability DSL: Target Selection Effects (`rpg-core`) — shipped in 1.7.0
`nearest_enemy{}`, `farthest_enemy{}`, `nearest_ally{priority=nearest|lowest_health|lowest_mana}`, `random_enemy{}`, `self{}` — each sets `ctx.target` via radius query; `allow_pvp=true` opt-in; null-safe. Full spec preserved below for reference.

<!--
Explicit target-selection effects that override `ctx.target` before downstream effects run. Currently `beam{}` is the only way to acquire a target — these give mob and item designers a richer palette.

```yaml
# Mob: every 3 seconds, drain the nearest enemy
- "nearest_enemy{range=12.0} drain{amount=15,leech=0.8} ~onTimer:60"

# Item: heal the lowest-health ally in range
- "nearest_ally{range=20.0,priority=lowest_health} heal{amount=50} right_click"

# Boss: pull the farthest enemy toward the caster, then slam
- "farthest_enemy{range=30.0} launch{force=2.0,direction=toward} aoe{radius=5.0,damage=20.0} ~onTimer:100"
```

**Effects to add:**

| Effect | Parameters | Sets `ctx.target` to |
|---|---|---|
| `nearest_enemy{}` | `range=` | Nearest hostile entity within range (enemy of caster) |
| `farthest_enemy{}` | `range=` | Farthest hostile entity within range |
| `nearest_ally{}` | `range=`, `priority=nearest\|lowest_health\|lowest_mana` | Nearest friendly entity (same team / player) |
| `random_enemy{}` | `range=`, `count=1` | Random hostile entity; `count>1` stores multiple for `chain{}` |
| `self{}` | — | Caster itself; useful to chain self-buffs after targeting an enemy |

**"Hostile" definition:** for mobs, hostile = any player; for players, hostile = any mob that `CoreDamagePipeline` would consider a valid target. Friendly fire between players is off by default — `allow_pvp=true` param opts in.

**Interaction with `beam{}`:** `nearest_enemy{}` and `beam{}` both set `ctx.target`. They can be combined — `beam{}` locks on via raycast, `nearest_enemy{}` locks on via radius. Use whichever fits the ability's feel.

**Implementation:** Each is a simple `TargetSelectEffect` subclass that queries the world for `LivingEntity` instances, applies the priority/filter, and writes the result to `ctx.target`. Returns early if no valid target found (subsequent effects no-op on null target as they already do).
-->

---

### ✅ Ability DSL: Conditional Flow (`rpg-core`) — shipped in 1.7.0
`if_health_below/above{}`, `if_mana_below/above{}`, `if_marked{}`, `if_target_has_status{id=}`, `if_flag{name=}`, `if_not_flag{name=}`, `set_flag{name=}`, `clear_flag{name=}` — all use the same `blocked` mechanism as `chance{}`; flags in entity metadata (cleared on death); phase-transition pattern documented. Full spec preserved below for reference.

<!--

Inline `if_*{}` effects that act as gates — they let the rest of the chain proceed only if a condition is true. This unlocks reactive, state-aware abilities without needing separate trigger lines for every case.

```yaml
# Mob enrage: if below 50% HP, deal bonus damage
- "if_health_below{percent=50} aoe{radius=6.0,damage=25.0,particle=FLAME} ~onTimer:40"

# Only detonate mark if the target is actually marked
- "nearest_enemy{range=10.0} if_marked{} damage{multiplier=3.0} ~onTimer:60"

# Item ability: heal only if the caster is below 30% HP
- "if_health_below{percent=30} heal{amount=80} cooldown{ticks=200} right_click"

# Boss phase transition: at 25% HP, spawn adds and shield
- "if_health_below{percent=25} if_not_flag{name=phase2_triggered} set_flag{name=phase2_triggered} spawn_mob{id=golem_shard,count=3,radius=5.0} shield{amount=500,duration=200,target=caster} ~onTimer:20"
```

**Conditions to add:**

| Effect | Parameters | Passes when |
|---|---|---|
| `if_health_below{}` | `percent=` | Caster's HP% < percent |
| `if_health_above{}` | `percent=` | Caster's HP% > percent |
| `if_mana_below{}` | `percent=` | Caster's mana% < percent |
| `if_mana_above{}` | `percent=` | Caster's mana% > percent |
| `if_marked{}` | — | `ctx.target` currently has an active `MarkEffect` |
| `if_target_has_status{}` | `id=` | `ctx.target` has the given status effect active |
| `if_not_flag{}` | `name=` | The named context/caster flag is NOT set (see Variables below) |
| `if_flag{}` | `name=` | The named context/caster flag IS set |

**Behaviour:** a failing condition sets the same `BLOCKED_KEY` flag on `AbilityContext` as `chance{}` — effects downstream are skipped. Multiple conditions AND together naturally. Conditions before the gate have already fired.

**`if_health_below` on mobs:** reads `CoreHealthService.currentHp(entity) / maxHp`. Requires mobs to be tracked in `CoreHealthService` — they already are via `MobSpawnListener`.
-->

---

### ✅ Ability DSL: Spawn Mob Effect (`rpg-core`) — shipped in 1.8.0
`spawn_mob{id=,count=1,at=caster|target|point,radius=0,offset_y=0,owned=false}` — `OwnedMobTracker` enforces per-caster cap + despawns on logout. Full spec preserved below for reference.

<!--

A `spawn_mob{}` effect that spawns a registered custom mob at or near the caster or target. Enormous design space — summon mechanics, boss adds, on-death spawns, trap abilities.

```yaml
# Boss spawns adds when hurt
- "spawn_mob{id=golem_shard,count=2,radius=4.0,offset_y=0} ~onHurt"

# On-death: the corpse spawns a weakened version
- "spawn_mob{id=shadow_fragment,count=1,at=caster} ~onDeath"

# Summoner item: right-click to summon a temporary ally
- "mana_cost{amount=50} cooldown{ticks=400} spawn_mob{id=summoned_skeleton,count=1,at=caster,radius=2.0,owned=true} right_click"
```

**Parameters:**

| Parameter | Default | Description |
|---|---|---|
| `id` | required | Mob ID from the mob registry |
| `count` | `1` | How many to spawn |
| `at` | `caster` | Spawn anchor: `caster` or `target` |
| `radius` | `0` | Spread radius around the anchor (spawns within a circle of this radius) |
| `offset_y` | `0` | Y offset from anchor (use `1.0` to spawn above the caster, etc.) |
| `owned` | `false` | If true, the spawned mob does not attack the caster (ally semantics) |

**`owned` semantics:** the spawned mob's `LivingEntity` is tagged with the caster's UUID in PDC. `MobAbilityEventListener` and `DamagePipelineListener` skip damage between a mob and its owner. Owned mobs despawn when the caster logs out or after a configurable `max_lifetime_ticks` (default from `abilities.spawn-mob.default-lifetime` in config).

**Safety cap:** `abilities.spawn-mob.max-per-caster: 10` in config. If a caster already has `N` owned mobs alive, `spawn_mob{}` with `owned=true` silently no-ops. Without `owned`, no cap (spawn is purely environmental, like a zone).

**Implementation:** `SpawnMobEffect.apply()` calls `MobRegistry.spawn(id, location)` for each instance. Uses the same path as `/rpg mob spawn`. For `owned=true`, tags the entity and registers a cleanup listener.
-->

---

### Ability DSL: Context Variables + Flags (`rpg-core`) — 🔴 Hard

Persistent per-caster variables stored alongside the ability context, enabling stateful ability scripting. Two tiers:

**Tier 1 — Session flags** (boolean, per-caster lifetime): simpler to implement, high immediate value.

```yaml
# One-time phase transition: fires once when HP drops below 25%, never again
- "if_health_below{percent=25} if_not_flag{name=phase2} set_flag{name=phase2} spawn_mob{id=golem_shard,count=3,radius=5.0} shield{amount=500,duration=200,target=caster} ~onTimer:20"
```

| Effect | Description |
|---|---|
| `set_flag{name=X}` | Sets boolean flag `X` on the caster (persists until caster dies or logs out) |
| `clear_flag{name=X}` | Clears flag `X` |
| `if_flag{name=X}` | Gate: pass if flag `X` is set |
| `if_not_flag{name=X}` | Gate: pass if flag `X` is NOT set |

Flags are stored in a `Map<String, Boolean>` on `AbilityContext` (session) or on `CoreRpgPlayer` (cross-ability-invocation persistence). For mobs, stored on a `Map` hanging off the mob entity's metadata. Flags clear on mob death and on player disconnect.

**Tier 2 — Numeric variables** (int/double counters, per-caster): enables combo counters, stacks, escalating abilities.

```yaml
# Combo counter: every 3rd hit fires a power strike
- "increment{name=combo_hits} ~onHit"
- "if_var_gte{name=combo_hits,value=3} reset{name=combo_hits} aoe{radius=4.0,damage=40.0,particle=CRIT} ~onHit"

# Stack tracker: boss gains power stacks below 50% HP
- "if_health_below{percent=50} increment{name=enrage_stacks,max=5} ~onTimer:100"
- "if_var_gte{name=enrage_stacks,value=1} damage{multiplier_from_var=enrage_stacks,multiplier_scale=0.2} ~onHit"
```

| Effect | Parameters | Description |
|---|---|---|
| `set_var{name=X,value=N}` | `name`, `value` (double) | Set variable `X` to `N` |
| `increment{name=X}` | `name`, `amount=1`, `max=` | Add `amount` to `X`; clamp to `max` if specified |
| `decrement{name=X}` | `name`, `amount=1`, `min=0` | Subtract `amount`; clamp to `min` |
| `reset{name=X}` | `name` | Set `X` to 0 |
| `if_var_gte{name=X,value=N}` | `name`, `value` | Gate: pass if `X >= N` |
| `if_var_lte{name=X,value=N}` | `name`, `value` | Gate: pass if `X <= N` |
| `if_var_eq{name=X,value=N}` | `name`, `value` | Gate: pass if `X == N` (within epsilon for doubles) |

Variables are stored in `Map<String, Double>` on `CoreRpgPlayer` (survives across ability invocations within a session) and on mob metadata. Cross-session persistence for players is opt-in: `persistent=true` on `set_var{}` writes to `DataStore` (saves on quit, loads on join). Non-persistent variables clear on disconnect.

**Implementation order:** Ship Tier 1 (flags only) first — it's a `HashMap` + 4 effect classes. Tier 2 (numeric vars) is larger and can be a separate pass.

---

### ✅ MagicFind Stat: Implement or Suppress (`rpg-core`) — shipped in 1.8.2
Configurable `loot.max-magic-find-multiplier` cap applied in both SHARED and PER_PLAYER roll modes. `MagicFindAffected: true` entries multiply roll chance by `(1 + magic_find / 100.0)`, capped at the config value.

---

### ✅ Consolidate `backend.yml` + `config.yml` Persistence Setting (`rpg-core`) — shipped in 1.10.3
Comment block added near the `persistence:` key in `rpg-core/config.yml` explaining that `backend.yml` is internal `BackendMigrator` bookkeeping and must not be edited manually. `docs/core/persistence.md` already contained the full explanation. No code change — docs/config-comment only.

---

### ✅ Timed Cooking + Brewing with Persistent Progress (`rpg-cooking` / `rpg-alchemy`) — shipped in 0.4.0
`CraftProgress` 4-tick task; 9-slot progress bar row 0; DataStore save/restore on close/reopen; ingredient locking; `CraftTime:` field in recipe YAML; cook time shown in recipe lore.

<!--
Currently recipes complete instantly when the player clicks the output slot. Add configurable craft time:

- Each recipe YAML gains an optional `CraftTime` field (in seconds; 0 or absent = instant, same as now)
- When a player starts a recipe in the GUI a progress bar fills over the configured duration
- **Visual progress feedback** — the output slot item cycles through a configurable set of `CustomModelData` values (e.g., empty flask → quarter full → half full → full) so players can see progress visually. Alternatively, show a dedicated progress-bar item in a fixed slot using filled/unfilled block items (e.g., lime vs gray glass panes). The approach should be consistent between cooking and brewing.
- **If the player closes the GUI mid-craft**, the in-progress state is saved to `DataStore` keyed by `<playerUUID>:<stationBlockLocation>`: which recipe is being crafted, how much time has elapsed, and the ingredients that were consumed (so they can't be double-spent)
- **When the player reopens that station GUI**, it restores the in-progress state — progress resumes from where it left off (not restarting). The ingredient slots show the items locked in for the current craft; players can't swap them out mid-craft.
- On completion the output appears in the output slot with a sound cue; the persisted state is cleared
- If the station block is destroyed mid-craft, the ingredients should be dropped at the block location and the persisted state cleared
- Applies to both cooking stations (`rpg-cooking`) and brewing stations (`rpg-alchemy`)
-->

---

### ✅ Timed Smelting with Persistent Progress (`rpg-smelting`) — shipped in 0.1.0
Single input slot GUI, orange progress bar, BLAST_FURNACE station block, DataStore save/restore, optional vanilla FurnaceRecipe registration, XP → Mining skill. `CraftTime:` field in recipe YAML.

---

### ✅ Enchanting: Costs Minecraft XP (`rpg-enchanting`) — shipped in 0.5.0

`XpCost:` field (integer XP levels) wired on enchants. Shown in the enchant slot lore (`&b5 levels`). Deducted on apply after a read-only pre-check — if either XP or currency is insufficient, neither is taken. Global switch: `charge-xp: false` in config. Mob XP drops (via `XP:` on mob YAML) and loot-pool `exp:` both ship in rpg-core 1.4.0 — see loot pools docs.

---

### ✅ Loot Pool System (`rpg-core`) — shipped in 1.4.0 / rpg-api 0.4.3

Named reusable loot pools in `plugins/rpg-core/loot-pools/*.yml`. Mobs reference pools via `LootPool: <id>` (single) or `LootPools: [id, ...]` (multiple, all roll independently on kill). Pools carry `exp:` (vanilla XP orbs), `combat-exp:` (skill XP to all damagers), `attribution`, `roll-mode`, `rolls`, `guaranteed`, `currency-rolls`. Inline `LootTable:` still works alongside pool references. `LootPoolRegistry` in `rpg-api`; `LootPoolLoader` in `rpg-core` loads before mobs. Full docs at `docs/content/loot-pools.md`.

---

### ✅ Telekinesis Effect — Drops Straight to Inventory (`rpg-enchanting`) — shipped in 0.6.0
A `telekinesis` property that intercepts mob-drop and block-break item entities and delivers them directly into the player's inventory instead of spawning them on the ground. Needs to be usable as an **enchant**, a **reforge**, or an **upgrade book** — the delivery mechanism should be identical regardless of how the player obtained it.

**Implementation:**
- Add a `Telekinesis: true` flag to `RpgItem` (readable via PDC on the `ItemStack`) — set by the enchant/reforge/upgrade application path
- In `EntityDeathListener` and `BlockBreakListener`, after computing drops, check the player's held item and full equipped set for the telekinesis flag
- If present and the player's inventory has space: call `player.getInventory().addItem(drop)` and cancel the item entity spawn
- If inventory is full: fall back to normal ground drop + send `&cInventory full — item dropped!` action bar message
- Configurable in `config.yml` which drop sources it applies to: `telekinesis.applies-to: [mob_drops, block_drops, both]`

**Three delivery paths (all mark the same PDC flag):**
- **Enchant** — `telekinesis` enchant applicable to weapons and tools; added via the enchanting station
- **Reforge** — a `Telekinetic Edge` reforge stone that applies the flag to a weapon or tool
- **Upgrade** — a `Telekinesis Scroll` upgrade book applicable to any weapon/tool

**Example YAML definitions to ship in defaults:**
```yaml
# enchants/example.yml
telekinesis:
  DisplayName: "&bTelekinesis"
  Description: "Drops teleport straight to your inventory."
  Rarity: RARE
  AppliesTo: [SWORD, AXE, PICKAXE, SHOVEL, HOE, BOW, WAND]
  MaxLevel: 1
```

---

### ✅ Damage Indicators: Float Down + Shrink (`rpg-core`) — shipped in 1.10.3
Animation changed from sin-arc-rising to linear downward drift + shrink. Spawn location raised to `victim.getHeight() + 0.3` so the drift starts above the mob head. New config keys under `damage-indicators`: `drop-blocks` (replaces `rise-blocks`), `start-scale`, `min-scale`. `DamageIndicatorListener.java` updated; `rpg-core/config.yml` updated.

---

### ✅ Mob Death Animation (`rpg-core`) — shipped in 1.6.0
`DeathParticle`, `DeathParticleCount`, `DeathParticleSpread`, `DeathSound` YAML fields on mobs. `MobDeathAnimListener` zeroes knockback velocity at death, spawns configured particle burst, plays configured sound. Loot still drops via RPG death event.

---

### Dungeon System Flesh-out (`rpg-dungeons`) — ⚫ Very Hard
> ⚠️ Fix the enter bug (see [Bugs](todo-bugs.md)) before working on anything below.

1. **Entry requirements not enforced** — `DungeonDef.requiredLevel`, item consumption on entry, currency cost, and party-size min/max are stored in YAML but `DungeonManager.enter()` never checks them.
2. **Per-player loot grants on completion** — `finishInstance()` evicts players without ever rolling the loot pool. Players leave with nothing.
3. **Dungeon editor GUI** — `/dungeon edit <id>` is described in docs but the command doesn't exist. Currently admins hand-edit YAML.
4. **Time limits** — no timer in `DungeonInstance`; no eviction when time expires.
5. **Composite win conditions** — only `KILL_ALL_MOBS` and `REACH_EXIT_BLOCK` work. `ADMIN_END` does nothing.
6. **Display entity instancing** — see dedicated section below.

---

### Dungeon Display Entity Instancing (`rpg-dungeons` + `rpg-holograms`) — 🟡 Medium
> ⚠️ Requires the Display Entity Suite (rpg-holograms) to be built first. Also requires the dungeon enter bug to be fixed.

When a dungeon instance is created via schematic paste, the paste only copies **blocks** — entities are not included. Any `TextDisplay`, `ItemDisplay`, or `BlockDisplay` entities the admin placed in the template region (e.g. floating boss name labels, decorative item floats, room description text) are silently absent from the instance. This needs an explicit capture-and-respawn system.

**Admin workflow:**
1. Admin builds the dungeon template with display entities placed using `/de create*` / `/de edit` as normal
2. Admin runs **`/dungeon capturedisplays <id>`** — scans the template region (the same bounding box used for the schematic paste), finds all entities tagged with the `rpg_display_id` PDC key (this key is defined when the display entity suite in `rpg-holograms` is built — it does not exist yet), records each one's **position relative to the template origin** and its full display entity definition (same YAML fields as `plugins/rpg-holograms/displays/`), and writes them into the dungeon YAML under a `DisplayEntities:` block
3. From this point, the template-world entities are only needed for re-capture — the dungeon YAML is the authoritative source, so the template world can be unloaded or the entities deleted without affecting instance spawning

**Instance spawn/despawn lifecycle:**
- When `DungeonManager.enter()` creates a new `DungeonInstance`, after the schematic paste completes it reads `DungeonDef.displayEntities`, computes `instanceOrigin + relativeOffset` for each entry, and spawns a fresh display entity at that world position in the instance world
- These entities are tagged with a `rpg_dungeon_instance_id` PDC key so they can be batch-removed
- They are **never written to the persistent `displays/` YAML files** — they are ephemeral, existing only for the lifetime of the instance
- When the instance is cleaned up (`DungeonManager.finishInstance()` / timeout / all players leave), all entities carrying the `rpg_dungeon_instance_id` tag for that instance UUID are removed

**`/dungeon capturedisplays` command in detail:**
- Requires `rpg.dungeons.admin` permission
- Reports how many display entities were found and saved: `&aCaptured 7 display entities for dungeon 'crypt_of_doom'.`
- Re-running it overwrites the previous `DisplayEntities:` block (so admins can update after making changes)
- `/dungeon listdisplays <id>` — prints the captured display entity list (type, relative offset, id) so admins can verify without re-entering the template world

**YAML schema (inside the dungeon definition):**
```yaml
crypt_of_doom:
  TemplateWorld: dungeon_templates
  # ... other fields ...
  DisplayEntities:
    - Id: boss_nameplate        # original display entity id from rpg-holograms
      Type: TEXT
      Offset: {x: 0.5, y: 3.2, z: 0.5}   # relative to template origin corner
      Definition:               # full display entity YAML, same fields as displays/text/*.yml
        Lines: ["&4&lThe Lich King"]
        Billboard: CENTER
        Shadowed: true
        Scale: {x: 1.5, y: 1.5, z: 1.5}
        Brightness: {block: 15, sky: 15}
    - Id: entry_skull
      Type: ITEM
      Offset: {x: 4.0, y: 1.5, z: 8.0}
      Definition:
        Item: "vanilla:SKELETON_SKULL"
        ItemDisplayTransform: HEAD
        Billboard: FIXED
        Scale: {x: 2.0, y: 2.0, z: 2.0}
```

**Cross-plugin dependency:** `rpg-dungeons` soft-depends on `rpg-holograms`. If `rpg-holograms` is not loaded, the `capturedisplays` command and the instance-spawn step are silently skipped — dungeons still work without display entities.

---

### ✅ Stats GUI Redesign (`rpg-core`) — shipped in 1.7.0
54-slot inventory GUI with gear column (helmet/chest/legs/boots/weapon), 7 stat-category items (Combat/Survival/Caster/Mobility/Loot/Wisdom/Skills) with full lore breakdowns, Trade button when viewing another player, nav bar with Close. `/stats [player]` with `rpg.core.stats.view.others` permission.

---

### ✅ HUD: Scoreboard + Tablist Improvements (`rpg-hud`) — shipped in 0.4.1
Scoreboard, tablist header/footer, PAPI `%rpg_X%` placeholders, ability cooldown display, nametag improvements all shipped. `RpgPlaceholderExpansion` registers all HUD keys as `%rpg_X%`; softdepends on PlaceholderAPI.

---

### ✅ Knockback on All Weapons + Wands (`rpg-core`) — shipped in 1.10.3
- **BeamEffect**: wired `KNOCKBACK` stat after target is set — reads caster stat via `RpgServices.player()` / `RpgServices.mobStats()`, applies repel velocity identical to the melee pipeline (`knockback / 100.0` strength, Y-clamped to max 0.5).
- **Arrows/melee**: already wired in `DamagePipelineListener.onDamage` (both projectile shooter and melee attacker paths). No gap remained.
- **Example items**: added `knockback:` stat to 9 weapons that were missing it (`phantom_blade`, `vampiric_dagger`, `blink_dagger`, `soul_siphon`, `frost_lance`, `chain_lightning_wand`, `dual_cast_wand`, `thunderstrike_wand`, `executioner_blade`). All combat weapons now have the stat.

---

### RPG-Farming Redesign (`rpg-farming`) — 🔴 Hard
Current state: XP for breaking vanilla crops + FARMING_FORTUNE drop multiplier only.

Planned redesign (mirrors the custom blocks system):
- Admins assign world blocks to custom farming block types (like `/rpg block convert`)
- Custom farming blocks cycle through visual growth stages
- Not breakable until fully grown (cancel + `§cNot ready` action bar message)
- Growth time configurable per crop type in `config.yml`
- Breaking a fully-grown crop drops configured loot + restarts the growth cycle
- Requires `DataStore` persistence for per-block growth timers (like `BlockPersistence` in rpg-core)

---

### Guild System Flesh-out (`rpg-guilds`) — 🔴 Hard
Current: create / invite / kick / promote / demote / leave / disband / deposit / withdraw / XP / perks all work. Missing:

1. **Tiered bank** — item vault (configurable slot count) + currency cap per tier; upgrade requires guild level + cost
2. **Configurable rank slots** — server admin defines rank names; guild owner renames per-guild slot instances
3. **Per-rank permission flags** — who can invite, kick, bank deposit/withdraw, etc.
4. **Audit log** — every bank transaction recorded and viewable in GUI
5. **Bank + ranks GUIs** — `/guild bank` and `/guild ranks` commands currently missing

---

### Fishing Content Slice (`rpg-fishing`) — 🟡 Medium
Current: XP per catch + FISHING_WISDOM scaling only. Missing:
- Custom fish YAML loader + registry (fish types, rarities, weights, display size)
- Custom loot table roll on each catch (replacing vanilla fishing loot)
- Biome + time-of-day catch restrictions
- Rod item stat scaling: `fishing_speed` (time-to-bite), `fishing_fortune` (drop quantity), `sea_creature_chance`
- Sea-creature spawning when `sea_creature_chance` rolls (spawn mob from mob registry at float location)

---

### Accessories: Tier Upgrades + Family Stacking + Bag Upgrade Button (`rpg-accessories`) — 🟡 Medium
Current: bag opens, only ACCESSORY items allowed, stats aggregate, persistence works. Missing:

1. **Tier upgrades** — expand bag slot count when player upgrades the bag tier
2. **Family-based stacking rules** — e.g., two rings stack, three of the same family don't
3. **In-bag upgrade button** — bottom row of the accessory bag GUI should have a dedicated upgrade button so players can upgrade the bag tier without typing a command. Show current tier, cost to upgrade, and disable the button if the player can't afford it or is at max tier.

---

### Quest Log GUI (`rpg-quests`) — 🔴 Hard
Current: `/quest list` prints to chat. Planned 54-slot inventory GUI:

**Main list view:**
- Three tab buttons at the top: `Active`, `Available`, `Completed`
- Quest entries fill the remaining slots — each is a named item (book for active, map for available, checkmark for completed)
- Item lore shows: quest display name, brief description (first line), objective count or `Completed` tag
- Pagination if more quests than slots (Previous/Next buttons in bottom corners)
- `/quests` command opens it; `/quest <id>` opens directly to that quest's detail view

**Detail view (click a quest):**
- Quest display name as inventory title
- Description lines shown on a named item in the top-left
- Objectives listed as separate items with current/required count (e.g., `Kill Goblin: 3/10`)
  - Completed objectives show a green checkmark; incomplete show a red X
  - Progress bar in the item lore using filled/unfilled block characters
- Rewards shown as a separate item listing currency, skill XP, and item rewards
- Accept button (green) / Abandon button (red) / Back button
- If a quest requires a prerequisite not yet completed, the accept button is grayed out and says which quest is blocking

---

### Holograms: Tab Completions + Full TextDisplay Control + GUI (`rpg-holograms`) — 🟡 Medium
Current: `/holograms create|delete|list|tp|move|line` commands and persistence work. Three things missing:

#### 1. Tab completions — 🟢 Easy
Every argument of every `/holograms` subcommand should tab-complete:

| Argument position | Completions |
|---|---|
| Subcommand | `create delete list tp move line set info` |
| `<id>` on any command | All existing hologram IDs |
| `/holograms line <id>` | `add set remove list` |
| `/holograms line <id> set\|remove` | Current line index numbers (0, 1, 2...) |
| `/holograms set <id>` | All property names (see list below) |
| Boolean properties | `true false` |
| `billboard` | `FIXED VERTICAL HORIZONTAL CENTER` |
| `alignment` | `LEFT CENTER RIGHT` |
| `background` | `transparent default` then r/g/b/a hint |

#### 2. Full TextDisplay property control — 🟡 Medium
Add a `/holograms set <id> <property> [values...]` command family covering every `TextDisplay` (and parent `Display`) API field. All properties should also be serialised to the hologram's YAML so they survive reload.

**Text content:**
| Property | Command syntax | Notes |
|---|---|---|
| `text` | `/holograms line ...` (existing) | Multi-line via `\n` in YAML |
| `alignment` | `set <id> alignment LEFT\|CENTER\|RIGHT` | Default: CENTER |
| `linewidth` | `set <id> linewidth <pixels>` | Default: 200px; controls word-wrap |

**Visual appearance:**
| Property | Command syntax | Notes |
|---|---|---|
| `background` | `set <id> background <r> <g> <b> <a>` or `transparent` or `default` | ARGB 0–255 each. `transparent` = `0,0,0,0`. `default` = vanilla translucent dark panel. |
| `opacity` | `set <id> opacity <0-255>` | Text opacity. 255 = fully visible. |
| `shadowed` | `set <id> shadowed true\|false` | Text drop shadow. |
| `seethrough` | `set <id> seethrough true\|false` | Visible through solid blocks. |

**Display orientation + distance:**
| Property | Command syntax | Notes |
|---|---|---|
| `billboard` | `set <id> billboard FIXED\|VERTICAL\|HORIZONTAL\|CENTER` | `CENTER` = always faces camera (most common). `FIXED` = world-locked, doesn't rotate. |
| `viewrange` | `set <id> viewrange <float>` | Render-distance multiplier. 1.0 ≈ 64 blocks. 2.0 = 128 blocks. |
| `brightness` | `set <id> brightness <block 0-15> <sky 0-15>` | Override local lighting. `15 15` = always fully lit regardless of darkness. |

**Scale, offset, shadow:**
| Property | Command syntax | Notes |
|---|---|---|
| `scale` | `set <id> scale <x> <y> <z>` | Floats. 1.0 = normal. Applied via `Transformation`. |
| `offset` | `set <id> offset <x> <y> <z>` | Sub-block translation without teleporting the entity. Useful for stacking multiple displays. |
| `shadowradius` | `set <id> shadowradius <float>` | Ground shadow circle size. 0 = no shadow. |
| `shadowstrength` | `set <id> shadowstrength <0.0-1.0>` | Ground shadow opacity. |

**Glow:**
| Property | Command syntax | Notes |
|---|---|---|
| `glow` | `set <id> glow true\|false` | Outline glow visible through blocks. |
| `glowcolor` | `set <id> glowcolor <r> <g> <b>` | Override glow outline color. |

**Smooth interpolation (for animated holograms):**
| Property | Command syntax | Notes |
|---|---|---|
| `interpolation` | `set <id> interpolation <delay_ticks> <duration_ticks>` | Smoothly interpolates transformation changes (scale, offset). `delay=0 duration=0` = instant (default). |
| `teleportduration` | `set <id> teleportduration <ticks>` | Smooth movement when the entity is teleported (e.g., via `/holograms move`). |

**YAML schema** — all properties optional, sane defaults apply if absent:
```yaml
my_hologram:
  Location: {world: world, x: 100.5, y: 65.0, z: 200.5}
  Lines:
    - "&6Welcome to the Server!"
    - "&7Right-click to open shop"
  Billboard: CENTER
  Background: transparent       # transparent | default | r,g,b,a
  Shadowed: true
  SeeThrough: false
  Opacity: 255
  Alignment: CENTER
  LineWidth: 200
  ViewRange: 1.0
  Brightness: null              # null = use world lighting; or {block: 15, sky: 15}
  Scale: {x: 1.0, y: 1.0, z: 1.0}
  Offset: {x: 0.0, y: 0.0, z: 0.0}
  ShadowRadius: 0.0
  ShadowStrength: 1.0
  Glowing: false
  GlowColor: null
  Animated: false
  FrameInterval: 20
```

#### 3. GUI editor — 🟡 Medium
See [GUI Redesigns](todo-gui.md) for the full layout spec. The GUI replaces manual `/holograms set` typing for non-technical admins and shows all settings at a glance.

---

### Regions: Polygon + Wand + GUI (`rpg-regions`) — 🔴 Hard
Current: cube-around-player only. Deferred:
- Two-point wand definition (left-click pos1, right-click pos2)
- Polygonal region support (2D polygon + Y range)
- Region-bounds GUI editor

---

### ✅ Chat: Staff Channel (`rpg-chat`) — shipped in 0.1.1
`/chat staff` channel added. Requires `rpg.chat.use.staff` permission to send and receive. `ChatFormatListener.pickRecipients` filters to all online players holding that permission (sender always included). Tab-complete lists `staff` alongside `global/party/guild`. Config key `channel-prefix-staff` added with default `"&8[&cStaff&8] "`. Custom admin-defined channels deferred to a later slice.

---

### HUD: Nametag Status-Effect Icons (`rpg-hud`) — 🟡 Medium
Current: nametags show name + prefix/suffix. Deferred:
- Active status-effect icons displayed on or above the nametag

---

### Mob Factions + AI Goals (`rpg-core`) — 🟡 Medium

Mobs belong to a **faction** (a string tag), and their targeting behaviour is defined by an ordered **goal list** instead of a single profile kind. This replaces the blunt `aggressive / defensive / passive / stationary` enum with fine-grained, composable targeting rules — and unlocks mob-vs-mob conflict, faction alliances, and dungeon faction warfare.

#### Faction field

```yaml
forest_guard:
  Faction: guards    # string ID — no separate registry needed; factions are just labels

undead_minion:
  Faction: undead
```

Any mob without a `Faction:` field is **neutral** — attacked by aggressive mobs but doesn't trigger faction defence chains.

**Special reserved faction:** `player` — any online player. `attack_faction{faction=player}` is equivalent to `attack_player` (which remains as a shorthand).

#### AiGoals list

`AiGoals:` is an ordered list of goal entries. The AI evaluates them top-to-bottom and acts on the first one whose condition is currently met.

```yaml
forest_guard:
  Faction: guards
  AiGoals:
  - attack_faction{faction=undead}      # primary target: any undead mob in range
  - attack_faction{faction=bandits}     # secondary: bandits
  - defend_faction{faction=guards}      # if a guard is under attack, target that attacker
  - assist_faction{faction=guards, radius=20}  # if a nearby guard is in combat, join them
  - idle                                # wander if nothing to do

undead_minion:
  Faction: undead
  AiGoals:
  - attack_player
  - attack_faction{faction=guards}
  - idle

skittish_villager:
  Faction: villagers
  AiGoals:
  - flee_from{faction=undead, health_threshold=100}  # always flees from undead
  - flee_from{faction=player, health_threshold=30}   # flees players when low HP
  - idle
```

#### Goal reference

| Goal | Parameters | Behaviour |
|---|---|---|
| `attack_player` | — | Target nearest player in aggro range |
| `attack_faction{faction=X}` | `faction`, `range=` | Target nearest mob of faction X in range |
| `defend_faction{faction=X}` | `faction`, `radius=` | If a member of faction X is attacked, target that attacker |
| `assist_faction{faction=X}` | `faction`, `radius=` | If a member of faction X is already in combat, help them by targeting their target |
| `flee_from{faction=X}` | `faction`, `range=`, `health_threshold=100` | Flee from the nearest entity of faction X; `health_threshold` sets the HP% at or below which fleeing activates (100 = always flee) |
| `call_for_help{faction=X}` | `faction`, `radius=` | When this mob is hurt, alert nearby faction X members to target the attacker |
| `guard_radius{radius=N}` | `radius` | If mob is pulled more than N blocks from spawn, disengage and return (leash behaviour) |
| `idle` | — | Wander / stand; no combat target. Fallback when no other goal is active |

Goals evaluate top-to-bottom each AI tick. First goal with a valid target wins.

#### Backwards compatibility

The existing `MobAiProfile.Kind` enum continues to work unchanged. Internally, each kind maps to an equivalent goal list:

| Old kind | Equivalent AiGoals |
|---|---|
| `aggressive` | `[attack_player, idle]` |
| `defensive` | `[defend_player, idle]` |
| `passive` | `[idle]` |
| `stationary` | `[idle]` + no movement |

Mobs that specify `AiGoals:` ignore `MobAiProfile.Kind`. Mobs without `AiGoals:` continue using the kind as before.

#### Faction-awareness in ability DSL

Once target-selection effects (todo #24) land, `nearest_enemy{}` and `nearest_ally{}` both accept an optional `faction=` parameter:

```yaml
# Mob timer: drain the nearest undead ally
- "nearest_ally{faction=undead, range=10.0} heal{amount=10, target=target} ~onTimer:40"

# Boss: pull the nearest guard toward the boss then slam
- "nearest_enemy{faction=guards, range=20.0} launch{force=1.5, direction=toward} aoe{radius=4.0, damage=30.0} ~onTimer:80"
```

Without `faction=`, `nearest_enemy{}` uses existing hostile-detection logic; with it, it targets only entities of that specific faction.

#### Implementation

- `CoreRpgMob` gets a `faction()` field (`String`, nullable). Added to mob YAML loader and `MobDef`.
- A new `AiGoalDef` sealed interface mirrors `MobAbilityTrigger` — one record per goal type, parsed from the YAML list.
- `MobAiTask` currently dispatches on `MobAiProfile.Kind`; add a second branch: if `mob.hasGoals()`, evaluate the goal list instead.
- For `defend_faction` and `assist_faction`, `MobAbilityEventListener.onPostDamage` fires a lightweight `FactionDefendEvent` that nearby faction members can react to — or simpler: a `FactionAlertMap` (victim UUID → attacker) that the AI task checks each tick.
- Faction membership check: `RpgServices.mobs().from(entity).map(m -> faction.equals(m.faction())).orElse(false)`.
- `guard_radius`: store spawn location in mob PDC on `OnSpawn`; check distance each AI tick.

---

### Mob AI Profiles Flesh-out (`rpg-core`) — 🔴 Hard
Current: `aggressive`, `passive`, `defensive`, `stationary` work. All others fall back to aggressive. Deferred:
- `ranged_kiter` — back up if player within melee range, fire ranged ability
- `boss` — phase transitions, ability rotations
- `swarming` — call nearby same-type mobs when aggro'd (implement via `call_for_help` goal once factions land)
- `pack_hunter` — coordinate target focus with nearby pack members (implement via `assist_faction` + `attack_faction` goals once factions land)
- `flying` — 3D pathfinding, strafe patterns

---

### Mob Patrol Waypoints (`rpg-core`) — 🔴 Hard
Currently mobs (and NPCs) stand still when no player is nearby. A patrol behaviour lets admins define a list of waypoints a mob walks between, making the world feel more alive.

- New AI profile: `patrol` — cycles through a list of `Waypoints` defined in mob YAML (world + x/y/z coordinates)
- Can optionally pause at each waypoint for a configurable `WaypointPauseTicks` before moving on
- If a player gets within aggro range, the mob switches to `aggressive` temporarily; on losing aggro, returns to patrol
- Admin commands: `/mob setpatrol <mobId> add` (adds player's current location as next waypoint), `/mob setpatrol <mobId> clear`
- NPCs with patrol defined follow the same waypoint path (compatible with `LookAtPlayers` — look at nearest player while patrolling, continue walking otherwise)

---

### ✅ Loot Tables: External File Reference (`rpg-core`) — shipped in 1.10.2
`LootTable: <id>` string references now work end-to-end. `LootTableLoader` loads `plugins/rpg-core/loot-tables/*.yml` before mob loading; `MobLoader` detects string vs section for `LootTable:`; `MobLootListener` resolves the registry and rolls the referenced table (including its vanilla XP and combat XP contributions). `example.yml` resource shipped with `forest_common` sample table. Coin drops via economy deposit were already wired through `depositCurrency`.

---

### ✅ NPC Command Overhaul + In-Game Editing (`rpg-npcs`) — shipped in 0.6.0

Per-NPC `EntityType` field + `/npc setentitytype` with tab-complete. `/npc setstyle` + `/npc setskin` for style/skin. Full in-game dialogue editing (`/npc dialogue add|set|remove|clear|list`). Full in-game shop editing (`/npc shop add|remove|list|clear`). Quest assignment tab-complete wired. Look-at-player task with `LookAtPlayers: true` + `LookRadius:` per NPC. `/npc info <id>` shows all settings. General `/npc` help listing. Patched to 0.6.1 for orphan sweep + ZOMBIE default + handler priority fix.

---

### ✅ Region: Enter/Exit Messages + More Flags (`rpg-regions`) — shipped in 0.6.0
`enter-message` / `leave-message` (title or `[actionbar]` prefix; `{player}`/`{region}` placeholders), `no-mob-spawn`, `no-damage`, `fly` (granted/revoked on enter/leave), `no-item-drop`, `keep-inventory`. Flag table in docs rewritten. Region priority field also shipped.

---

### Quest: Chains + Repeatable Quests (`rpg-quests`) — 🟡 Medium
Currently all quests are one-shot and independent. Missing:

1. **Quest chains** — `Requires: [quest_id, ...]` field on a quest definition. The quest is not offerable until all prerequisites are completed.
2. **Repeatable quests** — `Repeatable: true` + `CooldownSeconds: 86400` (e.g., daily quests). After completion, the quest becomes available again after the cooldown. Per-player last-completion timestamp tracked in `DataStore`.

---

### Animated Holograms (`rpg-holograms`) — 🟡 Medium
Static holograms only cycle when edited. Add support for cycling text:

- Optional `Animated: true` + `FrameInterval: 20` on a hologram definition
- Multiple entries under `Lines` become animation frames — the displayed text cycles through them at `FrameInterval` ticks
- Useful for animated signs, status displays, countdown timers

---

### Display Entity Suite: ItemDisplay, BlockDisplay + Physical Editor (`rpg-holograms`) — 🔴 Hard
Expand `rpg-holograms` beyond text holograms into a full display entity toolkit covering all four Minecraft display entity types. The headline feature is a **DEE-style physical editor** (inspired by the Display Entity Editor plugin): a `/de` command that clears the player's inventory, gives them a set of manipulation tools, and lets them push/scale/rotate the selected entity in real-time by clicking with those items. A second `/de` call restores their saved inventory.

---

#### New entity types and creation commands

| Command | Creates | Notes |
|---|---|---|
| `/de createtext [id]` | `TextDisplay` | Same as current holograms; bridges to existing `/holograms` system |
| `/de createitem <itemId> [id]` | `ItemDisplay` | Floating RPG item or vanilla material |
| `/de createblock <blockType> [id]` | `BlockDisplay` | Floating block |
| `/de list` | — | Lists all managed display entities (all types) with type tag + location |
| `/de delete <id>` | — | Removes entity + persisted data |
| `/de info <id>` | — | Chat dump of all current property values |
| `/de copy <id> [newId]` | — | Duplicates a display entity at the same location |
| `/de tp <id>` | — | Teleports player to the entity |
| `/de select` | — | Selects the display entity the player is looking at (within 10 blocks); required before `/de edit` |
| `/de edit` | — | Enters editor mode on the currently selected entity (saves + replaces inventory) |
| `/de edit <id>` | — | Selects by id and enters editor mode in one step |

All created entities are persisted to `plugins/rpg-holograms/displays/<type>/<id>.yml` and re-spawned on reload.

---

#### Physical editor mode (inventory replacement)

When a player runs `/de edit`, the plugin:
1. **Serialises the player's entire inventory** (all 36 slots + armor + offhand) to `DataStore` keyed by `player UUID`
2. **Clears the inventory** and fills it with the editor items below
3. Entering a new `/de edit` while already in editor mode is blocked with a warning — run `/de done` or `/de cancel` first

The player exits editor mode with:
- `/de done` — saves all changes + restores inventory
- `/de cancel` — reverts all transformations to entry state + restores inventory
- If the player disconnects mid-edit: their saved inventory is restored on next login; changes since last save are discarded

**Editor item layout (hotbar slots 1–9):**

| Slot | Item | Name | Function |
|---|---|---|---|
| 1 | RED_CONCRETE | `X Axis` | Active axis = X (red). Right-click = +step, Left-click = −step in current mode |
| 2 | GREEN_CONCRETE | `Y Axis` | Active axis = Y (green) |
| 3 | BLUE_CONCRETE | `Z Axis` | Active axis = Z (blue) |
| 4 | COMPARATOR | `Step Size` | Cycles step size: 0.001 → 0.01 → 0.1 → 0.5 → 1.0. Current size shown in item name. |
| 5 | COMPASS | `Mode` | Cycles manipulation mode: `Translate → Scale → Rotate → Left Rotation → Right Rotation`. Current mode shown in name. |
| 6 | BOOK | `Open GUI` | Opens the fine-detail GUI editor (see GUI spec in [GUI Redesigns](todo-gui.md)) |
| 7 | ENDER_PEARL | `Undo` | Reverts the last single manipulation step (up to 20 undo steps) |
| 8 | LIME_CONCRETE | `Done` | Save + exit editor mode |
| 9 | RED_CONCRETE | `Cancel` | Discard + exit editor mode |

**How manipulation works:**
- Player holds an axis item (slot 1/2/3) in their main hand and right/left-clicks
- Right-click = add current step in the active axis; Left-click = subtract
- The manipulation applied depends on the active **mode**:
  - `Translate` — moves the entity's transformation **translation** (offset from its world position) by the step along the chosen axis
  - `Scale` — multiplies the chosen axis scale component by `(1 + step)` or `(1 - step)` (left = shrink)
  - `Rotate` — rotates the entity's **right rotation** quaternion around the chosen axis by `step` radians
  - `Left Rotation` — same but modifies the **left rotation** component (applied before scale)
  - `Right Rotation` — explicit right rotation (same as Rotate — kept separate so admins can isolate which component they're touching)
- The axis color overlay on the entity flashes briefly on each step (particle burst in the corresponding color) as confirmation
- Entity updates immediately each click — no need to confirm each step

---

#### Entity-type-specific properties

**ItemDisplay:**
- `Item` — which item to show. In YAML: an RPG item id OR a vanilla Material name
- `ItemDisplayTransform` — controls which model transform preset applies. Options:
  - `NONE`, `GUI` (flat icon style), `GROUND` (flat on ground), `FIXED` (item-frame style), `HEAD` (worn-on-head style), `FIRSTPERSON_RIGHTHAND`, `FIRSTPERSON_LEFTHAND`, `THIRDPERSON_RIGHTHAND`, `THIRDPERSON_LEFTHAND`
  - Default: `FIXED` for most decorative uses; `GUI` for floating icon style
- Commands: `/de setitem <id> <itemId>`, `/de settransform <id> <preset>`

**BlockDisplay:**
- `Block` — which block data to show. In YAML: a Bukkit `BlockData` string (e.g., `minecraft:oak_stairs[facing=north,half=bottom]`)
- Commands: `/de setblock <id> <blockType> [blockState...]`

**TextDisplay:**
- Delegates to the `/holograms` command and editor GUI — the `/de` system treats existing hologram IDs as TextDisplay entities and vice versa. The same persistence file is shared.

---

#### Common Display properties (all entity types)

All properties from the existing TextDisplay spec also apply here. Key additions relevant to ItemDisplay/BlockDisplay that weren't emphasized before:

- **`DisplayWidth` / `DisplayHeight`** — the entity's culling box. If the box is outside the player's view frustum the entity is hidden. Default is 0×0 (never culled). For large block displays or multi-display builds, set this to encompass the visual size so culling works correctly.
- **`InterpolationDuration`** — crucial for animated display rigs: setting a duration then immediately changing Transformation causes a smooth tween. Setting to 0 = instant snap.

---

#### YAML schema (ItemDisplay example)

```yaml
# plugins/rpg-holograms/displays/item/floating_sword.yml
floating_sword:
  Type: ITEM
  Location: {world: world, x: 100.5, y: 65.0, z: 200.5}
  Item: iron_shortsword            # RPG item id or vanilla material
  ItemDisplayTransform: FIXED
  Billboard: FIXED
  Scale: {x: 1.5, y: 1.5, z: 1.5}
  Offset: {x: 0.0, y: 0.3, z: 0.0}
  LeftRotation:  {x: 0.0, y: 0.0, z: 0.0, w: 1.0}  # quaternion
  RightRotation: {x: 0.0, y: 0.7071, z: 0.0, w: 0.7071}
  Brightness: {block: 15, sky: 15}
  ViewRange: 1.5
  Glowing: false
  Animated: false
```

```yaml
# plugins/rpg-holograms/displays/block/floating_chest.yml
floating_chest:
  Type: BLOCK
  Location: {world: world, x: 50.5, y: 70.0, z: 80.5}
  Block: "minecraft:chest[facing=south,type=single,waterlogged=false]"
  Billboard: FIXED
  Scale: {x: 0.5, y: 0.5, z: 0.5}
  Offset: {x: 0.0, y: 0.0, z: 0.0}
  LeftRotation:  {x: 0.0, y: 0.0, z: 0.0, w: 1.0}
  RightRotation: {x: 0.0, y: 0.0, z: 0.0, w: 1.0}
  Brightness: null
```

---

#### See also
- [GUI Redesigns](todo-gui.md) — full slot layout for the fine-detail GUI editor
- The existing TextDisplay/hologram entries above for text-specific properties
- **Dungeon Display Entity Instancing** (below) — how display entities placed in dungeon templates get captured and re-spawned per-instance

---

### Party: HP/Status Display (`rpg-parties`) — 🟡 Medium
Players in a party have no way to see their teammates' health or status. Options:

- Boss bars (one per party member, shown to all other members) — simple but uses up boss bar slots fast
- Action bar or scoreboard sidebar section showing compact party HP (preferred)
- Configurable on/off in party settings; don't force it on everyone

---

### Action Bar Cooldown Notification for Wand Abilities (`rpg-core`) — 🟢 Easy
When a player uses a wand ability that has a `cooldown{}` in its chain, show a brief action bar message so the player knows how long until it's ready again. Currently there's no feedback — the ability just silently fails if it's on cooldown and the player clicks again.

**Desired behaviour:**
- On a blocked cast (ability fires but `cooldown{}` gate returns early), send an action bar message like: `&cAbility on cooldown — &e3.2s remaining`
- Only show it when the player tries to cast while on cooldown (not passively every tick)
- If multiple abilities are on cooldown, show the one the player just tried to cast

**Implementation pointers:**
- `CooldownEffect` in `rpg-core` is where `cooldown{}` checks remaining time — this is where the action bar message should fire
- `AbilityContext` carries the caster; cast via `caster.sendActionBar(...)` if it's a `Player`
- Time remaining: `(cooldownTicks - ticksElapsed) / 20.0` formatted to one decimal place
- Should respect the `[Sync]` rules — action bar sends are already per-player/client-side, no network concern

---

### Player Profile Command (`rpg-core`) — 🟡 Medium
No way to view another player's public info. Add `/profile [player]`:

- No args = your own profile; with a player name = their profile (requires `rpg.profile.view.others`)
- **GUI layout (27 or 54 slots):**
  - Player head item (top-left) with name, guild tag, party status in lore
  - Top skill levels shown as named items (e.g., "⚔ Combat Lv.12", "⛏ Mining Lv.8")
  - Most valuable equipped gear slot items (display only)
  - Balance shown on a gold coin item
  - Recent achievements (last 3 unlocked) shown as named items
  - "Send Trade Request" button if viewing another player
- Target player must be online to view their profile (or show a last-known snapshot if offline data is cached)
- Players can opt out of public profiles via `rpg.profile.private` permission — their profile shows "This player's profile is private."
- Tab-complete for the player argument lists online player names

---

### ✅ Economy: Transaction Log (`rpg-economy`) — shipped in 0.2.1
`TxLog` with DataStore persistence; `/money log [player] [page]`; reason-tagged deposits/withdrawals/transfers; mob drops tagged `mob_drop`. Log capped at 100 entries per player.

---

### ✅ Permission System: Consistency Audit + Fill Gaps (all plugins) — shipped in rpg-regions 0.5.1
Added `rpg.core.particle` + `rpg.regions.admin.global` to plugin.ymls; `docs/permissions.md` fully rewritten covering all 25 plugins. Convention `rpg.<plugin-short-name>.<verb>[.<qualifier>]` adopted suite-wide. See `docs/permissions.md` for the full node reference.

---

### Repurposed Vanilla Bars: Remove XP Bar (`rpg-core` / `rpg-hud`) — 🟢 Easy

The `vanilla-xp-bar: most-recent` setting in `docs/core/vanilla-suppression.md` repurposes the vanilla XP bar to show skill progress. This directly conflicts with `rpg-enchanting`: the enchanting station charges vanilla XP levels, and the XP bar is the standard visual signal for that cost. When the bar is being hijacked to display skill progress, players see misleading numbers and can't tell at a glance how many levels they'll lose on an enchant.

**Fix:** Remove the vanilla XP bar from the repurposed-bars feature entirely.

- Drop `vanilla-xp-bar` from the `vanilla-suppression` config block (or change its default to `hidden` and remove the `most-recent` / `pinned` options)
- Remove the skill-pinning logic that writes to the XP bar (`/skill pin`, `rpg.core.skill.pin` permission, `XpBarUpdater` or equivalent)
- Skill progress is already shown in the scoreboard HUD and the Skills GUI — no replacement display needed
- Update `docs/core/vanilla-suppression.md`: remove the XP row from the "Repurposed vanilla bars" table and the `vanilla-xp-bar:` config example
- Update `docs/commands.md` and `docs/permissions.md`: remove the `/skill pin` and `rpg.core.skill.pin` entries

---

### Add More Example Status Effects (`rpg-core`) — 🟢 Easy

The existing `status-effects/example.yml` ships four effects (`burning`, `frozen`, `silenced`, `haste`). That's enough to prove the system works but not enough to test the full range of what status effects can do. Add more built-in examples covering the remaining stat types and edge cases admins will actually want to copy from.

Suggested additions (one effect per notable mechanic — add whatever makes sense):

| ID | What it tests |
|---|---|
| `weakness` | Negative DAMAGE stat modifier |
| `speed_boost` | Positive SPEED modifier (movement) |
| `vulnerability` | Negative DEFENSE modifier (makes the player take more damage) |
| `regeneration` | Positive HEALTH_REGEN modifier |
| `mana_drain` | Negative max mana or mana_regen modifier |
| `thorns` | Could reflect a % of damage — or just a damage-on-hit note if not yet implemented |
| `blindness` | Applies vanilla blindness via `PotionEffect` block in effect YAML (if supported) |
| `slow` | Negative SPEED modifier — counterpart to `frozen` but lighter (e.g., -30%) |

Stick to effects that the existing `StatusEffectLoader` and `CoreStatusEffectService` can already handle — no new mechanics needed, just more YAML entries demonstrating existing params (`stat-modifiers`, `amplifier`, `duration`, `particles`, `apply-sound`).

---

### Fill Out Example Crafting Recipes (`rpg-crafting`) — 🟢 Easy

`rpg-crafting 0.1.0` ships but the bundled `recipes/example.yml` is minimal. A tester picking up the plugin can't easily verify shaped vs shapeless, RPG-item ingredients, or multi-item outputs without building their own recipes from scratch. Fill out the example file so the system is end-to-end testable out of the box.

Add at least:

1. **Shaped recipe using vanilla materials** — e.g., a 3×3 `red_gem_block` from 9 `red_gem` items (mirrors vanilla block-crafting pattern; good smoke test for grid matching)
2. **Shaped recipe mixing vanilla + RPG items** — e.g., craft an `iron_shortsword` from an iron ingot + a stick in a vertical pattern (tests that RPG item IDs resolve correctly in ingredient slots)
3. **Shapeless recipe** — e.g., combine any 2 different potions to get a `mixed_brew` (tests that slot order doesn't matter)
4. **Recipe with amount > 1 output** — e.g., 1 `bone` → 3 `bone_meal` shapeless (tests the `Amount:` field on the result)

Each recipe should use item IDs that already exist in `items/example.yml` so there's no dependency on custom server content. Annotate each with a comment explaining which mechanic it demonstrates.

---

### Unit Test Coverage (all plugins) — 🟡 Medium (ongoing)
Currently only two test files exist: `QuestObjectiveTest.java` and `DamageMathTest.java`. For a codebase this size, untested code means regressions are invisible until they hit the live server. Priority areas:

- `DamageMath` — expand existing tests: crit, defense reduction, level scaling edge cases
- `StationGui` — recipe matching logic (no separate `SlotResolver` class; matching lives inside `StationGui` directly)
- `ExpressionEvaluator` — skill curve calculations (accessible via `RpgServices.expressions()`)
- `QuestManager` — objective progression and completion
- `BossBarService` / `SignEntryService` once built

---

### ✅ Vanilla Suppression Remaining Flags (`rpg-core`) — already shipped
All 6 flags verified wired during session audit: `villager-trading` (PlayerInteractEntityEvent), `beacons` (BeaconEffectEvent), `pillager-patrols` (PATROL SpawnReason in EntitySpawnEvent), `block-explosion-damage` (EntityExplodeEvent + BlockExplodeEvent), `durability` (DurabilityListener.java), `death-drops` (PlayerDeathEvent). No gaps remaining.

---

### ✅ Economy: Vault Provider Bridge (`rpg-economy`) — shipped in 0.2.0
Vault provider registered on enable; `VaultEconomyAdapter` wraps `CoreEconomy`. Noted in suite 21 changelog.

---

### ✅ Status Effects: Catalog + New Built-in Types (`rpg-core`) — shipped in 1.10.2
Added `burning` (fire tick damage, FLAME particles on apply), `frozen` (-80% speed debuff, SNOWFLAKE particles on apply), `silenced` (blocks active ability use — enforced in `ItemAbilityListener` with §cSilenced! action bar feedback), and `haste` (+50% mining_speed per level) to `status-effects/example.yml`. All four load via the existing `StatusEffectLoader` YAML pipeline. `marked` and `shield_buff` are left to the ability-effect code paths that create them directly.

---
