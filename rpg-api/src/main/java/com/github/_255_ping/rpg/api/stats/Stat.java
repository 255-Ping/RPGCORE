package com.github._255_ping.rpg.api.stats;

public sealed interface Stat permits BuiltinStat, CustomStat {
    String id();
    String displayName();
    String colorCode();
    boolean percent();
}
