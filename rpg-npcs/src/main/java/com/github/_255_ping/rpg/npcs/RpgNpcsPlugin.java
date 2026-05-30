package com.github._255_ping.rpg.npcs;

import com.github._255_ping.rpg.api.RpgServices;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class RpgNpcsPlugin extends JavaPlugin {

    private NpcManager manager;
    private Messages messages;
    private BankerGui bankerGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureExample("npcs/example.yml");
        messages = new Messages(this);
        manager = new NpcManager(this);
        bankerGui = new BankerGui(this);
        manager.loadAll();

        var pm = getServer().getPluginManager();
        pm.registerEvents(new NpcInteractListener(manager, bankerGui, this), this);
        pm.registerEvents(new NpcProtectionListener(manager, this), this);
        pm.registerEvents(bankerGui, this);

        Objects.requireNonNull(getCommand("npc"), "command 'npc' missing")
               .setExecutor(new NpcCommand(this));

        scheduleBankerInterest();

        getLogger().info("rpg-npcs v" + getPluginMeta().getVersion()
                + " enabled with " + manager.all().size() + " NPCs.");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.saveAll();
            manager.despawnAll();
        }
        getLogger().info("rpg-npcs disabled.");
    }

    public void reloadAll() {
        reloadConfig();
        manager.loadAll();
        messages = new Messages(this);
    }

    public NpcManager manager() { return manager; }
    public Messages messages() { return messages; }

    private void ensureExample(String resourcePath) {
        File target = new File(getDataFolder(), resourcePath);
        if (target.exists()) return;
        try { saveResource(resourcePath, false); } catch (IllegalArgumentException ignored) {}
    }

    /** Accrue daily interest for all banker NPCs once per game-day (configurable ticks). */
    private void scheduleBankerInterest() {
        long intervalTicks = (long) (getConfig().getDouble("banker.interest-interval-hours", 24.0) * 72000L);
        if (intervalTicks <= 0) return;
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (NpcDef def : manager.all()) {
                if (def.behaviorType() != NpcDef.BehaviorType.BANKER) continue;
                NpcDef.BankerData data = def.bankerData();
                if (data == null) continue;
                try {
                    bankerGui.accrueInterest(def.id(), data.dailyInterestPercent());
                } catch (IllegalStateException ignored) {}
            }
        }, intervalTicks, intervalTicks);
    }
}
