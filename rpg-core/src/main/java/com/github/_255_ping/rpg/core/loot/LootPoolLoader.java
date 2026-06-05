package com.github._255_ping.rpg.core.loot;

import com.github._255_ping.rpg.api.loot.Attribution;
import com.github._255_ping.rpg.api.loot.RollMode;
import com.github._255_ping.rpg.core.mobs.CoreLootTable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads all {@code *.yml} files under {@code plugins/rpg-core/loot-pools/} and registers
 * the resulting {@link CoreLootPool} instances in a {@link CoreLootPoolRegistry}.
 *
 * <p>Pool YAML format (same style as the inline {@code LootTable:} block on mobs, with the
 * pool ID as the top-level key):
 * <pre>
 * goblin_drops:
 *   attribution: last-hit       # last-hit | top-damager | split-equal | weighted-by-damage
 *   roll-mode: per-player       # per-player | shared
 *   exp: 15                     # vanilla XP orbs spawned at corpse (optional, default 0)
 *   combat-exp: 50              # skill XP awarded to each eligible player (optional, default 0)
 *   combat-skill: combat        # skill ID — defaults to "combat"
 *   rolls:
 *     - { item: goblin_fang, chance: 60.0, min: 1, max: 2 }
 *     - { item: gold_nugget, chance: 40.0, min: 1, max: 5, magic-find-affected: true }
 *   guaranteed:
 *     - { item: bone, min: 1, max: 1 }
 *   currency-rolls:
 *     - { chance: 100.0, min: 20, max: 50 }
 * </pre>
 */
public final class LootPoolLoader {

    private final File folder;
    private final CoreLootPoolRegistry registry;
    private final Logger logger;

    public LootPoolLoader(File folder, CoreLootPoolRegistry registry, Logger logger) {
        this.folder = folder;
        this.registry = registry;
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
                logger.warning("Failed to parse loot-pools file " + f.getName() + ": " + ex.getMessage());
            }
        }
        logger.info("Loaded " + registry.all().size() + " loot pool(s).");
    }

    private void loadFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection s = yaml.getConfigurationSection(id);
            if (s == null) {
                logger.warning("loot-pool '" + id + "' in " + file.getName() + " is not a section, skipping");
                continue;
            }
            try {
                CoreLootPool pool = parse(id, s);
                registry.register(pool);
            } catch (Exception ex) {
                logger.warning("Skipping loot-pool '" + id + "' in " + file.getName() + ": " + ex.getMessage());
            }
        }
    }

    private CoreLootPool parse(String id, ConfigurationSection s) {
        Attribution attribution = parseAttribution(s.getString("attribution", "weighted-by-damage"));
        RollMode rollMode       = parseRollMode(s.getString("roll-mode", "per-player"));
        int vanillaExp          = s.getInt("exp", 0);
        long combatExp          = s.getLong("combat-exp", 0L);
        String combatSkill      = s.getString("combat-skill", "combat");

        List<CoreLootTable.Roll> rolls = new ArrayList<>();
        for (Map<?, ?> raw : s.getMapList("rolls")) {
            try {
                String item   = String.valueOf(raw.get("item"));
                double chance = raw.get("chance") instanceof Number n ? n.doubleValue() : 0;
                int min       = raw.get("min") instanceof Number n ? n.intValue() : 1;
                int max       = raw.get("max") instanceof Number n ? n.intValue() : min;
                boolean mf    = Boolean.TRUE.equals(raw.get("magic-find-affected"));
                rolls.add(new CoreLootTable.Roll(item, chance, min, max, mf));
            } catch (Exception ex) {
                logger.warning("loot-pool '" + id + "' roll bad: " + ex.getMessage());
            }
        }

        List<CoreLootTable.Guaranteed> guaranteed = new ArrayList<>();
        for (Map<?, ?> raw : s.getMapList("guaranteed")) {
            try {
                String item = String.valueOf(raw.get("item"));
                int min     = raw.get("min") instanceof Number n ? n.intValue() : 1;
                int max     = raw.get("max") instanceof Number n ? n.intValue() : min;
                guaranteed.add(new CoreLootTable.Guaranteed(item, min, max));
            } catch (Exception ex) {
                logger.warning("loot-pool '" + id + "' guaranteed bad: " + ex.getMessage());
            }
        }

        List<CoreLootTable.CurrencyRoll> currencyRolls = new ArrayList<>();
        for (Map<?, ?> raw : s.getMapList("currency-rolls")) {
            try {
                double chance = raw.get("chance") instanceof Number n ? n.doubleValue() : 0;
                long min      = raw.get("min") instanceof Number n ? n.longValue() : 1;
                long max      = raw.get("max") instanceof Number n ? n.longValue() : min;
                currencyRolls.add(new CoreLootTable.CurrencyRoll(chance, min, max));
            } catch (Exception ex) {
                logger.warning("loot-pool '" + id + "' currency-roll bad: " + ex.getMessage());
            }
        }

        return new CoreLootPool(id, attribution, rollMode, rolls, guaranteed, currencyRolls,
                vanillaExp, combatExp, combatSkill);
    }

    private static Attribution parseAttribution(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "last-hit", "last_hit"           -> Attribution.LAST_HIT;
            case "top-damager", "top_damager"     -> Attribution.TOP_DAMAGER;
            case "split-equal", "split_equal"     -> Attribution.SPLIT_EQUAL;
            default                               -> Attribution.WEIGHTED_BY_DAMAGE;
        };
    }

    private static RollMode parseRollMode(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "shared" -> RollMode.SHARED;
            default       -> RollMode.PER_PLAYER;
        };
    }
}
