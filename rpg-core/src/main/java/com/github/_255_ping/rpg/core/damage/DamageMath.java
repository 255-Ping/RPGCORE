package com.github._255_ping.rpg.core.damage;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.damage.DamageContext;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.api.stats.Stat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * v1 damage math.
 *
 * <pre>
 *   after_strength = base * (1 + strength / 100)
 *   crit_roll      = uniform[0, 100) &lt; crit_chance
 *   if crit: after_strength *= (1 + crit_damage / 100), and the multiplier is stored on the context
 *   defense_factor = 1 - defense / (defense + 100)        (true_defense if trueDamage)
 *   final          = max(0, after_strength * defense_factor)
 * </pre>
 *
 * Player stats are fully aggregated (base → milestones → equipment → accessories → status effects →
 * addon StatRecalcEvent injections). Mobs return 0 for all stats until the mob stat-holder lands.
 */
public final class DamageMath {

    private DamageMath() {}

    public static double compute(DamageContext ctx) {
        double base = ctx.baseDamage();

        double strength = statOf(ctx.attacker(), BuiltinStat.STRENGTH);
        double critChance = statOf(ctx.attacker(), BuiltinStat.CRIT_CHANCE);
        double critDamage = statOf(ctx.attacker(), BuiltinStat.CRIT_DAMAGE);
        double defenseStat = ctx.trueDamage()
                ? statOf(ctx.victim(), BuiltinStat.TRUE_DEFENSE)
                : statOf(ctx.victim(), BuiltinStat.DEFENSE);

        boolean isCrit = critChance > 0
                && ThreadLocalRandom.current().nextDouble(100.0) < Math.min(critChance, 100.0);
        double result = computePure(base, strength, defenseStat, isCrit ? critDamage : 0);
        if (isCrit) ctx.setCritMultiplier(1.0 + critDamage / 100.0);
        return result;
    }

    /**
     * Pure-math version of {@link #compute(DamageContext)} for unit testing. The {@code critDamage}
     * argument is treated as already-rolled — pass 0 for a non-crit, the raw stat value for a crit.
     */
    public static double computePure(double base, double strength, double defenseStat, double critDamage) {
        double after = base * (1.0 + strength / 100.0);
        if (critDamage > 0) after *= 1.0 + critDamage / 100.0;
        double defenseFactor = 1.0 - defenseStat / (defenseStat + 100.0);
        return Math.max(0.0, after * defenseFactor);
    }

    private static double statOf(LivingEntity entity, Stat stat) {
        if (entity instanceof Player p) {
            return RpgServices.player(p).get(stat);
        }
        return RpgServices.mobStats().forMob(entity).get(stat);
    }
}
