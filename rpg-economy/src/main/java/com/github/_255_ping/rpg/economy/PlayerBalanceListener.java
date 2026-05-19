package com.github._255_ping.rpg.economy;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerBalanceListener implements Listener {

    private final CoreEconomy economy;

    public PlayerBalanceListener(CoreEconomy economy) {
        this.economy = economy;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        economy.loadOne(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        economy.saveOne(e.getPlayer().getUniqueId());
    }
}
