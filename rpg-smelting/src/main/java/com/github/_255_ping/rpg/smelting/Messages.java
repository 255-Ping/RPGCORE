package com.github._255_ping.rpg.smelting;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

/** Loads message strings from {@code plugins/rpg-smelting/messages.yml}. */
public final class Messages {

    private YamlConfiguration cfg;
    private final JavaPlugin plugin;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File f = new File(plugin.getDataFolder(), "messages.yml");
        if (!f.exists()) plugin.saveResource("messages.yml", false);
        cfg = YamlConfiguration.loadConfiguration(f);
    }

    /** Returns the message for {@code key}, applying {@code replacements} ({@code {key} → value}). */
    public String get(String key, Map<String, Object> replacements) {
        String raw = cfg.getString(key, "§c[missing: " + key + "]");
        for (Map.Entry<String, Object> e : replacements.entrySet()) {
            raw = raw.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        return raw.replace("&", "§");
    }

    public String get(String key) {
        return get(key, Map.of());
    }
}
