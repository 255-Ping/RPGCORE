package com.github._255_ping.rpg.api.stats;

import java.util.Map;

public interface StatHolder {
    double get(Stat stat);
    Map<Stat, Double> snapshot();

    /**
     * Add a flat contribution to this stat. Used by addons that want to inject stats
     * during {@link StatRecalcEvent}. Default throws — only mutable impls support it.
     */
    default void add(Stat stat, double amount) {
        throw new UnsupportedOperationException("StatHolder is not mutable");
    }

    /**
     * Multiply this stat by {@code (1 + percent/100)}. Use after all flats during a recalc.
     * Default throws — only mutable impls support it.
     */
    default void multiply(Stat stat, double percent) {
        throw new UnsupportedOperationException("StatHolder is not mutable");
    }
}
