package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityInvocation;
import com.github._255_ping.rpg.api.loot.Attribution;
import com.github._255_ping.rpg.api.loot.RollMode;
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
        stats.put(BuiltinStat.DAMAGE, damage);
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

        List<MobAbilityBinding> bindings = new ArrayList<>();
        List<?> abilityList = s.getList("Abilities", List.of());
        for (Object entry : abilityList) {
            try {
                if (entry instanceof String str) {
                    bindings.add(parseAbilityBinding(str, 1));
                } else if (entry instanceof java.util.Map<?, ?> m) {
                    // Map format: {invocation: "...", trigger: "...", min-level: N}
                    String invStr = m.get("invocation") != null ? m.get("invocation").toString() : "";
                    String trigStr = m.get("trigger") != null ? m.get("trigger").toString() : "";
                    int minLevel = m.get("min-level") instanceof Number n ? n.intValue() : 1;
                    String combined = trigStr.isEmpty() ? invStr : invStr + "~" + trigStr;
                    bindings.add(parseAbilityBinding(combined, minLevel));
                }
            } catch (Exception ex) {
                logger.warning("mob '" + id + "' has bad ability entry '" + entry + "': " + ex.getMessage());
            }
        }

        CoreLootTable lootTable = parseLootTable(id, s.getConfigurationSection("LootTable"));
        MobAiProfile aiProfile = parseAiProfile(s.getConfigurationSection("AI"));
        long xp = s.getLong("XP", 0);

        // LootPool: "id"  or  LootPools: [id1, id2]
        List<String> lootPoolIds = new ArrayList<>();
        String singlePool = s.getString("LootPool");
        if (singlePool != null && !singlePool.isBlank()) {
            lootPoolIds.add(singlePool.trim());
        }
        List<String> multiPools = s.getStringList("LootPools");
        for (String pid : multiPools) {
            String trimmed = pid.trim();
            if (!trimmed.isBlank() && !lootPoolIds.contains(trimmed)) lootPoolIds.add(trimmed);
        }

        return new CoreRpgMob(id, displayName, type, health, damage, defense,
                stats, helmet, chest, legs, boots, hand, off, null, bindings, lootTable,
                lootPoolIds, aiProfile, xp, mobIdKey, healthService);
    }

    private MobAiProfile parseAiProfile(ConfigurationSection s) {
        if (s == null) return MobAiProfile.DEFAULT;
        MobAiProfile.Kind kind = MobAiProfile.parseKind(s.getString("profile", "aggressive"));
        double aggro = s.getDouble("aggression-range", 16);
        double attack = s.getDouble("attack-range", 2);
        double leash = s.getDouble("leash-range", 32);
        boolean immune = s.getBoolean("immune-to-knockback", false);
        return new MobAiProfile(kind, aggro, attack, leash, immune);
    }

    private static MobAbilityBinding parseAbilityBinding(String spec, int minLevel) {
        int triggerIdx = spec.indexOf('~');
        String invStr = triggerIdx < 0 ? spec : spec.substring(0, triggerIdx).trim();
        MobAbilityTrigger trigger;
        if (triggerIdx < 0) {
            trigger = new MobAbilityTrigger.OnSpawn();
        } else {
            trigger = MobAbilityTrigger.parse(spec.substring(triggerIdx + 1));
        }
        List<AbilityInvocation> invocations = AbilityDsl.parse(invStr);
        return new MobAbilityBinding(invocations, trigger, minLevel);
    }

    private CoreLootTable parseLootTable(String mobId, ConfigurationSection s) {
        if (s == null) return null;
        Attribution attribution = parseAttribution(s.getString("attribution", "weighted-by-damage"));
        RollMode rollMode = parseRollMode(s.getString("roll-mode", "per-player"));
        int vanillaExp  = s.getInt("exp", 0);
        long combatExp  = s.getLong("combat-exp", 0L);
        String combatSkill = s.getString("combat-skill", "combat");

        List<CoreLootTable.Roll> rolls = new ArrayList<>();
        for (Map<?, ?> raw : s.getMapList("rolls")) {
            try {
                String item = String.valueOf(raw.get("item"));
                double chance = raw.get("chance") instanceof Number n ? n.doubleValue() : 0;
                int min = raw.get("min") instanceof Number n ? n.intValue() : 1;
                int max = raw.get("max") instanceof Number n ? n.intValue() : min;
                boolean mf = Boolean.TRUE.equals(raw.get("magic-find-affected"));
                rolls.add(new CoreLootTable.Roll(item, chance, min, max, mf));
            } catch (Exception ex) {
                logger.warning("mob '" + mobId + "' loot roll bad: " + ex.getMessage());
            }
        }
        List<CoreLootTable.Guaranteed> guaranteed = new ArrayList<>();
        for (Map<?, ?> raw : s.getMapList("guaranteed")) {
            try {
                String item = String.valueOf(raw.get("item"));
                int min = raw.get("min") instanceof Number n ? n.intValue() : 1;
                int max = raw.get("max") instanceof Number n ? n.intValue() : min;
                guaranteed.add(new CoreLootTable.Guaranteed(item, min, max));
            } catch (Exception ex) {
                logger.warning("mob '" + mobId + "' guaranteed bad: " + ex.getMessage());
            }
        }

        List<CoreLootTable.CurrencyRoll> currencyRolls = new ArrayList<>();
        for (Map<?, ?> raw : s.getMapList("currency-rolls")) {
            try {
                double chance = raw.get("chance") instanceof Number n ? n.doubleValue() : 0;
                long min = raw.get("min") instanceof Number n ? n.longValue() : 1;
                long max = raw.get("max") instanceof Number n ? n.longValue() : min;
                currencyRolls.add(new CoreLootTable.CurrencyRoll(chance, min, max));
            } catch (Exception ex) {
                logger.warning("mob '" + mobId + "' currency-roll bad: " + ex.getMessage());
            }
        }

        return new CoreLootTable(mobId, attribution, rollMode, rolls, guaranteed, currencyRolls,
                vanillaExp, combatExp, combatSkill);
    }

    private static Attribution parseAttribution(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "last-hit", "last_hit" -> Attribution.LAST_HIT;
            case "top-damager", "top_damager" -> Attribution.TOP_DAMAGER;
            case "split-equal", "split_equal" -> Attribution.SPLIT_EQUAL;
            default -> Attribution.WEIGHTED_BY_DAMAGE;
        };
    }

    private static RollMode parseRollMode(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "shared" -> RollMode.SHARED;
            default -> RollMode.PER_PLAYER;
        };
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
