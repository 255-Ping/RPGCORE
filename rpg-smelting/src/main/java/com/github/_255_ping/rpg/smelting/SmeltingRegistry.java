package com.github._255_ping.rpg.smelting;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/** Loads and caches {@link SmeltRecipeDef} objects from {@code plugins/rpg-smelting/recipes/*.yml}. */
public final class SmeltingRegistry {

    private final File   dir;
    private final Logger logger;
    private final int    defaultSmeltTicks;
    private final ConcurrentMap<String, SmeltRecipeDef> recipes = new ConcurrentHashMap<>();

    public SmeltingRegistry(File dir, int defaultSmeltTicks, Logger logger) {
        this.dir               = dir;
        this.defaultSmeltTicks = defaultSmeltTicks;
        this.logger            = logger;
    }

    public void reload() {
        recipes.clear();
        if (!dir.isDirectory()) return;
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
                for (String key : y.getKeys(false)) {
                    ConfigurationSection s = y.getConfigurationSection(key);
                    if (s == null) continue;
                    try {
                        recipes.put(key.toLowerCase(Locale.ROOT),
                                parse(key.toLowerCase(Locale.ROOT), s));
                    } catch (Exception ex) {
                        logger.warning("Skipping smelting recipe '" + key + "' in "
                                + f.getName() + ": " + ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                logger.warning("Failed to parse " + f.getName() + ": " + ex.getMessage());
            }
        }
    }

    public Optional<SmeltRecipeDef> get(String id) {
        return Optional.ofNullable(recipes.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<SmeltRecipeDef> all() { return recipes.values(); }

    private SmeltRecipeDef parse(String id, ConfigurationSection s) {
        ConfigurationSection inSec  = s.getConfigurationSection("Input");
        ConfigurationSection outSec = s.getConfigurationSection("Output");
        if (inSec  == null) throw new IllegalArgumentException("missing Input");
        if (outSec == null) throw new IllegalArgumentException("missing Output");

        SmeltRecipeDef.Ingredient input  = new SmeltRecipeDef.Ingredient(
                requireItem(inSec,  "Item"), inSec.getInt("Amount",  1));
        SmeltRecipeDef.Ingredient output = new SmeltRecipeDef.Ingredient(
                requireItem(outSec, "Item"), outSec.getInt("Amount", 1));

        int   smeltTicks  = s.getInt("SmeltTicks", defaultSmeltTicks);
        int   reqLevel    = s.getInt("RequiredLevel", 1);
        float vanillaXp   = (float) s.getDouble("VanillaXP", 0.1);

        return new SmeltRecipeDef(id, input, output, smeltTicks, reqLevel, vanillaXp);
    }

    private static String requireItem(ConfigurationSection s, String key) {
        String val = s.getString(key);
        if (val == null || val.isBlank()) throw new IllegalArgumentException("missing " + key);
        return val;
    }
}
