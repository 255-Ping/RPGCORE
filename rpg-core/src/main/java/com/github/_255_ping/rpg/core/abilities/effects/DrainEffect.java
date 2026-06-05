package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import com.github._255_ping.rpg.api.damage.DamageContext;
import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Deals damage to the target and heals the caster for a fraction of the damage dealt —
 * a combined vampiric strike.
 *
 * <p>Unlike chaining {@code damage{} heal{}}, {@code drain} guarantees that the heal amount is
 * derived from the actual damage number (before mitigation at the HealthService layer), making
 * the leech feel consistent.
 *
 * <p>Parameters:
 * <ul>
 *   <li>{@code amount=10.0} — flat drain damage (independent of carriedDamage)</li>
 *   <li>{@code damage_multiplier=0.0} — additional multiplier on carriedDamage (stacks with amount)</li>
 *   <li>{@code leech=1.0} — fraction of damage converted to healing (1.0 = 100%, 0.5 = 50%)</li>
 * </ul>
 */
public final class DrainEffect implements AbilityEffect {

    private static final ThreadLocal<Boolean> FIRING_POST = ThreadLocal.withInitial(() -> false);

    private final double amount;
    private final double damageMultiplier;
    private final double leech;

    public DrainEffect(Map<String, String> params) {
        this.amount           = AbilityDsl.doubleParam(params, "amount", 10.0);
        this.damageMultiplier = AbilityDsl.doubleParam(params, "damage_multiplier", 0.0);
        this.leech            = AbilityDsl.doubleParam(params, "leech", 1.0);
    }

    @Override public String name() { return "drain"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (ctx.target() == null) return CompletableFuture.completedFuture(ctx);
        if (ctx.target() instanceof Player p && p.getGameMode() == GameMode.CREATIVE) {
            return CompletableFuture.completedFuture(ctx);
        }

        double damage = amount + ctx.carriedDamage() * damageMultiplier;
        if (damage <= 0) return CompletableFuture.completedFuture(ctx);

        RpgServices.health().damage(ctx.target(), damage, "ability_drain");

        if (leech > 0 && ctx.caster() != null) {
            RpgServices.health().heal(ctx.caster(), damage * leech);
        }

        if (!FIRING_POST.get()) {
            FIRING_POST.set(true);
            try {
                DamageContext dCtx = new DamageContext(ctx.caster(), ctx.target(), damage, "ability_drain");
                Bukkit.getPluginManager().callEvent(new PostDamageEvent(dCtx, damage));
            } finally {
                FIRING_POST.set(false);
            }
        }
        return CompletableFuture.completedFuture(ctx);
    }
}
