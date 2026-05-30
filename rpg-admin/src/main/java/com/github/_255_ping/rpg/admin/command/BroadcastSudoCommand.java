package com.github._255_ping.rpg.admin.command;

import com.github._255_ping.rpg.admin.RpgAdminPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class BroadcastSudoCommand implements CommandExecutor, TabCompleter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private final RpgAdminPlugin plugin;

    public BroadcastSudoCommand(RpgAdminPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String lbl = label.toLowerCase(Locale.ROOT);
        String perm = plugin.getConfig().getString("commands." + lbl + ".permission",
                "rpg.admin." + lbl);
        if (!sender.hasPermission(perm)) {
            sender.sendMessage(msg("&cNo permission.")); return true;
        }

        if (lbl.equals("broadcast")) return handleBroadcast(sender, args);
        return handleSudo(sender, args);
    }

    // ── /broadcast ───────────────────────────────────────────────────────────

    private boolean handleBroadcast(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(msg("&7Usage: &e/broadcast <message>")); return true;
        }
        String message = String.join(" ", args);
        String format = plugin.getConfig().getString("commands.broadcast.format",
                "&c[&lANNOUNCEMENT&c] &f{message}");
        Component text = LEGACY.deserialize(format.replace("{message}", message));
        Bukkit.broadcast(text);
        return true;
    }

    // ── /sudo ────────────────────────────────────────────────────────────────

    private boolean handleSudo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(msg("&7Usage: &e/sudo <player> <command>")); return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { sender.sendMessage(msg("&cPlayer not found: &7" + args[0])); return true; }

        String forced = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        // Strip leading slash if present
        if (forced.startsWith("/")) forced = forced.substring(1);

        Bukkit.dispatchCommand(target, forced);
        sender.sendMessage(msg("&aForced &e" + target.getName() + " &ato run: &7/" + forced));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (alias.equalsIgnoreCase("sudo") && args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private static Component msg(String s) { return LEGACY.deserialize(s); }
}
