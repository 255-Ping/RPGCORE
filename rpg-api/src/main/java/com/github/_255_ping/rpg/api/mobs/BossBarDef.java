package com.github._255_ping.rpg.api.mobs;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

/**
 * Display configuration for a mob's boss health bar. Attached to any {@link RpgMob} that has a
 * {@code BossBar:} section in its YAML definition. Consumed by {@code rpg-bossbar} to show and
 * update the bar when players are near the mob.
 *
 * @param name      Title shown above the bar. Defaults to the mob's DisplayName (legacy colours
 *                  stripped to plain text) if not explicitly set in YAML.
 * @param color     Bar fill colour. Defaults to {@link BarColor#RED}.
 * @param style     Bar segmentation style. Defaults to {@link BarStyle#SOLID}.
 * @param showRange Distance (blocks) within which the bar becomes visible to a player.
 *                  {@code 0} means "use the rpg-bossbar config default".
 */
public record BossBarDef(String name, BarColor color, BarStyle style, double showRange) {

    /** Sentinel value for {@link #showRange}: use the plugin's configured default. */
    public static final double USE_CONFIG_RANGE = 0.0;

    /** Convenience factory with colour/style/range defaults. */
    public static BossBarDef of(String name) {
        return new BossBarDef(name, BarColor.RED, BarStyle.SOLID, USE_CONFIG_RANGE);
    }
}
