package com.github._255_ping.rpg.enchanting;

import java.util.List;
import java.util.Map;

public record EnchantDef(
        String id,
        String displayName,
        /** Short description lines shown in item lore below the enchant name. May be empty. */
        List<String> description,
        int maxLevel,
        List<String> appliesTo,
        Map<String, Double> baseStats,
        double scalePerLevel,
        long xpCost,
        double currencyCost,
        int requiredSkillLevel
) {
    public Map<String, Double> statsAtLevel(int level) {
        if (level <= 0) return Map.of();
        double mult = 1.0 + (level - 1) * scalePerLevel;
        java.util.Map<String, Double> out = new java.util.HashMap<>();
        for (Map.Entry<String, Double> e : baseStats.entrySet()) {
            out.put(e.getKey(), e.getValue() * mult);
        }
        return out;
    }
}
