package com.github._255_ping.rpg.dungeons;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class DungeonRegistry {

    private final File dir;
    private final Logger logger;
    private final Map<String, DungeonDef> byId = new ConcurrentHashMap<>();

    public DungeonRegistry(File dir, Logger logger) {
        this.dir = dir;
        this.logger = logger;
    }

    public void reload() {
        byId.clear();
        if (!dir.isDirectory() && !dir.mkdirs()) {
            logger.warning("Could not create dungeons/ directory.");
            return;
        }
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
                for (String id : y.getKeys(false)) {
                    ConfigurationSection s = y.getConfigurationSection(id);
                    if (s == null) continue;
                    try {
                        byId.put(id.toLowerCase(Locale.ROOT), parse(id.toLowerCase(Locale.ROOT), s));
                    } catch (Exception ex) {
                        logger.warning("Skipping dungeon '" + id + "' in " + f.getName() + ": " + ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                logger.warning("Failed to parse " + f.getName() + ": " + ex.getMessage());
            }
        }
    }

    public Optional<DungeonDef> get(String id) {
        return Optional.ofNullable(byId.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<DungeonDef> all() { return byId.values(); }

    public void put(DungeonDef def) { byId.put(def.id(), def); }

    public boolean remove(String id) {
        return byId.remove(id.toLowerCase(Locale.ROOT)) != null;
    }

    public void saveAll() {
        if (!dir.isDirectory()) dir.mkdirs();
        YamlConfiguration out = new YamlConfiguration();
        for (DungeonDef def : byId.values()) {
            out.createSection(def.id(), def.toMap());
        }
        try {
            out.save(new File(dir, "all.yml"));
        } catch (IOException ex) {
            logger.warning("Failed to save dungeons: " + ex.getMessage());
        }
    }

    private DungeonDef parse(String id, ConfigurationSection s) {
        String name = s.getString("DisplayName", id);
        ConfigurationSection tmpl = s.getConfigurationSection("Template");
        if (tmpl == null) throw new IllegalArgumentException("missing Template");
        String world = tmpl.getString("World", "world");
        Vector min = readVector(tmpl.getConfigurationSection("Min"));
        Vector max = readVector(tmpl.getConfigurationSection("Max"));
        Vector spawn = readVector(s.getConfigurationSection("Spawn"));

        ConfigurationSection ent = s.getConfigurationSection("Entrance");
        String entWorld = ent != null ? ent.getString("World", "world") : "world";
        Vector entVec = readVector(ent);

        ConfigurationSection ext = s.getConfigurationSection("Exit");
        String extWorld = ext != null ? ext.getString("World", "world") : "world";
        Vector extVec = readVector(ext);

        int maxPlayers = s.getInt("MaxPlayers", 4);
        int requiredLevel = s.getInt("RequiredLevel", 0);

        return new DungeonDef(id, name, world, min, max, spawn, entWorld, entVec,
                extWorld, extVec, maxPlayers, requiredLevel);
    }

    private static Vector readVector(ConfigurationSection s) {
        if (s == null) return new Vector(0, 0, 0);
        return new Vector(s.getDouble("X"), s.getDouble("Y"), s.getDouble("Z"));
    }

    @SuppressWarnings("unused")
    Map<String, DungeonDef> snapshot() { return new HashMap<>(byId); }
}
