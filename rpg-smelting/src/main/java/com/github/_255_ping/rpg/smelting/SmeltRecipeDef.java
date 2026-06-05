package com.github._255_ping.rpg.smelting;

/**
 * Immutable definition of a single smelting recipe.
 *
 * <p>Used by both the custom {@link SmeltingGui} (timed station) and the
 * {@link FurnaceRecipeLoader} (vanilla furnace registration).
 */
public record SmeltRecipeDef(
        String     id,
        Ingredient input,
        Ingredient output,
        int        smeltTicks,    // 0 = instant
        int        requiredLevel,
        float      vanillaXp      // XP orbs for vanilla FurnaceRecipe
) {
    public record Ingredient(String itemId, int amount) {}
}
