package com.github._255_ping.rpg.admin.command;

import com.github._255_ping.rpg.admin.RpgAdminPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class TeleportCommand implements CommandExecutor, TabCompleter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private final RpgAdminPlugin plugin;

    public TeleportCommand(RpgAdminPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String lbl = label.toLowerCase(Locale.ROOT);
        String perm = plugin.getConfig().getString("commands." + lbl + ".permission", "rpg.admin.tp");
        if (!sender.hasPermission(perm)) {
            sender.sendMessage(msg("&cNo permission.")); return true;
        }

        if (lbl.equals("tphere")) {
            return handleTphere(sender, args);
        }
        return handleTp(sender, args);
    }

    private boolean handleTp(CommandSender sender, String[] args) {
        Player self = sender instanceof Player p ? p : null;

        if (self == null && args.length < 2) {
            sender.sendMessage(msg("&cConsole usage: /tp <player1> <player2> or /tp <player> <x> <y> <z>"));
            return true;
        }

        if (args.length == 1) {
            // /tp <player> — teleport self to player
            if (self == null) { sender.sendMessage(msg("&cConsole must specify two targets.")); return true; }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) { sender.sendMessage(msg("&cPlayer not found: &7" + args[0])); return true; }
            self.teleport(target.getLocation());
            sender.sendMessage(msg("&aTeleported to &e" + target.getName() + "&a."));

        } else if (args.length == 3) {
            // /tp <x> <y> <z> — teleport self to coords
            if (self == null) { sender.sendMessage(msg("&cConsole must specify a player target.")); return true; }
            Location dest = parseCoords(self.getLocation(), args[0], args[1], args[2]);
            if (dest == null) { sender.sendMessage(msg("&cInvalid coordinates.")); return true; }
            self.teleport(dest);
            sender.sendMessage(msg(String.format("&aTeleported to &e%.1f, %.1f, %.1f&a.", dest.getX(), dest.getY(), dest.getZ())));

        } else if (args.length == 2) {
            // /tp <from> <to> — teleport from to to
            Player from = Bukkit.getPlayer(args[0]);
            Player to   = Bukkit.getPlayer(args[1]);
            if (from == null) { sender.sendMessage(msg("&cPlayer not found: &7" + args[0])); return true; }
            if (to == null)   { sender.sendMessage(msg("&cPlayer not found: &7" + args[1])); return true; }
            from.teleport(to.getLocation());
            sender.sendMessage(msg("&aTeleported &e" + from.getName() + " &ato &e" + to.getName() + "&a."));

        } else if (args.length == 4) {
            // /tp <player> <x> <y> <z>
            Player from = Bukkit.getPlayer(args[0]);
            if (from == null) { sender.sendMessage(msg("&cPlayer not found: &7" + args[0])); return true; }
            Location dest = parseCoords(from.getLocation(), args[1], args[2], args[3]);
            if (dest == null) { sender.sendMessage(msg("&cInvalid coordinates.")); return true; }
            from.teleport(dest);
            sender.sendMessage(msg(String.format("&aTeleported &e%s &ato &e%.1f, %.1f, %.1f&a.",
                    from.getName(), dest.getX(), dest.getY(), dest.getZ())));

        } else {
            sender.sendMessage(msg("&7Usage: &e/tp <player> &7| &e/tp <x> <y> <z> &7| &e/tp <from> <to>"));
        }
        return true;
    }

    private boolean handleTphere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player self)) {
            sender.sendMessage(msg("&cConsole cannot use /tphere.")); return true;
        }
        if (args.length < 1) {
            sender.sendMessage(msg("&7Usage: &e/tphere <player>")); return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { sender.sendMessage(msg("&cPlayer not found: &7" + args[0])); return true; }
        target.teleport(self.getLocation());
        target.sendMessage(msg("&aYou were teleported to &e" + self.getName() + "&a."));
        sender.sendMessage(msg("&aTeleported &e" + target.getName() + " &ato you."));
        return true;
    }

    /** Parses x/y/z strings, supporting ~ relative notation from a base location. */
    private static Location parseCoords(Location base, String xs, String ys, String zs) {
        try {
            double x = parseRelative(xs, base.getX());
            double y = parseRelative(ys, base.getY());
            double z = parseRelative(zs, base.getZ());
            return new Location(base.getWorld(), x, y, z, base.getYaw(), base.getPitch());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double parseRelative(String s, double base) {
        if (s.startsWith("~")) {
            double offset = s.length() == 1 ? 0 : Double.parseDouble(s.substring(1));
            return base + offset;
        }
        return Double.parseDouble(s);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && alias.equalsIgnoreCase("tp")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private static Component msg(String s) { return LEGACY.deserialize(s); }
}
