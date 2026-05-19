package com.github._255_ping.rpg.api.persistence;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Persistence abstraction. Backend (YAML or MySQL) is chosen in the core config.
 *
 * <p>Records are stored as untyped {@code Map<String, Object>}; each addon is responsible
 * for serializing its own types to/from maps. Map keys are strings; values must be types
 * SnakeYAML and JDBC can both serialize (primitives, strings, lists, nested maps).
 */
public interface DataStore {

    Repository repository(String name);

    interface Repository {
        Optional<Map<String, Object>> get(String key);
        CompletableFuture<Void> save(String key, Map<String, Object> data);
        CompletableFuture<Void> delete(String key);
        Collection<String> keys();
    }
}
