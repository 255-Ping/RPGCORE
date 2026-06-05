package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.core.RpgCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StatsCommand implements CommandExecutor, TabCompleter {

    private final RpgCorePlugin plugin;
    private final StatsGui gui;

    public StatsCommand(RpgCorePlugin plugin, StatsGui gui) {
        this.plugin = plugin;
        this.gui    = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        Player viewer;

        if (args.length == 0) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(plugin.messages().component("command.player-only"));
                return true;
            }
            if (!sender.hasPermission("rpg.core.stats")) {
                sender.sendMessage(plugin.messages().component("command.no-permission"));
                return true;
            }
            target = p;
            viewer = p;
        } else {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(plugin.messages().component("command.player-only"));
                return true;
            }
            if (!sender.hasPermission("rpg.core.stats.other")) {
                sender.sendMessage(plugin.messages().component("command.no-permission"));
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(plugin.messages().component("player.not-found", Map.of("name", args[0])));
                return true;
            }
            viewer = p;
        }

        gui.open(viewer, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("rpg.core.stats.other")) {
            List<String> names = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) names.add(p.getName());
            }
            return names;
        }
        return List.of();
    }
}
