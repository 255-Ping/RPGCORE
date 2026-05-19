package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class DamageEffect implements AbilityEffect {

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
        return CompletableFuture.completedFuture(ctx);
    }
}
