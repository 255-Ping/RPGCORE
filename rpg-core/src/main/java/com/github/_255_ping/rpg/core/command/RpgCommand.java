package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.core.RpgCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                    Map.of("usage", "/rpg <version|reload>")));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "version" -> handleVersion(sender);
            case "reload", "reloadall" -> handleReload(sender);
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

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String s : List.of("version", "reload")) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
            return out;
        }
        return List.of();
    }
}
