package com.github._255_ping.rpg.trade;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the {@code /trade} command.
 *
 * <ul>
 *   <li>{@code /trade <player>} — send a trade invite</li>
 *   <li>{@code /trade accept}   — accept the pending invite</li>
 *   <li>{@code /trade deny}     — deny the pending invite</li>
 *   <li>{@code /trade cancel}   — cancel an active trade</li>
 * </ul>
 */
public final class TradeCommand implements CommandExecutor, TabCompleter {

    private final RpgTradePlugin plugin;
    private final TradeManager manager;
    private final TradeGui gui;
    private final Messages messages;

    public TradeCommand(RpgTradePlugin plugin, TradeManager manager, TradeGui gui, Messages messages) {
        this.plugin   = plugin;
        this.manager  = manager;
        this.gui      = gui;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!p.hasPermission("rpg.trade.use")) {
            p.sendMessage(messages.get("no-permission"));
            return true;
        }

        if (args.length == 0) {
            p.sendMessage(messages.get("player-not-found", Map.of("player", "<usage>")));
            p.sendMessage(messages.get("player-not-found", Map.of("player",
                    "/trade <player|accept|deny|cancel>")));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "accept" -> handleAccept(p);
            case "deny"   -> handleDeny(p);
            case "cancel" -> handleCancel(p);
            default       -> handleInvite(p, args[0]);
        }
        return true;
    }

    private void handleInvite(Player p, String targetName) {
        if (manager.inSession(p.getUniqueId())) {
            p.sendMessage(messages.get("already-in-trade"));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            p.sendMessage(messages.get("player-not-found", Map.of("player", targetName)));
            return;
        }
        if (target.equals(p)) {
            p.sendMessage(messages.get("cannot-trade-self"));
            return;
        }
        if (manager.inSession(target.getUniqueId())) {
            p.sendMessage(messages.get("target-in-trade", Map.of("player", target.getName())));
            return;
        }

        manager.sendInvite(p, target);
        int expiry = plugin.getConfig().getInt("trade.invite-expiry-seconds", 30);
        p.sendMessage(messages.get("invite-sent",
                Map.of("player", target.getName(), "seconds", expiry)));
        target.sendMessage(messages.get("invite-received",
                Map.of("player", p.getName())));
    }

    private void handleAccept(Player p) {
        if (!manager.hasPendingInvite(p.getUniqueId())) {
            p.sendMessage(messages.get("no-pending-invite"));
            return;
        }
        UUID inviterUuid = manager.getInviter(p.getUniqueId());
        manager.clearInvite(p.getUniqueId());

        if (manager.inSession(p.getUniqueId())) {
            p.sendMessage(messages.get("already-in-trade"));
            return;
        }
        Player inviter = Bukkit.getPlayer(inviterUuid);
        if (inviter == null || !inviter.isOnline()) {
            p.sendMessage(messages.get("player-not-found", Map.of("player", "inviter")));
            return;
        }
        if (manager.inSession(inviterUuid)) {
            p.sendMessage(messages.get("target-in-trade", Map.of("player", inviter.getName())));
            return;
        }

        TradeSession session = manager.createSession(inviter, p);
        gui.open(session);
    }

    private void handleDeny(Player p) {
        if (!manager.hasPendingInvite(p.getUniqueId())) {
            p.sendMessage(messages.get("no-pending-invite"));
            return;
        }
        UUID inviterUuid = manager.getInviter(p.getUniqueId());
        manager.clearInvite(p.getUniqueId());

        p.sendMessage(messages.get("invite-denied-self"));
        Player inviter = Bukkit.getPlayer(inviterUuid);
        if (inviter != null) {
            inviter.sendMessage(messages.get("invite-denied", Map.of("player", p.getName())));
        }
    }

    private void handleCancel(Player p) {
        TradeSession session = manager.getSession(p.getUniqueId());
        if (session == null) {
            p.sendMessage(messages.get("no-pending-invite")); // reuse "nothing to cancel" msg
            return;
        }
        Player other = Bukkit.getPlayer(session.otherUuid(p.getUniqueId()));
        gui.cancelAndReturn(session, p, other);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("accept", "deny", "cancel"));
            for (Player p : Bukkit.getOnlinePlayers()) options.add(p.getName());
            return filter(args[0], options);
        }
        return List.of();
    }

    private static List<String> filter(String prefix, List<String> options) {
        List<String> out = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String o : options) {
            if (o.toLowerCase().startsWith(lower)) out.add(o);
        }
        return out;
    }
}
