package com.github._255_ping.rpg.core.status;

import com.github._255_ping.rpg.api.status.StatusEffect;
import com.github._255_ping.rpg.api.status.StatusEffectRegistry;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CoreStatusEffectRegistry implements StatusEffectRegistry {

    private final ConcurrentMap<String, StatusEffect> byId = new ConcurrentHashMap<>();

    @Override
    public void register(StatusEffect effect) {
        byId.put(effect.id(), effect);
    }

    @Override
    public Optional<StatusEffect> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Collection<StatusEffect> all() {
        return byId.values();
    }

    public void clear() {
        byId.clear();
    }
}
