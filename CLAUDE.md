# RPGCORE — Project Guide for Claude

A suite of Paper Minecraft plugins (Paper API `26.1.2`, Java 25) building a fully custom RPG
framework. Total vanilla replacement in admin-built areas — custom damage, mana, skills, mobs,
crafting, enchanting, brewing, status effects, etc. The project is in active early implementation;
many systems are designed but not yet coded.

## Project layout

```
RPGCORE/
├── rpg-api/          ← interfaces only; no implementations; every addon's compile dep
├── rpg-core/         ← the only plugin that implements rpg-api; bootstraps all services
├── rpg-<addon>/      ← each addon is its own plugin jar; hard-depend on rpg-core
├── buildSrc/         ← Gradle convention plugins (rpg.module / rpg.plugin-module)
├── docs/             ← design documentation (some sections ahead of implementation)
├── gradle.properties ← per-module versions + suiteVersion (see Versioning below)
└── build.gradle.kts  ← nearly empty root script
```

## Hard rules

### 0. Keep CLAUDE.md AND docs/ in sync after every substantive change

Two things must stay accurate at all times:

**CLAUDE.md** — audit whenever you:
- Add or rename a service in `RpgServices` or a class in a module
- Change a versioning rule or bump discipline
- Add a new persistent field, save/load path, or player-data schema change
- Discover that any example or number in this file is wrong

**docs/** — update the relevant doc page in the same commit as every code change:

| What changed | Doc page to update |
|---|---|
| Status effect schema / hooks / tick | `docs/core/status-effects.md` |
| Skill XP, curves, milestones | `docs/core/skills.md` |
| Damage pipeline | `docs/core/damage.md` |
| Player data persistence / YAML schema | `docs/core/persistence.md` |
| Guild config / commands / features | `docs/addons/guilds.md` |
| Any other addon | `docs/addons/<name>.md` |
| New content type (item/mob/ability/block) | `docs/content/<type>.md` |
| New command or permission | `docs/commands.md` + `docs/permissions.md` |
| `gradle.properties` keys / build | `docs/configuration.md` |

If you add a feature without updating the relevant doc page, it **does not count as done**.
The docs describe what the plugin *actually does* — not design fiction.
When a doc page says "planned" and you implement it, remove the "planned" note and describe the real behavior.

### 1. Always depend only on `rpg-api`, never on `rpg-core`

Addon `build.gradle.kts` files list `rpg-api` as `compileOnly`. Never add `rpg-core` as a
dependency — addons must not reach into core internals. Access everything through `RpgServices`.

```java
// ✅ Right — go through the service locator
RpgServices.health().currentHp(player);

// ❌ Wrong — direct coupling to core internals
CoreHealthService.getInstance().currentHp(player);
```

`RpgServices` throws `IllegalStateException` if called before `rpg-core` enables. Since all
addons `depend: [rpg-core]` in their `plugin.yml`, this never happens in practice.

### 2. Use `SchedulerService`, not `Bukkit.getScheduler()`

All scheduled work must go through `RpgServices.scheduler()` so Folia compatibility is a
one-place change later. Direct calls to `Bukkit.getScheduler()` are only acceptable in
`rpg-core` itself where the scheduler is being bootstrapped.

### 3. New plugin module checklist

When adding `rpg-<name>`:

1. Create `rpg-<name>/build.gradle.kts` using `id("rpg.plugin-module")`
2. Create `plugin.yml` with `depend: [rpg-core]`, `load: POSTWORLD`, `api-version: '26.1.2'`
3. Add `<name>Version=0.0.1` to `gradle.properties`
4. Add `include(":rpg-<name>")` to `settings.gradle.kts`
5. Add a doc page at `docs/addons/<name>.md` and link it from `docs/README.md`
6. Add commands to `docs/commands.md`, permissions to `docs/permissions.md`

### 4. Extending `RpgServices` when a new addon introduces a service

When an addon ships a new service interface (`rpg-api`) with a core or addon implementation:

1. Add the interface to `rpg-api` under the appropriate subpackage
2. Add a private static field, accessor, and setter in `RpgServices`
3. Call the setter from the addon's `onEnable`
4. Document the service in `docs/development.md` under "Service types still planned"

### 5. Everything configurable — no hardcoded game values

Every value a server admin might want to tune **must** live in a `config.yml`, not hardcoded in Java.

| Kind of value | Where it goes |
|---|---|
| Balance numbers (rates, costs, caps, durations) | `config.yml` with a sensible default |
| Formulas / scaling curves | `config.yml` string, evaluated by `ExpressionEvaluator` at runtime |
| Feature flags (on/off toggles) | `config.yml` boolean |
| Display strings / messages | `config.yml` or `messages.yml` |
| Sound keys, particle types, visual parameters | Per-content YAML (e.g., status-effect file) |

Before declaring a feature done, scan every literal in the new Java for values that belong in config.
The only things that may be hardcoded are: Java enum names, plugin IDs, repository names, and
structural constants that are not observable by players.

### 6. One version bump per plugin per session

Within a single conversation, bump each plugin's version **at most once** — at the very end after
all changes are accumulated. Never bump the same plugin twice in one session.

## Versioning

There are two completely separate versioning concerns. Get them right independently.

---

### A. Plugin jar versioning (`gradle.properties`)

Every module has its own `<shortName>Version` property in the format `X.Y.Z`. The final jar
name is `<module>-<X.Y.Z>-<suiteVersion>.jar`, e.g., `rpg-core-0.0.10-18.jar`.

#### Per-module version (`X.Y.Z`)

Each segment has a defined meaning. When you finish a change, pick the highest-severity segment
that applies and bump only that one (reset lower segments to 0):

| Segment | When to bump | Examples |
|---|---|---|
| **X** (major) `X.0.0` | A large chunk of a single plugin's functionality is added or overhauled | Implementing the full guild bank system, adding the entire dungeon editor flow, rewriting the damage pipeline |
| **Y** (minor) `0.Y.0` | Smaller-to-medium changes within a single plugin | Adding a new command, wiring a new XP source, adding a config knob, implementing a missing mechanic from the docs |
| **Z** (patch) `0.0.Z` | Small bug fixes or tiny tweaks to a single plugin | Off-by-one fix, wrong permission node, typo in a message, single-line config default |

Examples:
- Implementing hold-to-break for mining → `rpg-mining` minor bump (`0.0.2` → `0.1.0`)
- Fixing a crash when a player has no skill data → `rpg-core` patch bump (`0.0.10` → `0.0.11`)
- Shipping the full guild perk stat application + item bank + tier upgrades in one PR → `rpg-guilds` major bump (`0.0.2` → `1.0.0`)

Reset lower segments: bumping Y resets Z to 0. Bumping X resets Y and Z to 0.

#### Suite version (`suiteVersion`)

`suiteVersion` is a single integer shared by all modules. When it bumps, **every** plugin jar
gets the new suffix — the suite number signals that this build is a cohesive snapshot of the
whole suite.

**Bump `suiteVersion` when a change touches a large number of plugins at once** — e.g., updating
the `rpg-api` interfaces that all addons implement, adding a new service to `RpgServices`,
changing the build convention, or any cross-cutting refactor.

When `suiteVersion` bumps:
1. Increment `suiteVersion` in `gradle.properties`
2. Run `.\gradlew.bat assemble` — every jar rebuilds with the new suffix automatically
3. No need to touch individual `<name>Version` properties unless that module also changed

#### Current versions

| Module | Property | Current |
|---|---|---|
| rpg-api | `apiVersion` | 0.0.7 |
| rpg-core | `coreVersion` | 0.1.1 |
| rpg-mining | `miningVersion` | 0.0.2 |
| rpg-combat | `combatVersion` | 0.0.1 |
| rpg-economy | `economyVersion` | 0.1.0 |
| rpg-hud | `hudVersion` | 0.1.0 |
| rpg-chat | `chatVersion` | 0.1.0 |
| rpg-accessories | `accessoriesVersion` | 0.0.2 |
| rpg-holograms | `hologramsVersion` | 0.0.2 |
| rpg-parties | `partiesVersion` | 0.1.0 |
| rpg-foraging | `foragingVersion` | 0.0.1 |
| rpg-fishing | `fishingVersion` | 0.0.1 |
| rpg-regions | `regionsVersion` | 0.1.0 |
| rpg-farming | `farmingVersion` | 0.0.1 |
| rpg-guilds | `guildsVersion` | 0.1.0 |
| rpg-enchanting | `enchantingVersion` | 0.0.2 |
| rpg-alchemy | `alchemyVersion` | 0.0.2 |
| rpg-npcs | `npcsVersion` | 0.1.0 |
| rpg-quests | `questsVersion` | 0.0.2 |
| rpg-dungeons | `dungeonsVersion` | 0.0.2 |
| rpg-cooking | `cookingVersion` | 0.0.0 |
| rpg-admin | `adminVersion` | 0.1.0 |
| suite-wide suffix | `suiteVersion` | 18 |

**Keep this table in sync** — update it in the same commit as any version bump.

---

### B. Schema versioning (player data / persistent records)

There are **two separate runners** for the two backends. They are not connected.

#### MySQL — `MigrationRunner` (implemented and working)

SQL migration files live in each plugin's jar at `src/main/resources/migrations/V<n>__<name>.sql`.
Format: `V1__create_players.sql`, `V2__add_guild_column.sql`, etc.

- `MigrationRunner` auto-discovers scripts from the classpath, runs them in ascending version order.
- Applied versions tracked in `<prefix>schema_version` table per plugin.
- Scripts can use `${prefix}` — it's substituted with the table prefix at runtime.
- Scripts run in a transaction; a failed migration aborts startup with a loud error.
- **Never edit a shipped migration** — add a new numbered script instead.

When you add a new table or alter a column for a MySQL-backed feature, add a new
`V<n+1>__<description>.sql` file to the relevant module's resources.

#### YAML — no migration runner (not yet implemented)

The docs claim YAML migrations "mutate the YAML in-place" — **this is not true yet**.
`PlayerLifecycleListener` writes `schema-version: 1` to player files but never reads that
field back or runs any upgrade logic. There is no YAML equivalent of `MigrationRunner`.

**What this means in practice:**

- All YAML field reads must be additive with fallbacks. If a field is absent (old save),
  fall through to a sane default. Example from `PlayerLifecycleListener`:

  ```java
  hp = numberOr(data.get("hp"), maxHp);  // fallback to maxHp if field missing
  ```

- Additive changes (new field with a default) are safe — old files load fine.
- Breaking changes (rename, type change, restructure) require either:
  1. Implementing a YAML migration runner first (check the `MigrationRunner` class in
     `rpg-core` — the same interface could be adapted for YAML), or
  2. Doing a server data wipe for the affected repository.
- Don't remove a field from the save/load code without handling old files that still have it.

---

## Architecture

### Service locator pattern

`RpgServices` (in `rpg-api`) is the single static gateway. `rpg-core` calls the setters in
`onEnable`; every addon calls the getters. Never bypass this.

```java
RpgServices.stats()         // StatRegistry
RpgServices.items()         // ItemRegistry
RpgServices.mobs()          // MobRegistry
RpgServices.abilities()     // AbilityRegistry
RpgServices.skills()        // SkillsService
RpgServices.skillRegistry() // SkillRegistry
RpgServices.health()        // HealthService
RpgServices.mana()          // ManaService
RpgServices.dataStore()     // DataStore (YAML or MySQL)
RpgServices.statusEffects() // StatusEffectService
RpgServices.cooldowns()     // CooldownService
RpgServices.expressions()   // ExpressionEvaluator
RpgServices.lootTables()    // LootTableRegistry
RpgServices.currencies()    // CurrencyRegistry
RpgServices.blocks()        // BlockRegistry
// addons set their own services on enable:
RpgServices.economy()       // Economy (rpg-economy)
RpgServices.accessories()   // AccessoryService (rpg-accessories)
RpgServices.parties()       // PartyService (rpg-parties)
RpgServices.guilds()        // GuildService (rpg-guilds)
RpgServices.regionService() // RegionService (rpg-regions)
RpgServices.wands()         // WandService (rpg-core)
```

### Persistence API

`DataStore` exposes `Repository` instances keyed by name (maps to a folder/table per plugin).
Records are `Map<String, Object>` — each module serializes its own types.

```java
DataStore.Repository repo = RpgServices.dataStore().repository("players");
Optional<Map<String, Object>> rec = repo.get(uuid.toString());   // sync read
repo.save(uuid.toString(), data).join();                         // async write
repo.delete(key).join();
Collection<String> keys = repo.keys();
```

Repository names map to `plugins/<module>/data/<name>/` for YAML, or a prefixed table for MySQL.

### Player data lifecycle

`PlayerLifecycleListener` (rpg-core) loads on join, saves on quit. Player file schema
(`data/players/<uuid>.yml`):

```yaml
schema-version: 1
hp: 87.5
mana: 95.0
skills:
  combat: 1500
  mining: 350
bonus-stats:          # permanent milestone bonuses — absent if none earned yet
  max_health: 25.0
  strength: 10.0
```

Base stats are applied from `starting-state.base-stats` in `config.yml` each session — not
stored per player. When adding new persistent player fields, update both the save block in
`onQuit` and the read block in `onJoin` with a fallback.

**Stat aggregation order in `CoreRpgPlayer.recalculateStats()`:**
1. Base stats (config)
2. Bonus stats (permanent milestone gains — stored in `bonusStats` field, saved per-player)
3. Equipment (armor + main hand)
4. Accessories (via `rpg-accessories` if loaded)
5. Status-effect flat modifiers
6. Status-effect percent modifiers
7. `StatRecalcEvent` — addons inject transient bonuses (e.g. guild perks via `holder().add()`)

- `rpg-admin` — standalone admin utility addon (`depend: [rpg-core]`). Provides `/gmc /gms /gma /gmsp /fly /god /tp /tphere /heal /feed /speed /clear /broadcast /sudo`. Every command individually enable/disable-able via `config.yml`; permission nodes configurable per command. God mode hooks into `PreDamageEvent` to cancel RPG damage. Heal integrates with `RpgServices.health()` and falls back to vanilla.

### Key systems in rpg-core

| Class | What it does |
|---|---|
| `RpgCorePlugin` | Bootstrap — wires all services and registers all listeners |
| `PlayerLifecycleListener` | Load/save player data on join/quit |
| `DamagePipelineListener` | Cancels vanilla damage, fires `PreDamageEvent` → math → `PostDamageEvent` |
| `DamageMath` | Pure math for melee/ability/crit/defense (has unit tests) |
| `CoreSkillsService` | XP award, level-up, curve evaluation |
| `CoreHealthService` | HP tracking independent of Bukkit's 20-heart cap |
| `CoreStatusEffectService` | Apply/tick/expire status effects |
| `MigrationRunner` | MySQL schema version tracking (SQL files from jar classpath) |
| `YamlDataStore` / `YamlRepository` | YAML backend (default) |
| `MysqlDataStore` / `MysqlRepository` | MySQL backend (falls back to YAML on connect failure) |
| `CoreExpressionEvaluator` | Formula parser used by damage formulas and skill curves |
| `MobLoader` / `ItemLoader` / `AbilityLoader` | YAML content loaders; errors skip the bad file, not crash |

### Content authoring

All game content (items, mobs, abilities, blocks, recipes, status effects) is authored as YAML
under the plugin's data folder and loaded at startup + `/rpg reloadall`. A malformed file logs
clearly and is skipped — the plugin keeps running.

## Build

| Command | Effect |
|---|---|
| `./gradlew assemble` | Builds all modules, symlinks jars to test server |
| `./gradlew :rpg-core:runServer` | Launches Paper test server with rpg-core |
| `./gradlew test` | Runs all unit tests (JUnit 5 + Mockito) |
| `./gradlew :rpg-core:test` | Tests for one module |

The `syncTestServerSymlinks` task (wired into `assemble`) deletes stale jars and symlinks the
new one into `testServerPluginsDir`. Update that property in `gradle.properties` to your local
test server path — the original value is a Linux path from the repo author's machine.

## Testing

JUnit 5 + Mockito for pure logic. Bukkit integration uses the live test server via symlinks.
When writing a new service, extract a `computePure` static variant for testable math (see
`DamageMath.computePure` as the canonical example). Avoid `mockStatic` on Bukkit's surface —
prefer refactoring to extract the logic instead.

~36 tests exist covering: `DamageMath`, `CoreExpressionEvaluator`, `MutableStatHolder`,
`EnchantDef.statsAtLevel`, `ItemModifier` PDC round-trip, `QuestObjective.Type.fromString`.

## Planned features (do not implement — document only)

These features are scoped and documented under `docs/planned/` but have no code yet.
When starting implementation, move the doc to `docs/addons/<name>.md` and remove it from this list.

| Feature | Doc | Notes |
|---|---|---|
| Auction House | `docs/planned/auction-house.md` | Player listings; sign entry for coin amounts |
| Bazaar | `docs/planned/bazaar.md` | Admin-configured fixed-price shop; configurable items/categories |
| GUI Overhaul | `docs/planned/gui-overhaul.md` | Party, Guild, Quest, Hologram, Spawner GUIs; chat entry for player names; sign entry for numbers |

Key planned interaction patterns:
- **Chat entry** — for text/player-name inputs: close GUI, send prompt, capture next chat message, reopen GUI
- **Sign entry** — for numeric inputs (prices, amounts): open virtual sign via packet, parse on submit
- Reference sign implementation: `https://github.com/255-Ping/SurvivalCore`

## Git discipline

Commit and push after any meaningful change. When multiple plugins change as one logical unit,
batch their version bumps in the same commit. Branch: `main`.

Remote: `git@github.com:255-Ping/RPGCORE.git`
