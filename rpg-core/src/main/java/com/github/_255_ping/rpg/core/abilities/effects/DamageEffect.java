package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import com.github._255_ping.rpg.api.damage.DamageContext;
import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class DamageEffect implements AbilityEffect {

    /**
     * Re-entrancy guard: prevents PostDamageEvent fired by an ability from triggering
     * OnHurt/OnHit mob bindings that contain DamageEffect and would cause a stack overflow.
     * The indicator fires on the first level; nested ability-triggered hits do not re-fire it.
     */
    private static final ThreadLocal<Boolean> FIRING_POST = ThreadLocal.withInitial(() -> false);

    private final double amount;
    private final double damageMultiplier;
    private final String type;

    public DamageEffect(Map<String, String> params) {
        this.amount = AbilityDsl.doubleParam(params, "amount", 0);
        this.damageMultiplier = AbilityDsl.doubleParam(params, "damage_multiplier", 1.0);
        this.type = params.getOrDefault("type", "physical");
    }

    @Override public String name() { return "damage"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (ctx.target() == null) return CompletableFuture.completedFuture(ctx);
        double base = amount + ctx.carriedDamage() * damageMultiplier;
        if (base <= 0) return CompletableFuture.completedFuture(ctx);
        String source = "true".equals(type) ? "ability_true" : "ability";
        RpgServices.health().damage(ctx.target(), base, source);
        // Fire PostDamageEvent for damage indicators. Guard against re-entrancy: if an OnHurt
        // ability also contains DamageEffect, suppressing the nested PostDamageEvent breaks
        // the recursion — the secondary hit still lands, but doesn't fire further triggers.
        if (!FIRING_POST.get()) {
            FIRING_POST.set(true);
            try {
                DamageContext dCtx = new DamageContext(ctx.caster(), ctx.target(), base, source);
                Bukkit.getPluginManager().callEvent(new PostDamageEvent(dCtx, base));
            } finally {
                FIRING_POST.set(false);
            }
        }
        return CompletableFuture.completedFuture(ctx);
    }
}
