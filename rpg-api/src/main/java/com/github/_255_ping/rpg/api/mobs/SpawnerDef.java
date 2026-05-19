package com.github._255_ping.rpg.api.mobs;

import org.bukkit.Location;

public record SpawnerDef(
        String id,
        Location origin,
        double radius,
        int cooldownTicks,
        int maxMobs,
        String mobType
) {}
