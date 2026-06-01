![banner-skills](../assets/banners/banner-skills.png)

# Skills framework

> **Status:** In Progress — `SkillsService` tracks per-player XP, computes level from configured curves via the expression evaluator, fires `SkillXpAwardEvent`, persists across sessions. Pinned skill tracking works. Milestones, per-level implicit rewards, and per-skill addon XP sources arrive with each skill addon.

`rpg-core` ships the skills *framework*. Each individual skill is its own addon ([rpg-combat](../addons/skills.md#combat), [rpg-mining](../addons/skills.md#mining), etc.).

## Per-skill config

Each skill addon owns its own `config.yml`. The framework reads a common shape:

```yaml
skill:
  id: mining                     # canonical id
  max-level: 50
  curve: "100 * level ^ 1.5"     # expression (see below)
  xp-sources:                    # how players earn XP — addon-specific
    break-block:
      coal_ore: 5
      diamond_ore: 30
  milestones:                    # explicit per-level rewards
    5:  { stats: { mining_speed: 2 } }
    10: { stats: { mining_speed: 5, mining_fortune: 1 }, message: "&aYou feel quicker mining!" }
    25: { stats: { mining_speed: 10, mining_fortune: 3 } }
  implicit-per-level:            # added every level
    stats:
      mining_speed: 0.5
      mining_fortune: 0.1
```

Defaults for `max-level` and `curve` come from `rpg-core/config.yml` if a skill addon doesn't override them:

```yaml
# rpg-core/config.yml
skills:
  default-max-level: 50
  default-curve: "100 * level ^ 1.5"
```

## Level curves

The `curve` field is an expression for **XP required to advance from `level` to `level+1`**.

### Expression evaluator

The evaluator is in-house, no scripting, no I/O. Whitelisted symbols only.

**Variables:**

- `level` — current level (1-based)
- `prev_xp_total` — sum of XP costs for all previous levels (only useful in some formulas)

**Operators:** `+ - * / % ^` (plus parentheses)

**Functions:** `pow(x, y)`, `sqrt(x)`, `min(x, y)`, `max(x, y)`, `floor(x)`, `ceil(x)`, `round(x)`, `abs(x)`, `log(x)`, `ln(x)`

Unknown identifiers reject with a clear error including position.

### Examples

```yaml
# Linear
curve: "1000 * level"

# Quadratic
curve: "50 * level ^ 2"

# Hypixel SkyBlock-ish (rounded)
curve: "round(50 * level ^ 1.7 + 100)"
```

## XP-source binding

Each skill addon declares its XP sources (which Bukkit events / our events award XP and how much):

- `rpg-combat` — `PostDamageEvent` proportional to damage dealt
- `rpg-mining` — custom-block break
- `rpg-foraging` — custom-block break (axe-target blocks)
- `rpg-farming` — crop harvest
- `rpg-fishing` — successful catch
- `rpg-cooking` — cooking station craft completion
- `rpg-alchemy` — brewing station craft completion
- `rpg-enchanting` — applying an enchant or reforge

XP awarded is multiplied by `(1 + <skill>_wisdom / 100)`.

## Milestones

Per-skill explicit list. Each entry: `{ stats, message, unlocks }`. Granted on level-up.

`unlocks` is a free-form string list — addons interpret them (e.g., `"recipe:iron_pickaxe"` to gate a recipe).

`implicit-per-level` is folded in for every level regardless of milestones.

## API

```java
SkillsService skills = RpgServices.skills();

skills.awardXp(player, "mining", 50);
int level = skills.level(player, "mining");
long totalXp = skills.totalXp(player, "mining");
long xpToNext = skills.xpToNext(player, "mining");
```

Curve changes don't lose progress — total XP is invariant. On reload, levels recompute from total XP using the new curve.

## Pinning a skill

`/skill pin <skill>` (perm `rpg.core.skill.pin`, default true) pins a skill to the vanilla XP bar so the player can see that specific skill's progress regardless of recent activity. Without a pin, the bar shows the most-recently-active skill.

## Related

- [Stats reference](../stats.md) — wisdom and skill-related stats
- [Damage pipeline](damage.md) — combat XP source
- [Per-skill addon docs](../addons/skills.md)
