package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Pushes a target entity away from (or toward) the caster, or straight up.
 *
 * <p>Parameters:
 * <ul>
 *   <li>{@code force=1.0} — horizontal velocity magnitude (blocks/tick)</li>
 *   <li>{@code up=0.2} — upward Y component added alongside the horizontal push</li>
 *   <li>{@code target=target} — {@code "target"} or {@code "caster"}</li>
 *   <li>{@code direction=away} — {@code "away"} (from caster), {@code "toward"} (to caster),
 *       or {@code "up"} (pure vertical, ignores horizontal)</li>
 * </ul>
 */
public final class KnockbackEffect implements AbilityEffect {

    private final double force;
    private final double up;
    private final String targetParam;
    private final String direction;

    public KnockbackEffect(Map<String, String> params) {
        this.force       = AbilityDsl.doubleParam(params, "force", 1.0);
        this.up          = AbilityDsl.doubleParam(params, "up", 0.2);
        this.targetParam = params.getOrDefault("target", "target");
        this.direction   = params.getOrDefault("direction", "away");
    }

    @Override public String name() { return "knockback"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        LivingEntity entity = "caster".equals(targetParam) ? ctx.caster() : ctx.target();
        if (entity == null) return CompletableFuture.completedFuture(ctx);

        Vector vel;
        if ("up".equals(direction)) {
            vel = new Vector(0, force, 0);
        } else {
            // Determine which entity is the push origin and which is being pushed
            LivingEntity from = "toward".equals(direction) ? entity : ctx.caster();
            LivingEntity to   = "toward".equals(direction) ? ctx.caster() : entity;
            if (from == null || to == null) return CompletableFuture.completedFuture(ctx);

            Vector dir = to.getLocation().toVector().subtract(from.getLocation().toVector());
            dir.setY(0);
            if (dir.lengthSquared() < 0.001) dir = new Vector(1, 0, 0);
            dir.normalize().multiply(force).setY(up);
            vel = dir;
        }

        entity.setVelocity(vel);
        return CompletableFuture.completedFuture(ctx);
    }
}
