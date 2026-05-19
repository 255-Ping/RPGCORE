package com.github._255_ping.rpg.api.abilities;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class AbilityPipeline {

    private final List<AbilityInvocation> invocations;

    public AbilityPipeline(List<AbilityInvocation> invocations) {
        this.invocations = List.copyOf(invocations);
    }

    public List<AbilityInvocation> invocations() {
        return invocations;
    }

    public CompletableFuture<AbilityContext> cast(AbilityContext initial, AbilityRegistry registry) {
        CompletableFuture<AbilityContext> chain = CompletableFuture.completedFuture(initial);
        for (AbilityInvocation inv : invocations) {
            chain = chain.thenCompose(ctx -> registry.build(inv.effectName(), inv.params()).apply(ctx));
        }
        return chain;
    }
}
