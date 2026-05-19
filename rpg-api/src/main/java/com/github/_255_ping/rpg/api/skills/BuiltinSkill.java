package com.github._255_ping.rpg.api.skills;

public enum BuiltinSkill implements Skill {
    COMBAT("combat", "Combat"),
    MINING("mining", "Mining"),
    FORAGING("foraging", "Foraging"),
    FARMING("farming", "Farming"),
    FISHING("fishing", "Fishing"),
    COOKING("cooking", "Cooking"),
    ALCHEMY("alchemy", "Alchemy"),
    ENCHANTING("enchanting", "Enchanting");

    private final String id;
    private final String displayName;

    BuiltinSkill(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return displayName; }
}
