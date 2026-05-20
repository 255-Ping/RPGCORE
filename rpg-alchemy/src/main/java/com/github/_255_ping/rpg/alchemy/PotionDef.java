package com.github._255_ping.rpg.alchemy;

import java.util.List;

public record PotionDef(
        String id,
        String displayName,
        int customModelData,
        List<EffectSpec> effects,
        boolean consumeOnDrink
) {
    public record EffectSpec(String id, int level, int durationTicks) {}
}
