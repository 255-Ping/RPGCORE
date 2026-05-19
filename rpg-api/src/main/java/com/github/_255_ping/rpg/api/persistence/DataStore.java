package com.github._255_ping.rpg.api.persistence;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface DataStore {

    <V> Repository<V> repository(String name, Class<V> type);

    interface Repository<V> {
        Optional<V> get(String key);
        CompletableFuture<Void> save(String key, V value);
        CompletableFuture<Void> delete(String key);
        Collection<String> keys();
    }
}
