package com.github._255_ping.rpg.cooking;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public final class CookingRegistry {

    private final File dir;
    private final Logger logger;
    private final int defaultCookTicks;
    private final ConcurrentMap<String, CookRecipeDef> recipes = new ConcurrentHashMap<>();

    public CookingRegistry(File dir, int defaultCookTicks, Logger logger) {
        this.dir = dir;
        this.defaultCookTicks = defaultCookTicks;
        this.logger = logger;
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
                        recipes.put(key.toLowerCase(Locale.ROOT), parse(key.toLowerCase(Locale.ROOT), s));
                    } catch (Exception ex) {
                        logger.warning("Skipping cooking recipe '" + key + "' in "
                                + f.getName() + ": " + ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                logger.warning("Failed to parse " + f.getName() + ": " + ex.getMessage());
            }
        }
    }

    public Optional<CookRecipeDef> get(String id) {
        return Optional.ofNullable(recipes.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<CookRecipeDef> all() { return recipes.values(); }

    private CookRecipeDef parse(String id, ConfigurationSection s) {
        int ticks = s.getInt("CookTicks", defaultCookTicks);
        int reqLevel = s.getInt("RequiredLevel", 1);
        List<CookRecipeDef.Ingredient> inputs = new ArrayList<>();
        List<?> rawInputs = s.getList("Inputs");
        if (rawInputs != null) {
            for (Object o : rawInputs) {
                if (o instanceof java.util.Map<?, ?> m) {
                    String item = String.valueOf(m.get("Item"));
                    int amount = m.get("Amount") instanceof Number n ? n.intValue() : 1;
                    inputs.add(new CookRecipeDef.Ingredient(item, amount));
                }
            }
        }
        ConfigurationSection out = s.getConfigurationSection("Output");
        if (out == null) throw new IllegalArgumentException("missing Output");
        CookRecipeDef.Ingredient output = new CookRecipeDef.Ingredient(
                out.getString("Item", ""), out.getInt("Amount", 1));
        return new CookRecipeDef(id, inputs, output, ticks, reqLevel);
    }
}
