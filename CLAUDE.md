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

### 0. Keep CLAUDE.md in sync after substantive changes

Audit this file whenever you:
- Add or rename a service in `RpgServices` or a class in a module
- Change a versioning rule or bump discipline
- Discover that what the docs say diverges from the code (it happens — docs are ahead of impl)

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

## Versioning

There are two completely separate versioning concerns. Get them right independently.

---

### A. Plugin jar versioning (`gradle.properties`)

Every module has its own `<shortName>Version` property. The final jar name is
`<module>-<moduleVersion>-<suiteVersion>.jar`, e.g., `rpg-core-0.0.10-18.jar`.

**Current versions (as of last update):**

| Module | Property | Current |
|---|---|---|
| rpg-api | `apiVersion` | 0.0.6 |
| rpg-core | `coreVersion` | 0.0.10 |
| rpg-mining | `miningVersion` | 0.0.2 |
| rpg-combat | `combatVersion` | 0.0.1 |
| rpg-economy | `economyVersion` | 0.0.2 |
| rpg-hud | `hudVersion` | 0.0.1 |
| rpg-chat | `chatVersion` | 0.0.2 |
| rpg-accessories | `accessoriesVersion` | 0.0.2 |
| rpg-holograms | `hologramsVersion` | 0.0.2 |
| rpg-parties | `partiesVersion` | 0.0.2 |
| rpg-foraging | `foragingVersion` | 0.0.1 |
| rpg-fishing | `fishingVersion` | 0.0.1 |
| rpg-regions | `regionsVersion` | 0.0.2 |
| rpg-farming | `farmingVersion` | 0.0.1 |
| rpg-guilds | `guildsVersion` | 0.0.2 |
| rpg-enchanting | `enchantingVersion` | 0.0.2 |
| rpg-alchemy | `alchemyVersion` | 0.0.2 |
| rpg-npcs | `npcsVersion` | 0.0.1 |
| rpg-quests | `questsVersion` | 0.0.2 |
| rpg-dungeons | `dungeonsVersion` | 0.0.2 |
| rpg-cooking | `cookingVersion` | 0.0.0 |
| suite-wide suffix | `suiteVersion` | 18 |

**Bump rules:**

- Bump **only the affected module's** `<name>Version` for any change to that module.
- Bump `suiteVersion` only when a new plugin module is added to the suite OR a major
  cross-cutting change touches nearly all modules.
- Batch version bumps for logically related multi-module changes into one commit.
- `testServerPluginsDir` in `gradle.properties` is set to the original developer's Linux path
  (`/home/ping/Documents/TestServer/plugins`) — update it to your local path before running
  `assemble` or the symlink task will fail silently.

**The docs say** "all modules share `0.0.0-${suiteVersion}`" — that's stale. The actual scheme
above has been in place since at least suiteVersion 18.

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
```

Base stats are applied from `starting-state.base-stats` in `config.yml` each session — not
stored per player. When adding new persistent player fields, update both the save block in
`onQuit` and the read block in `onJoin` with a fallback.

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

## Git discipline

Commit and push after any meaningful change. When multiple plugins change as one logical unit,
batch their version bumps in the same commit. Branch: `main`.

Remote: `git@github.com:255-Ping/RPGCORE.git`
