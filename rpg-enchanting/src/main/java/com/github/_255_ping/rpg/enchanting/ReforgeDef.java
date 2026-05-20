package com.github._255_ping.rpg.enchanting;

import java.util.List;
import java.util.Map;

public record ReforgeDef(
        String id,
        String displayName,
        List<String> appliesTo,
        double currencyCost,
        int requiredSkillLevel,
        Map<String, Map<String, Double>> statsByRarity
) {
    public Map<String, Double> statsFor(String rarityId) {
        return statsByRarity.getOrDefault(rarityId, Map.of());
    }
}
