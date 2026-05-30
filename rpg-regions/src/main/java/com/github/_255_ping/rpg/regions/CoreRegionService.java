package com.github._255_ping.rpg.regions;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.persistence.DataStore;
import com.github._255_ping.rpg.api.regions.Region;
import com.github._255_ping.rpg.api.regions.RegionService;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CoreRegionService implements RegionService {

    private static final String REPO = "regions";
    /** DataStore key for the global default flags. Not a real region — stored separately. */
    private static final String GLOBAL_KEY = "__global_flags__";

    private final JavaPlugin plugin;
    private final ConcurrentMap<String, CoreRegion> byId = new ConcurrentHashMap<>();
    /** Server-wide default flags — apply when no region covers a location. */
    private final ConcurrentMap<String, Object> globalFlags = new ConcurrentHashMap<>();

    public CoreRegionService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<Region> regionAt(Location location) {
        List<Region> regions = regionsAt(location);
        return regions.isEmpty() ? Optional.empty() : Optional.of(regions.get(0));
    }

    @Override
    public List<Region> regionsAt(Location location) {
        List<Region> out = new ArrayList<>();
        for (CoreRegion r : byId.values()) if (r.contains(location)) out.add(r);
        out.sort(Comparator.comparingInt(Region::priority).reversed());
        return out;
    }

    @Override
    public boolean flag(Location location, String key, boolean defaultValue) {
        // Check specific regions first (highest priority first).
        for (Region r : regionsAt(location)) {
            Object value = r.flags().get(key);
            if (value instanceof Boolean b) return b;
        }
        // Then global default flags.
        Object globalVal = globalFlags.get(key);
        if (globalVal instanceof Boolean b) return b;
        // Finally, fall back to config-level defaults.
        return plugin.getConfig().getBoolean("default-flags." + key, defaultValue);
    }

    @Override
    public Optional<Region> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Collection<Region> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    // ── Global flags ────────────────────────────────────────────────────────

    public Map<String, Object> globalFlags() {
        return Collections.unmodifiableMap(globalFlags);
    }

    public void setGlobalFlag(String key, Object value) {
        globalFlags.put(key, value);
        saveGlobalFlags();
    }

    public void removeGlobalFlag(String key) {
        globalFlags.remove(key);
        saveGlobalFlags();
    }

    private void saveGlobalFlags() {
        Map<String, Object> data = new HashMap<>();
        data.put("flags", new HashMap<>(globalFlags));
        RpgServices.dataStore().repository(REPO).save(GLOBAL_KEY, data);
    }

    private void loadGlobalFlags() {
        globalFlags.clear();
        RpgServices.dataStore().repository(REPO).get(GLOBAL_KEY).ifPresent(data -> {
            Object raw = data.get("flags");
            if (raw instanceof Map<?, ?> m) {
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    globalFlags.put(String.valueOf(e.getKey()), e.getValue());
                }
            }
        });
    }

    // ── CRUD ────────────────────────────────────────────────────────────────

    public void put(CoreRegion region) {
        byId.put(region.id(), region);
        saveOne(region.id());
    }

    public boolean remove(String id) {
        if (byId.remove(id) == null) return false;
        RpgServices.dataStore().repository(REPO).delete(id);
        return true;
    }

    public void saveOne(String id) {
        CoreRegion r = byId.get(id);
        if (r == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("schema-version", 1);
        data.put("world", r.worldName());
        data.put("minX", r.minX());
        data.put("minY", r.minY());
        data.put("minZ", r.minZ());
        data.put("maxX", r.maxX());
        data.put("maxY", r.maxY());
        data.put("maxZ", r.maxZ());
        data.put("priority", r.priority());
        data.put("flags", new HashMap<>(r.flags()));
        RpgServices.dataStore().repository(REPO).save(id, data);
    }

    public void saveAll() {
        for (String id : byId.keySet()) saveOne(id);
        saveGlobalFlags();
    }

    public void loadAll() {
        byId.clear();
        loadGlobalFlags();
        DataStore.Repository repo = RpgServices.dataStore().repository(REPO);
        for (String key : repo.keys()) {
            if (key.equals(GLOBAL_KEY)) continue; // handled separately
            repo.get(key).ifPresent(data -> {
                try {
                    String world = String.valueOf(data.get("world"));
                    int minX = num(data.get("minX")), minY = num(data.get("minY")), minZ = num(data.get("minZ"));
                    int maxX = num(data.get("maxX")), maxY = num(data.get("maxY")), maxZ = num(data.get("maxZ"));
                    int priority = num(data.get("priority"));
                    @SuppressWarnings("unchecked")
                    Map<String, Object> flags = data.get("flags") instanceof Map<?, ?> m
                            ? (Map<String, Object>) m
                            : new HashMap<>();
                    CoreRegion region = new CoreRegion(key, world, minX, minY, minZ, maxX, maxY, maxZ, priority, flags);
                    byId.put(key, region);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to load region " + key + ": " + ex.getMessage());
                }
            });
        }
    }

    private static int num(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }
}
