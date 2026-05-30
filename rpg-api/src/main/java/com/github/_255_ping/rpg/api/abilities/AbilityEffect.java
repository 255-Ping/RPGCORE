package com.github._255_ping.rpg.api.abilities;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface AbilityEffect {
    String name();
    CompletableFuture<AbilityContext> apply(AbilityContext ctx);

    /** Optional display name shown in item lore. Defaults to the effect id. */
    default String displayName() { return name(); }

    /** Optional description lines shown below the ability name in item lore. */
    default List<String> description() { return List.of(); }
}
