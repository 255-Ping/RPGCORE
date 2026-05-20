package com.github._255_ping.rpg.enchanting;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class RpgEnchantingPlugin extends JavaPlugin {

    private EnchantRegistry registry;
    private ItemModifier modifier;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureExample("enchants/example.yml");
        ensureExample("reforges/example.yml");
        ensureExample("upgrades/example.yml");

        File enchantsDir = new File(getDataFolder(), "enchants");
        File reforgesDir = new File(getDataFolder(), "reforges");
        File upgradesDir = new File(getDataFolder(), "upgrades");
        registry = new EnchantRegistry(enchantsDir, reforgesDir, upgradesDir, getLogger());
        registry.reload();

        modifier = new ItemModifier(
                new NamespacedKey(this, "rpg_enchants"),
                new NamespacedKey(this, "rpg_reforge"),
                new NamespacedKey(this, "rpg_upgrades"));

        StationGui gui = new StationGui(this, registry, modifier);
        getServer().getPluginManager().registerEvents(gui, this);
        getServer().getPluginManager().registerEvents(new StationInteractListener(this, gui), this);
        getServer().getPluginManager().registerEvents(new StatInjectionListener(registry, modifier), this);

        Objects.requireNonNull(getCommand("enchanting"), "command 'enchanting' missing from plugin.yml")
                .setExecutor(new EnchantingCommand(this));

        StationBlockInstaller.installInto(this);

        getLogger().info("rpg-enchanting v" + getPluginMeta().getVersion()
                + " enabled with " + registry.allEnchants().size() + " enchants, "
                + registry.allReforges().size() + " reforges, "
                + registry.allUpgrades().size() + " upgrades.");
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-enchanting disabled.");
    }

    public void reloadAll() {
        reloadConfig();
        registry.reload();
    }

    public EnchantRegistry registry() { return registry; }
    public ItemModifier modifier() { return modifier; }

    private void ensureExample(String resourcePath) {
        File target = new File(getDataFolder(), resourcePath);
        if (target.exists()) return;
        try {
            saveResource(resourcePath, false);
        } catch (IllegalArgumentException ex) {
            // resource missing — fine, admin can add their own.
        }
    }
}
