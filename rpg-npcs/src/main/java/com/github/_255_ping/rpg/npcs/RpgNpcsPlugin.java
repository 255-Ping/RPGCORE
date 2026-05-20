package com.github._255_ping.rpg.npcs;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class RpgNpcsPlugin extends JavaPlugin {

    private NpcManager manager;
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureExample("npcs/example.yml");
        messages = new Messages(this);
        manager = new NpcManager(this);
        manager.loadAll();

        getServer().getPluginManager().registerEvents(new NpcInteractListener(manager), this);

        Objects.requireNonNull(getCommand("npc"), "command 'npc' missing").setExecutor(new NpcCommand(this));

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
}
