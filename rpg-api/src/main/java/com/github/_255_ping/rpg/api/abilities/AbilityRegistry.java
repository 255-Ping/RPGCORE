package com.github._255_ping.rpg.api.abilities;

import java.util.Map;

public interface AbilityRegistry {
    void register(String name, AbilityEffectFactory factory);
    AbilityEffect build(String name, Map<String, String> params);

    @FunctionalInterface
    interface AbilityEffectFactory {
        AbilityEffect create(Map<String, String> params);
    }
}
