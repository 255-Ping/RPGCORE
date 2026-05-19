package com.github._255_ping.rpg.quests;

import org.bukkit.plugin.java.JavaPlugin;

public final class RpgQuestsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("rpg-quests enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-quests disabled.");
    }
}
