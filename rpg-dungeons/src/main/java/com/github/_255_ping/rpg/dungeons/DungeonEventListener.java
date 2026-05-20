package com.github._255_ping.rpg.dungeons;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class DungeonEventListener implements Listener {

    private final DungeonManager manager;

    public DungeonEventListener(DungeonManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        manager.handleDeath(e.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent e) {
        manager.instanceContainingMob(e.getEntity().getUniqueId())
                .ifPresent(inst -> manager.mobKilled(inst, e.getEntity().getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;
        manager.onPlayerMoveCheck(e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        manager.instanceOf(e.getPlayer()).ifPresent(inst -> {
            // Already evicted by handleDeath in solo + wipe paths; if still in the instance,
            // they remain spectator-tethered. The manager controls teleport.
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        manager.onQuit(e.getPlayer());
    }
}
