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

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class SpeedClearCommand implements CommandExecutor, TabCompleter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private final RpgAdminPlugin plugin;

    public SpeedClearCommand(RpgAdminPlugin plugin) {
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

        if (lbl.equals("speed")) return handleSpeed(sender, args);
        return handleClear(sender, args, perm);
    }

    // ── /speed ──────────────────────────────────────────────────────────────

    private boolean handleSpeed(CommandSender sender, String[] args) {
        // Usage: /speed <value> [player]
        //        /speed walk|fly <value> [player]
        if (args.length == 0) {
            sender.sendMessage(msg("&7Usage: &e/speed <value> [player] &7| &e/speed walk|fly <value> [player]"));
            return true;
        }

        float maxWalk = (float) plugin.getConfig().getDouble("commands.speed.max-walk-speed", 1.0);
        float maxFly  = (float) plugin.getConfig().getDouble("commands.speed.max-fly-speed", 1.0);

        String typeStr = null;
        float value;
        Player target = null;

        if (args[0].equalsIgnoreCase("walk") || args[0].equalsIgnoreCase("fly")) {
            typeStr = args[0].toLowerCase(Locale.ROOT);
            if (args.length < 2) { sender.sendMessage(msg("&7Usage: &e/speed walk|fly <value> [player]")); return true; }
            try { value = Float.parseFloat(args[1]); } catch (NumberFormatException e) {
                sender.sendMessage(msg("&cInvalid value: &7" + args[1])); return true;
            }
            if (args.length >= 3) { target = Bukkit.getPlayer(args[2]); }
        } else {
            try { value = Float.parseFloat(args[0]); } catch (NumberFormatException e) {
                sender.sendMessage(msg("&cInvalid value: &7" + args[0])); return true;
            }
            if (args.length >= 2) { target = Bukkit.getPlayer(args[1]); }
        }

        if (target == null) {
            if (!(sender instanceof Player p)) { sender.sendMessage(msg("&cConsole must specify a player.")); return true; }
            target = p;
        }
        if (target.getServer().getPlayer(target.getName()) == null) {
            sender.sendMessage(msg("&cPlayer not found.")); return true;
        }

        value = Math.max(0f, value);
        boolean fly = "fly".equals(typeStr) || (typeStr == null && target.isFlying());

        if (fly) {
            float clamped = Math.min(value, maxFly);
            target.setFlySpeed(clamped);
            target.sendMessage(msg("&7Fly speed set to &e" + clamped + "&7."));
            if (!target.equals(sender)) sender.sendMessage(msg("&7Set &e" + target.getName() + "&7's fly speed to &e" + clamped + "&7."));
        } else {
            float clamped = Math.min(value, maxWalk);
            target.setWalkSpeed(clamped);
            target.sendMessage(msg("&7Walk speed set to &e" + clamped + "&7."));
            if (!target.equals(sender)) sender.sendMessage(msg("&7Set &e" + target.getName() + "&7's walk speed to &e" + clamped + "&7."));
        }
        return true;
    }

    // ── /clear ───────────────────────────────────────────────────────────────

    private boolean handleClear(CommandSender sender, String[] args, String basePerm) {
        String othersPerm = plugin.getConfig().getString("commands.clear.others-permission",
                "rpg.admin.clear.others");

        Player target;
        if (args.length >= 1) {
            if (!sender.hasPermission(othersPerm)) {
                sender.sendMessage(msg("&cNo permission to clear others.")); return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) { sender.sendMessage(msg("&cPlayer not found: &7" + args[0])); return true; }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(msg("&cConsole must specify a player.")); return true;
        }

        target.getInventory().clear();
        target.sendMessage(msg("&aYour inventory has been cleared."));
        if (!target.equals(sender)) sender.sendMessage(msg("&aCleared &e" + target.getName() + "&a's inventory."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        String lbl = alias.toLowerCase(Locale.ROOT);
        if (lbl.equals("speed")) {
            if (args.length == 1) return List.of("walk", "fly");
            if (args.length == 3 && (args[0].equalsIgnoreCase("walk") || args[0].equalsIgnoreCase("fly"))) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 2 && !args[0].equalsIgnoreCase("walk") && !args[0].equalsIgnoreCase("fly")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (lbl.equals("clear") && args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private static Component msg(String s) { return LEGACY.deserialize(s); }
}
