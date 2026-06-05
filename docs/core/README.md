![banner-core](../assets/banners/banner-core.png)

# Core (`rpg-core`)

> **Status:** In Progress — persistence (YAML + MySQL), damage pipeline, block break handler, skills framework, stat registry, health display, status-effect framework, loot tables, natural spawning, admin spawners, expression evaluator, and content loaders (items/mobs/abilities/blocks/recipes) all working.

`rpg-core` is the bootstrap plugin that owns every shared service. Every addon depends on it.

## What lives here

| System | Page |
|---|---|
| Stats framework + registry | [stats reference](../stats.md) |
| Persistence (YAML / MySQL toggle) | [persistence](persistence.md) |
| Vanilla suppression master config | [vanilla-suppression](vanilla-suppression.md) |
| Damage pipeline | [damage](damage.md) |
| Status-effect framework | [status-effects](status-effects.md) |
| Skills framework (XP, levels, curves, milestones) | [skills](skills.md) |
| Cooldown service | (covered in [damage](damage.md) + [abilities](../content/abilities.md)) |
| Health display (heart-as-percent) | [health-display](health-display.md) |
| Selection wand (admin tool) | [selection-wand](selection-wand.md) |
| Name formatter (LuckPerms prefix/suffix) | (covered in [chat addon](../addons/chat.md)) |
| Message formatter (i18n keys) | (covered in [configuration](../configuration.md)) |
| Scheduler abstraction (Folia-ready wrapper) | (developer-facing; [development](../development.md)) |
| Loot-table primitive | [loot-tables](../content/loot-tables.md) |
| Expression evaluator (formula parser) | [skills](skills.md#level-curves) |
| Content loaders (items, mobs, abilities, blocks, recipes) | [content/](../content/README.md) |
| Spawner runtime + natural spawning | [spawning](../content/spawning.md) |
| Armor set bonuses + passive abilities | [armor-sets](armor-sets.md) |

## Service locator

Other plugins access core services via `RpgServices`:

```java
RpgServices.stats();        // StatRegistry
RpgServices.items();        // ItemRegistry
RpgServices.mobs();         // MobRegistry
RpgServices.abilities();    // AbilityRegistry
RpgServices.mana();         // ManaService
RpgServices.player(player); // RpgPlayer
// more services to be added as systems land — see development.md
```

The lookup throws `IllegalStateException` if called before `rpg-core` has bootstrapped. Since rpg-core uses `load: POSTWORLD` and all addons hard-depend on it, this never happens in practice.

## Core commands

See [commands.md](../commands.md#core-rpg-core) for the full table. Key ones:

- `/rpg version`
- `/rpg reloadall`
- `/rpg item give <id>`
- `/rpg mob spawn <id>`
- `/rpg wand [mode]`
- `/rpg block give <id>`
- `/rpg block convert <radius> <fromMaterial> <toBlockId>`
- `/stats`, `/skill`, `/effects`

## Core config knobs (high level)

The main `config.yml` covers:

- Persistence backend (`backend: yaml | mysql`) + MySQL connection
- `vanilla-suppression` master block ([dedicated page](vanilla-suppression.md))
- Stat modifier order, default stat caps
- Damage formula constants (defense reduction, crit cap, lifesteal cap)
- Default skill curve + per-skill overrides
- Health/Mana defaults
- Death-rule tiers
- Natural-spawning master toggle
- Combat tagging duration, vitality scaling
- Status-effect stacking defaults
- Chat name-format defaults (LuckPerms wiring)
- i18n locale
- **Resource pack auto-delivery** — `resource-pack.enabled/url/sha1/prompt/required`; sends pack to players on join. Disabled by default.
- **Damage indicators** — `damage-indicators.enabled/duration-ticks/rise-blocks`; TextDisplay entities animate via a sine arc (rise then fall) with linear scale shrink 1→0.
- **Mob death animation** — per-mob YAML fields `DeathParticle`, `DeathParticleCount`, `DeathParticleSpread`, `DeathSound`; optional burst + sound at death location, velocity zeroed to suppress knockback flop.

Each field has a comment block in the generated `config.yml` describing it.
