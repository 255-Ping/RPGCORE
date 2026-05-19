package com.github._255_ping.rpg.api.status;

import java.util.Collection;
import java.util.Optional;

public interface StatusEffectRegistry {
    void register(StatusEffect effect);
    Optional<StatusEffect> get(String id);
    Collection<StatusEffect> all();
}
