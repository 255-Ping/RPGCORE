package com.github._255_ping.rpg.core.stats;

import com.github._255_ping.rpg.api.stats.Stat;
import com.github._255_ping.rpg.api.stats.StatHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MutableStatHolder implements StatHolder {

    private final Map<Stat, Double> values = new ConcurrentHashMap<>();

    @Override
    public double get(Stat stat) {
        return values.getOrDefault(stat, 0.0);
    }

    @Override
    public Map<Stat, Double> snapshot() {
        return new HashMap<>(values);
    }

    public void set(Stat stat, double value) {
        values.put(stat, value);
    }

    public void clear() {
        values.clear();
    }
}
