# Skill addons

> **Status:** In progress — all skill addons shipped. See per-skill status notes below.

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

> **Status:** In progress — XP from `PostDamageEvent` (proportional to damage) and kill XP from `EntityDeathEvent` both live. `COMBAT_WISDOM` scales both. Per-mob kill-XP override table in config.

### XP sources

**Damage XP:** Proportional to HP dealt. `xp-per-damage` rate; ability-source damage gets a separate `combat-xp-multiplier-from-abilities` multiplier. The per-ability `CombatXpMultiplier` (in ability YAML) layers on top of that.

**Kill XP:** Flat award when the player lands the killing blow. `default-kill-xp` fallback; override per mob type (vanilla name or RPG mob id) in `xp-per-kill`.

```yaml
# rpg-combat/config.yml
xp-per-damage: 1.0
combat-xp-multiplier-from-abilities: 1.0
victim-whitelist: ["any"]      # entity types that award XP; ["any"] = all LivingEntities

default-kill-xp: 10
xp-per-kill:
  {}
  # zombie: 15
  # crypt_guardian: 100        # RPG mob id override
```

### Stats milestones (defaults)

`crit_chance +1` every 5 levels, `strength +2` every 10 levels, `max_health +5` every 25 levels. Configurable.

## Mining (`rpg-mining`)

> **Status:** In progress — Mining XP awarded on `RpgBlockBreakEvent`. XP per block-id configurable in `rpg-mining/config.yml`, with a `default-xp` fallback. `MINING_WISDOM` scales the award. `MINING_SPEED` hold-to-break is live (BlockBreakHandler ticks at HP/sec). `MINING_FORTUNE` drop multiplier live (applied in `BlockBreakHandler.rollDrops`).

### Content

- Custom ores defined as [custom blocks](../content/blocks.md) with `RequiredToolType: pickaxe`
- Pickaxe items in `items/` with `MINING_SPEED`, `MINING_FORTUNE`, `BREAKING_POWER` stats

### XP source

`BlockBreakEvent` for tagged custom blocks; XP per ore type configurable.

### Milestones (defaults)

Per-level: `mining_speed +0.5`, `mining_fortune +0.1`. Tier breakpoints unlock recipes (e.g., level 10 unlocks `iron_pickaxe` recipe).

## Foraging (`rpg-foraging`)

> **Status:** In progress — Foraging XP awarded on `BlockBreakEvent` for log/stem materials (any `*_log` or `*_stem` plus explicitly configured block IDs). `FORAGING_WISDOM` scales XP. `FORAGING_FORTUNE` drop multiplier live via `BlockDropItemEvent`. `FORAGING_SPEED` and custom tree content arrive with the foraging block content slice.

### Content

- Custom logs / trees as custom blocks (`RequiredToolType: axe`)
- Axe items with `FORAGING_SPEED`, `FORAGING_FORTUNE`

### XP source

Custom-block break for axe-target blocks.

## Farming (`rpg-farming`)

> **Status:** In progress — Farming XP awarded on `BlockBreakEvent` for mature Ageable crops. Default config covers wheat/carrots/potatoes/beetroots/nether_wart/cocoa/pumpkin/melon/sugar_cane/bamboo. Scaled by `FARMING_WISDOM`. `FARMING_FORTUNE` drop multiplier live via `BlockDropItemEvent` (same formula as mining/foraging; only triggers at max age).

### Content

- Custom crops as custom blocks (vanilla growth cancelled; we tick growth ourselves so we can apply boosts)
- Hoe items with `FARMING_FORTUNE`

### XP source

Crop harvest (custom-block break of mature crops).

### Note

No `FARMING_SPEED` stat by design — farming uses `FARMING_FORTUNE` only. Crop growth tick rate is set per-crop in YAML and globally scalable in `rpg-farming/config.yml`.

## Fishing (`rpg-fishing`)

> See **[Fishing](fishing.md)** for the full page.

XP on successful `PlayerFishEvent` catch, scaled by `FISHING_WISDOM`. Stats: `FISHING_SPEED`, `FISHING_FORTUNE`, `SEA_CREATURE_CHANCE`. Sea creatures spawn mobs at the float location instead of a fish drop.

## Cooking (`rpg-cooking`)

> See **[Cooking](cooking.md)** for the full page.

XP on cooking-station recipe completion, scaled by `COOKING_WISDOM`. Cooking station is a custom block (`StationType: cooking`). Outputs are `CONSUMABLE` items with stat buffs and status effects.

## Alchemy (`rpg-alchemy`)

> See **[Alchemy](alchemy.md)** for the full page.

XP on brewing-station recipe completion, scaled by `ALCHEMY_WISDOM`. Brewing station is a custom block (`StationType: brewing`). Outputs are potion-style `CONSUMABLE` items that apply status effects.

## Related

- [Skills framework](../core/skills.md)
- [Stats reference](../stats.md)
- [Status effects](../core/status-effects.md)
- [Recipes](../content/recipes.md)
- [Blocks](../content/blocks.md)
- [Items (CONSUMABLE)](../content/items.md#consumable)
