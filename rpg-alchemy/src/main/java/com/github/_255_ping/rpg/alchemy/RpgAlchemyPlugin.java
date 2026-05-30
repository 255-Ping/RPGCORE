package com.github._255_ping.rpg.alchemy;

import com.github._255_ping.rpg.api.RpgServices;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class RpgAlchemyPlugin extends JavaPlugin {

    private AlchemyRegistry registry;
    private PotionItemFactory potionItems;
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureExample("potions/example.yml");
        ensureExample("recipes/example.yml");
        messages = new Messages(this);

        File potionsDir = new File(getDataFolder(), "potions");
        File recipesDir = new File(getDataFolder(), "recipes");
        int defaultBrew = getConfig().getInt("default-brew-ticks", 200);
        registry = new AlchemyRegistry(potionsDir, recipesDir, defaultBrew, getLogger());
        registry.reload();

        potionItems = new PotionItemFactory(new NamespacedKey(this, "rpg_potion_id"));

        BrewingGui gui = new BrewingGui(this, registry, potionItems);
        getServer().getPluginManager().registerEvents(gui, this);
        RpgServices.stations().register("brewing", (player, block) -> {
            if (getConfig().getBoolean("features.brewing", true)) gui.open(player);
        });
        getServer().getPluginManager().registerEvents(new PotionDrinkListener(this, registry, potionItems), this);

        Objects.requireNonNull(getCommand("alchemy"), "command 'alchemy' missing").setExecutor(new AlchemyCommand(this));

        StationBlockInstaller.installInto(this);

        getLogger().info("rpg-alchemy v" + getPluginMeta().getVersion()
                + " enabled with " + registry.allPotions().size() + " potions, "
                + registry.allRecipes().size() + " recipes.");
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-alchemy disabled.");
    }

    public void reloadAll() {
        reloadConfig();
        registry.reload();
        messages = new Messages(this);
    }

    public AlchemyRegistry registry() { return registry; }
    public PotionItemFactory potionItems() { return potionItems; }
    public Messages messages() { return messages; }

    private void ensureExample(String resourcePath) {
        File target = new File(getDataFolder(), resourcePath);
        if (target.exists()) return;
        try { saveResource(resourcePath, false); } catch (IllegalArgumentException ignored) {}
    }
}
