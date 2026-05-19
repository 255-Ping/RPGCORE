package com.github._255_ping.rpg.core.formatting;

import com.github._255_ping.rpg.api.formatting.MessageFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

public final class CoreMessageFormatter implements MessageFormatter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final File file;
    private YamlConfiguration messages;

    public CoreMessageFormatter(File file) {
        this.file = file;
        reload();
    }

    public void reload() {
        messages = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public String format(String key) {
        return format(key, Map.of());
    }

    @Override
    public String format(String key, Map<String, ?> placeholders) {
        String raw = messages.getString(key, "[missing:" + key + "]");
        if (placeholders.isEmpty()) return raw;
        for (Map.Entry<String, ?> e : placeholders.entrySet()) {
            raw = raw.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        return raw;
    }

    @Override
    public Component component(String key) {
        return LEGACY.deserialize(format(key));
    }

    @Override
    public Component component(String key, Map<String, ?> placeholders) {
        return LEGACY.deserialize(format(key, placeholders));
    }
}
