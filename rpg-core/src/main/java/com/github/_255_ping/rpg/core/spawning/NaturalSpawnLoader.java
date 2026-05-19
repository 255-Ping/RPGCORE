package com.github._255_ping.rpg.core.spawning;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public final class NaturalSpawnLoader {

    private final File folder;
    private final Logger logger;
    private final List<NaturalSpawnRule> rules = new ArrayList<>();

    public NaturalSpawnLoader(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public List<NaturalSpawnRule> rules() { return Collections.unmodifiableList(rules); }

    public void loadAll() {
        rules.clear();
        if (!folder.isDirectory()) return;
        File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                loadFile(f);
            } catch (Exception ex) {
                logger.warning("Failed to parse natural-spawning file " + f.getName() + ": " + ex.getMessage());
            }
        }
    }

    private void loadFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection s = yaml.getConfigurationSection(id);
            if (s == null) {
                logger.warning("natural-spawn '" + id + "' is not a section, skipping");
                continue;
            }
            try {
                rules.add(parse(id, s));
            } catch (Exception ex) {
                logger.warning("Skipping natural-spawn '" + id + "': " + ex.getMessage());
            }
        }
    }

    private NaturalSpawnRule parse(String id, ConfigurationSection s) {
        boolean enabled = s.getBoolean("enabled", true);

        List<NaturalSpawnRule.WeightedMob> mobs = new ArrayList<>();
        for (Object raw : s.getMapList("mobs")) {
            if (!(raw instanceof java.util.Map<?, ?> m)) continue;
            String mob = String.valueOf(m.get("mob"));
            int weight = m.get("weight") instanceof Number n ? n.intValue() : 1;
            mobs.add(new NaturalSpawnRule.WeightedMob(mob, weight));
        }

        ConfigurationSection cond = s.getConfigurationSection("conditions");
        List<String> tod = list(cond, "time-of-day", "any");
        List<String> weather = list(cond, "weather", "any");
        List<String> biomes = list(cond, "biomes", "any");

        Integer lightMin = null, lightMax = null;
        if (cond != null && cond.isConfigurationSection("light-level")) {
            ConfigurationSection ll = cond.getConfigurationSection("light-level");
            if (ll.contains("min")) lightMin = ll.getInt("min");
            if (ll.contains("max")) lightMax = ll.getInt("max");
        }
        Integer yMin = null, yMax = null;
        if (cond != null && cond.isConfigurationSection("y-range")) {
            ConfigurationSection yy = cond.getConfigurationSection("y-range");
            if (yy.contains("min")) yMin = yy.getInt("min");
            if (yy.contains("max")) yMax = yy.getInt("max");
        }

        ConfigurationSection rate = s.getConfigurationSection("rate");
        double perPlayer = rate == null ? 0.0005 : rate.getDouble("per-player-per-tick", 0.0005);
        int minDist = rate == null ? 24 : rate.getInt("min-distance-from-player", 24);
        int maxDist = rate == null ? 64 : rate.getInt("max-distance-from-player", 64);

        return new NaturalSpawnRule(id, enabled, mobs, tod, lightMin, lightMax, weather, biomes,
                yMin, yMax, perPlayer, minDist, maxDist);
    }

    private static List<String> list(ConfigurationSection s, String key, String fallback) {
        if (s == null) return List.of(fallback.toLowerCase(Locale.ROOT));
        List<String> raw = s.getStringList(key);
        if (!raw.isEmpty()) {
            List<String> out = new ArrayList<>(raw.size());
            for (String v : raw) out.add(v.toLowerCase(Locale.ROOT));
            return out;
        }
        String single = s.getString(key);
        if (single != null) return List.of(single.toLowerCase(Locale.ROOT));
        return List.of(fallback.toLowerCase(Locale.ROOT));
    }
}
