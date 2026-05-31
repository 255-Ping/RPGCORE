# Progression Guide

A practical guide for admins designing gear and mob stat budgets. The formulas here come from the damage pipeline — see [Damage](../core/damage.md) for the full math.

---

## The damage formula (quick reference)

```
final = base_damage × (1 + strength/100) × (1 - defense/(defense + 100))
```

- `base_damage` — the item's `damage` stat
- `strength` — attacker's total `strength` stat
- `defense` — defender's total `defense` stat (mob or player)

On a crit, multiply by `(1 + crit_damage/100)`. Crit chance rolls `crit_chance/100` each hit.

---

## Tier stat budgets

Use these ranges as starting points. Adjust up or down based on your pacing preferences. The numbers assume the default formula with no extra modifiers.

| Tier | Gear level | Weapon `damage` | Attacker `strength` | Mob `defense` | Mob HP | Time to kill (approx) |
|---|---|---|---|---|---|---|
| **Starter** | 1–10 | 30–60 | 5–20 | 0–15 | 80–150 | 3–5 hits |
| **Common** | 10–25 | 60–120 | 20–50 | 15–40 | 150–400 | 4–6 hits |
| **Rare** | 25–40 | 120–250 | 50–100 | 40–80 | 400–1 000 | 5–8 hits |
| **Epic** | 40–60 | 250–500 | 100–180 | 80–130 | 1 000–3 000 | 6–10 hits |
| **Legendary** | 60+ | 500–1 000 | 180–300 | 130–180 | 3 000–10 000 | 8–15 hits |

### Worked example (Tier 2 — Common)

- Player weapon: `damage: 100`, `strength: 40`
- Mob: `defense: 30`, `MaxHealth: 300`

```
final = 100 × (1 + 40/100) × (1 - 30/130)
      = 100 × 1.40 × 0.769
      = 107.7
```

The mob dies in roughly **3 hits** — a bit fast for Common tier. Bump `MaxHealth` to 400–500 or raise `defense` to 50.

---

## Defense diminishing returns

Defense uses a diminishing-returns formula (`defense/(defense + 100)`), so stacking it past ~200 has rapidly shrinking gains.

| Defense | Damage reduction | Effective HP multiplier |
|---|---|---|
| 0 | 0% | 1.0× |
| 25 | 20% | 1.25× |
| 50 | 33% | 1.5× |
| 100 | 50% | 2.0× |
| 150 | 60% | 2.5× |
| 200 | 67% | 3.0× |
| 300 | 75% | 4.0× |
| 500 | 83% | 5.9× |

**Rule of thumb:** `defense: 100` doubles effective HP. Items beyond `defense: 200` are rarely worth the stat cost.

---

## Strength returns

Strength is a linear multiplier — every 100 strength is +100% damage (doubles damage at 100, triples at 200).

| Strength | Damage multiplier |
|---|---|
| 0 | 1.0× (no bonus) |
| 25 | 1.25× |
| 50 | 1.5× |
| 100 | 2.0× |
| 200 | 3.0× |
| 300 | 4.0× |

Since it's linear, there's no "cap" — but stat budgets (below) keep it from spiraling.

---

## Crit chance & damage

- `crit_chance` — percent chance per hit (0–100). At 100 every hit crits.
- `crit_damage` — bonus multiplier on a crit. Default: `crit_damage: 50` = 1.5× damage on crit.

**Expected DPS multiplier from crits:**

```
1 + (crit_chance/100) × (crit_damage/100)
```

Example: 30% crit chance, 100% crit damage → `1 + 0.30 × 1.0 = 1.3×` average damage.

Avoid giving players both very high crit chance **and** very high crit damage — the multiplier compounds fast:

| Crit chance | Crit damage | DPS multiplier |
|---|---|---|
| 20% | 50% | 1.10× |
| 30% | 100% | 1.30× |
| 50% | 150% | 1.75× |
| 80% | 200% | 2.60× |
| 100% | 300% | 4.00× |

---

## Mob design guidelines

### Trash mobs
Fast to kill. Dense spawns. Reward: small XP + common drops.

- HP: **2–4×** the player's expected hit
- Defense: **low** (≤ 30% of damage reduction)
- Abilities: none, or simple `~onTimer` with long cooldown

### Named / champion mobs
Slower to kill, solo encounter. Reward: uncommon drops.

- HP: **8–15× player hit** (~60 seconds of combat)
- Defense: moderate (40–60% reduction with appropriate gear)
- Abilities: 1–2 abilities with `~onHurt` or `~onTimer`

### Dungeon bosses
Sustained fight. Reward: rare/epic drops.

- HP: **30–60× player hit** (3–5 minutes of combat)
- Defense: high (60–70% reduction)
- Abilities: 2–4 abilities, mix of `~onTimer`, `~onHurt`, `~onSpawn`
- Consider adding a phase change at 50% HP (requires health threshold support — see [Patterns](patterns.md#boss-phase-change-planned))

---

## Player HP guidelines

`max_health` is additive on top of the player's base vanilla HP (20). The stat adds hearts.

| Tier | Recommended `max_health` bonus | Total effective HP |
|---|---|---|
| No gear | 0 | 20 |
| Starter set | +20–50 | 40–70 |
| Common set | +50–100 | 70–120 |
| Rare set | +100–200 | 120–220 |
| Epic set | +200–400 | 220–420 |
| Legendary set | +400–800 | 420–820 |

Scale mob damage accordingly — a Rare-tier player at 200 HP should feel threatened by a named mob doing 40–60 per hit.

---

## Quick-check before shipping content

1. **Spawn the mob, fight it with starter gear.** Does it die in a reasonable number of hits? Is it threatening?
2. **Fight it with the intended drop item.** Is the upgrade obvious?
3. **Admin-skip to end-game gear, fight a starter mob.** Should one-shot or near one-shot — if not, starter mobs feel spongey forever.
4. **Fight a legendary mob with starter gear.** Should be near-impossible — confirms tier gates are real.

> **See also:** [Damage pipeline](../core/damage.md), [Stats reference](../stats.md), [Mobs](mobs.md)
