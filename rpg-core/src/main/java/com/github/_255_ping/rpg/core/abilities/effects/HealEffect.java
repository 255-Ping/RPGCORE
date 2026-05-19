package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class HealEffect implements AbilityEffect {

    private final double amount;
    private final String target;

    public HealEffect(Map<String, String> params) {
        this.amount = AbilityDsl.doubleParam(params, "amount", 0);
        this.target = params.getOrDefault("target", "caster");
    }

    @Override public String name() { return "heal"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (amount <= 0) return CompletableFuture.completedFuture(ctx);
        LivingEntity recipient = "target".equals(target) ? ctx.target() : ctx.caster();
        if (recipient != null) {
            RpgServices.health().heal(recipient, amount);
        }
        return CompletableFuture.completedFuture(ctx);
    }
}
