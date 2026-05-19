package com.github._255_ping.rpg.mining;

import org.bukkit.plugin.java.JavaPlugin;

public final class RpgMiningPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("rpg-mining enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-mining disabled.");
    }
}
