package com.github._255_ping.rpg.api.stats;

public record CustomStat(String id, String displayName, String colorCode, boolean percent) implements Stat {}
