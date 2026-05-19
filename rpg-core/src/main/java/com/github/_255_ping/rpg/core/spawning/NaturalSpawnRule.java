package com.github._255_ping.rpg.core.spawning;

import java.util.List;

/**
 * One natural-spawn rule loaded from {@code plugins/rpg-core/natural-spawning/*.yml}.
 */
public record NaturalSpawnRule(
        String id,
        boolean enabled,
        List<WeightedMob> mobs,
        List<String> timeOfDay,        // e.g., ["night"], ["day"], or ["any"]
        Integer lightMin,
        Integer lightMax,
        List<String> weather,          // clear | rain | storm | any
        List<String> biomes,           // lowercase biome ids, or ["any"]
        Integer yMin,
        Integer yMax,
        double perPlayerPerTick,       // 0.0001 = 0.01% chance per online player per tick
        int minDistanceFromPlayer,
        int maxDistanceFromPlayer
) {

    public record WeightedMob(String mobId, int weight) {}
}
