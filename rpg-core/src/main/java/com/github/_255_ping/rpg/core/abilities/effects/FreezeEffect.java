package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Applies extreme slowness to the target for {@code duration} ticks, visually accompanied
 * by a snowflake particle burst.
 *
 * <p>This uses the vanilla {@link PotionEffectType#SLOWNESS} applied directly via the API,
 * independent of the custom status-effect system. No status-effect YAML definition is
 * required. Combine with {@code apply_status{}} for heavier custom debuffs.
 *
 * <p>The effect is tagged as non-ambient so the "bubbling potion" particles are visible,
 * with the status icon shown in the player HUD. Vanilla suppression does not interfere —
 * that suppresses splash/drinkable potion events, not direct API calls.
 *
 * <p>Parameters:
 * <ul>
 *   <li>{@code duration=60} — ticks (60 = 3 seconds)</li>
 *   <li>{@code amplifier=4} — Slowness amplifier level (0 = Slowness I … 4 = Slowness V)</li>
 *   <li>{@code target=target} — {@code "target"} or {@code "caster"}</li>
 * </ul>
 */
public final class FreezeEffect implements AbilityEffect {

    private final int duration;
    private final int amplifier;
    private final String targetParam;

    public FreezeEffect(Map<String, String> params) {
        this.duration    = AbilityDsl.intParam(params, "duration", 60);
        this.amplifier   = AbilityDsl.intParam(params, "amplifier", 4);
        this.targetParam = params.getOrDefault("target", "target");
    }

    @Override public String name() { return "freeze"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        LivingEntity entity = "caster".equals(targetParam) ? ctx.caster() : ctx.target();
        if (entity == null) return CompletableFuture.completedFuture(ctx);

        PotionEffectType slowType = PotionEffectType.SLOWNESS;
        if (slowType != null && duration > 0) {
            // ambient=false → visible bubbling particles; showParticles=true; showIcon=true
            entity.addPotionEffect(new PotionEffect(slowType, duration, amplifier, false, true, true));
        }

        // Snowflake burst at entity's mid-body
        entity.getWorld().spawnParticle(
                Particle.SNOWFLAKE,
                entity.getLocation().add(0, 1.0, 0),
                20, 0.4, 0.6, 0.4, 0.02);

        return CompletableFuture.completedFuture(ctx);
    }
}
