package com.github._255_ping.rpg.regions;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.regions.RegionService;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Enforces the region flag set:
 *   - no-break         → cancel all BlockBreakEvents (including custom)
 *   - no-break-vanilla → cancel BlockBreakEvent only for NON-custom blocks (players in creative exempt)
 *   - no-place         → cancel BlockPlaceEvent
 *   - pvp (false)      → cancel EntityDamageByEntityEvent player-on-player
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
        Location loc = event.getBlock().getLocation();

        // no-break: cancel ALL block breaks in the region.
        if (regions.flag(loc, "no-break", false)) {
            event.setCancelled(true);
            return;
        }

        // no-break-vanilla: cancel breaks of non-custom blocks only. Creative players bypass.
        if (regions.flag(loc, "no-break-vanilla", false)) {
            if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
            try {
                boolean isCustom = RpgServices.blocks().at(loc).isPresent();
                if (!isCustom) event.setCancelled(true);
            } catch (IllegalStateException ignored) {
                event.setCancelled(true); // fail-safe if core not available
            }
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
