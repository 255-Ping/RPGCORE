package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Restores mana to the caster or target. No-ops if the recipient is not a {@link Player}
 * (mana is tracked only for players).
 *
 * <p>Parameters:
 * <ul>
 *   <li>{@code amount=25} — mana to restore</li>
 *   <li>{@code target=caster} — {@code "caster"} or {@code "target"}</li>
 * </ul>
 */
public final class RestoreManaEffect implements AbilityEffect {

    private final double amount;
    private final String targetParam;

    public RestoreManaEffect(Map<String, String> params) {
        this.amount      = AbilityDsl.doubleParam(params, "amount", 25.0);
        this.targetParam = params.getOrDefault("target", "caster");
    }

    @Override public String name() { return "restore_mana"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (amount <= 0) return CompletableFuture.completedFuture(ctx);
        var entity = "target".equals(targetParam) ? ctx.target() : ctx.caster();
        if (entity instanceof Player p) {
            RpgServices.mana().restore(p, amount);
        }
        return CompletableFuture.completedFuture(ctx);
    }
}
