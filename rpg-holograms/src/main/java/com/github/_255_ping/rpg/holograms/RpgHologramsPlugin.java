package com.github._255_ping.rpg.holograms;

import org.bukkit.plugin.java.JavaPlugin;

public final class RpgHologramsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new DamageIndicatorListener(this), this);
        getLogger().info("rpg-holograms v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-holograms disabled.");
    }
}
