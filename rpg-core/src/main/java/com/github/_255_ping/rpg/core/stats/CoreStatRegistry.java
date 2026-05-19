package com.github._255_ping.rpg.core.stats;

import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.api.stats.Stat;
import com.github._255_ping.rpg.api.stats.StatRegistry;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CoreStatRegistry implements StatRegistry {

    private final ConcurrentMap<String, Stat> byId = new ConcurrentHashMap<>();

    public CoreStatRegistry() {
        for (BuiltinStat s : BuiltinStat.values()) {
            byId.put(s.id(), s);
        }
    }

    @Override
    public void register(Stat stat) {
        byId.put(stat.id(), stat);
    }

    @Override
    public Optional<Stat> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Collection<Stat> all() {
        return byId.values();
    }
}
