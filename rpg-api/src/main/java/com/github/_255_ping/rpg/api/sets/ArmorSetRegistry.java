package com.github._255_ping.rpg.api.sets;

import java.util.Collection;
import java.util.Optional;

/** Read-only view of all loaded armor set definitions. */
public interface ArmorSetRegistry {

    /** Returns the set with the given id, or empty if not loaded. */
    Optional<ArmorSetDef> get(String id);

    /** All registered set definitions. */
    Collection<ArmorSetDef> all();
}
