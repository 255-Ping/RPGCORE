package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Consumes mana from the caster. If the caster is not a Player, no-op. If the caster has
 * insufficient mana, the returned future completes exceptionally with an
 * {@link InsufficientMana} — the calling AbilityPipeline propagates the failure, aborting
 * the rest of the chain. The item-level listener catches the exception and shows a message.
 */
public final class ManaCostEffect implements AbilityEffect {

    private final double amount;

    public ManaCostEffect(Map<String, String> params) {
        this.amount = AbilityDsl.doubleParam(params, "amount", 0);
    }

    @Override public String name() { return "mana_cost"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (amount <= 0) return CompletableFuture.completedFuture(ctx);
        if (!(ctx.caster() instanceof Player p)) return CompletableFuture.completedFuture(ctx);
        if (!RpgServices.mana().consume(p, amount)) {
            CompletableFuture<AbilityContext> f = new CompletableFuture<>();
            f.completeExceptionally(new InsufficientMana(amount));
            return f;
        }
        return CompletableFuture.completedFuture(ctx);
    }

    public static final class InsufficientMana extends RuntimeException {
        public InsufficientMana(double required) {
            super("Insufficient mana (need " + required + ")");
        }
    }
}
