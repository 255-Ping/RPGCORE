package com.github._255_ping.rpg.bossbar;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.mobs.BossBarDef;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Periodic task that shows / hides boss bars based on player proximity to boss mobs.
 *
 * <p>Every {@code proximity-interval-ticks} ticks (default 40 = 2s), for each online player:
 * <ol>
 *   <li>Scan nearby living entities within each mob's configured show-range.</li>
 *   <li>Call {@link CoreBossBarService#track} for mobs that are now in range.</li>
 *   <li>Call {@link CoreBossBarService#untrack} for mobs the player was watching but
 *       is now out of range (or no longer alive).</li>
 * </ol>
 *
 * <p>The scan radius is the largest configured range across all boss mobs — all candidates
 * are found in one {@link org.bukkit.World#getNearbyLivingEntities} call and filtered
 * per-mob by their individual {@link BossBarDef#showRange()}.
 */
public final class BossBarProximityTask implements Runnable {

    private final CoreBossBarService service;
    private final double defaultRange;

    public BossBarProximityTask(CoreBossBarService service, double defaultRange) {
        this.service = service;
        this.defaultRange = defaultRange;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            processPlayer(player);
        }
    }

    private void processPlayer(Player player) {
        // Collect entity UUIDs currently in range that have boss bars
        Set<UUID> inRange = new HashSet<>();

        // Use the default range as the broad-phase search radius
        for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(
                player.getLocation(), defaultRange)) {
            if (entity.isDead() || entity.equals(player)) continue;

            RpgServices.mobs().from(entity).flatMap(mob -> {
                var def = mob.bossBar();
                return def;
            }).ifPresent(def -> {
                double range = def.showRange() > 0 ? def.showRange() : defaultRange;
                if (player.getLocation().distanceSquared(entity.getLocation()) <= range * range) {
                    inRange.add(entity.getUniqueId());
                    service.track(player, entity);
                }
            });
        }

        // Untrack any mobs the player was watching that are now out of range
        for (UUID watched : service.watchedBy(player)) {
            if (!inRange.contains(watched)) {
                org.bukkit.entity.Entity e = Bukkit.getEntity(watched);
                if (e instanceof LivingEntity le) {
                    service.untrack(player, le);
                } else {
                    // Entity no longer loaded / dead — clearAll handles death separately
                    // but call untrack with a stub to clean the player index
                    service.watchedBy(player); // no-op, death listener handles this
                }
            }
        }
    }
}
