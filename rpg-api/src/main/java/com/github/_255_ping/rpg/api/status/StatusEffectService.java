package com.github._255_ping.rpg.api.status;

import org.bukkit.entity.LivingEntity;

import java.util.Collection;

public interface StatusEffectService {
    void apply(LivingEntity target, String effectId, int level, int durationTicks, String sourceId);
    void clear(LivingEntity target, String effectId);
    void clearAll(LivingEntity target);
    Collection<ActiveStatusEffect> active(LivingEntity target);
}
