package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Gate that passes only when {@code ctx.target} currently has an active {@link MarkEffect} mark.
 *
 * <pre>
 * # Detonate mark for triple damage — but only if the target is actually marked
 * "nearest_enemy{range=10.0} if_marked{} damage{multiplier=3.0}"
 * </pre>
 *
 * <p>If {@code ctx.target} is null the gate fails (chain blocked). This keeps the pattern
 * safe: a targeting effect that found no one leaves target null, and the conditional
 * correctly skips the downstream damage.
 *
 * @see MarkEffect for mark application and querying
 * @see ChanceEffect for the blocking mechanism
 */
public final class IfMarkedEffect implements AbilityEffect {

    public IfMarkedEffect(Map<String, String> params) { /* no params */ }

    @Override public String name() { return "if_marked"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (ctx.isBlocked()) return CompletableFuture.completedFuture(ctx);

        boolean passes = ctx.target() != null && MarkEffect.isMarked(ctx.target().getUniqueId());
        if (!passes) ctx.setBlocked(true);
        return CompletableFuture.completedFuture(ctx);
    }
}
