package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import com.github._255_ping.rpg.core.RpgCorePlugin;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class DelayEffect implements AbilityEffect {

    private final long ticks;

    public DelayEffect(Map<String, String> params) {
        this.ticks = AbilityDsl.intParam(params, "ticks", 0);
    }

    @Override public String name() { return "delay"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (ticks <= 0) return CompletableFuture.completedFuture(ctx);
        CompletableFuture<AbilityContext> f = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskLater(RpgCorePlugin.get(), () -> f.complete(ctx), ticks);
        return f;
    }
}
