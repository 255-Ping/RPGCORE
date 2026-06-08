package com.github._255_ping.rpg.core.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /settings} — opens the player Settings GUI. */
public final class SettingsCommand implements CommandExecutor {

    private final SettingsGui gui;

    public SettingsCommand(SettingsGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        if (!player.hasPermission("rpg.settings.view")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        gui.open(player);
        return true;
    }
}
