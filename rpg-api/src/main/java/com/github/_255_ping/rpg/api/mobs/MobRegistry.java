package com.github._255_ping.rpg.api.mobs;

import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.Optional;

public interface MobRegistry {
    void register(RpgMob mob);
    Optional<RpgMob> get(String id);
    Optional<RpgMob> from(LivingEntity entity);
    Collection<RpgMob> all();
}
