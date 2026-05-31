package com.github._255_ping.rpg.enchanting;

import java.util.List;
import java.util.Map;

public record UpgradeDef(
        String id,
        String displayName,
        List<String> appliesTo,
        int maxTier,
        double currencyCost,
        int requiredSkillLevel,
        Map<String, Double> statsPerTier,
        /** Optional RPG item id the player must have in inventory when applying. Null = no reagent. */
        String reagent
) {}
