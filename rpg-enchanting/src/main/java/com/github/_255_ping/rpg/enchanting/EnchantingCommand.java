package com.github._255_ping.rpg.enchanting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
            sender.sendMessage(Component.text("/enchanting <reload|list|give>").color(NamedTextColor.YELLOW));
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
                sender.sendMessage(Component.text("Enchants: " + plugin.registry().allEnchants().size()).color(NamedTextColor.AQUA));
                sender.sendMessage(Component.text("Reforges: " + plugin.registry().allReforges().size()).color(NamedTextColor.AQUA));
                sender.sendMessage(Component.text("Upgrades: " + plugin.registry().allUpgrades().size()).color(NamedTextColor.AQUA));
            }
            case "give" -> {
                if (!sender.hasPermission("rpg.enchanting.admin.give")) {
                    sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
                    return true;
                }
                // /enchanting give <reforge|upgrade> <player> <id> [amount]
                if (args.length < 4) {
                    sender.sendMessage(Component.text(
                            "/enchanting give <reforge|upgrade> <player> <id> [amount]")
                            .color(NamedTextColor.YELLOW));
                    return true;
                }
                String type    = args[1].toLowerCase(Locale.ROOT);
                Player target  = plugin.getServer().getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found: " + args[2]).color(NamedTextColor.RED));
                    return true;
                }
                String defId = args[3].toLowerCase(Locale.ROOT);
                int amount   = args.length >= 5 ? parseIntSafe(args[4], 1) : 1;
                amount = Math.max(1, Math.min(64, amount));

                ItemStack item;
                switch (type) {
                    case "reforge" -> {
                        ReforgeDef def = plugin.registry().reforge(defId).orElse(null);
                        if (def == null) {
                            sender.sendMessage(Component.text("Unknown reforge: " + defId).color(NamedTextColor.RED));
                            return true;
                        }
                        item = plugin.modifier().createReforgeStone(def);
                    }
                    case "upgrade" -> {
                        UpgradeDef def = plugin.registry().upgrade(defId).orElse(null);
                        if (def == null) {
                            sender.sendMessage(Component.text("Unknown upgrade: " + defId).color(NamedTextColor.RED));
                            return true;
                        }
                        item = plugin.modifier().createUpgradeBook(def);
                    }
                    default -> {
                        sender.sendMessage(Component.text("Type must be 'reforge' or 'upgrade'.").color(NamedTextColor.RED));
                        return true;
                    }
                }
                item.setAmount(amount);
                target.getInventory().addItem(item);
                sender.sendMessage(Component.text(
                        "Gave " + amount + "x " + defId + " to " + target.getName() + ".")
                        .color(NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Unknown: " + args[0]).color(NamedTextColor.YELLOW));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1)
            return filter(List.of("reload", "list", "give"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("give"))
            return filter(List.of("reforge", "upgrade"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("give"))
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .toList();
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            String type = args[1].toLowerCase(Locale.ROOT);
            if (type.equals("reforge"))
                return plugin.registry().allReforges().stream()
                        .map(ReforgeDef::id)
                        .filter(id -> id.startsWith(args[3].toLowerCase(Locale.ROOT)))
                        .toList();
            if (type.equals("upgrade"))
                return plugin.registry().allUpgrades().stream()
                        .map(UpgradeDef::id)
                        .filter(id -> id.startsWith(args[3].toLowerCase(Locale.ROOT)))
                        .toList();
        }
        return List.of();
    }

    private static List<String> filter(List<String> opts, String prefix) {
        String lp = prefix.toLowerCase(Locale.ROOT);
        return opts.stream().filter(s -> s.startsWith(lp)).toList();
    }

    private static int parseIntSafe(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (NumberFormatException ex) { return fallback; }
    }
}
