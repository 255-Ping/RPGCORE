package com.github._255_ping.rpg.alchemy;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.blocks.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public final class BrewingStationInteractListener implements Listener {

    private final JavaPlugin plugin;
    private final BrewingGui gui;

    public BrewingStationInteractListener(JavaPlugin plugin, BrewingGui gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        if (!plugin.getConfig().getBoolean("features.brewing", true)) return;
        Optional<Block> opt = RpgServices.blocks().at(e.getClickedBlock().getLocation());
        if (opt.isEmpty()) return;
        String expected = plugin.getConfig().getString("brewing-block", "rpg_brewing_stand");
        if (!opt.get().id().equals(expected)) return;
        e.setCancelled(true);
        gui.open(e.getPlayer());
    }
}
