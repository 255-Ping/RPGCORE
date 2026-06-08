package com.github._255_ping.rpg.core.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /profile [player]} — opens the {@link ProfileGui} for the given player.
 *
 * <ul>
 *   <li>No argument → viewer's own profile (requires {@code rpg.profile.view})</li>
 *   <li>With argument → another player's profile (requires {@code rpg.profile.view.others})</li>
 * </ul>
 */
public final class ProfileCommand implements CommandExecutor, TabCompleter {

    private final ProfileGui gui;

    public ProfileCommand(ProfileGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        if (args.length == 0) {
            // Self-view
            if (!viewer.hasPermission("rpg.profile.view")) {
                viewer.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }
            gui.open(viewer, viewer);
            return true;
        }

        // Other-player view
        if (!viewer.hasPermission("rpg.profile.view.others")) {
            viewer.sendMessage("§cYou don't have permission to view other players' profiles.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            viewer.sendMessage("§cPlayer not found or not online: §e" + args[0]);
            return true;
        }

        gui.open(viewer, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length != 1) return List.of();
        if (!(sender instanceof Player viewer)) return List.of();
        if (!viewer.hasPermission("rpg.profile.view.others")) return List.of();

        String prefix = args[0].toLowerCase();
        List<String> completions = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().toLowerCase().startsWith(prefix)) {
                completions.add(online.getName());
            }
        }
        return completions;
    }
}
