package com.github._255_ping.rpg.trade;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class RpgTradePlugin extends JavaPlugin {

    private TradeManager manager;
    private Messages messages;
    private TradeGui gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager  = new TradeManager();
        messages = new Messages(this);
        gui      = new TradeGui(this, manager, messages);

        TradeCommand tradeCmd = new TradeCommand(this, manager, gui, messages);
        var cmd = Objects.requireNonNull(getCommand("trade"), "command 'trade' missing in plugin.yml");
        cmd.setExecutor(tradeCmd);
        cmd.setTabCompleter(tradeCmd);

        getServer().getPluginManager().registerEvents(gui, this);

        // Periodic invite expiry check (every 5 seconds)
        getServer().getScheduler().runTaskTimer(this, this::cleanExpiredInvites, 100L, 100L);

        getLogger().info("rpg-trade v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        // Cancel all active trades and return items to players
        // Collect all unique sessions (both players map to same session)
        var seen = new java.util.HashSet<TradeSession>();
        for (Player p : getServer().getOnlinePlayers()) {
            TradeSession session = manager.getSession(p.getUniqueId());
            if (session != null && session.state != TradeSession.State.DONE && seen.add(session)) {
                Player other = getServer().getPlayer(session.otherUuid(p.getUniqueId()));
                gui.cancelAndReturn(session, p, other);
            }
        }
        getLogger().info("rpg-trade disabled.");
    }

    private void cleanExpiredInvites() {
        int expiry = getConfig().getInt("trade.invite-expiry-seconds", 30);
        Map<UUID, UUID> expired = manager.drainExpiredInvites(expiry);
        for (Map.Entry<UUID, UUID> entry : expired.entrySet()) {
            UUID inviteeUuid = entry.getKey();
            UUID inviterUuid = entry.getValue();
            Player invitee = Bukkit.getPlayer(inviteeUuid);
            Player inviter = Bukkit.getPlayer(inviterUuid);
            if (invitee != null)
                invitee.sendMessage(messages.get("invite-expired", Map.of("player",
                        inviter != null ? inviter.getName() : "?")));
            if (inviter != null)
                inviter.sendMessage(messages.get("invite-expired",
                        Map.of("player", invitee != null ? invitee.getName() : "?")));
        }
    }
}
