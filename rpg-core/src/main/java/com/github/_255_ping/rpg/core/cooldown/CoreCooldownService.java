package com.github._255_ping.rpg.core.cooldown;

import com.github._255_ping.rpg.api.cooldown.CooldownService;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CoreCooldownService implements CooldownService {

    private record Key(UUID player, String key) {}

    private static final long MS_PER_TICK = 50L;

    private final ConcurrentHashMap<Key, Long> expirations = new ConcurrentHashMap<>();

    @Override
    public boolean isOnCooldown(UUID playerId, String key) {
        Long expiry = expirations.get(new Key(playerId, key));
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            expirations.remove(new Key(playerId, key));
            return false;
        }
        return true;
    }

    @Override
    public long remainingTicks(UUID playerId, String key) {
        Long expiry = expirations.get(new Key(playerId, key));
        if (expiry == null) return 0;
        long remainingMs = expiry - System.currentTimeMillis();
        return Math.max(0L, remainingMs / MS_PER_TICK);
    }

    @Override
    public void set(UUID playerId, String key, long ticks) {
        long expiry = System.currentTimeMillis() + ticks * MS_PER_TICK;
        expirations.put(new Key(playerId, key), expiry);
    }

    @Override
    public void clear(UUID playerId, String key) {
        expirations.remove(new Key(playerId, key));
    }
}
