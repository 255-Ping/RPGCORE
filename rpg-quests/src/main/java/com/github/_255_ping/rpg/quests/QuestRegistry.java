package com.github._255_ping.rpg.quests;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class QuestRegistry {

    private final File dir;
    private final Logger logger;
    private final Map<String, QuestDef> byId = new ConcurrentHashMap<>();

    public QuestRegistry(File dir, Logger logger) {
        this.dir = dir;
        this.logger = logger;
    }

    public Optional<QuestDef> get(String id) {
        return Optional.ofNullable(byId.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<QuestDef> all() { return byId.values(); }

    public void reload() {
        byId.clear();
        if (!dir.isDirectory()) return;
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
                for (String id : y.getKeys(false)) {
                    ConfigurationSection s = y.getConfigurationSection(id);
                    if (s == null) continue;
                    try {
                        byId.put(id.toLowerCase(Locale.ROOT), parse(id.toLowerCase(Locale.ROOT), s));
                    } catch (Exception ex) {
                        logger.warning("Skipping quest '" + id + "' in " + f.getName() + ": " + ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                logger.warning("Failed to parse " + f.getName() + ": " + ex.getMessage());
            }
        }
    }

    private QuestDef parse(String id, ConfigurationSection s) {
        String name = s.getString("DisplayName", id);
        List<String> desc = s.getStringList("Description");
        int requiredLevel = s.getInt("RequiredLevel", 0);

        List<QuestObjective> objectives = new ArrayList<>();
        for (Object o : s.getList("Objectives", List.of())) {
            if (o instanceof Map<?, ?> m) {
                QuestObjective.Type type = QuestObjective.Type.fromString(String.valueOf(m.get("Type")));
                String target = String.valueOf(m.get("Target")).toLowerCase(Locale.ROOT);
                int count = m.get("Count") instanceof Number n ? n.intValue() : 1;
                objectives.add(new QuestObjective(type, target, count));
            }
        }

        Map<String, Long> xp = new LinkedHashMap<>();
        ConfigurationSection rewards = s.getConfigurationSection("Rewards");
        double currency = 0;
        List<QuestDef.ItemReward> items = new ArrayList<>();
        if (rewards != null) {
            ConfigurationSection xpSec = rewards.getConfigurationSection("Xp");
            if (xpSec != null) {
                for (String skill : xpSec.getKeys(false)) {
                    xp.put(skill.toLowerCase(Locale.ROOT), xpSec.getLong(skill));
                }
            }
            currency = rewards.getDouble("Currency", 0);
            for (Object o : rewards.getList("Items", List.of())) {
                if (o instanceof Map<?, ?> m) {
                    Object amt = m.get("Amount");
                    items.add(new QuestDef.ItemReward(
                            String.valueOf(m.get("Item")),
                            amt instanceof Number n ? n.intValue() : 1));
                }
            }
        }

        return new QuestDef(id, name, desc, requiredLevel, objectives, xp, currency, items);
    }

    @SuppressWarnings("unused")
    Map<String, QuestDef> snapshot() { return new HashMap<>(byId); }
}
