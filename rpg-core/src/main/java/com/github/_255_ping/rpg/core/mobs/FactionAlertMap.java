package com.github._255_ping.rpg.core.mobs;

import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks recent "hurt" events for RPG mobs so {@link AiGoalDef.DefendFaction} goals can
 * locate active attackers on the next AI tick.
 *
 * <p>Key = victim mob UUID; value = the entity that last dealt damage to it.
 * Entries are written by {@link MobAbilityEventListener} on every RPG-damage event
 * and read by {@link MobAiTask} during goal evaluation. Stale entries are pruned lazily
 * when the attacker entity is no longer valid, or explicitly on mob death.
 */
public final class FactionAlertMap {

    private final ConcurrentHashMap<UUID, LivingEntity> victimToAttacker = new ConcurrentHashMap<>();

    /** Record that {@code attacker} just damaged the mob with id {@code victimId}. */
    public void recordAttack(UUID victimId, LivingEntity attacker) {
        if (attacker != null) victimToAttacker.put(victimId, attacker);
    }

    /** Remove a mob's entry; called when the mob dies so the map stays bounded. */
    public void remove(UUID mobId) {
        victimToAttacker.remove(mobId);
    }

    /**
     * Returns a stable snapshot of current (victimId, attacker) pairs for iteration.
     * Invalid attackers are pruned from the live map during this call.
     */
    public List<Map.Entry<UUID, LivingEntity>> snapshot() {
        victimToAttacker.entrySet().removeIf(e -> !e.getValue().isValid());
        return new ArrayList<>(victimToAttacker.entrySet());
    }
}
