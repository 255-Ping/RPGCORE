package com.github._255_ping.rpg.cooking;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.blocks.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

public final class CookingStationInteractListener implements Listener {

    private final RpgCookingPlugin plugin;
    private final CookingGui gui;

    public CookingStationInteractListener(RpgCookingPlugin plugin, CookingGui gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        if (!plugin.getConfig().getBoolean("features.cooking", true)) return;
        Optional<Block> opt = RpgServices.blocks().at(e.getClickedBlock().getLocation());
        if (opt.isEmpty()) return;
        String expected = plugin.getConfig().getString("cooking-block", "rpg_cooking_station");
        if (!opt.get().id().equals(expected)) return;
        e.setCancelled(true);
        gui.open(e.getPlayer());
    }
}
