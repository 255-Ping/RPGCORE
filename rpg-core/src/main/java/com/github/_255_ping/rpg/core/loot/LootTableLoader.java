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
 * Loads all {@code *.yml} files under {@code plugins/rpg-core/loot-tables/} and registers
 * the resulting {@link CoreLootTable} instances in a {@link CoreLootTableRegistry}.
 *
 * <p>Mobs can then reference a registered table by name instead of embedding the full
 * section inline:
 * <pre>
 * LootTable: my_table_id
 * </pre>
 *
 * <p>File format (same schema as inline mob {@code LootTable:} sections, with the table
 * ID as the top-level key):
 * <pre>
 * forest_common:
 *   attribution: weighted-by-damage   # last-hit | top-damager | split-equal | weighted-by-damage
 *   roll-mode: per-player             # per-player | shared
 *   exp: 10                           # vanilla XP orbs (optional, default 0)
 *   combat-exp: 30                    # skill XP per eligible player (optional, default 0)
 *   combat-skill: combat              # skill ID — defaults to "combat"
 *   rolls:
 *     - { item: stick,   chance: 50.0, min: 1, max: 3 }
 *     - { item: leather, chance: 25.0, min: 1, max: 1, magic-find-affected: true }
 *   guaranteed:
 *     - { item: bone, min: 1, max: 1 }
 *   currency-rolls:
 *     - { chance: 100.0, min: 5, max: 20 }
 * </pre>
 */
public final class LootTableLoader {

    private final File folder;
    private final CoreLootTableRegistry registry;
    private final Logger logger;

    public LootTableLoader(File folder, CoreLootTableRegistry registry, Logger logger) {
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
                logger.warning("Failed to parse loot-tables file " + f.getName() + ": " + ex.getMessage());
            }
        }
        logger.info("Loaded " + registry.all().size() + " loot table(s) from loot-tables/.");
    }

    private void loadFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection s = yaml.getConfigurationSection(id);
            if (s == null) {
                logger.warning("loot-table '" + id + "' in " + file.getName() + " is not a section, skipping");
                continue;
            }
            try {
                CoreLootTable table = parse(id, s);
                registry.register(table);
            } catch (Exception ex) {
                logger.warning("Skipping loot-table '" + id + "' in " + file.getName() + ": " + ex.getMessage());
            }
        }
    }

    private CoreLootTable parse(String id, ConfigurationSection s) {
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
                logger.warning("loot-table '" + id + "' roll bad: " + ex.getMessage());
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
                logger.warning("loot-table '" + id + "' guaranteed bad: " + ex.getMessage());
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
                logger.warning("loot-table '" + id + "' currency-roll bad: " + ex.getMessage());
            }
        }

        return new CoreLootTable(id, attribution, rollMode, rolls, guaranteed, currencyRolls,
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
