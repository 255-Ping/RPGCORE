package com.github._255_ping.rpg.api.abilities;

import java.util.List;
import java.util.Map;

public interface AbilityRegistry {
    void register(String name, AbilityEffectFactory factory);
    AbilityEffect build(String name, Map<String, String> params);

    /**
     * Store display name and description for a custom (YAML-defined) ability.
     * Called by AbilityLoader after registering the factory.
     * Default no-op so built-in effects don't need to implement it.
     */
    default void registerMeta(String id, String displayName, List<String> description) {}

    /** Returns the display name for an ability id, or the id itself if none registered. */
    default String abilityDisplayName(String id) { return id; }

    /** Returns the description lines for an ability id, or empty list if none registered. */
    default List<String> abilityDescription(String id) { return List.of(); }

    @FunctionalInterface
    interface AbilityEffectFactory {
        AbilityEffect create(Map<String, String> params);
    }
}
