package com.github._255_ping.rpg.core.status;

import com.github._255_ping.rpg.api.stats.Stat;

public record StatModifier(Stat stat, Kind kind, double value) {

    public enum Kind { FLAT, PERCENT }
}
