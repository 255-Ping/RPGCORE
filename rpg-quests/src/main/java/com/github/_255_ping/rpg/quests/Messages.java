package com.github._255_ping.rpg.quests;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

/**
 * Loads {@code messages.yml} once at plugin enable and resolves keys with {placeholder} expansion.
 * Falls back to {@code "[missing:key]"} when an entry isn't present so admins notice typos.
 */
public final class Messages {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final YamlConfiguration cfg;

    public Messages(JavaPlugin plugin) {
        File f = new File(plugin.getDataFolder(), "messages.yml");
        if (!f.exists()) plugin.saveResource("messages.yml", false);
        this.cfg = YamlConfiguration.loadConfiguration(f);
    }

    public Component get(String key) { return get(key, Map.of()); }

    public Component get(String key, Map<String, ?> placeholders) {
        String raw = cfg.getString(key, "[missing:" + key + "]");
        for (Map.Entry<String, ?> e : placeholders.entrySet()) {
            raw = raw.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        return LEGACY.deserialize(raw);
    }
}
