package com.github._255_ping.rpg.core.achievement;

import com.github._255_ping.rpg.api.achievement.AchievementDef;
import com.github._255_ping.rpg.api.achievement.AchievementReward;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Loads {@link AchievementDef} instances from YAML files in {@code achievements/}.
 *
 * <p>Each top-level key in a file is an achievement ID. Fields:
 * <pre>
 *   Title:       Short display name
 *   Description: How-to-unlock hint
 *   Category:    Grouping label (Combat, Economy, …)
 *   Icon:        Material name for GUI icon (default: BOOK)
 *   Trigger:     MANUAL | COUNTER (default: MANUAL)
 *   Counter:     counter key (required when Trigger: COUNTER)
 *   Target:      counter threshold (required when Trigger: COUNTER)
 *   Reward:
 *     Money: 1000
 *     XP:    500
 * </pre>
 */
public final class AchievementLoader {

    private final File folder;
    private final Logger logger;

    public AchievementLoader(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public List<AchievementDef> loadAll() {
        List<AchievementDef> defs = new ArrayList<>();
        if (!folder.isDirectory()) return defs;
        File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return defs;
        for (File f : files) {
            try {
                defs.addAll(loadFile(f));
            } catch (Exception ex) {
                logger.warning("[achievements] Failed to parse " + f.getName() + ": " + ex.getMessage());
            }
        }
        return defs;
    }

    private List<AchievementDef> loadFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<AchievementDef> defs = new ArrayList<>();
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection s = yaml.getConfigurationSection(id);
            if (s == null) {
                logger.warning("[achievements] '" + id + "' in " + file.getName() + " is not a section");
                continue;
            }
            try {
                defs.add(parse(id, s));
            } catch (Exception ex) {
                logger.warning("[achievements] Skipping '" + id + "' in " + file.getName() + ": " + ex.getMessage());
            }
        }
        return defs;
    }

    private AchievementDef parse(String id, ConfigurationSection s) {
        String title       = s.getString("Title", id);
        String description = s.getString("Description", "");
        String category    = s.getString("Category", "General");
        String icon        = s.getString("Icon", "BOOK");
        String trigger     = s.getString("Trigger", "MANUAL").toUpperCase();
        String counterKey  = s.getString("Counter", null);
        long   target      = s.getLong("Target", 1L);

        if ("COUNTER".equals(trigger) && (counterKey == null || counterKey.isBlank())) {
            throw new IllegalArgumentException("COUNTER achievement '" + id + "' missing Counter key");
        }

        ConfigurationSection rewardSec = s.getConfigurationSection("Reward");
        AchievementReward reward = AchievementReward.NONE;
        if (rewardSec != null) {
            BigDecimal money = BigDecimal.valueOf(rewardSec.getDouble("Money", 0));
            long xp = rewardSec.getLong("XP", 0L);
            reward = AchievementReward.of(money, xp);
        }

        return new AchievementDef(id, title, description, category, icon, trigger, counterKey, target, reward);
    }
}
