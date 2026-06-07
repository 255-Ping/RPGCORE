package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Fires a particle beam from the caster's eye in the look direction, up to {@code range}
 * blocks. The first living entity hit (excluding caster) becomes the new {@code ctx.target};
 * {@code ctx.point} is set to the endpoint (hit point or max range).
 *
 * <p>Beam does NOT apply damage directly — it only sets the target and scales
 * {@code ctx.carriedDamage} by {@code damage_multiplier}. Follow with an explicit
 * {@code damage} step for a direct hit, or {@code explode} for AoE. This keeps
 * each effect's responsibility distinct and prevents double-dipping when beam and
 * explode are chained together.
 *
 * <p>The {@code pierce_cap} parameter controls how many distinct entities the beam
 * can hit before stopping (default 1 = single-target). Set to 0 for unlimited piercing.
 * {@code ctx.target} is always the <em>first</em> entity hit; knockback is applied to all.
 */
public final class BeamEffect implements AbilityEffect {

    private final double range;
    private final double damageMultiplier;
    private final Particle particle;
    /** Maximum entities to hit. 0 = unlimited; defaults to 1 (single-target). */
    private final int pierceCap;

    public BeamEffect(Map<String, String> params) {
        this.range = AbilityDsl.doubleParam(params, "range", 5);
        this.damageMultiplier = AbilityDsl.doubleParam(params, "damage_multiplier", 1.0);
        this.particle = parseParticle(params.getOrDefault("particle", "crit"));
        this.pierceCap = AbilityDsl.intParam(params, "pierce_cap", 1);
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

        // Collect hits by repeating rayTrace with an exclusion set.
        // Each pass skips already-hit entities so the beam tunnels through them.
        List<LivingEntity> hits = new ArrayList<>();
        Set<UUID> hitIds = new HashSet<>();
        int maxHits = pierceCap > 0 ? pierceCap : Integer.MAX_VALUE;
        Location end = eye.clone().add(dir.clone().multiply(range));

        while (hits.size() < maxHits) {
            RayTraceResult result = caster.getWorld().rayTrace(
                    eye, dir, range, FluidCollisionMode.NEVER, true, 0.3,
                    entity -> entity != caster && entity instanceof LivingEntity
                              && !hitIds.contains(entity.getUniqueId()));
            if (result == null) break;
            if (result.getHitEntity() instanceof LivingEntity hit) {
                hits.add(hit);
                hitIds.add(hit.getUniqueId());
                end = result.getHitPosition().toLocation(caster.getWorld());
                // Continue loop — beam may pierce through this entity.
            } else {
                // Beam hit a block; stop here.
                end = result.getHitPosition().toLocation(caster.getWorld());
                break;
            }
        }

        if (!hits.isEmpty()) {
            ctx.setTarget(hits.get(0));
            ctx.setCarriedDamage(ctx.carriedDamage() * damageMultiplier);
        }

        // Look up knockback once for the caster, then apply to each hit entity.
        double knockback = 0;
        try {
            if (caster instanceof Player ap) {
                knockback = RpgServices.player(ap).get(BuiltinStat.KNOCKBACK);
            } else {
                knockback = RpgServices.mobStats().forMob(caster).get(BuiltinStat.KNOCKBACK);
            }
        } catch (IllegalStateException ignored) {}

        for (LivingEntity hit : hits) {
            if (knockback > 0) {
                Vector kbDir = hit.getLocation().toVector().subtract(caster.getLocation().toVector());
                kbDir.setY(0);
                if (kbDir.lengthSquared() < 0.001) kbDir = new Vector(1, 0, 0);
                kbDir.normalize();
                double strength = knockback / 100.0;
                hit.setVelocity(kbDir.multiply(strength).setY(Math.min(0.3 + strength * 0.1, 0.5)));
            }
        }

        ctx.setPoint(end);

        // Stripe particles along the beam (3 steps per block = dense trail).
        double dist = eye.distance(end);
        int steps = Math.max(1, (int) Math.round(dist * 3));
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
