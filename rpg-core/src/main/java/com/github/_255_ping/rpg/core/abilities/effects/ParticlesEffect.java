package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ParticlesEffect implements AbilityEffect {

    private final Particle particle;
    private final int count;
    private final double offset;

    public ParticlesEffect(Map<String, String> params) {
        this.particle = parseParticle(params.getOrDefault("type", "flame"));
        this.count = AbilityDsl.intParam(params, "count", 10);
        this.offset = AbilityDsl.doubleParam(params, "offset", 0.5);
    }

    @Override public String name() { return "particles"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (ctx.caster() == null || ctx.caster().getWorld() == null) {
            return CompletableFuture.completedFuture(ctx);
        }
        Location at = ctx.point() != null ? ctx.point() : ctx.caster().getLocation();
        at.getWorld().spawnParticle(particle, at, count, offset, offset, offset, 0);
        return CompletableFuture.completedFuture(ctx);
    }

    private static Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Particle.FLAME;
        }
    }
}
