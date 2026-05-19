package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityInvocation;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.api.stats.Stat;
import com.github._255_ping.rpg.core.health.CoreHealthService;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Scans {@code plugins/rpg-core/mobs/} for *.yml files. Each top-level key is a mob ID.
 */
public final class MobLoader {

    private final File folder;
    private final CoreMobRegistry registry;
    private final NamespacedKey mobIdKey;
    private final CoreHealthService healthService;
    private final Logger logger;

    public MobLoader(File folder, CoreMobRegistry registry, NamespacedKey mobIdKey,
                     CoreHealthService healthService, Logger logger) {
        this.folder = folder;
        this.registry = registry;
        this.mobIdKey = mobIdKey;
        this.healthService = healthService;
        this.logger = logger;
    }

    public void loadAll() {
        registry.clear();
        if (!folder.isDirectory()) return;
        File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                loadFile(f);
            } catch (Exception ex) {
                logger.warning("Failed to parse mobs file " + f.getName() + ": " + ex.getMessage());
            }
        }
    }

    private void loadFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(id);
            if (section == null) {
                logger.warning("mob '" + id + "' in " + file.getName() + " is not a section, skipping");
                continue;
            }
            try {
                CoreRpgMob mob = parse(id, section);
                registry.register(mob);
            } catch (Exception ex) {
                logger.warning("Skipping mob '" + id + "' in " + file.getName() + ": " + ex.getMessage());
            }
        }
    }

    private CoreRpgMob parse(String id, ConfigurationSection s) {
        String entityName = s.getString("MinecraftMob");
        if (entityName == null) throw new IllegalArgumentException("missing MinecraftMob");
        EntityType type;
        try {
            type = EntityType.valueOf(entityName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unknown MinecraftMob: " + entityName);
        }

        String displayName = s.getString("DisplayName");
        double health = s.getDouble("Health", 20);
        double damage = s.getDouble("Damage", 1);
        double defense = s.getDouble("Armor", 0);

        Map<Stat, Double> stats = new HashMap<>();
        stats.put(BuiltinStat.DEFENSE, defense);
        ConfigurationSection bonus = s.getConfigurationSection("Stats");
        if (bonus != null) {
            for (String statId : bonus.getKeys(false)) {
                Optional<Stat> stat = RpgServices.stats().get(statId);
                if (stat.isEmpty()) {
                    logger.warning("mob '" + id + "' references unknown stat '" + statId + "'");
                    continue;
                }
                stats.merge(stat.get(), bonus.getDouble(statId), Double::sum);
            }
        }

        ItemStack helmet = null, chest = null, legs = null, boots = null, hand = null, off = null;
        for (String line : s.getStringList("Equipment")) {
            String[] parts = line.trim().split("\\s+", 2);
            if (parts.length != 2) {
                logger.warning("mob '" + id + "' bad Equipment line: " + line);
                continue;
            }
            String slot = parts[0].toUpperCase(Locale.ROOT);
            ItemStack item = resolveItem(parts[1]);
            if (item == null) {
                logger.warning("mob '" + id + "' equipment item not found: " + parts[1]);
                continue;
            }
            switch (slot) {
                case "HELMET" -> helmet = item;
                case "CHEST", "CHESTPLATE" -> chest = item;
                case "LEGS", "LEGGINGS" -> legs = item;
                case "BOOTS" -> boots = item;
                case "HAND", "MAINHAND" -> hand = item;
                case "OFFHAND" -> off = item;
                default -> logger.warning("mob '" + id + "' unknown equipment slot: " + slot);
            }
        }

        List<AbilityInvocation> abilities = new ArrayList<>();
        for (String inv : s.getStringList("Abilities")) {
            String invStripped = inv;
            int triggerIdx = invStripped.indexOf('~');
            if (triggerIdx >= 0) invStripped = invStripped.substring(0, triggerIdx).trim();
            try {
                abilities.addAll(AbilityDsl.parse(invStripped));
            } catch (Exception ex) {
                logger.warning("mob '" + id + "' has bad ability invocation '" + inv + "': " + ex.getMessage());
            }
        }

        return new CoreRpgMob(id, displayName, type, health, damage, defense,
                stats, helmet, chest, legs, boots, hand, off, null, abilities,
                mobIdKey, healthService);
    }

    private static ItemStack resolveItem(String token) {
        Optional<?> custom = RpgServices.items().get(token);
        if (custom.isPresent()) {
            return ((com.github._255_ping.rpg.api.items.RpgItem) custom.get()).toItemStack();
        }
        Material m = Material.matchMaterial(token);
        if (m != null) return new ItemStack(m);
        return null;
    }
}
