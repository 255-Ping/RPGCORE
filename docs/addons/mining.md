# Mining (`rpg-mining`)

> **Status:** In Progress — Mining XP from breaking custom RPG blocks fully working. `breaking_power` gating, `mining_fortune`, `mining_speed`, and `mining_wisdom` stats all wired. Custom block definitions live in `rpg-core`. Mining Fatigue is suppressed so the stat-gating system is the only barrier.

The mining addon hooks into `rpg-core`'s custom block system. Vanilla stone/ore breaking is unaffected unless you've defined custom versions. Breaking a custom block grants mining skill XP, applies fortune extra-roll logic, and checks `breaking_power` against the block's `RequiredPower`.

## How it connects to rpg-core blocks

Mining itself has no YAML — it reacts to events on blocks you define in `plugins/rpg-core/blocks/`. The block definition controls what drops and what power is required:

```yaml
# plugins/rpg-core/blocks/ores.yml
ruby_ore:
  MinecraftBlock: red_terracotta  # visual stand-in
  Toughness: 150                  # HP pool — mining_speed drains this
  RequiredPower: 3                # player needs breaking_power >= 3 to mine this
  Drops:
  - { item: ruby, chance: 100, min: 1, max: 1 }
  - { item: ruby, chance: 50, min: 1, max: 1 }  # base 50% chance of a second ruby
  XpReward: 25                    # mining XP awarded on break
```

Convert an existing world block to a custom block with `/rpg block convert ruby_ore` while looking at it.

## Stats

| Stat | Effect |
|---|---|
| `breaking_power` | Gates which blocks you can break. Player needs `breaking_power >= block.RequiredPower`. Shown first in item lore for gathering tools. |
| `mining_speed` | HP/second removed from a block's `Toughness`. Higher = faster break. Default tool break speed still applies as a baseline; this scales on top. |
| `mining_fortune` | Extra roll multiplier on block drops. Formula: `floor(fortune/100)` guaranteed extra rolls + fractional probabilistic extra. `mining_fortune: 100` = double drops on average. |
| `mining_wisdom` | % bonus to mining XP. `mining_wisdom: 50` = +50% XP per break. |

## Example mining tool

```yaml
iron_pickaxe_custom:
  MinecraftItem: iron_pickaxe
  Type: MATERIAL
  DisplayName: "&7Iron Pickaxe"
  Rarity: "&f&lCOMMON"
  Stats:
    breaking_power: 3
    mining_speed: 40
    mining_fortune: 25
    mining_wisdom: 10
```

Give it with: `/rpg item give iron_pickaxe_custom`

## Config

`plugins/rpg-mining/config.yml`:

```yaml
# Base XP multiplier applied on top of the per-block XpReward.
# Final XP = block.XpReward * base-xp-multiplier * (1 + mining_wisdom / 100)
base-xp-multiplier: 1.0
```

## Commands

| Command | Permission |
|---|---|
| `/mining reload` | `rpg.mining.admin.reload` |

## Related

- [Blocks](../content/blocks.md) — define mineable custom blocks here
- [Stats reference](../stats.md) — `breaking_power`, `mining_speed`, `mining_fortune`, `mining_wisdom`
- [Skills framework](../core/skills.md)
- [Vanilla suppression](../core/vanilla-suppression.md) — Mining Fatigue is suppressed by default
