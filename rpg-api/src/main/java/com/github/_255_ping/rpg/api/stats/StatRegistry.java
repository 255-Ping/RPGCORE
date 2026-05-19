package com.github._255_ping.rpg.api.stats;

import java.util.Collection;
import java.util.Optional;

public interface StatRegistry {
    void register(Stat stat);
    Optional<Stat> get(String id);
    Collection<Stat> all();
}
