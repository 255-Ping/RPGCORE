package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AoE damage centered on {@code ctx.point} (or caster location if point unset).
 * Damages all LivingEntities in radius excluding the caster.
 */
public final class ExplodeEffect implements AbilityEffect {

    private final double radius;
    private final double damageMultiplier;
    private final double flatDamage;
    private final Particle particle;

    public ExplodeEffect(Map<String, String> params) {
        this.radius = AbilityDsl.doubleParam(params, "radius", 3);
        this.damageMultiplier = AbilityDsl.doubleParam(params, "damage_multiplier", 1.0);
        this.flatDamage = AbilityDsl.doubleParam(params, "damage", 0);
        this.particle = parseParticle(params.getOrDefault("particle", "explosion"));
    }

    @Override public String name() { return "explode"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (ctx.caster() == null || ctx.caster().getWorld() == null) {
            return CompletableFuture.completedFuture(ctx);
        }
        Location center = ctx.point() != null ? ctx.point() : ctx.caster().getLocation();
        center.getWorld().spawnParticle(particle, center, 1);

        double finalDamage = flatDamage + ctx.carriedDamage() * damageMultiplier;
        if (finalDamage <= 0) return CompletableFuture.completedFuture(ctx);

        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le.equals(ctx.caster())) continue;
            RpgServices.health().damage(le, finalDamage, "ability_aoe");
        }
        return CompletableFuture.completedFuture(ctx);
    }

    private static Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Particle.EXPLOSION;
        }
    }
}
