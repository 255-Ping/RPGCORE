package com.github._255_ping.rpg.economy;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerBalanceListener implements Listener {

    private final CoreEconomy economy;
    private final TxLog txLog;

    public PlayerBalanceListener(CoreEconomy economy, TxLog txLog) {
        this.economy = economy;
        this.txLog = txLog;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        economy.loadOne(e.getPlayer().getUniqueId());
        txLog.load(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        economy.saveOne(e.getPlayer().getUniqueId());
        txLog.save(e.getPlayer().getUniqueId());
        txLog.evict(e.getPlayer().getUniqueId());
    }
}
