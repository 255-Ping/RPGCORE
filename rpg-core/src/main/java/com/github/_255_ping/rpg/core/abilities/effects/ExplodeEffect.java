package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import com.github._255_ping.rpg.api.damage.DamageContext;
import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AoE damage centered on {@code ctx.point} (or caster location if point unset).
 * Damages all LivingEntities within radius, excluding the caster.
 * Also registered as {@code aoe} — both names work identically.
 *
 * <p>Particles: spawns a burst of particles spread across the blast radius, not a single
 * explosion particle. Use {@code particle=FLAME} for a fire burst, {@code particle=SNOWBALL}
 * for ice, etc. Defaults to {@code EXPLOSION_EMITTER} for a classic boom visual.
 */
public final class ExplodeEffect implements AbilityEffect {

    /** Re-entrancy guard — same purpose as DamageEffect.FIRING_POST. */
    private static final ThreadLocal<Boolean> FIRING_POST = ThreadLocal.withInitial(() -> false);

    private final double radius;
    private final double damageMultiplier;
    private final double flatDamage;
    private final Particle particle;

    public ExplodeEffect(Map<String, String> params) {
        this.radius = AbilityDsl.doubleParam(params, "radius", 3);
        this.damageMultiplier = AbilityDsl.doubleParam(params, "damage_multiplier", 1.0);
        this.flatDamage = AbilityDsl.doubleParam(params, "damage", 0);
        this.particle = parseParticle(params.getOrDefault("particle", "explosion_emitter"));
    }

    @Override public String name() { return "explode"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (ctx.caster() == null || ctx.caster().getWorld() == null) {
            return CompletableFuture.completedFuture(ctx);
        }
        Location center = ctx.point() != null ? ctx.point() : ctx.caster().getLocation();

        // Burst particles spread across the radius — not a single point spawn.
        // For EXPLOSION_EMITTER we only need 1 (it's already a big effect); for others, scatter many.
        if (particle == Particle.EXPLOSION_EMITTER) {
            center.getWorld().spawnParticle(particle, center, 1);
        } else {
            double spread = radius * 0.5;
            center.getWorld().spawnParticle(particle, center, 30, spread, spread, spread, 0.05);
        }

        double finalDamage = flatDamage + ctx.carriedDamage() * damageMultiplier;
        if (finalDamage <= 0) return CompletableFuture.completedFuture(ctx);

        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le.equals(ctx.caster())) continue;
            // Skip the primary beam target — it was already hit by an explicit damage{} step.
            if (ctx.target() != null && le.equals(ctx.target())) continue;
            RpgServices.health().damage(le, finalDamage, "ability_aoe");
            if (!FIRING_POST.get()) {
                FIRING_POST.set(true);
                try {
                    DamageContext dCtx = new DamageContext(ctx.caster(), le, finalDamage, "ability_aoe");
                    Bukkit.getPluginManager().callEvent(new PostDamageEvent(dCtx, finalDamage));
                } finally {
                    FIRING_POST.set(false);
                }
            }
        }
        return CompletableFuture.completedFuture(ctx);
    }

    private static Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Particle.EXPLOSION_EMITTER;
        }
    }
}
