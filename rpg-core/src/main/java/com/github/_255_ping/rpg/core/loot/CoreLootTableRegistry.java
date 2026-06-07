package com.github._255_ping.rpg.core.loot;

import com.github._255_ping.rpg.api.loot.LootTable;
import com.github._255_ping.rpg.api.loot.LootTableRegistry;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CoreLootTableRegistry implements LootTableRegistry {

    private final ConcurrentMap<String, LootTable> byId = new ConcurrentHashMap<>();

    public void clear() {
        byId.clear();
    }

    @Override
    public void register(LootTable table) {
        byId.put(table.id(), table);
    }

    @Override
    public Optional<LootTable> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Collection<LootTable> all() {
        return byId.values();
    }
}
