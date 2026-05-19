package com.github._255_ping.rpg.regions;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.persistence.DataStore;
import com.github._255_ping.rpg.api.regions.Region;
import com.github._255_ping.rpg.api.regions.RegionService;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CoreRegionService implements RegionService {

    private static final String REPO = "regions";

    private final JavaPlugin plugin;
    private final ConcurrentMap<String, CoreRegion> byId = new ConcurrentHashMap<>();

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
        for (Region r : regionsAt(location)) {
            Object value = r.flags().get(key);
            if (value instanceof Boolean b) return b;
        }
        return plugin.getConfig().getBoolean("default-flags." + key, defaultValue);
    }

    @Override
    public Optional<Region> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Collection<Region> all() {
        return java.util.Collections.unmodifiableCollection(byId.values());
    }

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
    }

    public void loadAll() {
        byId.clear();
        DataStore.Repository repo = RpgServices.dataStore().repository(REPO);
        for (String key : repo.keys()) {
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
