package com.github._255_ping.rpg.quests;

import java.util.List;
import java.util.Map;

public record QuestDef(
        String id,
        String displayName,
        List<String> description,
        int requiredLevel,
        /** Quest IDs that must be in the player's completed list before this quest can be accepted. */
        List<String> requires,
        /** If true, the quest can be re-accepted after {@link #cooldownSeconds} has elapsed. */
        boolean repeatable,
        /** Seconds after completion before a repeatable quest can be accepted again. 0 = immediate. */
        long cooldownSeconds,
        List<QuestObjective> objectives,
        Map<String, Long> xpRewards,
        double currencyReward,
        List<ItemReward> itemRewards
) {
    public record ItemReward(String itemId, int amount) {}
}
