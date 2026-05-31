package com.github._255_ping.rpg.enchanting;

import java.util.List;
import java.util.Map;

public record ReforgeDef(
        String id,
        String displayName,
        List<String> appliesTo,
        double currencyCost,
        int requiredSkillLevel,
        Map<String, Map<String, Double>> statsByRarity,
        /** Optional RPG item id the player must have in inventory when applying. Null = no reagent. */
        String reagent
) {
    public Map<String, Double> statsFor(String rarityId) {
        // Fall back to "common" if the exact rarity isn't listed, then to the first defined rarity.
        if (statsByRarity.containsKey(rarityId)) return statsByRarity.get(rarityId);
        if (statsByRarity.containsKey("common")) return statsByRarity.get("common");
        return statsByRarity.values().stream().findFirst().orElse(Map.of());
    }
}
