package com.github._255_ping.rpg.dungeons;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class RpgDungeonsPlugin extends JavaPlugin {

    private DungeonRegistry registry;
    private DungeonManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureExample("dungeons/example.yml");

        registry = new DungeonRegistry(new File(getDataFolder(), "dungeons"), getLogger());
        registry.reload();

        manager = new DungeonManager(this, registry);
        manager.initialize();

        getServer().getPluginManager().registerEvents(new DungeonEventListener(manager), this);
        Objects.requireNonNull(getCommand("dungeon"), "command 'dungeon' missing").setExecutor(new DungeonCommand(this));

        getLogger().info("rpg-dungeons v" + getPluginMeta().getVersion()
                + " enabled with " + registry.all().size() + " dungeons.");
    }

    @Override
    public void onDisable() {
        if (registry != null) registry.saveAll();
        getLogger().info("rpg-dungeons disabled.");
    }

    public void reloadAll() {
        reloadConfig();
        registry.reload();
    }

    public DungeonRegistry registry() { return registry; }
    public DungeonManager manager() { return manager; }

    private void ensureExample(String resourcePath) {
        File target = new File(getDataFolder(), resourcePath);
        if (target.exists()) return;
        try { saveResource(resourcePath, false); } catch (IllegalArgumentException ignored) {}
    }
}
