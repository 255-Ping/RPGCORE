package com.github._255_ping.rpg.bossbar;

import com.github._255_ping.rpg.api.RpgServices;
import org.bukkit.plugin.java.JavaPlugin;

public final class RpgBossBarPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        double defaultRange = getConfig().getDouble("bossbar.default-show-range", 64.0);
        long interval       = getConfig().getLong("bossbar.proximity-interval-ticks", 40L);

        CoreBossBarService service = new CoreBossBarService(this);
        getServer().getPluginManager().registerEvents(service, this);
        RpgServices.setBossBar(service);

        BossBarProximityTask task = new BossBarProximityTask(service, defaultRange);
        getServer().getScheduler().runTaskTimer(this, task, interval, interval);

        getLogger().info("rpg-bossbar v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        // BossBar instances are removed when players disconnect naturally.
        // No explicit cleanup needed — the server handles bar removal on shutdown.
    }
}
