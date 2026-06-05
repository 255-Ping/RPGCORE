package com.github._255_ping.rpg.kits;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Loads and stores kit definitions from {@code plugins/rpg-kits/kits/*.yml}.
 * Definitions are keyed by lowercase ID and are immutable after load.
 */
public final class KitRegistry {

    private final File   dir;
    private final Logger logger;
    private final ConcurrentHashMap<String, KitDef> kits = new ConcurrentHashMap<>();

    public KitRegistry(File dir, Logger logger) {
        this.dir    = dir;
        this.logger = logger;
    }

    public void reload() {
        kits.clear();
        if (!dir.isDirectory()) return;
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                for (String key : yaml.getKeys(false)) {
                    ConfigurationSection s = yaml.getConfigurationSection(key);
                    if (s == null) continue;
                    try {
                        KitDef def = parse(key.toLowerCase(Locale.ROOT), s);
                        kits.put(def.id(), def);
                    } catch (Exception ex) {
                        logger.warning("Skipping kit '" + key + "' in " + f.getName() + ": " + ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                logger.warning("Failed to parse " + f.getName() + ": " + ex.getMessage());
            }
        }
        logger.info("Loaded " + kits.size() + " kit(s).");
    }

    public Optional<KitDef> get(String id) {
        return Optional.ofNullable(kits.get(id.toLowerCase(Locale.ROOT)));
    }

    /** Sorted list of all kits. */
    public List<KitDef> all() {
        return kits.values().stream()
                .sorted(Comparator.comparing(KitDef::id))
                .toList();
    }

    // -------------------------------------------------------------------------

    private static KitDef parse(String id, ConfigurationSection s) {
        String displayName  = s.getString("DisplayName", "&7" + id);
        List<String> desc   = s.getStringList("Description");
        boolean oneTime     = s.getBoolean("OneTime", true);
        int cooldown        = s.getInt("Cooldown", 0);
        String permission   = s.getString("Permission", null);
        if (permission != null && permission.isBlank()) permission = null;

        List<KitDef.ItemEntry> items = new ArrayList<>();
        List<?> rawItems = s.getList("Items");
        if (rawItems != null) {
            for (Object o : rawItems) {
                if (o instanceof Map<?, ?> m) {
                    String item  = String.valueOf(m.get("Item"));
                    int    amount = m.get("Amount") instanceof Number n ? n.intValue() : 1;
                    items.add(new KitDef.ItemEntry(item, amount));
                }
            }
        }
        if (items.isEmpty()) throw new IllegalArgumentException("kit '" + id + "' has no Items");
        return new KitDef(id, displayName.replace("&", "§"), desc, items, permission, oneTime, cooldown);
    }
}
