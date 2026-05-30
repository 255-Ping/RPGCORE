package com.github._255_ping.rpg.core.spawners;

import org.bukkit.Location;

public final class SpawnerDef {

    private final String id;
    private final String mobId;
    private final String worldName;
    private final int x, y, z;
    private int spawnRadius;
    private int cooldownTicks;
    private int maxAlive;
    private boolean continuous;
    private int minLevel;
    private int maxLevel;

    public SpawnerDef(String id, String mobId, Location loc) {
        this.id = id;
        this.mobId = mobId;
        this.worldName = loc.getWorld() == null ? "world" : loc.getWorld().getName();
        this.x = loc.getBlockX();
        this.y = loc.getBlockY();
        this.z = loc.getBlockZ();
        this.spawnRadius = 4;
        this.cooldownTicks = 100;
        this.maxAlive = 5;
        this.continuous = true;
        this.minLevel = 1;
        this.maxLevel = 1;
    }

    public SpawnerDef(String id, String mobId, String worldName, int x, int y, int z,
                       int spawnRadius, int cooldownTicks, int maxAlive, boolean continuous,
                       int minLevel, int maxLevel) {
        this.id = id;
        this.mobId = mobId;
        this.worldName = worldName;
        this.x = x; this.y = y; this.z = z;
        this.spawnRadius = spawnRadius;
        this.cooldownTicks = cooldownTicks;
        this.maxAlive = maxAlive;
        this.continuous = continuous;
        this.minLevel = Math.max(1, minLevel);
        this.maxLevel = Math.max(this.minLevel, maxLevel);
    }

    /** Legacy constructor without levels — defaults to level 1. */
    public SpawnerDef(String id, String mobId, String worldName, int x, int y, int z,
                       int spawnRadius, int cooldownTicks, int maxAlive, boolean continuous) {
        this(id, mobId, worldName, x, y, z, spawnRadius, cooldownTicks, maxAlive, continuous, 1, 1);
    }

    public String id() { return id; }
    public String mobId() { return mobId; }
    public String worldName() { return worldName; }
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public int spawnRadius() { return spawnRadius; }
    public int cooldownTicks() { return cooldownTicks; }
    public int maxAlive() { return maxAlive; }
    public boolean continuous() { return continuous; }
    public int minLevel() { return minLevel; }
    public int maxLevel() { return maxLevel; }

    public void setSpawnRadius(int v) { this.spawnRadius = Math.max(0, v); }
    public void setCooldownTicks(int v) { this.cooldownTicks = Math.max(1, v); }
    public void setMaxAlive(int v) { this.maxAlive = Math.max(0, v); }
    public void setContinuous(boolean v) { this.continuous = v; }
    public void setMinLevel(int v) { this.minLevel = Math.max(1, v); if (maxLevel < minLevel) maxLevel = minLevel; }
    public void setMaxLevel(int v) { this.maxLevel = Math.max(1, v); if (minLevel > maxLevel) minLevel = maxLevel; }
}
