package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Mana-percentage gate. Blocks the rest of the chain if the condition is not met.
 *
 * <p>Two variants registered under separate DSL names:
 * <ul>
 *   <li>{@code if_mana_below{percent=30}} — passes when caster mana% &lt; threshold</li>
 *   <li>{@code if_mana_above{percent=70}} — passes when caster mana% &gt; threshold</li>
 * </ul>
 *
 * <p>If the caster is not a {@link Player}, the check always <em>fails</em> (chain blocked)
 * — mobs have no mana pool. This keeps ability YAML portable: a mob accidentally using a
 * mana gate silently does nothing rather than acting as if the condition passed.
 *
 * @see ChanceEffect for the blocking mechanism
 */
public final class IfManaEffect implements AbilityEffect {

    public enum Mode { BELOW, ABOVE }

    private final Mode mode;
    private final double percent;

    public IfManaEffect(Mode mode, Map<String, String> params) {
        this.mode    = mode;
        this.percent = AbilityDsl.doubleParam(params, "percent", 50.0);
    }

    @Override public String name() { return mode == Mode.BELOW ? "if_mana_below" : "if_mana_above"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (ctx.isBlocked()) return CompletableFuture.completedFuture(ctx);

        if (!(ctx.caster() instanceof Player p)) {
            // Non-player entities have no mana — condition can never pass.
            ctx.setBlocked(true);
            return CompletableFuture.completedFuture(ctx);
        }

        RpgPlayer rp  = RpgServices.player(p);
        double max    = rp.maxMana();
        double cur    = rp.mana();
        double pct    = max > 0 ? (cur / max) * 100.0 : 0.0;

        boolean passes = switch (mode) {
            case BELOW -> pct < percent;
            case ABOVE -> pct > percent;
        };

        if (!passes) ctx.setBlocked(true);
        return CompletableFuture.completedFuture(ctx);
    }
}
