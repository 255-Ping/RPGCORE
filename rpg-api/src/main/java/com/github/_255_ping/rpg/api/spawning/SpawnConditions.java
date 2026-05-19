package com.github._255_ping.rpg.api.spawning;

import java.util.List;

public record SpawnConditions(
        List<String> timeOfDay,
        Range lightLevel,
        List<String> weather,
        List<String> biomes,
        String region,
        String forbidRegion,
        Range yRange,
        List<Integer> moonPhases,
        Range playersWithinBlocks,
        Integer minDistanceFromPlayers
) {
    public record Range(Integer min, Integer max) {}
}
