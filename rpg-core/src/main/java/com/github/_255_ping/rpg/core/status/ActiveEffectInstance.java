package com.github._255_ping.rpg.core.status;

final class ActiveEffectInstance {

    final String effectId;
    int level;
    long expiryMs;
    long lastTickMs;
    final String sourceId;

    ActiveEffectInstance(String effectId, int level, long expiryMs, String sourceId) {
        this.effectId = effectId;
        this.level = level;
        this.expiryMs = expiryMs;
        this.lastTickMs = 0;
        this.sourceId = sourceId;
    }
}
