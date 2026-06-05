package com.github._255_ping.rpg.smelting;

import com.github._255_ping.rpg.api.RpgServices;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

/**
 * rpg-smelting — timed smelting station and vanilla furnace recipe loader.
 *
 * <p>Provides:
 * <ul>
 *   <li>A custom block station (StationType: smelting) that opens a progress-bar GUI.</li>
 *   <li>Optional vanilla {@code FurnaceRecipe} registration so the same items also
 *       smelt in regular furnaces (toggle via {@code features.vanilla-furnace-recipes}).</li>
 * </ul>
 */
public final class RpgSmeltingPlugin extends JavaPlugin implements CommandExecutor {

    private SmeltingRegistry   registry;
    private FurnaceRecipeLoader furnaceLoader;
    private Messages            messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) saveResource("messages.yml", false);
        ensureExample("recipes/example.yml");

        messages = new Messages(this);

        File recipesDir = new File(getDataFolder(), "recipes");
        if (!recipesDir.isDirectory()) recipesDir.mkdirs();
        int defaultTicks = getConfig().getInt("default-smelt-ticks", 200);
        registry = new SmeltingRegistry(recipesDir, defaultTicks, getLogger());
        registry.reload();

        furnaceLoader = new FurnaceRecipeLoader(this);
        registerFurnaceRecipes();

        SmeltingGui gui = new SmeltingGui(this, registry);
        getServer().getPluginManager().registerEvents(gui, this);
        RpgServices.stations().register("smelting", (player, block) -> {
            if (getConfig().getBoolean("features.smelting", true)) gui.open(player);
        });

        Objects.requireNonNull(getCommand("smelting"), "command 'smelting' missing from plugin.yml")
                .setExecutor(this);

        StationBlockInstaller.installInto(this);

        getLogger().info("rpg-smelting v" + getPluginMeta().getVersion()
                + " enabled — " + registry.all().size() + " recipes.");
    }

    @Override
    public void onDisable() {
        if (furnaceLoader != null) furnaceLoader.unregisterAll();
        getLogger().info("rpg-smelting disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7Usage: §e/smelting <reload|list>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("rpg.smelting.admin.reload")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                reloadConfig();
                if (furnaceLoader != null) furnaceLoader.unregisterAll();
                registry.reload();
                furnaceLoader = new FurnaceRecipeLoader(this);
                registerFurnaceRecipes();
                messages = new Messages(this);
                sender.sendMessage("§arpg-smelting reloaded — " + registry.all().size() + " recipes.");
            }
            case "list" -> {
                if (!sender.hasPermission("rpg.smelting.admin.list")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                sender.sendMessage("§7Smelting recipes loaded: §e" + registry.all().size());
            }
            default -> sender.sendMessage("§cUnknown sub-command: " + args[0]);
        }
        return true;
    }

    public SmeltingRegistry registry() { return registry; }
    public Messages         messages() { return messages; }

    private void registerFurnaceRecipes() {
        if (!getConfig().getBoolean("features.vanilla-furnace-recipes", true)) return;
        for (SmeltRecipeDef def : registry.all()) furnaceLoader.register(def);
    }

    private void ensureExample(String resourcePath) {
        File target = new File(getDataFolder(), resourcePath);
        if (target.exists()) return;
        try { saveResource(resourcePath, false); } catch (IllegalArgumentException ignored) {}
    }
}
