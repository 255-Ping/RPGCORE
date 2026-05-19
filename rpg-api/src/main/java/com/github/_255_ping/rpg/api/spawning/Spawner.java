package com.github._255_ping.rpg.api.spawning;

import org.bukkit.Location;

public interface Spawner {
    String id();
    String mobId();
    Location location();
    SpawnConditions conditions();
    Mode mode();
    int maxAlive();
    int spawnRadius();
    int cooldownTicks();
    int batchSize();

    enum Mode { CONTINUOUS, ONE_SHOT, BOUNDED }
}
