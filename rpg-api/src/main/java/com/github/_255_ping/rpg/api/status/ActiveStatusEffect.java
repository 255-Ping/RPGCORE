package com.github._255_ping.rpg.api.status;

public record ActiveStatusEffect(
        String effectId,
        int level,
        int remainingTicks,
        String sourceId
) {}
