package com.github._255_ping.rpg.alchemy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class AlchemyCommand implements CommandExecutor, TabCompleter {

    private final RpgAlchemyPlugin plugin;

    public AlchemyCommand(RpgAlchemyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("/alchemy <reload|list|give <potionId>>").color(NamedTextColor.YELLOW));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (!sender.hasPermission("rpg.alchemy.admin.reload")) {
                    sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
                    return true;
                }
                plugin.reloadAll();
                sender.sendMessage(Component.text("rpg-alchemy reloaded.").color(NamedTextColor.GREEN));
            }
            case "list" -> {
                if (!sender.hasPermission("rpg.alchemy.admin.list")) {
                    sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
                    return true;
                }
                sender.sendMessage(Component.text("Potions: " + plugin.registry().allPotions().size())
                        .color(NamedTextColor.AQUA));
                sender.sendMessage(Component.text("Recipes: " + plugin.registry().allRecipes().size())
                        .color(NamedTextColor.AQUA));
            }
            case "give" -> {
                if (!sender.hasPermission("rpg.alchemy.admin.reload")) {
                    sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2 || !(sender instanceof Player p)) {
                    sender.sendMessage(Component.text("Usage: /alchemy give <potionId>").color(NamedTextColor.YELLOW));
                    return true;
                }
                Optional<PotionDef> def = plugin.registry().potion(args[1].toLowerCase(Locale.ROOT));
                if (def.isEmpty()) {
                    sender.sendMessage(Component.text("Unknown potion: " + args[1]).color(NamedTextColor.RED));
                    return true;
                }
                p.getInventory().addItem(plugin.potionItems().build(def.get()));
                sender.sendMessage(Component.text("Gave " + def.get().id() + ".").color(NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Unknown: " + args[0]).color(NamedTextColor.YELLOW));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("reload", "list", "give");
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return plugin.registry().allPotions().stream().map(PotionDef::id).toList();
        }
        return List.of();
    }
}
