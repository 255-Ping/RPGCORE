package com.github._255_ping.rpg.regions;

import com.github._255_ping.rpg.api.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

public final class CoreRegion implements Region {

    private final String id;
    private final String worldName;
    private final int minX, minY, minZ, maxX, maxY, maxZ;
    private int priority;
    private final Map<String, Object> flags;

    public CoreRegion(String id, String worldName,
                       int minX, int minY, int minZ,
                       int maxX, int maxY, int maxZ,
                       int priority,
                       Map<String, Object> flags) {
        this.id = id;
        this.worldName = worldName;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        this.priority = priority;
        this.flags = new HashMap<>(flags);
    }

    @Override public String id() { return id; }
    @Override public World world() { return Bukkit.getWorld(worldName); }
    public String worldName() { return worldName; }
    @Override public int minX() { return minX; }
    @Override public int minY() { return minY; }
    @Override public int minZ() { return minZ; }
    @Override public int maxX() { return maxX; }
    @Override public int maxY() { return maxY; }
    @Override public int maxZ() { return maxZ; }
    @Override public int priority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    @Override public Map<String, Object> flags() { return flags; }
    public void setFlag(String key, Object value) { flags.put(key, value); }

    @Override
    public boolean contains(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) return false;
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
}
