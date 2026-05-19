package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import com.github._255_ping.rpg.core.RpgCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RpgCommand implements CommandExecutor, TabCompleter {

    private final RpgCorePlugin plugin;

    public RpgCommand(RpgCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.messages().component("command.usage",
                    Map.of("usage", "/rpg <version|reload|item|mob>")));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "version" -> handleVersion(sender);
            case "reload", "reloadall" -> handleReload(sender);
            case "item" -> handleItem(sender, args);
            case "mob" -> handleMob(sender, args);
            default -> sender.sendMessage(plugin.messages().component("command.unknown",
                    Map.of("sub", args[0])));
        }
        return true;
    }

    private void handleVersion(CommandSender sender) {
        if (!sender.hasPermission("rpg.core.version")) {
            sender.sendMessage(plugin.messages().component("command.no-permission"));
            return;
        }
        sender.sendMessage(plugin.messages().component("version.header"));
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            if (p.getName().startsWith("rpg-")) {
                sender.sendMessage(plugin.messages().component("version.entry",
                        Map.of("module", p.getName(),
                                "version", p.getPluginMeta().getVersion())));
            }
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("rpg.core.reload-all")) {
            sender.sendMessage(plugin.messages().component("command.no-permission"));
            return;
        }
        sender.sendMessage(plugin.messages().component("reload.starting"));
        plugin.reloadAll();
        sender.sendMessage(plugin.messages().component("reload.success"));
    }

    private void handleItem(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("give")) {
            sender.sendMessage(plugin.messages().component("command.usage",
                    Map.of("usage", "/rpg item give <id> [player] [amount]")));
            return;
        }
        if (!sender.hasPermission("rpg.core.item.give")) {
            sender.sendMessage(plugin.messages().component("command.no-permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.messages().component("command.usage",
                    Map.of("usage", "/rpg item give <id> [player] [amount]")));
            return;
        }
        String id = args[2];
        Optional<RpgItem> item = RpgServices.items().get(id);
        if (item.isEmpty()) {
            sender.sendMessage(plugin.messages().component("item.not-found", Map.of("id", id)));
            return;
        }
        Player target;
        if (args.length >= 4) {
            target = Bukkit.getPlayerExact(args[3]);
            if (target == null) {
                sender.sendMessage(plugin.messages().component("player.not-found",
                        Map.of("name", args[3])));
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(plugin.messages().component("command.player-only"));
            return;
        }
        int amount = 1;
        if (args.length >= 5) {
            try { amount = Math.max(1, Integer.parseInt(args[4])); }
            catch (NumberFormatException ex) { amount = 1; }
        }
        var stack = item.get().toItemStack();
        stack.setAmount(amount);
        target.getInventory().addItem(stack);
        sender.sendMessage(plugin.messages().component("item.given",
                Map.of("id", id, "amount", amount, "player", target.getName())));
    }

    private void handleMob(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("spawn")) {
            sender.sendMessage(plugin.messages().component("command.usage",
                    Map.of("usage", "/rpg mob spawn <id> [count]")));
            return;
        }
        if (!sender.hasPermission("rpg.core.mob.spawn")) {
            sender.sendMessage(plugin.messages().component("command.no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.messages().component("command.player-only"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.messages().component("command.usage",
                    Map.of("usage", "/rpg mob spawn <id> [count]")));
            return;
        }
        String id = args[2];
        Optional<RpgMob> mob = RpgServices.mobs().get(id);
        if (mob.isEmpty()) {
            sender.sendMessage(plugin.messages().component("mob.not-found", Map.of("id", id)));
            return;
        }
        int count = 1;
        if (args.length >= 4) {
            try { count = Math.max(1, Math.min(50, Integer.parseInt(args[3]))); }
            catch (NumberFormatException ex) { count = 1; }
        }
        for (int i = 0; i < count; i++) {
            LivingEntity spawned = mob.get().spawn(player.getLocation());
            if (spawned == null) {
                sender.sendMessage(plugin.messages().component("mob.spawn-failed", Map.of("id", id)));
                return;
            }
        }
        sender.sendMessage(plugin.messages().component("mob.spawned",
                Map.of("id", id, "count", count)));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filtered(args[0], List.of("version", "reload", "item", "mob"));
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("item")) return filtered(args[1], List.of("give"));
            if (args[0].equalsIgnoreCase("mob")) return filtered(args[1], List.of("spawn"));
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("give")) {
                return filtered(args[2], RpgServices.items().all().stream().map(RpgItem::id).toList());
            }
            if (args[0].equalsIgnoreCase("mob") && args[1].equalsIgnoreCase("spawn")) {
                return filtered(args[2], RpgServices.mobs().all().stream().map(RpgMob::id).toList());
            }
        }
        return List.of();
    }

    private static List<String> filtered(String prefix, List<String> candidates) {
        List<String> out = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String c : candidates) {
            if (c.toLowerCase().startsWith(lower)) out.add(c);
        }
        return out;
    }
}
