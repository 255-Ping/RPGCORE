package com.github._255_ping.rpg.core.suppression;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

/**
 * Suppresses Minecraft item durability loss when {@code vanilla-suppression.durability: true}.
 * Items will never break or lose durability regardless of use.
 */
public final class DurabilityListener implements Listener {

    private final FileConfiguration config;

    public DurabilityListener(FileConfiguration config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (config.getBoolean("vanilla-suppression.durability", false)) {
            event.setCancelled(true);
        }
    }
}
