package com.github._255_ping.rpg.enchanting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;

public final class EnchantingCommand implements CommandExecutor, TabCompleter {

    private final RpgEnchantingPlugin plugin;

    public EnchantingCommand(RpgEnchantingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("/enchanting <reload|list>").color(NamedTextColor.YELLOW));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (!sender.hasPermission("rpg.enchanting.admin.reload")) {
                    sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
                    return true;
                }
                plugin.reloadAll();
                sender.sendMessage(Component.text("rpg-enchanting reloaded.").color(NamedTextColor.GREEN));
            }
            case "list" -> {
                if (!sender.hasPermission("rpg.enchanting.admin.list")) {
                    sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
                    return true;
                }
                sender.sendMessage(Component.text("Enchants: " + plugin.registry().allEnchants().size())
                        .color(NamedTextColor.AQUA));
                sender.sendMessage(Component.text("Reforges: " + plugin.registry().allReforges().size())
                        .color(NamedTextColor.AQUA));
                sender.sendMessage(Component.text("Upgrades: " + plugin.registry().allUpgrades().size())
                        .color(NamedTextColor.AQUA));
            }
            default -> sender.sendMessage(Component.text("Unknown: " + args[0]).color(NamedTextColor.YELLOW));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("reload", "list");
        return List.of();
    }
}
