package com.github._255_ping.rpg.alchemy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public final class AlchemyRegistry {

    private final File potionsDir;
    private final File recipesDir;
    private final Logger logger;
    private final int defaultBrewTicks;

    private final ConcurrentMap<String, PotionDef> potions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BrewRecipeDef> recipes = new ConcurrentHashMap<>();

    public AlchemyRegistry(File potionsDir, File recipesDir, int defaultBrewTicks, Logger logger) {
        this.potionsDir = potionsDir;
        this.recipesDir = recipesDir;
        this.defaultBrewTicks = defaultBrewTicks;
        this.logger = logger;
    }

    public void reload() {
        potions.clear();
        recipes.clear();
        loadPotions();
        loadRecipes();
    }

    public Optional<PotionDef> potion(String id) { return Optional.ofNullable(potions.get(id)); }
    public Optional<BrewRecipeDef> recipe(String id) { return Optional.ofNullable(recipes.get(id)); }

    public Collection<PotionDef> allPotions() { return potions.values(); }
    public Collection<BrewRecipeDef> allRecipes() { return recipes.values(); }

    private void loadPotions() {
        forEachYaml(potionsDir, (id, s) -> {
            String displayName = s.getString("DisplayName", id);
            int cmd = s.getInt("CustomModelData", 0);
            boolean consume = s.getBoolean("ConsumeOnDrink", true);
            // null means "use the global config default"; any explicit string overrides per-potion.
            String returnItem = s.isSet("ReturnItem") ? s.getString("ReturnItem") : null;
            List<PotionDef.EffectSpec> effects = new ArrayList<>();
            List<?> rawEffects = s.getList("Effects");
            if (rawEffects != null) {
                for (Object e : rawEffects) {
                    if (e instanceof java.util.Map<?, ?> m) {
                        String eid = String.valueOf(m.get("Id"));
                        int level = m.get("Level") instanceof Number n ? n.intValue() : 1;
                        int seconds = m.get("DurationSeconds") instanceof Number n ? n.intValue() : 30;
                        effects.add(new PotionDef.EffectSpec(eid, level, seconds * 20));
                    }
                }
            }
            potions.put(id, new PotionDef(id, displayName, cmd, effects, consume, returnItem));
        });
    }

    private void loadRecipes() {
        forEachYaml(recipesDir, (id, s) -> {
            int brewTicks = s.getInt("BrewTicks", defaultBrewTicks);
            int reqLevel = s.getInt("RequiredLevel", 1);
            List<BrewRecipeDef.Ingredient> inputs = new ArrayList<>();
            List<?> rawInputs = s.getList("Inputs");
            if (rawInputs != null) {
                for (Object o : rawInputs) {
                    if (o instanceof java.util.Map<?, ?> m) {
                        String item = String.valueOf(m.get("Item"));
                        int amount = m.get("Amount") instanceof Number n ? n.intValue() : 1;
                        inputs.add(new BrewRecipeDef.Ingredient(item, amount));
                    }
                }
            }
            ConfigurationSection out = s.getConfigurationSection("Output");
            if (out == null) throw new IllegalArgumentException("missing Output");
            BrewRecipeDef.Ingredient output = new BrewRecipeDef.Ingredient(
                    out.getString("Item", ""), out.getInt("Amount", 1));
            recipes.put(id, new BrewRecipeDef(id, inputs, output, brewTicks, reqLevel));
        });
    }

    private void forEachYaml(File dir, java.util.function.BiConsumer<String, ConfigurationSection> visitor) {
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
                        visitor.accept(key.toLowerCase(), s);
                    } catch (Exception ex) {
                        logger.warning("Failed to load '" + key + "' from " + f.getName() + ": " + ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                logger.warning("Failed to parse " + f.getName() + ": " + ex.getMessage());
            }
        }
    }
}
