package com.github._255_ping.rpg.homes;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.persistence.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;

/**
 * Per-player home storage.
 *
 * <p>Homes are lazily loaded from DataStore on first access and cached in memory
 * until the player disconnects. Keys are normalised to lowercase.
 */
public final class HomeManager {

    private static final String REPO = "homes";

    private final int    maxHomes;
    private final Logger logger;
    /** UUID → (lowercase-name → Location) */
    private final Map<UUID, Map<String, Location>> cache = new HashMap<>();

    public HomeManager(int maxHomes, Logger logger) {
        this.maxHomes = maxHomes;
        this.logger   = logger;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** @return {@code true} if the home was set, {@code false} if the player hit the limit. */
    public boolean setHome(Player p, String name, Location loc) {
        Map<String, Location> homes = getOrLoad(p.getUniqueId());
        String key = name.toLowerCase(Locale.ROOT);
        if (!homes.containsKey(key) && homes.size() >= maxHomes) return false;
        homes.put(key, loc);
        save(p.getUniqueId(), homes);
        return true;
    }

    public Optional<Location> getHome(Player p, String name) {
        return Optional.ofNullable(getOrLoad(p.getUniqueId()).get(name.toLowerCase(Locale.ROOT)));
    }

    /** @return {@code true} if the home existed and was deleted. */
    public boolean deleteHome(Player p, String name) {
        Map<String, Location> homes = getOrLoad(p.getUniqueId());
        String key = name.toLowerCase(Locale.ROOT);
        if (homes.remove(key) == null) return false;
        save(p.getUniqueId(), homes);
        return true;
    }

    /** Sorted list of home names for a player. */
    public List<String> listHomes(Player p) {
        List<String> names = new ArrayList<>(getOrLoad(p.getUniqueId()).keySet());
        Collections.sort(names);
        return names;
    }

    public int maxHomes() { return maxHomes; }

    /** Called on PlayerQuitEvent — flushes cache entry (data already saved on mutation). */
    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private Map<String, Location> getOrLoad(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    private Map<String, Location> load(UUID uuid) {
        Map<String, Location> result = new LinkedHashMap<>();
        Optional<Map<String, Object>> saved = repo().get(uuid.toString());
        if (saved.isEmpty()) return result;
        for (Map.Entry<String, Object> e : saved.get().entrySet()) {
            if (!(e.getValue() instanceof Map<?, ?> raw)) continue;
            Location loc = parseLocation(raw);
            if (loc != null) result.put(e.getKey(), loc);
        }
        return result;
    }

    private void save(UUID uuid, Map<String, Location> homes) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (Map.Entry<String, Location> e : homes.entrySet()) {
            data.put(e.getKey(), serialiseLocation(e.getValue()));
        }
        repo().save(uuid.toString(), data);
    }

    private static Map<String, Object> serialiseLocation(Location loc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("world", loc.getWorld().getName());
        m.put("x",     loc.getX());
        m.put("y",     loc.getY());
        m.put("z",     loc.getZ());
        m.put("yaw",   (double) loc.getYaw());
        m.put("pitch", (double) loc.getPitch());
        return m;
    }

    private Location parseLocation(Map<?, ?> raw) {
        try {
            String worldName = (String) raw.get("world");
            World  world     = Bukkit.getWorld(worldName);
            if (world == null) { logger.warning("Home references unknown world '" + worldName + "'"); return null; }
            double x     = ((Number) raw.get("x")).doubleValue();
            double y     = ((Number) raw.get("y")).doubleValue();
            double z     = ((Number) raw.get("z")).doubleValue();
            float  yaw   = raw.get("yaw")   instanceof Number n ? n.floatValue() : 0f;
            float  pitch = raw.get("pitch") instanceof Number n ? n.floatValue() : 0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception ex) {
            logger.warning("Could not parse home location: " + ex.getMessage());
            return null;
        }
    }

    private DataStore.Repository repo() {
        return RpgServices.dataStore().repository(REPO);
    }
}
