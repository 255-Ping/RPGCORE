package com.github._255_ping.rpg.npcs;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Belt-and-suspenders protection on top of setInvulnerable — cancels all damage events for NPC entities. */
public final class NpcProtectionListener implements Listener {

    private final NpcManager manager;
    private final JavaPlugin plugin;

    public NpcProtectionListener(NpcManager manager, JavaPlugin plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent e) {
        if (manager.fromEntity(e.getEntity()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        if (manager.fromEntity(e.getEntity()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTarget(EntityTargetEvent e) {
        if (e.getTarget() != null && manager.fromEntity(e.getTarget()).isPresent()) {
            e.setCancelled(true);
        }
    }

    /** Safety net: if an NPC somehow dies anyway, respawn it after 1 tick. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        manager.fromEntity(e.getEntity()).ifPresent(def -> {
            e.getDrops().clear();
            e.setDroppedExp(0);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> manager.rebuild(def), 1L);
        });
    }
}
