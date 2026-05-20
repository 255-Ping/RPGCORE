package com.github._255_ping.rpg.alchemy;

import java.util.List;

public record BrewRecipeDef(
        String id,
        List<Ingredient> inputs,
        Ingredient output,
        int brewTicks,
        int requiredLevel
) {
    public record Ingredient(String itemId, int amount) {}
}
