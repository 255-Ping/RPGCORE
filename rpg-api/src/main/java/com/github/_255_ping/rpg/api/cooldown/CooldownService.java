package com.github._255_ping.rpg.api.cooldown;

import java.util.Map;
import java.util.UUID;

public interface CooldownService {
    boolean isOnCooldown(UUID playerId, String key);
    long remainingTicks(UUID playerId, String key);
    void set(UUID playerId, String key, long ticks);
    void clear(UUID playerId, String key);

    /**
     * Returns all currently active cooldowns for the player as
     * {@code key → remaining ticks}. Keys include any prefix (e.g. {@code "cooldown:ability"}).
     */
    Map<String, Long> active(UUID playerId);
}
