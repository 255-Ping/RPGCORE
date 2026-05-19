package com.github._255_ping.rpg.api.cooldown;

import java.util.UUID;

public interface CooldownService {
    boolean isOnCooldown(UUID playerId, String key);
    long remainingTicks(UUID playerId, String key);
    void set(UUID playerId, String key, long ticks);
    void clear(UUID playerId, String key);
}
