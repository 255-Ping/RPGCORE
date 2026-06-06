package com.github._255_ping.rpg.api.mobs;

/**
 * Defines elite-promotion parameters for a mob type. When present on a {@link RpgMob}, each
 * naturally-spawned or spawner-created instance has a {@link #chance()} to be promoted to elite
 * status — boosted HP, a glowing outline, a coloured name prefix, increased damage output, and
 * improved loot drops.
 *
 * <p>Configure under a mob's {@code Elite:} YAML block:
 * <pre>{@code
 * Elite:
 *   Chance: 0.05          # 5% per spawn (default)
 *   HpMultiplier: 3.0     # 3× base HP
 *   DamageMultiplier: 1.5 # 1.5× outgoing damage
 *   LootMultiplier: 2.0   # roll loot tables 2× (integer; fractional = extra roll chance)
 *   Prefix: "§6✦ Elite §r"
 *   Glow: true
 * }</pre>
 */
public record EliteDef(
        double chance,
        double hpMultiplier,
        double damageMultiplier,
        double lootMultiplier,
        String prefix,
        boolean glow
) {
    /** Sensible defaults — 5% chance, 3× HP, 1.5× dmg, 2× loot, gold prefix, glow on. */
    public static EliteDef withDefaults() {
        return new EliteDef(0.05, 3.0, 1.5, 2.0, "§6✦ Elite §r", true);
    }
}
