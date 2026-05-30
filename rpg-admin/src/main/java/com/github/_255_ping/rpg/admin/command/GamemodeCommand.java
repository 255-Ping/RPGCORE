package com.github._255_ping.rpg.admin.command;

import com.github._255_ping.rpg.admin.RpgAdminPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class GamemodeCommand implements CommandExecutor, TabCompleter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private final RpgAdminPlugin plugin;

    public GamemodeCommand(RpgAdminPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String key = label.toLowerCase(Locale.ROOT);
        String perm = plugin.getConfig().getString("commands." + key + ".permission", "rpg.admin.gamemode");
        if (!sender.hasPermission(perm)) {
            sender.sendMessage(msg("&cNo permission.")); return true;
        }

        GameMode mode = switch (key) {
            case "gmc"  -> GameMode.CREATIVE;
            case "gms"  -> GameMode.SURVIVAL;
            case "gma"  -> GameMode.ADVENTURE;
            case "gmsp" -> GameMode.SPECTATOR;
            default     -> null;
        };
        if (mode == null) { sender.sendMessage(msg("&cUnknown gamemode.")); return true; }

        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) { sender.sendMessage(msg("&cPlayer not found: &7" + args[0])); return true; }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(msg("&cConsole must specify a player.")); return true;
        }

        target.setGameMode(mode);
        String modeName = mode.name().toLowerCase(Locale.ROOT);
        target.sendMessage(msg("&aYour gamemode is now &e" + modeName + "&a."));
        if (!target.equals(sender)) {
            sender.sendMessage(msg("&aSet &e" + target.getName() + "&a's gamemode to &e" + modeName + "&a."));
        }
        return true;
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
