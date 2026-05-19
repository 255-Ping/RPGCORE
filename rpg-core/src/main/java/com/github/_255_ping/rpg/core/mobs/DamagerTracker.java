package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks damage dealt to each victim by each player attacker, for loot attribution
 * on death. Cleared per-victim by {@link MobLootListener} after the loot roll.
 */
public final class DamagerTracker implements Listener {

    private final ConcurrentHashMap<UUID, LinkedHashMap<UUID, Double>> log = new ConcurrentHashMap<>();
    // Cached Player references — UUIDs in the map above can outlive Player references on death.
    private final ConcurrentHashMap<UUID, Player> playerRefs = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPostDamage(PostDamageEvent event) {
        if (!(event.context().attacker() instanceof Player attacker)) return;
        LivingEntity victim = event.context().victim();
        if (victim == null || victim instanceof Player) return;

        playerRefs.put(attacker.getUniqueId(), attacker);
        log.compute(victim.getUniqueId(), (k, existing) -> {
            LinkedHashMap<UUID, Double> map = existing == null ? new LinkedHashMap<>() : existing;
            // LinkedHashMap iteration order = insertion order; re-insert to bump "last hit".
            Double prev = map.remove(attacker.getUniqueId());
            map.put(attacker.getUniqueId(), (prev == null ? 0 : prev) + event.dealtDamage());
            return map;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        // The listener that runs the loot table will consume the entry; just keep the
        // entry around for one tick to make sure both the PostDamageEvent (from the
        // killing blow) and EntityDeathEvent have already fired before we clear.
        // For simplicity here, we don't auto-clear — MobLootListener clears after use.
    }

    /** Returns the per-attacker damage log for a victim and clears it. */
    public Map<Player, Double> takeFor(UUID victimId) {
        LinkedHashMap<UUID, Double> raw = log.remove(victimId);
        if (raw == null || raw.isEmpty()) return Collections.emptyMap();
        // Preserve insertion order: the *last* entry is the most recent damager.
        LinkedHashMap<Player, Double> out = new LinkedHashMap<>();
        for (Map.Entry<UUID, Double> e : raw.entrySet()) {
            Player p = playerRefs.get(e.getKey());
            if (p != null && p.isOnline()) {
                out.put(p, e.getValue());
            }
        }
        return out;
    }

    /** Returns the most recent damager (LAST_HIT) or null. */
    public Player lastHitter(UUID victimId) {
        LinkedHashMap<UUID, Double> raw = log.get(victimId);
        if (raw == null || raw.isEmpty()) return null;
        UUID lastId = null;
        for (UUID id : raw.keySet()) lastId = id;  // last insertion
        return lastId == null ? null : playerRefs.get(lastId);
    }
}
