package com.github._255_ping.rpg.api.mobs;

import com.github._255_ping.rpg.api.stats.Stat;
import com.github._255_ping.rpg.api.stats.StatHolder;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;

/**
 * Stores per-entity stat holders for custom mobs.
 *
 * <p>When a custom mob spawns, its definition's stats are registered here so the damage
 * pipeline can read them via {@link #forMob(LivingEntity)}. Entries are removed
 * automatically when the entity dies or is removed from the world.
 *
 * <p>Access via {@code RpgServices.mobStats()}.
 */
public interface MobStatService {

    /**
     * Returns the stat holder for a living entity.
     * If the entity has no registered holder (e.g. a vanilla mob), returns an immutable
     * all-zeroes holder — never null, safe to call unconditionally.
     */
    StatHolder forMob(LivingEntity entity);

    /**
     * Register a stat holder for a freshly-spawned mob.
     * Called from {@code CoreRpgMob.spawn()} immediately after the entity is created.
     *
     * @param entity the spawned entity
     * @param stats  the mob definition's stat map (damage, defense, any extras)
     */
    void register(LivingEntity entity, Map<Stat, Double> stats);

    /**
     * Remove the stat holder for an entity that has died or been removed from the world.
     * Safe to call with a UUID that was never registered.
     */
    void unregister(UUID uuid);
}
