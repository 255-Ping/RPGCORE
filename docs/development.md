# Development

> **Status:** Stable (build infra)

For developers / contributors. Admins running the plugin don't need this page.

## Project layout

```
RPG-PLUGINS/
├── buildSrc/                          ← Gradle convention plugins
│   └── src/main/kotlin/
│       ├── rpg.module.gradle.kts      ← base for every module
│       └── rpg.plugin-module.gradle.kts  ← adds TestServer sync task
├── rpg-api/                           ← SDK only (interfaces, no impl)
├── rpg-core/                          ← bootstrap plugin (shadows rpg-api in)
├── rpg-mining/                        ← skeleton skill addon
├── rpg-quests/                        ← skeleton addon
├── docs/                              ← this documentation tree
├── build.gradle.kts                   ← almost empty (just `group = ...`)
├── settings.gradle.kts                ← `include(":rpg-api", ...)`
└── gradle.properties                  ← suiteVersion, paperApiVersion, etc.
```

## Build

| Command | Effect |
|---|---|
| `./gradlew assemble` | Builds every module, symlinks plugin jars to TestServer |
| `./gradlew :rpg-core:runServer` | Launches a Paper test server with rpg-core (via `xyz.jpenilla.run-paper`) |
| `./gradlew clean` | Wipes `build/` — note: symlinks in TestServer dangle until next build |
| `./gradlew :rpg-mining:assemble` | Just builds rpg-mining |

`assemble` finalizes with the per-module `syncTestServerSymlinks` task, which:

1. Deletes stale `<moduleName>-*.jar` in `build/libs/`
2. Deletes stale `<moduleName>-*.jar` in `$testServerPluginsDir`
3. Symlinks the new jar to `$testServerPluginsDir`

## Adding a new plugin module

1. Create the directory: `mkdir rpg-<name>`
2. Create `rpg-<name>/build.gradle.kts`:

   ```kotlin
   plugins {
       id("rpg.plugin-module")
   }

   dependencies {
       "compileOnly"(project(":rpg-api"))
       // any other deps
   }
   ```

3. Create `rpg-<name>/src/main/resources/plugin.yml`:

   ```yaml
   name: rpg-<name>
   version: '${version}'
   main: com.github._255_ping.rpg.<name>.Rpg<Name>Plugin
   api-version: '26.1.2'
   load: POSTWORLD
   depend: [rpg-core]
   authors: [Ping]
   description: <short description>
   ```

4. Create the main plugin class under `src/main/java/com/github/_255_ping/rpg/<name>/Rpg<Name>Plugin.java`.
5. Add to `settings.gradle.kts`: `include(":rpg-<name>")`
6. Add a doc page: `docs/addons/<name>.md` and link from `docs/README.md` + `docs/addons/README.md`.
7. Add commands to `docs/commands.md`, permissions to `docs/permissions.md`, stats (if any new) to `docs/stats.md`.
8. Run `./gradlew assemble`.

## API surface

`rpg-api` declares the public types. `rpg-core` implements them. Other modules depend only on `rpg-api`.

Public types live in:

- `com.github._255_ping.rpg.api` — `RpgServices` (service locator with setters for rpg-core, getters for everyone)
- `com.github._255_ping.rpg.api.stats` — `Stat` (sealed), `BuiltinStat` (~39 entries with groups), `CustomStat`, `StatRegistry`, `StatHolder`, `StatRecalcEvent`
- `com.github._255_ping.rpg.api.items` — `RpgItem`, `ItemRegistry`, `ItemType` (sealed), `BuiltinItemType` (`SWORD, WAND, BOW, ARMOR, MATERIAL, QUEST, CONSUMABLE, UPGRADE, ACCESSORY`), `CustomItemType`, `Rarity`
- `com.github._255_ping.rpg.api.mobs` — `RpgMob`, `MobRegistry`, `SpawnerDef`
- `com.github._255_ping.rpg.api.abilities` — `AbilityEffect`, `AbilityRegistry`, `AbilityPipeline`, `AbilityDsl`, `AbilityContext`, `AbilityInvocation`
- `com.github._255_ping.rpg.api.damage` — `DamageContext`, `PreDamageEvent`, `PostDamageEvent`
- `com.github._255_ping.rpg.api.player` — `RpgPlayer`, `ManaService`
- `com.github._255_ping.rpg.api.persistence` — `DataStore` (+ inner `Repository` — keys are strings, values are `Map<String, Object>`)
- `com.github._255_ping.rpg.api.skills` — `Skill` (sealed), `BuiltinSkill`, `CustomSkill`, `SkillRegistry`, `SkillsService`, `SkillXpAwardEvent`
- `com.github._255_ping.rpg.api.status` — `StatusEffect`, `StatusEffectRegistry`, `StatusEffectService`, `ActiveStatusEffect`, `StackingStrategy`
- `com.github._255_ping.rpg.api.cooldown` — `CooldownService`
- `com.github._255_ping.rpg.api.formatting` — `NameFormatter`, `MessageFormatter`
- `com.github._255_ping.rpg.api.scheduler` — `SchedulerService` (Folia-compatible signatures)
- `com.github._255_ping.rpg.api.loot` — `LootTable`, `LootTableRegistry`, `LootContext`, `Attribution`, `RollMode`
- `com.github._255_ping.rpg.api.health` — `HealthService`
- `com.github._255_ping.rpg.api.formula` — `ExpressionEvaluator` (+ inner `Compiled`)
- `com.github._255_ping.rpg.api.currency` — `Currency`, `CurrencyRegistry`
- `com.github._255_ping.rpg.api.spawning` — `SpawnConditions` (+ inner `Range`), `Spawner` (+ `Mode` enum)
- `com.github._255_ping.rpg.api.regions` — `Region`
- `com.github._255_ping.rpg.api.blocks` — `Block`, `BlockRegistry`, `RequiredToolType`

Service types still planned (not yet in code; expected when their owning addon ships):

- `RegionService` (rpg-regions)
- `MobAi` (rpg-core, when AI override system lands)
- `Hologram`, `DamageIndicator` (rpg-holograms)
- `Npc` (rpg-npcs)
- `Station` (rpg-core, station block dispatch)
- `Economy` (rpg-economy — `CurrencyRegistry` already in api)
- `PartyService` (rpg-parties)
- `GuildService` (rpg-guilds)
- `DungeonService` (rpg-dungeons)
- `HudService` (rpg-hud)
- `SelectionWand` (rpg-core, planned)

## Versioning

All plugin modules share `version = "0.0.0-${suiteVersion}"`. `suiteVersion` lives in `gradle.properties`.

Bump rule (current): bump when a new plugin module is added OR a major feature affects nearly all plugins.

## Git push discipline

After any meaningful change to a plugin module — commit and push to `git@github.com:255-Ping/RPGCORE.git` on `main`. Don't pile up local-only commits.

When changing multiple plugins as one logical change, batch their version bumps into a single commit.

## Tests

JUnit 5 + Mockito for pure-logic units. Bukkit integration tests are deferred — anything that
needs a live server gets exercised via the test-server symlinks instead.

```bash
./gradlew test                     # runs every module's tests
./gradlew :rpg-core:test           # one module
```

Each module has `src/test/java/...` mirroring its main sources. The `rpg.module` build
convention wires JUnit + Mockito and puts Paper-API on the test runtime classpath (most of
our classes touch Adventure types at static-init time, so they need it to load).

### What's covered today (~36 tests)

| Module | Class | What it asserts |
|---|---|---|
| `rpg-core` | `DamageMath.computePure` | strength/crit/defense math and clamping |
| `rpg-core` | `CoreExpressionEvaluator` | arithmetic, precedence, builtins, variable substitution, error paths |
| `rpg-core` | `MutableStatHolder` | `set`/`add`/`multiply`/`clear`/`snapshot` semantics |
| `rpg-enchanting` | `EnchantDef.statsAtLevel` | linear per-level scaling math |
| `rpg-enchanting` | `ItemModifier.{parse,encode}LevelMap` | PDC level-map round-trip |
| `rpg-quests` | `QuestObjective.Type.fromString` | objective-type parsing + error path |

### Future test sites worth covering

Pure-logic surfaces that would benefit from tests but aren't covered yet:

- `AbilityDsl` parser (`rpg-api`) — the `name{k=v,k=v} next{...}` DSL + trigger suffixes
- `CoreSkillsService` threshold-builder (`rpg-core`) — verify `levelForTotal` against known
  curve points
- `CoreLootTable.roll` (`rpg-core`) — weighted-roll math + attribution
- `RecipeLoader` / `SmeltingLoader` shape and ingredient parsers (`rpg-core`)
- `MigrationRunner` (`rpg-core`) — version split logic + `${prefix}` substitution (H2 in-memory
  DB would let this run without a real MySQL)
- `BrewRecipeDef` / `CookRecipeDef` / `DungeonRegistry` YAML parsers — happy path + missing-
  field handling
- `LootChestRegistry.{serialize,parse}Key` (`rpg-core`)
- `Messages.get` placeholder expansion (per-addon `Messages` helpers)
- `ItemModifier.contributedStats` (`rpg-enchanting`) — needs `RpgServices` mocked

### Testing principles

- Pure functions first. If a method reads through `RpgServices`, extract a `computePure`
  variant that takes the inputs explicitly (as we did for `DamageMath`).
- Mocking Bukkit's static surface (`Bukkit.getServer()`, `RpgServices.*`) via Mockito's
  `mockStatic` is possible but brittle — prefer the refactor.
- A failing test should explain the math, not just print the value. A short comment with the
  expected formula above each assertion saves future maintainers an hour.

## Folia readiness

All scheduler use should go through `SchedulerService` (planned), not raw `Bukkit.getScheduler()`. The wrapper makes Folia migration a one-place change later.

## Related

- [README](README.md)
- [Configuration overview](configuration.md)
