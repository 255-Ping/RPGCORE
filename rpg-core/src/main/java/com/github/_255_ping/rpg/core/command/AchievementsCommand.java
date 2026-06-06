package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.core.achievement.AchievementGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code /achievements [player]} — opens the achievement GUI.
 *
 * <ul>
 *   <li>No args: opens your own GUI (must be a player).</li>
 *   <li>{@code [player]}: opens another player's GUI (requires {@code rpg.core.achievements.others}).</li>
 * </ul>
 */
public final class AchievementsCommand implements CommandExecutor, TabCompleter {

    private final AchievementGui gui;

    public AchievementsCommand(AchievementGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(Component.text("Must be a player.", NamedTextColor.RED));
            return true;
        }

        Player target = viewer;
        if (args.length >= 1) {
            if (!viewer.hasPermission("rpg.core.achievements.others")) {
                viewer.sendMessage(Component.text("You don't have permission to view other players' achievements.",
                        NamedTextColor.RED));
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                viewer.sendMessage(Component.text("Player '" + args[0] + "' is not online.", NamedTextColor.RED));
                return true;
            }
        }

        gui.open(viewer, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("rpg.core.achievements.others")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
