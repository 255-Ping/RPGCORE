package com.github._255_ping.rpg.core.persistence;

import com.github._255_ping.rpg.api.persistence.DataStore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class YamlRepository implements DataStore.Repository {

    private final File dir;

    public YamlRepository(File dir) {
        this.dir = dir;
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Could not create repo dir: " + dir);
        }
    }

    @Override
    public Optional<Map<String, Object>> get(String key) {
        File f = file(key);
        if (!f.exists()) return Optional.empty();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
        return Optional.of(yaml.getValues(false));
    }

    @Override
    public CompletableFuture<Void> save(String key, Map<String, Object> data) {
        return CompletableFuture.runAsync(() -> {
            File f = file(key);
            YamlConfiguration yaml = new YamlConfiguration();
            for (Map.Entry<String, Object> e : data.entrySet()) {
                yaml.set(e.getKey(), e.getValue());
            }
            try {
                yaml.save(f);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to save " + f, ex);
            }
        });
    }

    @Override
    public CompletableFuture<Void> delete(String key) {
        return CompletableFuture.runAsync(() -> {
            File f = file(key);
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        });
    }

    @Override
    public Collection<String> keys() {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return List.of();
        return Arrays.stream(files)
                .map(f -> f.getName().substring(0, f.getName().length() - 4))
                .toList();
    }

    private File file(String key) {
        return new File(dir, sanitize(key) + ".yml");
    }

    private static String sanitize(String key) {
        return key.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }
}
