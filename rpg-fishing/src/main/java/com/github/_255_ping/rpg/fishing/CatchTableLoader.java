package com.github._255_ping.rpg.fishing;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads {@link CatchTable} definitions from {@code *.yml} files in a directory.
 * Each file becomes one table; the table id is the filename without the {@code .yml} extension.
 *
 * <p>YAML schema per file:
 * <pre>
 * suppress-vanilla: true
 * entries:
 *   - item: cod
 *     chance: 55.0
 *     min: 1
 *     max: 2
 *     fortune-affected: true
 * </pre>
 */
public final class CatchTableLoader {

    private final File dir;
    private final Logger log;

    public CatchTableLoader(File dir, Logger log) {
        this.dir = dir;
        this.log = log;
    }

    public Map<String, CatchTable> loadAll() {
        Map<String, CatchTable> result = new HashMap<>();
        if (!dir.isDirectory()) return result;

        File[] files = dir.listFiles();
        if (files == null) return result;

        for (File file : files) {
            if (!file.getName().endsWith(".yml")) continue;
            String tableId = file.getName().substring(0, file.getName().length() - 4);
            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                boolean suppressVanilla = cfg.getBoolean("suppress-vanilla", true);

                List<CatchEntry> entries = new ArrayList<>();
                for (Map<?, ?> raw : cfg.getMapList("entries")) {
                    String itemId = raw.get("item") instanceof String s ? s : null;
                    if (itemId == null || itemId.isBlank()) continue;

                    double chance = raw.get("chance") instanceof Number n ? n.doubleValue() : 100.0;
                    int min = raw.get("min") instanceof Number n ? n.intValue() : 1;
                    int max = raw.get("max") instanceof Number n ? n.intValue() : min;
                    boolean fortune = raw.get("fortune-affected") instanceof Boolean b && b;
                    entries.add(new CatchEntry(itemId, chance, Math.min(min, max), Math.max(min, max), fortune));
                }

                result.put(tableId, new CatchTable(tableId, suppressVanilla, List.copyOf(entries)));
                log.info("[rpg-fishing] Loaded catch table '" + tableId + "' ("
                        + entries.size() + " " + (entries.size() == 1 ? "entry" : "entries") + ")");
            } catch (Exception ex) {
                log.warning("[rpg-fishing] Failed to load catch table '" + tableId + "': " + ex.getMessage());
            }
        }
        return result;
    }
}
