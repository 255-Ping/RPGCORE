package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * HP-percentage gate. Blocks the rest of the chain if the condition is not met.
 *
 * <p>Two variants registered under separate DSL names:
 * <ul>
 *   <li>{@code if_health_below{percent=50}} — passes when caster HP% &lt; threshold</li>
 *   <li>{@code if_health_above{percent=50}} — passes when caster HP% &gt; threshold</li>
 * </ul>
 *
 * <p>Uses {@link com.github._255_ping.rpg.api.health.HealthService#currentHp} /
 * {@code maxHp} so custom mob health pools are respected. Works for both players and
 * mobs as long as they are tracked in {@code CoreHealthService}.
 *
 * <p>If the caster is null the gate is treated as <em>failed</em> (chain blocked).
 *
 * @see ChanceEffect for the blocking mechanism
 */
public final class IfHealthEffect implements AbilityEffect {

    public enum Mode { BELOW, ABOVE }

    private final Mode mode;
    private final double percent;

    public IfHealthEffect(Mode mode, Map<String, String> params) {
        this.mode    = mode;
        this.percent = AbilityDsl.doubleParam(params, "percent", 50.0);
    }

    @Override public String name() { return mode == Mode.BELOW ? "if_health_below" : "if_health_above"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (ctx.isBlocked()) return CompletableFuture.completedFuture(ctx);

        LivingEntity caster = ctx.caster();
        if (caster == null) {
            ctx.setBlocked(true);
            return CompletableFuture.completedFuture(ctx);
        }

        double max  = RpgServices.health().maxHp(caster);
        double cur  = RpgServices.health().currentHp(caster);
        double pct  = max > 0 ? (cur / max) * 100.0 : 100.0;

        boolean passes = switch (mode) {
            case BELOW -> pct < percent;
            case ABOVE -> pct > percent;
        };

        if (!passes) ctx.setBlocked(true);
        return CompletableFuture.completedFuture(ctx);
    }
}
