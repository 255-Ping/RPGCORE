package com.github._255_ping.rpg.cooking;

import com.github._255_ping.rpg.api.RpgServices;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class RpgCookingPlugin extends JavaPlugin implements CommandExecutor {

    private CookingRegistry registry;
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureExample("recipes/example.yml");
        messages = new Messages(this);

        File recipesDir = new File(getDataFolder(), "recipes");
        int defaultTicks = getConfig().getInt("default-cook-ticks", 200);
        registry = new CookingRegistry(recipesDir, defaultTicks, getLogger());
        registry.reload();

        CookingGui gui = new CookingGui(this, registry);
        getServer().getPluginManager().registerEvents(gui, this);
        RpgServices.stations().register("cooking", (player, block) -> {
            if (getConfig().getBoolean("features.cooking", true)) gui.open(player);
        });

        Objects.requireNonNull(getCommand("cooking"), "command 'cooking' missing").setExecutor(this);

        StationBlockInstaller.installInto(this);

        getLogger().info("rpg-cooking v" + getPluginMeta().getVersion()
                + " enabled with " + registry.all().size() + " recipes.");
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-cooking disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7Usage: §e/cooking <reload|list>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("rpg.cooking.admin.reload")) {
                    sender.sendMessage("§cNo permission."); return true;
                }
                reloadConfig();
                registry.reload();
                messages = new Messages(this);
                sender.sendMessage("§arpg-cooking reloaded.");
            }
            case "list" -> {
                if (!sender.hasPermission("rpg.cooking.admin.list")) {
                    sender.sendMessage("§cNo permission."); return true;
                }
                sender.sendMessage("§7Recipes: §e" + registry.all().size());
            }
            default -> sender.sendMessage("§cUnknown: " + args[0]);
        }
        return true;
    }

    public CookingRegistry registry() { return registry; }
    public Messages messages() { return messages; }

    private void ensureExample(String resourcePath) {
        File target = new File(getDataFolder(), resourcePath);
        if (target.exists()) return;
        try { saveResource(resourcePath, false); } catch (IllegalArgumentException ignored) {}
    }
}
