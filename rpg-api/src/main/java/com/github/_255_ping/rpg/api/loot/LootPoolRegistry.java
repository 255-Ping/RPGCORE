package com.github._255_ping.rpg.api.loot;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry of named {@link LootPool} definitions loaded from
 * {@code plugins/rpg-core/loot-pools/*.yml}.
 *
 * <p>Accessible via {@code RpgServices.lootPools()}.
 */
public interface LootPoolRegistry {

    /** Register or replace a pool by its {@link LootPool#id()}. */
    void register(LootPool pool);

    /** Look up a pool by ID. Returns empty if the ID is unknown. */
    Optional<LootPool> get(String id);

    /** All currently registered pools. */
    Collection<LootPool> all();

    /** Remove all registered pools (called before a reload). */
    void clear();
}
