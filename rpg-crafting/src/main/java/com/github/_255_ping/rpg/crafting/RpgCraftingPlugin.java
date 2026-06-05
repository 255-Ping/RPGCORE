package com.github._255_ping.rpg.crafting;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

/**
 * rpg-crafting — custom crafting-table recipe loader.
 *
 * <p>Drop this jar alongside rpg-core to enable shaped / shapeless custom recipes
 * defined in {@code plugins/rpg-crafting/recipes/*.yml}.
 */
public final class RpgCraftingPlugin extends JavaPlugin implements CommandExecutor {

    private RecipeLoader loader;

    @Override
    public void onEnable() {
        File recipesDir = new File(getDataFolder(), "recipes");
        if (!recipesDir.isDirectory()) recipesDir.mkdirs();
        ensureExample("recipes/example.yml");

        loader = new RecipeLoader(this, recipesDir);
        loader.reload();

        Objects.requireNonNull(getCommand("crafting"), "command 'crafting' missing from plugin.yml")
                .setExecutor(this);

        getLogger().info("rpg-crafting v" + getPluginMeta().getVersion()
                + " enabled — " + loader.size() + " recipes loaded.");
    }

    @Override
    public void onDisable() {
        if (loader != null) loader.unregisterAll();
        getLogger().info("rpg-crafting disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7Usage: §e/crafting <reload|list>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("rpg.crafting.admin.reload")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                loader.reload();
                sender.sendMessage("§arpg-crafting reloaded — " + loader.size() + " recipes.");
            }
            case "list" -> {
                if (!sender.hasPermission("rpg.crafting.admin.list")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                sender.sendMessage("§7Custom crafting recipes loaded: §e" + loader.size());
            }
            default -> sender.sendMessage("§cUnknown sub-command: " + args[0]);
        }
        return true;
    }

    private void ensureExample(String resourcePath) {
        File target = new File(getDataFolder(), resourcePath);
        if (target.exists()) return;
        try { saveResource(resourcePath, false); } catch (IllegalArgumentException ignored) {}
    }
}
