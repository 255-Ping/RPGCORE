package com.github._255_ping.rpg.fishing;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class RpgFishingPlugin extends JavaPlugin {

    private FishingListener fishingListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        extractDefaultTable();

        fishingListener = new FishingListener(this);
        getServer().getPluginManager().registerEvents(fishingListener, this);

        Objects.requireNonNull(getCommand("fishing"), "command 'fishing' missing from plugin.yml").setExecutor(this);
        getLogger().info("rpg-fishing v" + getPluginMeta().getVersion() + " enabled.");
    }

    /** Copies the bundled default.yml into plugins/rpg-fishing/catch-tables/ if it doesn't exist yet. */
    private void extractDefaultTable() {
        File tablesDir = new File(getDataFolder(), "catch-tables");
        if (!tablesDir.isDirectory()) tablesDir.mkdirs();
        if (!new File(tablesDir, "default.yml").exists()) {
            saveResource("catch-tables/default.yml", false);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rpg.fishing.admin.reload")) {
                sender.sendMessage("§cNo permission."); return true;
            }
            reloadConfig();
            fishingListener.reload();
            sender.sendMessage("§arpg-fishing config and catch tables reloaded.");
            return true;
        }
        sender.sendMessage("§7Usage: §e/fishing reload");
        return true;
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-fishing disabled.");
    }
}
