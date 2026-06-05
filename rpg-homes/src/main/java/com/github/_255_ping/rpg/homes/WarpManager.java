package com.github._255_ping.rpg.homes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Server-wide warp points, persisted as {@code warps.yml} in the plugin data folder.
 * Unlike homes, warps are admin-managed and not per-player.
 */
public final class WarpManager {

    private final File   file;
    private final Logger logger;
    private final Map<String, Location> warps = new LinkedHashMap<>();

    public WarpManager(File dataFolder, Logger logger) {
        this.file   = new File(dataFolder, "warps.yml");
        this.logger = logger;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void setWarp(String name, Location loc) {
        warps.put(name.toLowerCase(Locale.ROOT), loc);
        save();
    }

    public Optional<Location> getWarp(String name) {
        return Optional.ofNullable(warps.get(name.toLowerCase(Locale.ROOT)));
    }

    /** @return {@code true} if the warp existed and was removed. */
    public boolean deleteWarp(String name) {
        if (warps.remove(name.toLowerCase(Locale.ROOT)) == null) return false;
        save();
        return true;
    }

    /** Sorted list of warp names. */
    public List<String> listWarps() {
        List<String> names = new ArrayList<>(warps.keySet());
        Collections.sort(names);
        return names;
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    public void load() {
        warps.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection s = yaml.getConfigurationSection(key);
            if (s == null) continue;
            Location loc = parseLocation(s, key);
            if (loc != null) warps.put(key, loc);
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Location> e : warps.entrySet()) {
            String key = e.getKey();
            Location loc = e.getValue();
            yaml.set(key + ".world", loc.getWorld().getName());
            yaml.set(key + ".x",     loc.getX());
            yaml.set(key + ".y",     loc.getY());
            yaml.set(key + ".z",     loc.getZ());
            yaml.set(key + ".yaw",   (double) loc.getYaw());
            yaml.set(key + ".pitch", (double) loc.getPitch());
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            logger.severe("Failed to save warps.yml: " + ex.getMessage());
        }
    }

    private Location parseLocation(ConfigurationSection s, String key) {
        try {
            String worldName = s.getString("world", "world");
            World  world     = Bukkit.getWorld(worldName);
            if (world == null) { logger.warning("Warp '" + key + "' references unknown world '" + worldName + "'"); return null; }
            double x     = s.getDouble("x");
            double y     = s.getDouble("y");
            double z     = s.getDouble("z");
            float  yaw   = (float) s.getDouble("yaw");
            float  pitch = (float) s.getDouble("pitch");
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception ex) {
            logger.warning("Could not parse warp '" + key + "': " + ex.getMessage());
            return null;
        }
    }
}
