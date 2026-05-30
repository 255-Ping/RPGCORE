package com.github._255_ping.rpg.admin.command;

import com.github._255_ping.rpg.admin.RpgAdminPlugin;
import com.github._255_ping.rpg.api.RpgServices;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class HealFeedCommand implements CommandExecutor, TabCompleter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private final RpgAdminPlugin plugin;

    public HealFeedCommand(RpgAdminPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String lbl = label.toLowerCase(Locale.ROOT);
        String perm = plugin.getConfig().getString("commands." + lbl + ".permission",
                "rpg.admin." + lbl);
        String othersPerm = plugin.getConfig().getString("commands." + lbl + ".others-permission",
                "rpg.admin." + lbl + ".others");

        if (!sender.hasPermission(perm)) {
            sender.sendMessage(msg("&cNo permission.")); return true;
        }

        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission(othersPerm)) {
                sender.sendMessage(msg("&cNo permission to " + lbl + " others.")); return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) { sender.sendMessage(msg("&cPlayer not found: &7" + args[0])); return true; }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(msg("&cConsole must specify a player.")); return true;
        }

        if (lbl.equals("heal")) {
            doHeal(target);
            target.sendMessage(msg("&aYou have been fully healed."));
            if (!target.equals(sender)) sender.sendMessage(msg("&aHealed &e" + target.getName() + "&a."));
        } else {
            doFeed(target);
            target.sendMessage(msg("&aYour hunger has been restored."));
            if (!target.equals(sender)) sender.sendMessage(msg("&aFed &e" + target.getName() + "&a."));
        }
        return true;
    }

    private static void doHeal(Player target) {
        try {
            double max = RpgServices.health().maxHp(target);
            RpgServices.health().setCurrentHp(target, max);
        } catch (Exception ignored) {
            // rpg-core not available or not yet initialised — fall back to vanilla
            AttributeInstance attr = target.getAttribute(Attribute.MAX_HEALTH);
            double max = attr != null ? attr.getValue() : 20.0;
            target.setHealth(max);
        }
    }

    private static void doFeed(Player target) {
        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.setExhaustion(0f);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private static Component msg(String s) { return LEGACY.deserialize(s); }
}
