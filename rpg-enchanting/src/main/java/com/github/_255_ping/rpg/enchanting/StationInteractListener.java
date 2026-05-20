package com.github._255_ping.rpg.enchanting;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.blocks.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/** Opens the appropriate station GUI when the player right-clicks an enchanting/anvil block. */
public final class StationInteractListener implements Listener {

    private final JavaPlugin plugin;
    private final StationGui gui;

    public StationInteractListener(JavaPlugin plugin, StationGui gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        Optional<Block> blockOpt = RpgServices.blocks().at(e.getClickedBlock().getLocation());
        if (blockOpt.isEmpty()) return;
        Block block = blockOpt.get();

        String enchId = plugin.getConfig().getString("stations.enchanting-block", "rpg_enchanting_table");
        String anvilId = plugin.getConfig().getString("stations.anvil-block", "rpg_custom_anvil");

        if (block.id().equals(enchId)) {
            if (!plugin.getConfig().getBoolean("features.enchanting", true)) return;
            e.setCancelled(true);
            gui.open(e.getPlayer(), StationGui.Mode.ENCHANTING);
        } else if (block.id().equals(anvilId)) {
            if (!plugin.getConfig().getBoolean("features.reforges", true)
                    && !plugin.getConfig().getBoolean("features.upgrades", true)) return;
            e.setCancelled(true);
            gui.open(e.getPlayer(), StationGui.Mode.ANVIL);
        }
    }
}
