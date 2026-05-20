package com.github._255_ping.rpg.quests;

import java.util.List;
import java.util.Map;

public record QuestDef(
        String id,
        String displayName,
        List<String> description,
        int requiredLevel,
        List<QuestObjective> objectives,
        Map<String, Long> xpRewards,
        double currencyReward,
        List<ItemReward> itemRewards
) {
    public record ItemReward(String itemId, int amount) {}
}
