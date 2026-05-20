package com.github._255_ping.rpg.quests;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class RpgQuestsPlugin extends JavaPlugin {

    private QuestRegistry registry;
    private QuestManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureExample("quests/example.yml");

        File questsDir = new File(getDataFolder(), "quests");
        registry = new QuestRegistry(questsDir, getLogger());
        registry.reload();

        manager = new QuestManager(this, registry);

        getServer().getPluginManager().registerEvents(new QuestEventListener(manager), this);

        Objects.requireNonNull(getCommand("quest"), "command 'quest' missing").setExecutor(new QuestCommand(this));

        new QuestNpcHandoff(manager).register(this);

        getLogger().info("rpg-quests v" + getPluginMeta().getVersion()
                + " enabled with " + registry.all().size() + " quests.");
    }

    @Override
    public void onDisable() {
        // Save all online players' state before shutdown.
        if (manager != null) {
            for (var p : getServer().getOnlinePlayers()) {
                manager.unload(p.getUniqueId());
            }
        }
        getLogger().info("rpg-quests disabled.");
    }

    public void reloadAll() {
        reloadConfig();
        registry.reload();
    }

    public QuestRegistry registry() { return registry; }
    public QuestManager manager() { return manager; }

    private void ensureExample(String resourcePath) {
        File target = new File(getDataFolder(), resourcePath);
        if (target.exists()) return;
        try { saveResource(resourcePath, false); } catch (IllegalArgumentException ignored) {}
    }
}
