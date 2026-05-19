package com.github._255_ping.rpg.regions;

import com.github._255_ping.rpg.api.regions.RegionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Enforces the region flag set:
 *   - no-break    → cancel BlockBreakEvent
 *   - no-place    → cancel BlockPlaceEvent
 *   - pvp (false) → cancel EntityDamageByEntityEvent player-on-player
 *
 * no-ability-use and no-mob-spawning are enforced at their source callsites in their
 * respective addons (ItemAbilityListener for abilities, SpawnerManager for spawners).
 */
public final class RegionEnforcer implements Listener {

    private final RegionService regions;

    public RegionEnforcer(RegionService regions) {
        this.regions = regions;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (regions.flag(event.getBlock().getLocation(), "no-break", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (regions.flag(event.getBlock().getLocation(), "no-place", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;
        if (!regions.flag(event.getEntity().getLocation(), "pvp", true)) {
            event.setCancelled(true);
        }
    }
}
