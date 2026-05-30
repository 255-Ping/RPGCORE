package com.github._255_ping.rpg.api.stats;

public enum BuiltinStat implements Stat {
    DAMAGE("damage", "Damage", "&c", false, "combat"),
    STRENGTH("strength", "Strength", "&c", false, "combat"),
    CRIT_CHANCE("crit_chance", "Crit Chance", "&9", true, "combat"),
    CRIT_DAMAGE("crit_damage", "Crit Damage", "&9", true, "combat"),
    ABILITY_DAMAGE("ability_damage", "Ability Damage", "&5", false, "combat"),
    ATTACK_SPEED("attack_speed", "Attack Speed", "&e", true, "combat"),
    FEROCITY("ferocity", "Ferocity", "&c", true, "combat"),
    LIFESTEAL("lifesteal", "Lifesteal", "&c", true, "combat"),
    KNOCKBACK("knockback", "Knockback", "&f", false, "combat"),
    AMMO_USAGE_REDUCTION("ammo_usage_reduction", "Ammo Usage Reduction", "&a", true, "combat"),
    PROJECTILE_SPEED("projectile_speed", "Projectile Speed", "&b", false, "combat"),

    MAX_HEALTH("max_health", "Health", "&c", false, "survival"),
    HEALTH_REGEN("health_regen", "Health Regen", "&c", false, "survival"),
    VITALITY("vitality", "Vitality", "&c", true, "survival"),
    DEFENSE("defense", "Defense", "&a", false, "survival"),
    TRUE_DEFENSE("true_defense", "True Defense", "&f", false, "survival"),

    MAX_MANA("max_mana", "Mana", "&b", false, "caster"),
    MANA_REGEN("mana_regen", "Mana Regen", "&b", false, "caster"),
    INTELLIGENCE("intelligence", "Intelligence", "&b", false, "caster"),
    COOLDOWN_REDUCTION("cooldown_reduction", "Cooldown Reduction", "&b", true, "caster"),

    SPEED("speed", "Speed", "&f", false, "mobility"),
    SWING_RANGE("swing_range", "Swing Range", "&f", false, "mobility"),

    MAGIC_FIND("magic_find", "Magic Find", "&b", true, "loot"),
    PRISTINE("pristine", "Pristine", "&5", true, "loot"),

    BREAKING_POWER("breaking_power", "Breaking Power", "&8", false, "gathering"),

    MINING_SPEED("mining_speed", "Mining Speed", "&6", false, "mining"),
    MINING_FORTUNE("mining_fortune", "Mining Fortune", "&6", true, "mining"),

    FORAGING_SPEED("foraging_speed", "Foraging Speed", "&2", false, "foraging"),
    FORAGING_FORTUNE("foraging_fortune", "Foraging Fortune", "&2", true, "foraging"),

    FARMING_FORTUNE("farming_fortune", "Farming Fortune", "&e", true, "farming"),

    FISHING_SPEED("fishing_speed", "Fishing Speed", "&3", false, "fishing"),
    FISHING_FORTUNE("fishing_fortune", "Fishing Fortune", "&3", true, "fishing"),
    SEA_CREATURE_CHANCE("sea_creature_chance", "Sea Creature Chance", "&3", true, "fishing"),

    COMBAT_WISDOM("combat_wisdom", "Combat Wisdom", "&3", true, "wisdom"),
    MINING_WISDOM("mining_wisdom", "Mining Wisdom", "&3", true, "wisdom"),
    FORAGING_WISDOM("foraging_wisdom", "Foraging Wisdom", "&3", true, "wisdom"),
    FARMING_WISDOM("farming_wisdom", "Farming Wisdom", "&3", true, "wisdom"),
    FISHING_WISDOM("fishing_wisdom", "Fishing Wisdom", "&3", true, "wisdom"),
    COOKING_WISDOM("cooking_wisdom", "Cooking Wisdom", "&3", true, "wisdom"),
    ALCHEMY_WISDOM("alchemy_wisdom", "Alchemy Wisdom", "&3", true, "wisdom"),
    ENCHANTING_WISDOM("enchanting_wisdom", "Enchanting Wisdom", "&3", true, "wisdom"),

    ENCHANTING_LUCK("enchanting_luck", "Enchanting Luck", "&d", true, "enchanting"),

    PET_LUCK("pet_luck", "Pet Luck", "&d", true, "pets");

    private final String id;
    private final String displayName;
    private final String colorCode;
    private final boolean percent;
    private final String group;

    BuiltinStat(String id, String displayName, String colorCode, boolean percent, String group) {
        this.id = id;
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.percent = percent;
        this.group = group;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return displayName; }
    @Override public String colorCode() { return colorCode; }
    @Override public boolean percent() { return percent; }
    @Override public String group() { return group; }
}
