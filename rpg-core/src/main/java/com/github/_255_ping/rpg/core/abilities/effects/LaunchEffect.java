package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Launches an entity upward, with an optional horizontal forward boost.
 *
 * <p>Parameters:
 * <ul>
 *   <li>{@code force=1.5} — upward Y velocity in blocks/tick (1.5 ≈ a very high jump)</li>
 *   <li>{@code horizontal=0.0} — forward velocity added in the caster's look direction</li>
 *   <li>{@code target=caster} — {@code "caster"} or {@code "target"}</li>
 * </ul>
 */
public final class LaunchEffect implements AbilityEffect {

    private final double force;
    private final double horizontal;
    private final String targetParam;

    public LaunchEffect(Map<String, String> params) {
        this.force       = AbilityDsl.doubleParam(params, "force", 1.5);
        this.horizontal  = AbilityDsl.doubleParam(params, "horizontal", 0.0);
        this.targetParam = params.getOrDefault("target", "caster");
    }

    @Override public String name() { return "launch"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        LivingEntity entity = "target".equals(targetParam) ? ctx.target() : ctx.caster();
        if (entity == null) return CompletableFuture.completedFuture(ctx);

        Vector vel = new Vector(0, force, 0);
        if (horizontal > 0 && ctx.caster() != null) {
            Vector fwd = ctx.caster().getEyeLocation().getDirection();
            fwd.setY(0);
            if (fwd.lengthSquared() > 0.001) {
                fwd.normalize().multiply(horizontal);
                vel.add(fwd);
            }
        }

        entity.setVelocity(vel);
        return CompletableFuture.completedFuture(ctx);
    }
}
