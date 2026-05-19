package com.github._255_ping.rpg.api.loot;

import java.util.Collection;
import java.util.Optional;

public interface LootTableRegistry {
    void register(LootTable table);
    Optional<LootTable> get(String id);
    Collection<LootTable> all();
}
