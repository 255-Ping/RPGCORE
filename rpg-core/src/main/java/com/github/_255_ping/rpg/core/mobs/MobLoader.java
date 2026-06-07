package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityInvocation;
import com.github._255_ping.rpg.api.loot.Attribution;
import com.github._255_ping.rpg.api.loot.RollMode;
import com.github._255_ping.rpg.api.mobs.BossBarDef;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.api.stats.Stat;
import com.github._255_ping.rpg.core.health.CoreHealthService;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import com.github._255_ping.rpg.api.mobs.EliteDef;
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

        // LootTable: can be an inline section or a deprecated plain-string pool reference.
        // Detect which case we're in so getConfigurationSection() doesn't silently swallow the value.
        CoreLootTable lootTable;
        if (s.isString("LootTable")) {
            String poolId = s.getString("LootTable");
            logger.warning("mob '" + id + "' uses deprecated 'LootTable: " + poolId
                    + "' — use 'LootPool: " + poolId + "' instead.");
            if (poolId != null && !poolId.isBlank()) {
                String trimmed = poolId.trim();
                if (!lootPoolIds.contains(trimmed)) lootPoolIds.add(trimmed);
            }
            lootTable = null;
        } else {
            lootTable = parseLootTable(id, s.getConfigurationSection("LootTable"));
        }
        MobAiProfile aiProfile = parseAiProfile(s.getConfigurationSection("AI"));
        String faction = s.getString("Faction");
        List<AiGoalDef> aiGoals = new ArrayList<>();
        for (Object entry : s.getList("AiGoals", List.of())) {
            if (!(entry instanceof String spec)) continue;
            AiGoalDef goal = parseAiGoal(id, spec.trim());
            if (goal != null) aiGoals.add(goal);
        }
        long xp = s.getLong("XP", 0);

        // Optional death animation
        org.bukkit.Particle deathParticle = null;
        String deathParticleStr = s.getString("DeathParticle");
        if (deathParticleStr != null && !deathParticleStr.isBlank()) {
            try { deathParticle = org.bukkit.Particle.valueOf(deathParticleStr.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) {
                logger.warning("mob '" + id + "' has unknown DeathParticle: " + deathParticleStr);
            }
        }
        int    deathParticleCount  = s.getInt("DeathParticleCount", 20);
        double deathParticleSpread = s.getDouble("DeathParticleSpread", 0.3);
        org.bukkit.Sound deathSound = null;
        String deathSoundStr = s.getString("DeathSound");
        if (deathSoundStr != null && !deathSoundStr.isBlank()) {
            try { deathSound = org.bukkit.Sound.valueOf(deathSoundStr.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) {
                logger.warning("mob '" + id + "' has unknown DeathSound: " + deathSoundStr);
            }
        }

        BossBarDef bossBarDef = parseBossBar(id, displayName, s.getConfigurationSection("BossBar"));
        EliteDef eliteDef    = parseEliteDef(id, s.getConfigurationSection("Elite"));

        return new CoreRpgMob(id, displayName, type, health, damage, defense,
                stats, helmet, chest, legs, boots, hand, off, null, bindings, lootTable,
                lootPoolIds, aiProfile, faction, aiGoals, xp,
                bossBarDef, eliteDef,
                deathParticle, deathParticleCount, deathParticleSpread, deathSound,
                mobIdKey, healthService);
    }

    private BossBarDef parseBossBar(String mobId, String mobDisplayName, ConfigurationSection s) {
        if (s == null) return null;

        // Name defaults to the mob's DisplayName with legacy colour codes stripped
        String name = s.getString("Name");
        if (name == null || name.isBlank()) {
            name = mobDisplayName != null
                    ? net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                            .legacyAmpersand().deserialize(mobDisplayName).toString()
                    : mobId;
        }

        BarColor color = BarColor.RED;
        String colorStr = s.getString("Color");
        if (colorStr != null) {
            try { color = BarColor.valueOf(colorStr.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) {
                logger.warning("mob '" + mobId + "' BossBar.Color unknown: " + colorStr);
            }
        }

        BarStyle style = BarStyle.SOLID;
        String styleStr = s.getString("Style");
        if (styleStr != null) {
            try { style = BarStyle.valueOf(styleStr.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) {
                logger.warning("mob '" + mobId + "' BossBar.Style unknown: " + styleStr);
            }
        }

        double range = s.getDouble("Range", BossBarDef.USE_CONFIG_RANGE);
        return new BossBarDef(name, color, style, range);
    }

    private EliteDef parseEliteDef(String mobId, ConfigurationSection s) {
        if (s == null) return null;
        double chance     = s.getDouble("Chance",          0.05);
        double hpMult     = s.getDouble("HpMultiplier",    3.0);
        double dmgMult    = s.getDouble("DamageMultiplier",1.5);
        double lootMult   = s.getDouble("LootMultiplier",  2.0);
        String prefix     = s.getString("Prefix",          "§6✦ Elite §r");
        boolean glow      = s.getBoolean("Glow",           true);
        return new EliteDef(
                Math.max(0.0, Math.min(1.0, chance)),
                Math.max(1.0, hpMult),
                Math.max(1.0, dmgMult),
                Math.max(1.0, lootMult),
                prefix,
                glow);
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

    /**
     * Parse one goal entry from the {@code AiGoals:} list.
     * Format: {@code goal_name} or {@code goal_name{key=value,...}}.
     * Returns {@code null} for unknown goal names (with a warning logged).
     */
    private AiGoalDef parseAiGoal(String mobId, String spec) {
        int brace = spec.indexOf('{');
        String name = (brace < 0 ? spec : spec.substring(0, brace)).trim().toLowerCase(java.util.Locale.ROOT);
        java.util.Map<String, String> p = new java.util.HashMap<>();
        if (brace >= 0) {
            int end = spec.lastIndexOf('}');
            String inside = (end > brace) ? spec.substring(brace + 1, end) : spec.substring(brace + 1);
            for (String kv : inside.split(",")) {
                String[] pair = kv.split("=", 2);
                if (pair.length == 2) p.put(pair[0].trim(), pair[1].trim());
            }
        }
        return switch (name) {
            case "attack_player"   -> new AiGoalDef.AttackPlayer();
            case "attack_faction"  -> new AiGoalDef.AttackFaction(
                    p.getOrDefault("faction", ""), dbl(p.get("range"), 0));
            case "defend_faction"  -> new AiGoalDef.DefendFaction(
                    p.getOrDefault("faction", ""), dbl(p.get("radius"), 20));
            case "assist_faction"  -> new AiGoalDef.AssistFaction(
                    p.getOrDefault("faction", ""), dbl(p.get("radius"), 20));
            case "flee_from"       -> new AiGoalDef.FleeFrom(
                    p.getOrDefault("faction", ""), dbl(p.get("range"), 16),
                    dbl(p.get("health_threshold"), 100));
            case "call_for_help"   -> new AiGoalDef.CallForHelp(
                    p.getOrDefault("faction", ""), dbl(p.get("radius"), 20));
            case "guard_radius"    -> new AiGoalDef.GuardRadius(dbl(p.get("radius"), 32));
            case "idle"            -> new AiGoalDef.Idle();
            default -> {
                logger.warning("mob '" + mobId + "' has unknown AiGoal: " + name);
                yield null;
            }
        };
    }

    private static double dbl(String s, double def) {
        if (s == null) return def;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return def; }
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
