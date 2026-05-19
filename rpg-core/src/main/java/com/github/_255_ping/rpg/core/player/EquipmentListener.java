package com.github._255_ping.rpg.core.player;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.core.health.CoreHealthService;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Triggers stat recalculation when a player's equipped gear changes. After recalc,
 * resyncs HealthService max HP so equipping +max_health gear actually raises the cap.
 */
public final class EquipmentListener implements Listener {

    private final JavaPlugin plugin;
    private final CoreHealthService health;

    public EquipmentListener(JavaPlugin plugin, CoreHealthService health) {
        this.plugin = plugin;
        this.health = health;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent e) {
        recalc(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHotbarSwitch(PlayerItemHeldEvent e) {
        // Defer one tick so the held-slot index has updated.
        plugin.getServer().getScheduler().runTask(plugin, () -> recalc(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        plugin.getServer().getScheduler().runTask(plugin, () -> recalc(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) {
            recalc(p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> recalc(p));
    }

    private void recalc(Player player) {
        if (player == null || !player.isOnline()) return;
        RpgPlayer rp = RpgServices.player(player);
        rp.recalculateStats();
        // Sync max HP so equip/unequip of max_health gear updates the bar cap.
        double newMax = rp.get(BuiltinStat.MAX_HEALTH);
        if (newMax > 0) {
            health.setMaxHp(player, newMax);
        }
    }
}
