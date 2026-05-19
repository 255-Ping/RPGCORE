package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ApplyStatusEffect implements AbilityEffect {

    private final String effectId;
    private final int level;
    private final int durationTicks;
    private final String target;

    public ApplyStatusEffect(Map<String, String> params) {
        this.effectId = params.getOrDefault("id", "");
        this.level = AbilityDsl.intParam(params, "level", 1);
        this.durationTicks = AbilityDsl.intParam(params, "duration", 100);
        this.target = params.getOrDefault("target", "target");
    }

    @Override public String name() { return "apply_status"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (effectId.isBlank()) return CompletableFuture.completedFuture(ctx);
        LivingEntity recipient = "caster".equals(target) ? ctx.caster() : ctx.target();
        if (recipient == null) return CompletableFuture.completedFuture(ctx);
        String sourceTag = ctx.caster() != null ? ctx.caster().getUniqueId().toString() : "ability";
        RpgServices.statusEffects().apply(recipient, effectId, level, durationTicks, sourceTag);
        return CompletableFuture.completedFuture(ctx);
    }
}
