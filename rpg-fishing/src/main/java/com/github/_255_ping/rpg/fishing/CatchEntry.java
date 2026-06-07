package com.github._255_ping.rpg.fishing;

/**
 * A single entry in a {@link CatchTable}: an item with a roll chance, amount range,
 * and a flag indicating whether the {@code FISHING_FORTUNE} stat boosts its chance.
 */
public record CatchEntry(
        String itemId,
        double chance,
        int min,
        int max,
        boolean fortuneAffected
) {}
