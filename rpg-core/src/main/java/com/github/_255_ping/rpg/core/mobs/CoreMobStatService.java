package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.mobs.MobStatService;
import com.github._255_ping.rpg.api.stats.Stat;
import com.github._255_ping.rpg.api.stats.StatHolder;
import com.github._255_ping.rpg.core.stats.MutableStatHolder;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory map of entity UUID → {@link MutableStatHolder}.
 * Thread-safe via ConcurrentHashMap; stat reads are lock-free.
 */
public final class CoreMobStatService implements MobStatService {

    /** Returned for any entity that has no registered holder — avoids null checks everywhere. */
    private static final StatHolder EMPTY = new StatHolder() {
        @Override public double get(Stat stat) { return 0.0; }
        @Override public Map<Stat, Double> snapshot() { return Map.of(); }
    };

    private final ConcurrentHashMap<UUID, MutableStatHolder> holders = new ConcurrentHashMap<>();

    @Override
    public StatHolder forMob(LivingEntity entity) {
        MutableStatHolder h = holders.get(entity.getUniqueId());
        return h != null ? h : EMPTY;
    }

    @Override
    public void register(LivingEntity entity, Map<Stat, Double> stats) {
        MutableStatHolder holder = new MutableStatHolder();
        stats.forEach(holder::set);
        holders.put(entity.getUniqueId(), holder);
    }

    @Override
    public void unregister(UUID uuid) {
        holders.remove(uuid);
    }
}
