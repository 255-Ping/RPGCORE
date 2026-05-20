package com.github._255_ping.rpg.enchanting;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public final class EnchantRegistry {

    private final File enchantsDir;
    private final File reforgesDir;
    private final File upgradesDir;
    private final Logger logger;

    private final ConcurrentMap<String, EnchantDef> enchants = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ReforgeDef> reforges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UpgradeDef> upgrades = new ConcurrentHashMap<>();

    public EnchantRegistry(File enchantsDir, File reforgesDir, File upgradesDir, Logger logger) {
        this.enchantsDir = enchantsDir;
        this.reforgesDir = reforgesDir;
        this.upgradesDir = upgradesDir;
        this.logger = logger;
    }

    public void reload() {
        enchants.clear();
        reforges.clear();
        upgrades.clear();
        loadEnchants();
        loadReforges();
        loadUpgrades();
    }

    public Optional<EnchantDef> enchant(String id) { return Optional.ofNullable(enchants.get(id)); }
    public Optional<ReforgeDef> reforge(String id) { return Optional.ofNullable(reforges.get(id)); }
    public Optional<UpgradeDef> upgrade(String id) { return Optional.ofNullable(upgrades.get(id)); }

    public Collection<EnchantDef> allEnchants() { return enchants.values(); }
    public Collection<ReforgeDef> allReforges() { return reforges.values(); }
    public Collection<UpgradeDef> allUpgrades() { return upgrades.values(); }

    private void loadEnchants() {
        forEachYaml(enchantsDir, (id, s) -> {
            int maxLevel = s.getInt("MaxLevel", 1);
            String displayName = s.getString("DisplayName", id);
            List<String> appliesTo = lower(s.getStringList("AppliesTo"));
            Map<String, Double> stats = readDoubleMap(s.getConfigurationSection("Stats"));
            double scale = s.getDouble("ScalePerLevel", 1.0);
            long xpCost = s.getLong("XpCost", 0);
            double currencyCost = s.getDouble("CurrencyCost", 0);
            int reqLevel = s.getInt("RequiredLevel", 1);
            enchants.put(id, new EnchantDef(id, displayName, maxLevel,
                    appliesTo, stats, scale, xpCost, currencyCost, reqLevel));
        });
    }

    private void loadReforges() {
        forEachYaml(reforgesDir, (id, s) -> {
            String displayName = s.getString("DisplayName", id);
            List<String> appliesTo = lower(s.getStringList("AppliesTo"));
            double currencyCost = s.getDouble("CurrencyCost", 0);
            int reqLevel = s.getInt("RequiredLevel", 1);
            ConfigurationSection sbr = s.getConfigurationSection("StatsByRarity");
            Map<String, Map<String, Double>> byRarity = new HashMap<>();
            if (sbr != null) {
                for (String rarity : sbr.getKeys(false)) {
                    ConfigurationSection inner = sbr.getConfigurationSection(rarity);
                    byRarity.put(rarity.toLowerCase(), readDoubleMap(inner));
                }
            }
            reforges.put(id, new ReforgeDef(id, displayName, appliesTo,
                    currencyCost, reqLevel, byRarity));
        });
    }

    private void loadUpgrades() {
        forEachYaml(upgradesDir, (id, s) -> {
            String displayName = s.getString("DisplayName", id);
            List<String> appliesTo = lower(s.getStringList("AppliesTo"));
            int maxTier = s.getInt("MaxTier", 1);
            double currencyCost = s.getDouble("CurrencyCost", 0);
            int reqLevel = s.getInt("RequiredLevel", 1);
            Map<String, Double> stats = readDoubleMap(s.getConfigurationSection("StatsPerTier"));
            upgrades.put(id, new UpgradeDef(id, displayName, appliesTo,
                    maxTier, currencyCost, reqLevel, stats));
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

    private static List<String> lower(List<String> in) {
        List<String> out = new java.util.ArrayList<>(in.size());
        for (String s : in) out.add(s.toLowerCase());
        return out;
    }

    private static Map<String, Double> readDoubleMap(ConfigurationSection s) {
        if (s == null) return Map.of();
        Map<String, Double> out = new LinkedHashMap<>();
        for (String k : s.getKeys(false)) {
            out.put(k.toLowerCase(), s.getDouble(k));
        }
        return out;
    }
}
