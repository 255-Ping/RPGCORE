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

    @Override
    public void add(Stat stat, double amount) {
        values.merge(stat, amount, Double::sum);
    }

    @Override
    public void multiply(Stat stat, double percent) {
        double cur = values.getOrDefault(stat, 0.0);
        values.put(stat, cur * (1.0 + percent / 100.0));
    }

    public void clear() {
        values.clear();
    }
}
