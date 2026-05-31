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

The following are registered in `RpgServices` and set by their owning plugin on enable:

- `RpgServices.economy()` — `Economy` (rpg-economy)
- `RpgServices.accessories()` — `AccessoryService` (rpg-accessories)
- `RpgServices.parties()` — `PartyService` (rpg-parties)
- `RpgServices.guilds()` — `GuildService` (rpg-guilds)
- `RpgServices.regionService()` — `RegionService` (rpg-regions)
- `RpgServices.wands()` — `WandService` (rpg-core)
- `RpgServices.stations()` — `StationService` (rpg-core) — set by rpg-core on enable; addons call `register` on it to claim station types

Calling any of these before the owning addon loads throws `IllegalStateException`. Use `try/catch IllegalStateException` in addons that soft-depend.

Service types still planned (not yet in code):

- `Hologram`, `DamageIndicator` (rpg-holograms)
- `Npc` (rpg-npcs)
- `DungeonService` (rpg-dungeons)
- `HudService` (rpg-hud)
- `MobAi` (rpg-core, when AI profile override system lands)

Implemented since initial design:

- `RpgServices.stations()` — `StationService` (rpg-core) — central right-click dispatch for interactable blocks; addons call `RpgServices.stations().register("type", handler)` in `onEnable`.

## Events

All RPGCORE events are in `rpg-api` and fired by `rpg-core` (or the relevant addon). Listen to them with standard Bukkit `@EventHandler` in any plugin that has `rpg-core` as a dependency.

### Damage events

**`PreDamageEvent`** (`com.github._255_ping.rpg.api.damage`) — fired before damage is applied; cancellable.

```java
@EventHandler
public void onPreDamage(PreDamageEvent event) {
    DamageContext ctx = event.context();
    // ctx.attacker(), ctx.victim(), ctx.baseDamage(), ctx.critMultiplier(), ctx.trueDamage(), ctx.source()
    // Mutate: ctx.setBaseDamage(x), ctx.setCritMultiplier(x), ctx.setTrueDamage(true)
    event.setCancelled(true);  // abort damage entirely
}
```

**`PostDamageEvent`** (`com.github._255_ping.rpg.api.damage`) — fired after damage is resolved; not cancellable. `dealtDamage()` is the final value after all pipeline steps.

```java
@EventHandler
public void onPostDamage(PostDamageEvent event) {
    double dealt = event.dealtDamage();
    LivingEntity victim = event.context().victim();
}
```

### Stat events

**`StatRecalcEvent`** (`com.github._255_ping.rpg.api.stats`) — fired after a player's stats are fully recalculated (on equip/unequip). The `holder` snapshot reflects the new final stat values.

```java
@EventHandler
public void onStatRecalc(StatRecalcEvent event) {
    StatHolder stats = event.holder();
    double totalDamage = stats.get(BuiltinStat.DAMAGE);
}
```

### Skill events

**`SkillXpAwardEvent`** (`com.github._255_ping.rpg.api.skills`) — fired before XP is added to a skill; cancellable. The `amount` is mutable — set it to change how much XP is actually awarded.

```java
@EventHandler
public void onSkillXp(SkillXpAwardEvent event) {
    if (event.skillId().equals("mining")) {
        event.setAmount(event.amount() * 2);  // double XP from mining
    }
}
```

**`SkillLevelUpEvent`** (`com.github._255_ping.rpg.api.skills`) — fired after a skill levels up. Spans multiple levels in one event if a single XP award caused more than one level gain.

```java
@EventHandler
public void onLevelUp(SkillLevelUpEvent event) {
    int gained = event.newLevel() - event.previousLevel();
    event.getPlayer().sendMessage("Gained " + gained + " levels in " + event.skillId() + "!");
}
```

### Block events

**`RpgBlockBreakEvent`** (`com.github._255_ping.rpg.api.blocks`) — fired when a player breaks an RPG-tagged custom block, just before the location is untagged and drops are rolled. Cancellable (cancelling aborts the break entirely — no drops, no respawn).

```java
@EventHandler
public void onRpgBlockBreak(RpgBlockBreakEvent event) {
    Block block = event.block();      // the rpg-api Block definition
    Location loc = event.location();
    Player player = event.player();
}
```

### Combat events

**`CombatTagEvent`** (`com.github._255_ping.rpg.api.combat`) — fired once when a player transitions from out-of-combat to in-combat. Not re-fired while the tag is refreshed by continued fighting. `durationSeconds()` is how long the combat tag lasts.

```java
@EventHandler
public void onCombatTag(CombatTagEvent event) {
    event.getPlayer().sendMessage("You entered combat!");
}
```

### Region events

**`RegionEnterEvent`** / **`RegionLeaveEvent`** (`com.github._255_ping.rpg.api.regions`) — fired when a player moves into or out of a named region.

---

## Extension points

### Custom `AbilityEffect`

Register new DSL primitives that content authors can use in item and mob ability sequences.

```java
public class MyEffect implements AbilityEffect {

    @Override
    public String name() { return "my_effect"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        // Read from context
        LivingEntity caster = ctx.caster();
        LivingEntity target = ctx.target();   // may be null — check!
        Location point = ctx.point();         // may be null — check!
        double damage = ctx.carriedDamage();

        // Write back to context if you're setting target/point for later effects
        ctx.setTarget(someEntity);
        ctx.setPoint(someLocation);

        // Return immediately or use thenApply / CompletableFuture.completedFuture
        return CompletableFuture.completedFuture(ctx);
    }

    // Optional: show parameter descriptions in item lore
    @Override
    public List<String> description() {
        return List.of("&7Does something custom");
    }
}
```

Register in your addon's `onEnable`:

```java
RpgServices.abilityRegistry().register(new MyEffect());
```

Content authors can then use `my_effect{k=v}` in any ability sequence. Parse your DSL params from `AbilityDsl.parse(rawString)` which returns a `Map<String, String>` for the current effect's `{...}` block.

### Custom `StatusEffect`

Define new named status effects. The effect type controls how the status is listed in `/effects` and whether it stacks.

```java
public class BurningEffect implements StatusEffect {
    @Override public String id() { return "burn"; }
    @Override public String displayName() { return "Burning"; }
    @Override public Category category() { return Category.DEBUFF; }
    @Override public StackingStrategy stacking() { return StackingStrategy.REPLACE; }
    @Override public boolean hidden() { return false; }
}
```

Register and handle tick behavior (damage per tick, expiry cleanup, etc.) by listening to the scheduler and querying `RpgServices.statusEffects().active(entity)`.

Register the definition in `onEnable`:

```java
RpgServices.statusEffectRegistry().register(new BurningEffect());
```

### Planned extension points

These interfaces exist in `rpg-api` but are not yet hookable from addons:

- `QuestObjective` — custom objective types (e.g. `craft_item`, `explore_region`)
- `StatProvider` — inject stats onto players from external sources (e.g. rpg-pets contributing pet bonuses)

## Versioning

Each plugin module has its own version in `gradle.properties` (`coreVersion`, `combatVersion`, `foragingVersion`, etc.). Full jar version = `<pluginVersion>-<suiteVersion>` (e.g., `rpg-core-0.2.0-18.jar`).

**Bump rules:**

- **Per-plugin version** — bump only the plugin(s) you changed. One bump per plugin per session regardless of how many features landed. Minor bumps (`0.x.0`) for new features; patch bumps (`0.0.x`) for fixes.
- **`suiteVersion`** — bump only when a new module is added OR a major change affects nearly every plugin simultaneously.

`suiteVersion` lives in `gradle.properties` alongside all per-plugin version properties.

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
