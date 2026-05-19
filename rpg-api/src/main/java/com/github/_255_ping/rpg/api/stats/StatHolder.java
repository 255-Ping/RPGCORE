package com.github._255_ping.rpg.api.stats;

import java.util.Map;

public interface StatHolder {
    double get(Stat stat);
    Map<Stat, Double> snapshot();
}
