package com.github._255_ping.rpg.regions;

import com.github._255_ping.rpg.api.RpgServices;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class RpgRegionsPlugin extends JavaPlugin {

    private CoreRegionService regions;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        regions = new CoreRegionService(this);
        regions.loadAll();
        RpgServices.setRegionService(regions);

        Objects.requireNonNull(getCommand("region"), "command 'region' missing")
                .setExecutor(new RegionCommand(regions));
        getServer().getPluginManager().registerEvents(new RegionEnforcer(regions), this);

        long pollTicks = Math.max(1, getConfig().getLong("move-poll-ticks", 5));
        getServer().getScheduler().runTaskTimer(
                this, new RegionTransitionTask(this, regions), pollTicks, pollTicks);

        getLogger().info("rpg-regions v" + getPluginMeta().getVersion()
                + " enabled; loaded " + regions.all().size() + " regions.");
    }

    @Override
    public void onDisable() {
        if (regions != null) regions.saveAll();
        getLogger().info("rpg-regions disabled.");
    }
}
