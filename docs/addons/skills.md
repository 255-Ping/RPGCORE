# Skill addons

> **Status:** Planned

Each skill is its own addon. They all follow the same shape — described once here, with skill-specific deltas in each section.

## Common shape

Every skill addon ships:

- A `config.yml` matching the [skills framework schema](../core/skills.md#per-skill-config) (max level, curve, XP sources, milestones, implicit-per-level)
- Listeners that turn in-game events into `SkillsService.awardXp(player, skillId, amount)` calls
- A reload command and an admin "give content" command
- Optional content folders (e.g., `recipes/`, `ores/`, `crops/`)

Common commands:

| Command | Permission |
|---|---|
| `/<skill> reload` | `rpg.<skill>.reload` |
| `/<skill> give <item>` | `rpg.<skill>.admin.give` |

## Combat (`rpg-combat`)

> **Status:** In progress — `rpg-combat` module ships. Awards XP on `PostDamageEvent` proportional to dealt damage, scaled by `COMBAT_WISDOM`. Configurable `xp-per-damage` rate; ability-source damage gets an extra `combat-xp-multiplier-from-abilities` knob.

### XP source

Listens to `PostDamageEvent`. XP awarded proportional to damage dealt (configurable per victim-type and per source).

```yaml
# rpg-combat/config.yml
xp-sources:
  damage-per-hp:
    default: 1.0                 # XP per HP of damage dealt
    per-victim-rarity:
      common: 1.0
      rare: 2.0
      epic: 5.0
combat-xp-multiplier-from-abilities: 1.0
```

The per-ability `CombatXpMultiplier` (in ability YAML) layers on top.

### Stats milestones (defaults)

`crit_chance +1` every 5 levels, `strength +2` every 10 levels, `max_health +5` every 25 levels. Configurable.

## Mining (`rpg-mining`)

> **Status:** In progress — Mining XP awarded on `RpgBlockBreakEvent` (fired by core's BlockBreakHandler when a tagged custom block breaks). XP per block-id configurable in `rpg-mining/config.yml`, with a `default-xp` fallback. `MINING_WISDOM` stat scales the award. `MINING_SPEED` HP-per-second ticking still pending the hold-to-break polish slice.

### Content

- Custom ores defined as [custom blocks](../content/blocks.md) with `RequiredToolType: pickaxe`
- Pickaxe items in `items/` with `MINING_SPEED`, `MINING_FORTUNE`, `BREAKING_POWER` stats

### XP source

`BlockBreakEvent` for tagged custom blocks; XP per ore type configurable.

### Milestones (defaults)

Per-level: `mining_speed +0.5`, `mining_fortune +0.1`. Tier breakpoints unlock recipes (e.g., level 10 unlocks `iron_pickaxe` recipe).

## Foraging (`rpg-foraging`)

### Content

- Custom logs / trees as custom blocks (`RequiredToolType: axe`)
- Axe items with `FORAGING_SPEED`, `FORAGING_FORTUNE`

### XP source

Custom-block break for axe-target blocks.

## Farming (`rpg-farming`)

### Content

- Custom crops as custom blocks (vanilla growth cancelled; we tick growth ourselves so we can apply boosts)
- Hoe items with `FARMING_FORTUNE`

### XP source

Crop harvest (custom-block break of mature crops).

### Note

No `FARMING_SPEED` stat by design — farming uses `FARMING_FORTUNE` only. Crop growth tick rate is set per-crop in YAML and globally scalable in `rpg-farming/config.yml`.

## Fishing (`rpg-fishing`)

### Content

- Fish definitions in YAML (rarity, weight, biome, time-of-day restrictions)
- Rod items with `FISHING_SPEED`, `FISHING_FORTUNE`, `SEA_CREATURE_CHANCE`

### XP source

Successful catches.

### Sea creatures

A configurable chance (`SEA_CREATURE_CHANCE` stat) per cast that the catch becomes a mob from the mob registry spawning at the float location instead of a fish item.

## Cooking (`rpg-cooking`)

### Content

- Cooking station custom block (`StationType: cooking`)
- Recipes under `plugins/rpg-cooking/recipes/`
- Output items are `CONSUMABLE` items that apply stat buffs / status effects

### XP source

Cooking-recipe completion.

### Stats

Mostly enables food / consumables; no new combat stats. `COOKING_WISDOM` for XP scaling.

## Alchemy (`rpg-alchemy`)

### Content

- Brewing station custom block (`StationType: brewing`)
- Brewing recipes under `plugins/rpg-alchemy/recipes/`
- Output items are potion-style `CONSUMABLE`s that apply status effects
- Custom status effects shipped here (e.g., `regen`, `strength`, `slowness`)

### XP source

Brewing-recipe completion.

### Stats

`ALCHEMY_WISDOM`, plus content uses `INTELLIGENCE` for effect duration scaling.

## Related

- [Skills framework](../core/skills.md)
- [Stats reference](../stats.md)
- [Status effects](../core/status-effects.md)
- [Recipes](../content/recipes.md)
- [Blocks](../content/blocks.md)
- [Items (CONSUMABLE)](../content/items.md#consumable)
