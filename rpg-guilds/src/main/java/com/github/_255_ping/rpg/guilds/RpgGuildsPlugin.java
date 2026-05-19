package com.github._255_ping.rpg.guilds;

import com.github._255_ping.rpg.api.RpgServices;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class RpgGuildsPlugin extends JavaPlugin {

    private GuildManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new GuildManager(this);
        manager.loadAll();
        RpgServices.setGuilds(manager);

        Objects.requireNonNull(getCommand("guild"), "command 'guild' missing")
                .setExecutor(new GuildCommand(this, manager));
        getServer().getScheduler().runTaskTimer(this, manager::cleanExpiredInvites, 200L, 200L);

        getLogger().info("rpg-guilds v" + getPluginMeta().getVersion()
                + " enabled; loaded " + manager.all().size() + " guilds.");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.saveAll();
        getLogger().info("rpg-guilds disabled.");
    }
}
