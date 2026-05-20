package com.github._255_ping.rpg.holograms;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HologramDef {

    private final String id;
    private String worldName;
    private double x, y, z;
    private final List<String> lines;

    public HologramDef(String id, Location loc, List<String> lines) {
        this.id = id;
        this.worldName = loc.getWorld().getName();
        this.x = loc.getX(); this.y = loc.getY(); this.z = loc.getZ();
        this.lines = new ArrayList<>(lines);
    }

    public HologramDef(String id, String worldName, double x, double y, double z, List<String> lines) {
        this.id = id;
        this.worldName = worldName;
        this.x = x; this.y = y; this.z = z;
        this.lines = new ArrayList<>(lines);
    }

    public String id() { return id; }
    public String worldName() { return worldName; }
    public double x() { return x; } public double y() { return y; } public double z() { return z; }
    public List<String> lines() { return lines; }

    public Location location() {
        var w = Bukkit.getWorld(worldName);
        return w == null ? null : new Location(w, x, y, z);
    }

    public void moveTo(Location loc) {
        this.worldName = loc.getWorld().getName();
        this.x = loc.getX(); this.y = loc.getY(); this.z = loc.getZ();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("World", worldName);
        out.put("X", x); out.put("Y", y); out.put("Z", z);
        out.put("Lines", lines);
        return out;
    }
}
