package com.github._255_ping.rpg.core.abilities;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import com.github._255_ping.rpg.api.abilities.AbilityInvocation;
import com.github._255_ping.rpg.api.abilities.AbilityPipeline;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * An admin-defined ability (from {@code abilities/*.yml}) wrapped as an {@link AbilityEffect}
 * so it can be invoked from item / mob {@code Abilities:} lists by its id.
 *
 * <p>The {@code Cooldown:} field in the ability YAML is a <b>hard floor</b> — items using
 * this ability can't reduce it below the value declared here.
 */
public final class CompositeAbilityEffect implements AbilityEffect {

    private final String id;
    private final AbilityPipeline pipeline;
    private final long hardCooldownTicks;

    public CompositeAbilityEffect(String id, List<AbilityInvocation> sequence, long hardCooldownTicks) {
        this.id = id;
        this.pipeline = new AbilityPipeline(sequence);
        this.hardCooldownTicks = hardCooldownTicks;
    }

    @Override public String name() { return id; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (ctx.caster() instanceof Player p && hardCooldownTicks > 0) {
            String key = "ability:" + id;
            if (RpgServices.cooldowns().isOnCooldown(p.getUniqueId(), key)) {
                return CompletableFuture.completedFuture(ctx);
            }
            RpgServices.cooldowns().set(p.getUniqueId(), key, hardCooldownTicks);
        }
        return pipeline.cast(ctx, RpgServices.abilities());
    }
}
