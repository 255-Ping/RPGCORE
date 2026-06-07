package com.github._255_ping.rpg.alchemy;

import java.util.List;

public record PotionDef(
        String id,
        String displayName,
        int customModelData,
        List<EffectSpec> effects,
        boolean consumeOnDrink,
        /**
         * Item to give back after drinking. {@code null} means "fall through to the global
         * {@code drink-return.item} config value". {@code "none"} means nothing is given.
         * Any other value is tried as an RPG item id first, then as a vanilla
         * {@link org.bukkit.Material} name (case-insensitive).
         */
        String returnItem
) {
    public record EffectSpec(String id, int level, int durationTicks) {}
}
