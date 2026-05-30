package com.github._255_ping.rpg.enchanting;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.blocks.Block;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * Opens the appropriate station GUI when a player right-clicks an enchanting or anvil block.
 *
 * <p>Triggers on either:
 * <ul>
 *   <li>A custom block whose {@code StationType} is {@code enchanting} or {@code anvil}.</li>
 *   <li>A custom block whose id matches the legacy config keys (backwards compat).</li>
 *   <li>Vanilla enchanting table / anvil, when {@code intercept-vanilla-*} is true (default).</li>
 * </ul>
 */
public final class StationInteractListener implements Listener {

    private final JavaPlugin plugin;
    private final StationGui gui;

    public StationInteractListener(JavaPlugin plugin, StationGui gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;

        // Check custom RPG block — match by stationType (preferred) or legacy block-id config.
        Optional<Block> blockOpt = RpgServices.blocks().at(e.getClickedBlock().getLocation());
        if (blockOpt.isPresent()) {
            Block block = blockOpt.get();
            String st = block.stationType();
            String enchId = plugin.getConfig().getString("stations.enchanting-block", "rpg_enchanting_table");
            String anvilId = plugin.getConfig().getString("stations.anvil-block", "rpg_custom_anvil");

            boolean isEnchanting = "enchanting".equalsIgnoreCase(st) || block.id().equals(enchId);
            boolean isAnvil = "anvil".equalsIgnoreCase(st) || block.id().equals(anvilId);

            if (isEnchanting && plugin.getConfig().getBoolean("features.enchanting", true)) {
                e.setCancelled(true);
                gui.open(e.getPlayer(), StationGui.Mode.ENCHANTING);
                return;
            }
            if (isAnvil && (plugin.getConfig().getBoolean("features.reforges", true)
                    || plugin.getConfig().getBoolean("features.upgrades", true))) {
                e.setCancelled(true);
                gui.open(e.getPlayer(), StationGui.Mode.ANVIL);
                return;
            }
        }

        // Intercept vanilla enchanting table / anvil (enabled by default so admins don't need
        // a custom block placed — they can just use the vanilla ones).
        Material mat = e.getClickedBlock().getType();
        if (mat == Material.ENCHANTING_TABLE
                && plugin.getConfig().getBoolean("features.enchanting", true)
                && plugin.getConfig().getBoolean("intercept-vanilla-enchanting", true)) {
            e.setCancelled(true);
            gui.open(e.getPlayer(), StationGui.Mode.ENCHANTING);
        } else if ((mat == Material.ANVIL || mat == Material.CHIPPED_ANVIL || mat == Material.DAMAGED_ANVIL)
                && (plugin.getConfig().getBoolean("features.reforges", true)
                    || plugin.getConfig().getBoolean("features.upgrades", true))
                && plugin.getConfig().getBoolean("intercept-vanilla-anvil", true)) {
            e.setCancelled(true);
            gui.open(e.getPlayer(), StationGui.Mode.ANVIL);
        }
    }
}
