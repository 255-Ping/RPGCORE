package com.github._255_ping.rpg.api.stats;

public enum BuiltinStat implements Stat {
    DAMAGE("damage", "Damage", "&c", false),
    STRENGTH("strength", "Strength", "&c", false),
    CRIT_CHANCE("crit_chance", "Crit Chance", "&9", true),
    CRIT_DAMAGE("crit_damage", "Crit Damage", "&9", true),
    SPEED("speed", "Speed", "&f", false),
    MAX_HEALTH("max_health", "Health", "&c", false),
    MAX_MANA("max_mana", "Mana", "&b", false),
    MANA_REGEN("mana_regen", "Mana Regen", "&b", false),
    DEFENSE("defense", "Defense", "&a", false),
    TRUE_DEFENSE("true_defense", "True Defense", "&f", false),
    LIFESTEAL("lifesteal", "Lifesteal", "&c", true);

    private final String id;
    private final String displayName;
    private final String colorCode;
    private final boolean percent;

    BuiltinStat(String id, String displayName, String colorCode, boolean percent) {
        this.id = id;
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.percent = percent;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return displayName; }
    @Override public String colorCode() { return colorCode; }
    @Override public boolean percent() { return percent; }
}
