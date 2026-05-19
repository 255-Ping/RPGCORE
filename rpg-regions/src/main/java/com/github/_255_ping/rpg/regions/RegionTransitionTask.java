package com.github._255_ping.rpg.regions;

import com.github._255_ping.rpg.api.regions.Region;
import com.github._255_ping.rpg.api.regions.RegionEnterEvent;
import com.github._255_ping.rpg.api.regions.RegionLeaveEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Polls each online player's location periodically and fires
 * {@link RegionEnterEvent} / {@link RegionLeaveEvent} when the highest-priority
 * region at their location changes. Polling is cheaper than listening to
 * PlayerMoveEvent for this; the move-poll-ticks knob in config tunes the cadence.
 */
public final class RegionTransitionTask implements Runnable {

    private final JavaPlugin plugin;
    private final CoreRegionService regions;
    private final Map<UUID, String> lastRegion = new HashMap<>();

    public RegionTransitionTask(JavaPlugin plugin, CoreRegionService regions) {
        this.plugin = plugin;
        this.regions = regions;
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Region current = regions.regionAt(p.getLocation()).orElse(null);
            String currentId = current == null ? null : current.id();
            String prevId = lastRegion.get(p.getUniqueId());
            if (java.util.Objects.equals(prevId, currentId)) continue;

            if (prevId != null) {
                regions.get(prevId).ifPresent(r -> Bukkit.getPluginManager().callEvent(new RegionLeaveEvent(p, r)));
            }
            if (currentId != null) {
                Bukkit.getPluginManager().callEvent(new RegionEnterEvent(p, current));
            }
            if (currentId == null) lastRegion.remove(p.getUniqueId());
            else lastRegion.put(p.getUniqueId(), currentId);
        }
    }
}
