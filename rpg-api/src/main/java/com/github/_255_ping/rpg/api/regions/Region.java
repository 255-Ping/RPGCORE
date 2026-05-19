package com.github._255_ping.rpg.api.regions;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;

public interface Region {
    String id();
    World world();
    int minX(); int minY(); int minZ();
    int maxX(); int maxY(); int maxZ();
    int priority();
    Map<String, Object> flags();
    boolean contains(Location loc);
}
