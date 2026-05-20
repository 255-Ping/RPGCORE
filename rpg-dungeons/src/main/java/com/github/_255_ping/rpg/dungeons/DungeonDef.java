package com.github._255_ping.rpg.dungeons;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.LinkedHashMap;

public record DungeonDef(
        String id,
        String displayName,
        String templateWorld,
        Vector min,
        Vector max,
        Vector spawnOffset,
        String entranceWorld,
        Vector entrance,
        String exitWorld,
        Vector exit,
        int maxPlayers,
        int requiredLevel
) {

    public BoundingBox templateBox() {
        return BoundingBox.of(min.toLocation(Bukkit.getWorlds().get(0)),
                max.toLocation(Bukkit.getWorlds().get(0)));
    }

    public Location entranceLocation() {
        if (Bukkit.getWorld(entranceWorld) == null) return null;
        return new Location(Bukkit.getWorld(entranceWorld), entrance.getX(), entrance.getY(), entrance.getZ());
    }

    public Location exitLocation() {
        if (Bukkit.getWorld(exitWorld) == null) return null;
        return new Location(Bukkit.getWorld(exitWorld), exit.getX(), exit.getY(), exit.getZ());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("DisplayName", displayName);
        Map<String, Object> tmpl = new LinkedHashMap<>();
        tmpl.put("World", templateWorld);
        tmpl.put("Min", Map.of("X", min.getX(), "Y", min.getY(), "Z", min.getZ()));
        tmpl.put("Max", Map.of("X", max.getX(), "Y", max.getY(), "Z", max.getZ()));
        out.put("Template", tmpl);
        out.put("Spawn", Map.of("X", spawnOffset.getX(), "Y", spawnOffset.getY(), "Z", spawnOffset.getZ()));
        out.put("Entrance", Map.of("World", entranceWorld,
                "X", entrance.getX(), "Y", entrance.getY(), "Z", entrance.getZ()));
        out.put("Exit", Map.of("World", exitWorld,
                "X", exit.getX(), "Y", exit.getY(), "Z", exit.getZ()));
        out.put("MaxPlayers", maxPlayers);
        out.put("RequiredLevel", requiredLevel);
        return out;
    }
}
