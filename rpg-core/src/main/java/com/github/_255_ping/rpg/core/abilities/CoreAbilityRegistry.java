package com.github._255_ping.rpg.core.abilities;

import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import com.github._255_ping.rpg.api.abilities.AbilityRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CoreAbilityRegistry implements AbilityRegistry {

    private final ConcurrentMap<String, AbilityEffectFactory> byName = new ConcurrentHashMap<>();

    @Override
    public void register(String name, AbilityEffectFactory factory) {
        byName.put(name, factory);
    }

    @Override
    public AbilityEffect build(String name, Map<String, String> params) {
        AbilityEffectFactory f = byName.get(name);
        if (f == null) return null;
        return f.create(params == null ? Map.of() : params);
    }

    public boolean has(String name) {
        return byName.containsKey(name);
    }

    public void unregister(String name) {
        byName.remove(name);
    }
}
