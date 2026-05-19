package com.github._255_ping.rpg.core.blocks;

import com.github._255_ping.rpg.api.persistence.DataStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Saves and loads the {@link CoreBlockRegistry}'s location→id map via the {@link DataStore}.
 * Single record keyed {@code "tagged-locations"} in the {@code blocks} repository, holding
 * a list of {@code "<world>:<x>:<y>:<z>:<blockId>"} strings.
 */
public final class BlockPersistence {

    private static final String REPO = "blocks";
    private static final String KEY = "tagged-locations";

    private final DataStore dataStore;
    private final CoreBlockRegistry registry;

    public BlockPersistence(DataStore dataStore, CoreBlockRegistry registry) {
        this.dataStore = dataStore;
        this.registry = registry;
    }

    public void save() {
        Map<BlockKey, String> snapshot = registry.snapshotLocations();
        List<String> serialized = new ArrayList<>(snapshot.size());
        for (Map.Entry<BlockKey, String> e : snapshot.entrySet()) {
            BlockKey k = e.getKey();
            serialized.add(k.world() + ":" + k.x() + ":" + k.y() + ":" + k.z() + ":" + e.getValue());
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("schema-version", 1);
        payload.put("locations", serialized);
        dataStore.repository(REPO).save(KEY, payload).join();
    }

    public void load() {
        dataStore.repository(REPO).get(KEY).ifPresent(map -> {
            Object raw = map.get("locations");
            if (!(raw instanceof List<?> list)) return;
            Map<BlockKey, String> snapshot = new HashMap<>();
            for (Object o : list) {
                if (!(o instanceof String s)) continue;
                String[] parts = s.split(":", 5);
                if (parts.length != 5) continue;
                try {
                    snapshot.put(new BlockKey(parts[0],
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[3])), parts[4]);
                } catch (NumberFormatException ignored) {}
            }
            registry.restoreLocations(snapshot);
        });
    }
}
