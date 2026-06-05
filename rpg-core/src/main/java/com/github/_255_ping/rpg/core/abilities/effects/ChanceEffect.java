package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Inline probability gate. Rolls a random number; if the roll fails, every effect
 * <em>after</em> this one in the same chain is skipped.
 *
 * <pre>
 * # 20% chance to freeze on hit
 * "~on_hit chance{percent=20} freeze{duration=60,amplifier=4}"
 *
 * # 35% chance to chain after a beam hit
 * "mana_cost{amount=35} beam{range=14.0} damage{} chance{percent=35} chain{count=3,range=8.0}"
 * </pre>
 *
 * <p>Effects <em>before</em> {@code chance{}} have already executed and are unaffected.
 * Stacking multiple {@code chance{}} calls is AND logic:
 * {@code chance{percent=50} chance{percent=50}} ≈ 25% net.
 *
 * <p>{@code percent} is a double, so fractional values like {@code percent=12.5} work.
 *
 * <p>The skip mechanism lives in {@link com.github._255_ping.rpg.api.abilities.AbilityPipeline}:
 * it checks {@link AbilityContext#isBlocked()} before each effect and short-circuits. This class
 * only needs to set the flag — no other effects need to be modified.
 */
public final class ChanceEffect implements AbilityEffect {

    private final double percent;

    public ChanceEffect(Map<String, String> params) {
        this.percent = AbilityDsl.doubleParam(params, "percent", 100.0);
    }

    @Override
    public String name() { return "chance"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        // If a previous gate already blocked the chain, stay blocked — don't "un-block" it.
        if (ctx.isBlocked()) return CompletableFuture.completedFuture(ctx);

        if (ThreadLocalRandom.current().nextDouble(100.0) >= percent) {
            ctx.setBlocked(true);
        }
        return CompletableFuture.completedFuture(ctx);
    }
}
