package com.github._255_ping.rpg.core;

import org.bukkit.plugin.java.JavaPlugin;

public final class RpgCorePlugin extends JavaPlugin {

    private static RpgCorePlugin instance;

    public static RpgCorePlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("rpg-core enabled. Registries and services will be wired here.");
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-core disabled.");
        instance = null;
    }
}
