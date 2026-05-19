package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Fires a particle beam from the caster's eye in the look direction, up to {@code range}
 * blocks. The first living entity hit (excluding caster) becomes the new {@code ctx.target};
 * {@code ctx.point} is set to the endpoint (hit point or max range).
 */
public final class BeamEffect implements AbilityEffect {

    private final double range;
    private final double damageMultiplier;
    private final Particle particle;

    public BeamEffect(Map<String, String> params) {
        this.range = AbilityDsl.doubleParam(params, "range", 5);
        this.damageMultiplier = AbilityDsl.doubleParam(params, "damage_multiplier", 1.0);
        this.particle = parseParticle(params.getOrDefault("particle", "crit"));
    }

    @Override public String name() { return "beam"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (ctx.caster() == null || ctx.caster().getWorld() == null) {
            return CompletableFuture.completedFuture(ctx);
        }
        LivingEntity caster = ctx.caster();
        Location eye = caster.getEyeLocation();
        Vector dir = eye.getDirection();

        RayTraceResult result = caster.getWorld().rayTrace(
                eye, dir, range, FluidCollisionMode.NEVER, true, 0.3,
                entity -> entity != caster && entity instanceof LivingEntity);

        Location end;
        if (result != null && result.getHitEntity() instanceof LivingEntity hit) {
            ctx.setTarget(hit);
            end = result.getHitPosition().toLocation(caster.getWorld());
            ctx.setCarriedDamage(ctx.carriedDamage() * damageMultiplier);
        } else if (result != null) {
            end = result.getHitPosition().toLocation(caster.getWorld());
        } else {
            end = eye.clone().add(dir.clone().multiply(range));
        }
        ctx.setPoint(end);

        // Stripe particles along the beam.
        double dist = eye.distance(end);
        int steps = Math.max(1, (int) Math.round(dist * 2));
        Vector step = dir.clone().normalize().multiply(dist / steps);
        Location cursor = eye.clone();
        for (int i = 0; i < steps; i++) {
            cursor.add(step);
            caster.getWorld().spawnParticle(particle, cursor, 1, 0, 0, 0, 0);
        }

        return CompletableFuture.completedFuture(ctx);
    }

    private static Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Particle.CRIT;
        }
    }
}
