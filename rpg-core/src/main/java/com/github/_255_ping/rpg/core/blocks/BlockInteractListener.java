package com.github._255_ping.rpg.core.blocks;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.blocks.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;

/**
 * Intercepts right-click on custom interactable blocks and dispatches to the
 * registered {@link com.github._255_ping.rpg.api.station.StationService} handler
 * for that block's {@code stationType}. Fires at HIGH priority so it runs before
 * most addon listeners but after LOW-priority suppressors.
 */
public final class BlockInteractListener implements Listener {

    private final CoreBlockRegistry registry;

    public BlockInteractListener(CoreBlockRegistry registry) {
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        // Only fire for the main hand to avoid double-dispatch on two-handed interactions.
        if (e.getHand() != null && e.getHand() != EquipmentSlot.HAND) return;

        Optional<Block> opt = registry.at(e.getClickedBlock().getLocation());
        if (opt.isEmpty()) return;
        Block block = opt.get();
        if (!block.interactable()) return;

        String stationType = block.stationType();
        if (stationType == null || stationType.isBlank()) return;

        try {
            if (RpgServices.stations().open(stationType, e.getPlayer(), block)) {
                e.setCancelled(true);
            }
        } catch (IllegalStateException ignored) {
            // StationService not yet registered — ignore during early-boot edge cases.
        }
    }
}
