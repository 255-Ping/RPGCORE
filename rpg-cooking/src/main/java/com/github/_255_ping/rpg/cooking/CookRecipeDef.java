package com.github._255_ping.rpg.cooking;

import java.util.List;

public record CookRecipeDef(
        String id,
        List<Ingredient> inputs,
        Ingredient output,
        int cookTicks,
        int requiredLevel
) {
    public record Ingredient(String itemId, int amount) {}
}
