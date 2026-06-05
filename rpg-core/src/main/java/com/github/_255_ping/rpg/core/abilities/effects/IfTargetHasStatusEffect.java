package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import com.github._255_ping.rpg.api.status.ActiveStatusEffect;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Gate that passes only when {@code ctx.target} has a specific status effect active.
 *
 * <pre>
 * # Extra damage against poisoned targets
 * "nearest_enemy{range=8.0} if_target_has_status{id=poison} damage{multiplier=1.5}"
 *
 * # Consume burn for an ignite burst
 * "beam{range=10.0} if_target_has_status{id=burn} explode{radius=3.0,damage=30.0,particle=FLAME}"
 * </pre>
 *
 * <p>If {@code ctx.target} is null or has no active effects the gate fails (chain blocked).
 *
 * @see ChanceEffect for the blocking mechanism
 */
public final class IfTargetHasStatusEffect implements AbilityEffect {

    private final String effectId;

    public IfTargetHasStatusEffect(Map<String, String> params) {
        this.effectId = params.getOrDefault("id", "");
    }

    @Override public String name() { return "if_target_has_status"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (ctx.isBlocked()) return CompletableFuture.completedFuture(ctx);

        if (effectId.isBlank() || ctx.target() == null) {
            ctx.setBlocked(true);
            return CompletableFuture.completedFuture(ctx);
        }

        Collection<ActiveStatusEffect> active = RpgServices.statusEffects().active(ctx.target());
        boolean passes = active.stream().anyMatch(s -> effectId.equals(s.effectId()));
        if (!passes) ctx.setBlocked(true);
        return CompletableFuture.completedFuture(ctx);
    }
}
