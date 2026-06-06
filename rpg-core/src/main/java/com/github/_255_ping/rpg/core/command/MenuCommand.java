package com.github._255_ping.rpg.core.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** /menu — opens the main menu GUI for the sender. */
public final class MenuCommand implements CommandExecutor {

    private final MainMenuGui gui;

    public MenuCommand(MainMenuGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command is player-only.", NamedTextColor.RED));
            return true;
        }
        gui.open(player);
        return true;
    }
}
