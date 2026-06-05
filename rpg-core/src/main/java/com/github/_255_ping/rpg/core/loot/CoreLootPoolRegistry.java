package com.github._255_ping.rpg.core.loot;

import com.github._255_ping.rpg.api.loot.LootPool;
import com.github._255_ping.rpg.api.loot.LootPoolRegistry;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CoreLootPoolRegistry implements LootPoolRegistry {

    private final ConcurrentMap<String, LootPool> byId = new ConcurrentHashMap<>();

    @Override
    public void register(LootPool pool) {
        byId.put(pool.id(), pool);
    }

    @Override
    public Optional<LootPool> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Collection<LootPool> all() {
        return byId.values();
    }

    @Override
    public void clear() {
        byId.clear();
    }
}
