package com.github._255_ping.rpg.api.status;

public interface StatusEffect {
    String id();
    String displayName();
    Category category();
    StackingStrategy stacking();
    boolean hidden();

    enum Category { BUFF, DEBUFF, NEUTRAL }
}
