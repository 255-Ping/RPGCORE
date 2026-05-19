package com.github._255_ping.rpg.core.health;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Ticks once per second by default. For each online player: out-of-combat HEALTH_REGEN
 * and unconditional MANA_REGEN both apply.
 */
public final class RegenTask implements Runnable {

    private final CoreHealthService health;

    public RegenTask(CoreHealthService health) {
        this.health = health;
    }

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            RpgPlayer rp = RpgServices.player(p);

            if (!health.inCombat(p)) {
                double regenHp = rp.get(BuiltinStat.HEALTH_REGEN);
                if (regenHp > 0 && health.currentHp(p) < health.maxHp(p)) {
                    health.heal(p, regenHp);
                }
            }

            double regenMana = rp.get(BuiltinStat.MANA_REGEN);
            if (regenMana > 0 && rp.mana() < rp.maxMana()) {
                rp.setMana(rp.mana() + regenMana);
            }
        }
    }
}
