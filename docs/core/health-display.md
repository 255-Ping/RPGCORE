# Health display (heart-as-percent)

> **Status:** In progress — heart-as-percent display, in-combat tagging, and out-of-combat HEALTH_REGEN tick all working. Stat aggregation from gear/effects layered on top is pending; for now `max_health` comes from `starting-state.base-stats`.

The vanilla health bar shows **20 hearts max** (two rows of 10 hearts) regardless of the player's actual `max_health` stat.

## The math

- Bukkit's attribute `GENERIC_MAX_HEALTH` is locked to **`40.0`** (20 hearts × 2 HP per heart).
- Every tick (or on HP change), our `HealthService` recomputes:

```
bukkitHealth = (currentRpgHp / maxRpgHp) * 40.0
```

So a player at `currentRpgHp = 500 / maxRpgHp = 1000` displays **10 hearts** (one full row). At `1000 / 1000`, both rows are filled. At `100 / 1000`, 2 hearts filled.

## Where the numeric HP shows up

The bar is a percent gauge only. Players see the actual numbers in:

- **Action bar** — `&c❤ 850/1000` (configurable in [rpg-hud](../addons/hud.md))
- **Stats GUI** — `/stats`
- **Damage indicators** — floating numbers at hit location ([rpg-holograms](../addons/holograms.md))
- **Scoreboard** — optional line

## Edge cases

- **Commands that set vanilla HP** (`/heal`, `/minecraft:effect give @s instant_health`) — we re-sync on the next tick. Vanilla effect application is cancelled per [vanilla suppression](vanilla-suppression.md), so this rarely fires.
- **Absorption hearts** — vanilla absorption is also cancelled. We have our own "shield" mechanic if needed (TBD, not in v1).
- **Hardcore mode** — visual hearts don't change. We rebuild the bar regardless.

## Heart aesthetics

The heart icons themselves remain vanilla red. Admins can re-skin them via resource pack (the icon texture sits in `assets/minecraft/textures/gui/icons.png`). We document a [resource-pack reservation](../resource-pack.md) but don't ship a pack.

## Config

In `plugins/rpg-core/config.yml`:

```yaml
health:
  default-max: 100               # default max_health stat at player creation
  bukkit-max-health: 40.0        # 20 hearts × 2 — only change if you really know what you're doing
  resync-interval-ticks: 1
  out-of-combat-regen-source: HEALTH_REGEN  # stat that drives the regen rate
```

## Related

- [Damage pipeline](damage.md)
- [Vanilla suppression](vanilla-suppression.md)
- [Stats](../stats.md) — `max_health`, `health_regen`, `vitality`
