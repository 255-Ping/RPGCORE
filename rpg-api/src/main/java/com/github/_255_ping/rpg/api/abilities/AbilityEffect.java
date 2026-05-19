package com.github._255_ping.rpg.api.abilities;

import java.util.concurrent.CompletableFuture;

public interface AbilityEffect {
    String name();
    CompletableFuture<AbilityContext> apply(AbilityContext ctx);
}
