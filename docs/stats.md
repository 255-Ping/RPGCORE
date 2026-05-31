# Master stat reference

> **Status:** Working — stat registry, lore rendering, and aggregation from gear/accessories/status effects all active. **Four stats are defined but not yet wired to gameplay logic:** `speed`, `ferocity`, `swing_range`, and `pristine` show on item lore but currently have no effect — avoid putting them on items until the wire-up is done (tracked in [Bugs](planned/todo-bugs.md)).

Every stat in the framework. Built-in stats are part of `rpg-api`'s `BuiltinStat` enum so any addon can reference them. Custom stats can be registered at runtime via `StatRegistry.register(new CustomStat(...))`.

Stats marked **percent** display with a `%` suffix and behave as percentage values (e.g., `CRIT_CHANCE: 25` = 25%).

Stats whose owning addon isn't loaded are hidden from menus via per-stat config flags.

---

## Combat

| ID | Display | Percent | Description |
|---|---|---|---|
| `damage` | Damage | no | Base weapon damage |
| `strength` | Strength | no | % bonus to melee damage |
| `crit_chance` | Crit Chance | yes | Chance per hit to crit (caps at 100) |
| `crit_damage` | Crit Damage | yes | Crit multiplier |
| `ability_damage` | Ability Damage | no | % bonus to ability damage (separate channel from strength) |
| `attack_speed` | Attack Speed | yes | Reduces melee swing cooldown |
| `ferocity` | Ferocity | yes | % chance per hit for an extra strike (can roll multiple) — **⚠ not yet wired** |
| `lifesteal` | Lifesteal | yes | % of dealt damage healed |

## Survival

| ID | Display | Percent | Description |
|---|---|---|---|
| `max_health` | Health | no | Max HP |
| `health_regen` | Health Regen | no | HP/sec out of combat |
| `vitality` | Vitality | yes | Reduces in-combat tag duration (caps at 50%) |
| `defense` | Defense | no | Reduces non-true damage |
| `true_defense` | True Defense | no | Reduces true damage |

## Caster

| ID | Display | Percent | Description |
|---|---|---|---|
| `max_mana` | Mana | no | Max mana |
| `mana_regen` | Mana Regen | no | Mana/sec |
| `intelligence` | Intelligence | no | Scales `max_mana` and `ability_damage` (configurable scaling) |
| `cooldown_reduction` | Cooldown Reduction | yes | % off ability cooldowns (does not bypass hard-floor cooldowns) |

## Mobility

| ID | Display | Percent | Description |
|---|---|---|---|
| `speed` | Speed | no | Walk speed — **⚠ not yet wired** |
| `swing_range` | Swing Range | no | Melee reach in blocks — **⚠ not yet wired** |

## Loot

| ID | Display | Percent | Description |
|---|---|---|---|
| `magic_find` | Magic Find | yes | Multiplies rare-loot chance on flagged drops |
| `pristine` | Pristine | yes | Gemstone / rare drop quality (reserved for future) — **⚠ not yet wired** |

## Breaking & gathering — universal

| ID | Display | Percent | Description |
|---|---|---|---|
| `breaking_power` | Breaking Power | no | Gates which custom blocks you can break (vs the block's `RequiredPower`) |

## Mining

| ID | Display | Percent | Description |
|---|---|---|---|
| `mining_speed` | Mining Speed | no | HP/second removed from a mineable block's `Toughness` |
| `mining_fortune` | Mining Fortune | yes | Extra rolls / multiplied drops on ores |

## Foraging

| ID | Display | Percent | Description |
|---|---|---|---|
| `foraging_speed` | Foraging Speed | no | HP/second on choppable blocks |
| `foraging_fortune` | Foraging Fortune | yes | Extra log drops |

## Farming

| ID | Display | Percent | Description |
|---|---|---|---|
| `farming_fortune` | Farming Fortune | yes | Extra crop drops (no farming-speed stat — by design) |

## Fishing

| ID | Display | Percent | Description |
|---|---|---|---|
| `fishing_speed` | Fishing Speed | no | Time-to-bite multiplier |
| `fishing_fortune` | Fishing Fortune | yes | Extra fish / catch quality |
| `sea_creature_chance` | Sea Creature Chance | yes | % chance a catch becomes a sea-creature mob |

## Wisdom (per-skill XP bonuses)

| ID | Display | Percent | Description |
|---|---|---|---|
| `combat_wisdom` | Combat Wisdom | yes | % bonus combat XP |
| `mining_wisdom` | Mining Wisdom | yes | % bonus mining XP |
| `foraging_wisdom` | Foraging Wisdom | yes | % bonus foraging XP |
| `farming_wisdom` | Farming Wisdom | yes | % bonus farming XP |
| `fishing_wisdom` | Fishing Wisdom | yes | % bonus fishing XP |
| `cooking_wisdom` | Cooking Wisdom | yes | % bonus cooking XP |
| `alchemy_wisdom` | Alchemy Wisdom | yes | % bonus alchemy XP |
| `enchanting_wisdom` | Enchanting Wisdom | yes | % bonus enchanting XP |

## Enchanting

| ID | Display | Percent | Description |
|---|---|---|---|
| `enchanting_luck` | Enchanting Luck | yes | Improves quality of random reforge / upgrade outcomes |

## Reserved (future)

| ID | Display | Percent | Description |
|---|---|---|---|
| `pet_luck` | Pet Luck | yes | Reserved for the future `rpg-pets` addon |

---

## Stat sources

A `StatHolder` (player or mob) aggregates stats from:

- Base stats (set by the entity at creation)
- Equipped items (armor slots, main/off hand)
- Accessories in the accessory bag (only the highest of each `accessory.id` counts by default; configurable)
- Active status effects (positive or negative modifiers, duration-bound)
- Skill milestones (per-level bonuses)
- Guild perks
- Region buffs (set by region flag)

Modifier resolution order (configurable): `flat → percent → multiplicative`.

## Related

- [Damage pipeline](core/damage.md)
- [Skills framework](core/skills.md)
- [Status effects](core/status-effects.md)
- [Health display](core/health-display.md)
