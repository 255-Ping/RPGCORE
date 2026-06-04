# 🟠 Improvements — In Progress / Major Missing Chunks

_These systems exist and partially work, but have significant gaps._

> **Difficulty scale:** 🟢 Easy (< 1 day) · 🟡 Medium (1–2 days) · 🔴 Hard (several days) · ⚫ Very Hard (week+)

---

### Expand Example Content — Mobs, Abilities, and Items (`rpg-core`) — 🟡 Medium
The current example files are thin: 3 abilities, 2 mobs, and items that don't cover many interaction patterns. Need a richer out-of-the-box set so new server owners can see the full range of the system.

**New abilities to add (`abilities/example.yml`):**
- `fireball_barrage` — fire 3 delayed fireballs in a spread (beam × 3 with short delays between)
- `death_nova` — on-death explosion that damages all nearby players (uses `~onDeath` trigger)
- `enrage` — applies a self-buff status effect on hurt, making the mob hit harder when low HP (`~onHurt`)
- `ground_slam` — melee AoE: particles + explode in radius around caster, knocking targets back
- `soul_drain` — beam that deals damage and heals the caster for a % of damage dealt (beam + heal chained)
- `warp_strike` — teleports caster to target then immediately damages (needs `teleport` effect — see new effects below)
- `chain_lightning` — chains damage to multiple nearby targets (needs `chain` effect — see below)
- `zone_of_pain` — creates a lingering zone that pulses a status effect on everything inside (needs `zone` effect)
- `player_shield` — absorbs incoming damage for a short window (needs `shield` effect)
- `blink_forward` — short-range dash in look direction (needs `blink` effect)

**New mobs to add (`mobs/example.yml`):**
- `forest_sprite` — passive mob, flees combat, drops rare herbs; demonstrates `passive` AI + `~onHurt` flee ability
- `goblin` — fast, low-HP melee mob; uses `aggressive` AI, enrage ability `~onHurt`, small loot table
- `skeleton_archer` — ranged mob (skeleton base); uses `~onTimer` ranged ability, moderate HP
- `cave_troll` — tank mob (iron_golem base); high HP/armor, `ground_slam ~onTimer:60`, slow movement, rich loot
- `corrupted_mage` — caster mob (witch base); uses `soul_drain ~onTimer:40`, `chain_lightning ~onTimer:80`, mana-themed
- `dungeon_boss` — showcase boss: multiple abilities on different timers, `death_nova ~onDeath`, large loot pool, named with health bar

**New items to add (`items/example.yml`):**
- `goblin_fang` — material drop from goblin, used in recipes
- `troll_hide` — material drop from cave_troll
- `void_crystal` — epic material, rare drop, used in high-tier crafting
- `mages_robes` (4-piece set) — caster armor set demonstrating full intelligence/mana stats
- `shadow_dagger` — fast sword (low AttackCooldown), high crit stats, demonstrates ferocity
- `berserker_axe` — AXE type weapon, demonstrates strength + ferocity, uses `ground_slam` ability
- `soul_staff` — WAND using `soul_drain` ability, lifesteal stats
- `rangers_shortbow` — lighter bow, faster fire rate, `ammo_usage_reduction`, demonstrates BOW type fully
- `mana_potion` — CONSUMABLE that restores mana (new `restore_mana` OnConsume effect)
- `stamina_crystal` — ACCESSORY with `health_regen` + `mana_regen` + `vitality`

---

### New Built-in Ability Effects (`rpg-core`) — 🔴 Hard
The current effect set (`damage`, `heal`, `beam`, `explode`, `particles`, `sound`, `delay`, `apply_status`, `mana_cost`, `cooldown`) covers the basics but needs more building blocks for interesting abilities. Proposed additions:

| Effect | Parameters | Description | Est. |
|---|---|---|---|
| `knockback` | `force=`, `direction=away/toward/up` | Push or pull the target. `away` = repel from caster, `toward` = pull in, `up` = launch upward. Works on both player→mob and mob→player. | 🟢 Easy |
| `teleport` | `mode=to_target/behind_target/random_near`, `distance=` | Teleport the caster. `to_target` = land on top of target, `behind_target` = appear behind, `random_near` = random point within `distance`. | 🟢 Easy |
| `blink` | `distance=` | Teleport the caster forward in their look direction by up to `distance` blocks, stopping at the first solid block. | 🟢 Easy |
| `chain` | `targets=`, `range=`, `damage_multiplier=`, `particle=` | Bounce a damage hit to up to `targets` additional entities within `range` of each successive target. Damage decays per bounce. | 🟡 Medium |
| `zone` | `radius=`, `duration_ticks=`, `interval_ticks=`, `effect_id=`, `effect_level=` | Spawn a persistent zone at the cast location. Every `interval_ticks`, applies `effect_id` at `effect_level` to all entities inside the radius. Despawns after `duration_ticks`. | 🟡 Medium |
| `shield` | `amount=`, `duration_ticks=`, `target=caster/target` | Apply a damage-absorb shield that blocks up to `amount` HP of incoming damage, expiring after `duration_ticks`. | 🟡 Medium |
| `drain` | `amount=`, `steal_percent=`, `target=caster/target` | Deal `amount` damage to the target and heal the caster for `steal_percent`% of the damage dealt. Stacks with `lifesteal` stat. | 🟢 Easy |
| `mark` | `duration_ticks=`, `damage_amplify=` | Mark the target; all damage they receive is multiplied by `damage_amplify` while the mark is active. Visual: a particle ring around the target. | 🟡 Medium |
| `restore_mana` | `amount=`, `target=caster/target` | Restore `amount` mana to caster or target. Counterpart to `mana_cost`. | 🟢 Easy |
| `launch` | `force=`, `direction=up/away/toward`, `target=caster/target` | Apply velocity to caster or target. Softer than `knockback` — suitable for mobility abilities rather than combat disruption. | 🟢 Easy |
| `freeze` | `duration_ticks=`, `target=caster/target` | Severely slow the target (apply a high-amplifier slowness + mining fatigue equivalent). Not the same as `apply_status slow` — `freeze` is much stronger and visually distinct (ice particle burst). | 🟢 Easy |

---

### Ability Trigger Types: Expand (`rpg-core`) — 🟡 Medium
Currently supported triggers: `~onTimer`, `~onHurt`, `~onDeath`. Several common RPG triggers are missing:

| Trigger | When it fires | Notes |
|---|---|---|
| `~onAttack` | When the caster lands a melee hit | Separate from the hit registration — fires for the attacker, not the defender |
| `~onKill` | When the caster kills an entity | Useful for "on-kill effects" like lifesteal, speed burst on kill, death mark explosion |
| `~onBlock` | When the caster right-clicks with the item (wand/tool use, not bow) | Already close to existing item-use logic; formalize as a trigger |
| `~onJump` | When the player jumps | Blink / launch builds — relatively niche but useful |
| `~onLogin` | Once per server join | Apply persistent buffs on login (e.g. guild bonuses, rested XP) |

- Each trigger maps to a Bukkit event or a custom RPG event
- Triggers that make no sense for mobs (e.g. `~onLogin`) should be skipped silently if the caster is a mob

---

### Ability Pierce Cap (`rpg-core`) — 🟢 Easy

Abilities that travel (beam, projectile) currently have no limit on how many entities they hit. A `PierceCap` field on any traveling effect controls how many distinct entities the effect can hit before it stops.

**YAML — per-effect field:**
```yaml
my_beam:
  Effects:
    - Type: beam
      Damage: 25
      PierceCap: 1    # hits 1 entity then stops; 0 or absent = unlimited
```

**Behaviour:**
- `PierceCap: 1` — single-target: beam stops on first hit (most common default for wand builds)
- `PierceCap: 3` — pierces through up to 3 entities before dissipating
- `PierceCap: 0` / field absent — no cap, hits everything along the path (current behaviour)
- The cap counts **distinct entities hit this tick**, not total invocations across ticks — a lingering beam legitimately re-hitting the same target on a later tick is fine and doesn't count against the cap

**Per-tick per-ability dedup (separate from pierce cap):**
Every ability invocation tracks a `Set<UUID>` of entities already hit **this tick**. Any entity in that set is skipped for the remainder of that tick's processing, regardless of pierce cap. The set is cleared at the start of the next tick. This is what prevents the beam from hitting the same enemy 3× in one tick (the current bug). The two guards work together:

1. **Dedup set** — same entity, same tick → skip (always enforced, not configurable)
2. **Pierce cap** — total distinct entities this tick → stop the beam once the cap is reached (configurable)

**Also applies to:** any future `projectile` effect type. AoE/explode effects already scope by radius so pierce doesn't apply there; `chain` has its own `targets=` param.

**Implementation note:** `BeamEffect` already iterates over entities in the beam path. Add a `Set<UUID> hitThisTick` that resets each tick and a `int hitCount` counter for the pierce cap check. Skip any entity whose UUID is already in `hitThisTick`; after a successful hit add to the set, increment the counter, and break if `pierceCap > 0 && hitCount >= pierceCap`.

---

### MagicFind Stat: Implement or Suppress (`rpg-core`) — 🟡 Medium
`magic_find` is referenced in the loot pool spec as `MagicFindAffected: true` on individual loot entries, but there's no evidence the stat is actually read when rolling those entries. Confirm and implement:

- Read the caster's effective `magic_find` stat value when rolling a loot pool
- For entries marked `MagicFindAffected: true`, multiply the roll chance by `(1 + magic_find / 100.0)` — e.g. `+50 magic_find` → 1.5× chance on affected drops
- Cap the multiplier at a configurable max (default `max-magic-find-multiplier: 3.0` in config) to prevent absurd stacking
- If the stat isn't worth implementing yet, suppress it from item lore (same `hidden` flag approach as other unimplemented stats — see [Bugs](todo-bugs.md))

---

### Consolidate `backend.yml` + `config.yml` Persistence Setting (`rpg-core`) — 🟢 Easy
**Why there are two files — document this clearly:**
- `config.yml → persistence.backend` is the **admin's desired setting** (what the server owner configured).
- `backend.yml` is a **runtime state file written by `BackendMigrator`** at startup. It records which backend was actually active last session so the migrator can detect a YAML↔MySQL switch and auto-migrate data before anything reads it.

They intentionally serve different purposes and must stay separate. The risk of merging them is that if `config.yml` were both the setting AND the last-active record, a partial migration crash would corrupt the desired setting.

**Action:** Add a comment block near the `persistence:` section in `config.yml` explaining this, and add a similar comment at the top of `backend.yml` when it is first generated. Also document it in `docs/core/persistence.md` so admins don't think it's a bug or duplicate.

---

### Timed Cooking + Brewing with Persistent Progress (`rpg-cooking` / `rpg-alchemy`) — 🔴 Hard
Currently recipes complete instantly when the player clicks the output slot. Add configurable craft time:

- Each recipe YAML gains an optional `CraftTime` field (in seconds; 0 or absent = instant, same as now)
- When a player starts a recipe in the GUI a progress bar fills over the configured duration
- **Visual progress feedback** — the output slot item cycles through a configurable set of `CustomModelData` values (e.g., empty flask → quarter full → half full → full) so players can see progress visually. Alternatively, show a dedicated progress-bar item in a fixed slot using filled/unfilled block items (e.g., lime vs gray glass panes). The approach should be consistent between cooking and brewing.
- **If the player closes the GUI mid-craft**, the in-progress state is saved to `DataStore` keyed by `<playerUUID>:<stationBlockLocation>`: which recipe is being crafted, how much time has elapsed, and the ingredients that were consumed (so they can't be double-spent)
- **When the player reopens that station GUI**, it restores the in-progress state — progress resumes from where it left off (not restarting). The ingredient slots show the items locked in for the current craft; players can't swap them out mid-craft.
- On completion the output appears in the output slot with a sound cue; the persisted state is cleared
- If the station block is destroyed mid-craft, the ingredients should be dropped at the block location and the persisted state cleared
- Applies to both cooking stations (`rpg-cooking`) and brewing stations (`rpg-alchemy`)

---

### Timed Smelting with Persistent Progress (`rpg-smelting`) — 🟡 Medium
Same timed-crafting treatment as cooking and brewing — apply when building `rpg-smelting` rather than tacking it on later.

- Each smelting recipe YAML gains an optional `CraftTime` field (seconds; 0 or absent = instant)
- `rpg-smelting` should use a **custom station GUI** (same pattern as `rpg-cooking` / `rpg-alchemy`) rather than hooking into the vanilla furnace, so the same progress-bar slot approach works cleanly
- In-progress state saved to `DataStore` keyed by `<playerUUID>:<stationBlockLocation>` — same persistence model as cooking and brewing
- If the station block is destroyed mid-smelt, ingredients drop at the block location and state clears
- On completion, output appears in the output slot with a sound cue
- Ingredient slots lock during an active smelt — player cannot swap them mid-craft
- All timing parameters configurable in `rpg-smelting/config.yml`

---

### Enchanting: Costs Minecraft XP (`rpg-enchanting`) — 🟡 Medium
Currently enchanting costs in-game currency only. Add Minecraft (vanilla) XP cost:

- Each enchant YAML gains an optional `XpCost` field (integer levels; e.g., `XpCost: 5` costs 5 XP levels). The unit is always levels, not raw points, to match the mental model players have from vanilla enchanting.
- The XP cost is shown on the **apply button** in the enchanting GUI — e.g., `&aApply &7| &b5 XP levels &7| &e250 coins`. Both costs must be met; if either is insufficient the button shows in red with the blocking reason.
- The cost is deducted from the player's vanilla XP bar on apply; if they don't have enough XP the apply is blocked with a clear message
- **Mob XP drops** — add an `Exp` field to mob YAML (separate from skill XP / `CombatExp`). This is vanilla XP orbs dropped on death. Currently custom mobs drop 0 XP. This feeds the enchanting economy.
- **Loot pool XP** — loot pool entries (see Loot Pool System below) support an `Exp` field for vanilla XP orbs on that entry rolling
- Admins can set `XpCost: 0` or omit the field to keep an enchant currency-only

---

### Loot Pool System (`rpg-core`) — 🔴 Hard
Admins need a way to define reusable named loot pools and assign them to mobs (and dungeons, chests, etc.) rather than embedding loot inline everywhere. This also fixes the broken external `LootTable: <id>` reference system (see Loot Tables entry below — both should be consolidated into this).

**Pool definition** — new content folder: `plugins/rpg-core/loot-pools/<file>.yml`

```yaml
goblin_drops:
  Attribution: last-hit           # last-hit | top-damager | split-equal | weighted-by-damage
  RollMode: per-player            # per-player | shared
  Rolls:
    - { Item: goblin_fang,    Chance: 60.0, Min: 1, Max: 2 }
    - { Item: gold_nugget,    Chance: 40.0, Min: 1, Max: 5, MagicFindAffected: true }
    - { Item: rare_goblin_hat, Chance: 1.0, Min: 1, Max: 1, MagicFindAffected: true }
  Guaranteed:
    - { Item: coin_pouch, Min: 1, Max: 1 }
  Exp: 15              # vanilla XP orbs dropped on kill (separate from any entry-level Exp)
  CombatExp: 50        # skill XP awarded to killer's combat skill
```

Each entry can also carry its own `Exp` field (vanilla XP orbs if *that specific entry* rolls).

**Usage in mob YAML:**
```yaml
goblin:
  LootPool: goblin_drops          # single pool by id
  # or
  LootPools:                      # multiple pools, all roll independently
    - goblin_drops
    - rare_event_pool
```

**Attribution modes** (same as inline — important to document clearly):
- `last-hit` — loot goes to whoever landed the killing blow
- `top-damager` — loot goes to whoever dealt the most damage
- `split-equal` — every damager gets the same loot roll
- `weighted-by-damage` — each damager's chance is proportional to % of damage dealt

**Also applies to:** dungeon loot chests, loot chest blocks, future fishing loot, future farming loot drops.

---

### Telekinesis Effect — Drops Straight to Inventory (`rpg-enchanting` / `rpg-core`) — 🟡 Medium
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

### Damage Indicators: Float Down + Shrink (`rpg-holograms`) — 🟢 Easy
Current behaviour: damage numbers appear and stay in place until their duration expires.

New behaviour:
- Numbers **float downward** (not upward) over their lifetime
- Numbers **scale down** (shrink) continuously as they age via `TextDisplay` transformation
- When they reach minimum scale (configurable `min-scale` in config), they are removed immediately rather than waiting for `duration-ticks`
- All motion/scale parameters should be configurable: `float-speed`, `start-scale`, `min-scale`, `duration-ticks`

---

### Mob Death Animation (`rpg-core`) — 🟡 Medium
Currently mobs play Minecraft's default death animation (fall to side, then despawn). Replace this with a custom death sequence:

- When a custom mob's HP reaches 0, **cancel the vanilla death animation** (remove the entity before it can play the fall)
- Spawn configured **particles** at the death location (admin-configurable particle type, count, spread)
- Play a configured **sound** at the death location (admin-configurable sound, volume, pitch)
- Both `Particles` and `DeathSound` fields go on the mob YAML definition
- Loot still drops as normal (triggered by the RPG death event, not the vanilla entity death)
- Example in `mobs/example.yml` should demonstrate both fields

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

### Stats GUI Redesign (`rpg-core`) — 🔴 Hard
Currently `/stats` prints a chat dump. Planned 54-slot (6-row) inventory GUI layout:

```
[ Helmet ]  [ Empty ]  [ Empty ]  [ Combat ]  [ Survival ]  [ Caster  ]  [ Empty ]  [ Empty ]  [ Empty ]
[ Chest  ]  [ Empty ]  [ Empty ]  [ Gather ]  [ Loot     ]  [ Wisdom  ]  [ Empty ]  [ Empty ]  [ Empty ]
[ Legs   ]  [ Empty ]  [ Empty ]  [  ...   ]  [   ...    ]  [  ...    ]  [ Empty ]  [ Empty ]  [ Empty ]
[ Boots  ]  [ Empty ]  [ Empty ]  [ Empty ]  [ Empty ]  [ Empty ]  [ Empty ]  [ Empty ]  [ Empty ]
[ Weapon ]  [ Offhand]  [ Pet ▫️]  [ Empty ]  [ Empty ]  [ Empty ]  [ Empty ]  [Trade⚔️]  [  AH 🏪]
[ BG ][ BG ][ BG ][ BG ][ BG ][ BG ][ BG ][ BG ][ BG ]
```

- **Left column (rows 1–4):** actual worn gear items (helmet/chest/legs/boots) — clicking does nothing, just shows the item tooltip
- **Row 5 left:** main-hand weapon + offhand + companion/pet slot placeholder (BARRIER item until rpg-pets exists)
- **Centre columns:** stat category items — one named item per category (Combat, Survival, Caster, Mobility, Gathering, Loot, Wisdom). Hovering shows all stats in that category with current value
- **Bottom-right:** "Send Trade Request" button (fires `/trade <player>` if viewing someone else; hidden when viewing self); "View Auctions" button (grayed out until AH is built)
- **Active set bonuses** displayed as a named item in one of the centre slots if any sets are active
- Title: `<PlayerName>'s Stats` (supports viewing other players — `/stats <player>`)
- `/stats` with no args opens your own; `/stats <player>` opens theirs (requires `rpg.core.stats.other` permission)

---

### HUD: Scoreboard + Tablist Improvements (`rpg-hud`) — 🟡 Medium
Several improvements needed:

**Scoreboard:**
- Show more useful info by default: player name, online player count, party member list + status, recently gained skill XP (last skill + amount, fades after a few seconds)
- All lines should be fully configurable as placeholder templates (RPGCORE `{placeholders}` + PAPI `%placeholders%` once PAPI support lands)

**Tablist header:**
- Show server TPS, RAM usage, and player ping
- All lines configurable as placeholder templates

**Tablist footer:**
- Show the player's currently active status effects (effect name + level + remaining duration)
- All lines configurable as placeholder templates

---

### Knockback on All Weapons + Wands (`rpg-core` / `rpg-combat`) — 🟢 Easy
Knockback is missing or broken across multiple item types. Confirmed issues:

- **Beam wand** — `BeamEffect` deals damage but applies no knockback to the hit entity. The `KNOCKBACK` stat on the held item is never read in the beam path. Needs an explicit knockback velocity application after the damage call in `BeamEffect`, scaled the same way melee does it (stat value / 100 = strength).
- **Arrows** — arrow hits deal damage but no knockback is applied through the RPG pipeline.
- **Example items** — all example swords, bows, and wands in the default YAML files are missing a `Knockback:` stat entry entirely, so there's nothing to apply even where the code path exists.

**Fix checklist:**
1. Wire `KNOCKBACK` stat application into `BeamEffect` (after damage, repel target away from caster)
2. Wire `KNOCKBACK` into the arrow/bow hit path
3. Add `Knockback: 50` (or appropriate value) to every example sword, bow, crossbow, and wand in `items/example.yml` so knockback is demonstrated out of the box

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

### Chat: Staff Channel + Custom Channels (`rpg-chat`) — 🟢 Easy
Current: global / party / guild channels work. Deferred:
- Staff channel (`/chat staff`, requires `rpg.chat.use.staff`)
- Admin-defined custom channels in `config.yml`

---

### HUD: Nametag Status-Effect Icons (`rpg-hud`) — 🟡 Medium
Current: nametags show name + prefix/suffix. Deferred:
- Active status-effect icons displayed on or above the nametag

---

### Mob AI Profiles Flesh-out (`rpg-core`) — 🔴 Hard
Current: `aggressive`, `passive`, `defensive`, `stationary` work. All others fall back to aggressive. Deferred:
- `ranged_kiter` — back up if player within melee range, fire ranged ability
- `boss` — phase transitions, ability rotations
- `swarming` — call nearby same-type mobs when aggro'd
- `pack_hunter` — coordinate target focus with nearby pack members
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

### Loot Tables: External File Reference (`rpg-core`) — 🟢 Easy
Current: inline loot tables on mob YAML work. External `LootTable: <id>` references parsed but never rolled — only inline tables produce drops. Missing:
- `LootTableRegistry` lookup by id when rolling mob drops
- Coin drops wired to economy deposit on kill

---

### NPC Command Overhaul + In-Game Editing (`rpg-npcs`) — 🔴 Hard
The `/npc` command is bare-bones and requires YAML editing for almost everything. Multiple gaps:

**Per-NPC entity type (currently missing entirely):**
- `NpcDef` has no per-NPC entity type field — all entity-style NPCs share a single global `display.body-entity` setting in `config.yml`. If you have a blacksmith NPC and a quest giver, they're both the same entity type.
- Add an `EntityType` field to `NpcDef` YAML and the parser
- Add `/npc setentitytype <id> <VILLAGER|ZOMBIE|IRON_GOLEM|...>` command with tab-complete for entity type names
- Default to the global config value if not specified per-NPC

**Style + skin commands (data model exists, no commands):**
- `EntityStyle` (ENTITY vs PLAYER) and `SkinDef` are in `NpcDef` but can only be set by editing YAML directly
- Add `/npc setstyle <id> entity|player` — switches between a vanilla entity body and a fake-player skin
- Add `/npc setskin <id> <playerName>` — fetches the Mojang skin for `<playerName>` and applies it (calls `SkinFetcher` which already exists)
- Add `/npc setskin <id> raw <value> <signature>` — for custom skins via raw texture data

**In-game dialogue editing (currently: YAML only):**
- `/npc setbehavior dialogue <id> <line>` overwrites all dialogue with one line — no way to add/remove individual lines
- Add `/npc dialogue add <id> <line...>` — appends a line
- Add `/npc dialogue set <id> <index> <line...>` — replaces line at index
- Add `/npc dialogue remove <id> <index>` — removes line at index
- Add `/npc dialogue clear <id>` — removes all lines
- Add `/npc dialogue list <id>` — shows all current lines with indices

**In-game shop editing (currently: YAML only + "edit npcs/all.yml" message):**
- When `setbehavior shop` is set, the only instruction is "Edit npcs/all.yml to add items." — completely unusable for non-technical admins
- Add `/npc shop add <id> <itemId> <buyPrice> <sellPrice>` — adds an item to the shop
- Add `/npc shop remove <id> <index>` — removes item at index
- Add `/npc shop list <id>` — shows current shop items with indices, prices, and whether the item exists in the registry
- Add `/npc shop clear <id>` — removes all shop items

**In-game quest assignment (partial — no tab-complete):**
- `/npc setbehavior quest <id> <questId>` works but offers no tab-complete for quest IDs
- Add tab-complete for the fourth argument pulling from the quest registry (soft-dep lookup)

**NPC look-at-player (new):**
- NPCs should smoothly rotate to face the nearest player within a configurable radius, making them feel alive
- Add a `LookAtPlayers: true` boolean field per NPC YAML (default from `config.yml → npc.look-at-players.enabled`)
- Add `LookRadius: 8` (blocks) per NPC, also with a global default in `config.yml`
- A repeating Bukkit task (interval configurable, e.g., every 2 ticks) scans all loaded NPC entities that have `LookAtPlayers: true`, finds the nearest online player within `LookRadius`, and rotates the entity to face them
- For **entity-style NPCs**: update `yaw` via `entity.teleport(entity.getLocation().setDirection(dir))` — this is the cleanest way to rotate an entity without moving it
- For **PLAYER-style NPCs**: requires sending a head-rotation packet (`ClientboundRotateHeadPacket` / `ClientboundMoveEntityPacket`) to all nearby players each tick — NMS, same pattern as `FakePlayerNpc`
- If no player is within `LookRadius`, the NPC returns to its default facing direction (stored `yaw`/`pitch` from YAML)
- Add `/npc setlook <id> true|false` command and include it in tab-complete
- Add it to `/npc info <id>` output

**General help + info:**
- `/npc` with no args shows a one-liner — should show a formatted list of all subcommands with brief descriptions
- Add `/npc info <id>` — shows all current settings: location, world, entity style, entity type, skin name, behavior type, look-at-players enabled, dialogue line count, shop item count, quest ID

---

### Region: Enter/Exit Messages + More Flags (`rpg-regions`) — 🟡 Medium
Current regions only enforce `pvp`, `no-break`, `no-place`. A lot of standard use-cases are missing:

**New flags to add:**
- `enter-message` / `leave-message` — show a title (or action bar message) when a player crosses the boundary. Configurable text with `{player}` and `{region}` placeholders.
- `no-mob-spawn` — prevent mob spawners and natural spawning inside the region
- `no-damage` — players inside take no damage (safe zones, spawn areas)
- `fly` — allow flight inside the region even without `/fly` permission
- `no-item-drop` — items dropped inside the region are immediately returned to the player (useful for arenas)
- `keep-inventory` — death inside this region doesn't drop items (overrides global death rules)

**Also:**
- Region priority field — when regions overlap, higher priority wins for conflicting flags

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

### HUD: Ability Cooldown Display (`rpg-hud` / `rpg-core`) — 🟡 Medium
There's currently no way for a player to see how long is left on an ability cooldown. Options:

- Dedicated scoreboard section listing active cooldowns (`testability: 2.3s`)
- Action bar suffix showing the currently-on-cooldown abilities
- Configurable placeholder `{cooldowns}` that resolves to a compact list

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

### Economy: Transaction Log (`rpg-economy`) — 🟡 Medium
Admins and players have no visibility into their currency history. Add a transaction log:

- Every `deposit`, `withdraw`, and `transfer` call on `CoreEconomy` appends a log entry: timestamp, type, amount, source/target player, reason string
- Log stored in `DataStore` per player, capped at a configurable max entries (default 100)
- `/money log [player]` — shows the last N transactions in chat or a GUI
  - No `[player]` arg = view your own; with arg requires `rpg.economy.log.others`
- Reason strings: calling systems should pass a human-readable tag (e.g., `"quest:first_kill reward"`, `"npc:shop purchase"`, `"auction:sale proceeds"`)
- Useful for diagnosing currency duplication bugs and support tickets

---

### Permission System: Consistency Audit + Fill Gaps (all plugins) — 🟡 Medium
Every command in the suite should have a declared permission node. Several commands (especially ones being added — `/sethome`, `/setwarp`, `/home`, `/warp`, `/kit`, `/inbox`, `/top`, etc.) and existing ones may be missing permissions entirely or have inconsistent naming.

**Convention to adopt suite-wide:**
```
rpg.<plugin-short-name>.<verb>[.<qualifier>]
```
Plugin short names: `core`, `admin`, `npcs`, `enchanting`, `alchemy`, `cooking`, `mining`, `farming`, `fishing`, `quests`, `guilds`, `parties`, `trade`, `accessories`, `holograms`, `regions`, `dungeons`, `economy`, `hud`, `chat`

Qualifiers: `.others` (viewing/editing another player's data), `.admin` (elevated admin version of a command), `.bypass` (skip a restriction)

**Examples of what the convention produces:**
| Command | Permission |
|---|---|
| `/sethome` | `rpg.admin.home.set` |
| `/home` | `rpg.admin.home.use` |
| `/delhome` | `rpg.admin.home.delete` |
| `/homes` | `rpg.admin.home.list` |
| `/setwarp` | `rpg.admin.warp.manage` |
| `/warp` | `rpg.admin.warp.use` |
| `/kit` | `rpg.admin.kit.use` |
| `/npc` | `rpg.npcs.admin` |
| `/stats <other>` | `rpg.core.stats.view.others` |
| `/profile <other>` | `rpg.core.profile.view.others` |
| `/money log <other>` | `rpg.economy.log.others` |
| `/enchanting give` | `rpg.enchanting.admin.give` |
| `/top` | `rpg.core.leaderboard.view` |
| `/inbox` | `rpg.core.mail.use` |

**Audit steps:**
1. For every plugin, read `plugin.yml` → `commands` section and `permissions` section
2. Identify commands with no permission declared
3. Identify permissions that don't follow the naming convention above
4. Rename inconsistent nodes (bump plugin versions appropriately)
5. Ensure every permission has `default: true` for player-facing commands and `default: op` for admin commands
6. Add a `docs/permissions.md` reference page listing every permission node in the suite in one place — the go-to for server admins setting up LuckPerms groups

**Known existing inconsistencies to fix:**
- `rpg.core.stats.other` → should be `rpg.core.stats.view.others` (singular vs plural + missing "view")
- `rpg.profile.view.others` and `rpg.profile.private` use `rpg.profile.*` but should be `rpg.core.profile.*`
- `rpg.economy.log.others` is correct format; verify it actually exists in `plugin.yml`
- `rpg.chat.use.staff` is correct; verify others in `rpg-chat` match the pattern

---

### Unit Test Coverage (all plugins) — 🟡 Medium (ongoing)
Currently only two test files exist: `QuestObjectiveTest.java` and `DamageMathTest.java`. For a codebase this size, untested code means regressions are invisible until they hit the live server. Priority areas:

- `DamageMath` — expand existing tests: crit, defense reduction, level scaling edge cases
- `StationGui` — recipe matching logic (no separate `SlotResolver` class; matching lives inside `StationGui` directly)
- `ExpressionEvaluator` — skill curve calculations (accessible via `RpgServices.expressions()`)
- `QuestManager` — objective progression and completion
- `BossBarService` / `SignEntryService` once built

---

### Vanilla Suppression Remaining Flags (`rpg-core`) — 🟢 Easy
Audit `VanillaSuppressionListener.java` — these flags are accepted in `config.yml` but likely have no event handler wired yet:

| Flag | Config key | Likely missing handler |
|---|---|---|
| Villager trading | `villager-trading` | `VillagerAcquireTradeEvent` + `VillagerReplenishTradeEvent` + `InventoryOpenEvent` for villager GUIs |
| Beacons | `beacons` | `BeaconEffectEvent` |
| Pillager patrols | `pillager-patrols` | `EntitySpawnEvent` filtering `PILLAGER` patrol spawns |
| Block explosion damage | `block-explosion-damage` | `EntityDamageByBlockEvent` for explosion sources |
| Durability | `durability` | `PlayerItemDamageEvent` |
| Death drops | `death-drops` | `PlayerDeathEvent` item drop handling (separate from the custom death-rules system — this is the vanilla drop specifically) |

Verify each against the actual `VanillaSuppressionListener.java` event listener list and add any confirmed-missing handlers.

---

### Economy: Vault Provider Bridge (`rpg-economy`) — 🟢 Easy
External non-suite plugins (third-party shops, job plugins, etc.) that expect a Vault `Economy` service can't interact with `rpg-economy`. Missing:

- Add Vault as a `softDepend` in `rpg-economy/plugin.yml`
- On enable, if Vault is present, register a `net.milkbowl.vault.economy.Economy` provider via `getServer().getServicesManager().register(Economy.class, new VaultEconomyAdapter(coreEconomy), this, ServicePriority.Normal)`
- `VaultEconomyAdapter` wraps `CoreEconomy` — implement `has()`, `getBalance()`, `withdrawPlayer()`, `depositPlayer()`, `format()` using `RpgServices.economy()`
- Methods Vault doesn't support (multi-world, banks) can return `false` / throw `UnsupportedOperationException`
- This is one-way compatibility: Vault plugins can read/write rpg-economy balances; rpg-economy doesn't need to depend on Vault at compile time beyond the soft-dep

---

### Status Effects: Catalog + New Built-in Types (`rpg-core`) — 🟢 Easy
The `apply_status` effect is used in several example abilities, but there's no documented catalog of what built-in status IDs exist and what their parameters mean. Also several common RPG statuses are missing.

**Needed:**
- Write a reference table (inline in `config.yml` comments or `docs/core/status-effects.md`) listing every built-in status: id, description, `Level` meaning, `DurationTicks` behavior
- New built-in statuses to add:
  - `burning` — sets entity on fire for duration ticks (maps to Bukkit `setFireTicks`)
  - `frozen` — applies high-amplifier Slowness + Mining Fatigue; blue particle burst on apply
  - `marked` — damage amplification debuff (used by `mark` ability effect above); ring particles on target
  - `silenced` — prevents ability use for duration (add a silenced-status check at the top of `ItemAbilityListener` before the ability fires; no `AbilityService` class exists — the right-click dispatch lives in `ItemAbilityListener`)
  - `haste` — positive buff: increased mining speed (Haste potion effect)
  - `shield_buff` — absorbed damage indicator (visual only — used internally by `shield` effect)

---
