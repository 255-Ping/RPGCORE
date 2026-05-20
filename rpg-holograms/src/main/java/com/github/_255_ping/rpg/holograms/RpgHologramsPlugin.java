package com.github._255_ping.rpg.holograms;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class RpgHologramsPlugin extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new DamageIndicatorListener(this), this);
        Objects.requireNonNull(getCommand("holograms"), "command 'holograms' missing").setExecutor(this);
        getLogger().info("rpg-holograms v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rpg.holograms.admin.reload")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            reloadConfig();
            sender.sendMessage("§arpg-holograms reloaded.");
            return true;
        }
        sender.sendMessage("§7Usage: §e/holograms reload");
        return true;
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-holograms disabled.");
    }
}
