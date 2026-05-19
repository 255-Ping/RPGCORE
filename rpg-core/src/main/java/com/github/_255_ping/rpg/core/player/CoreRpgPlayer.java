package com.github._255_ping.rpg.core.player;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.api.stats.Stat;
import com.github._255_ping.rpg.api.stats.StatRecalcEvent;
import com.github._255_ping.rpg.core.status.CoreStatusEffectService;
import com.github._255_ping.rpg.core.status.StatModifier;
import com.github._255_ping.rpg.core.stats.MutableStatHolder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public final class CoreRpgPlayer implements RpgPlayer {

    private final Player bukkit;
    private final MutableStatHolder base = new MutableStatHolder();
    private final MutableStatHolder effective = new MutableStatHolder();
    private double currentMana;

    public CoreRpgPlayer(Player bukkit) {
        this.bukkit = bukkit;
    }

    @Override public Player bukkit() { return bukkit; }
    @Override public double mana() { return currentMana; }
    @Override public double maxMana() { return get(BuiltinStat.MAX_MANA); }

    @Override
    public void setMana(double mana) {
        currentMana = Math.max(0, Math.min(mana, maxMana()));
    }

    @Override
    public double get(Stat stat) {
        return effective.get(stat);
    }

    @Override
    public Map<Stat, Double> snapshot() {
        return effective.snapshot();
    }

    @Override
    public void recalculateStats() {
        effective.clear();

        for (Map.Entry<Stat, Double> e : base.snapshot().entrySet()) {
            effective.set(e.getKey(), e.getValue());
        }

        List<StatModifier> modifiers = collectStatusModifiers();
        for (StatModifier m : modifiers) {
            if (m.kind() != StatModifier.Kind.FLAT) continue;
            double cur = effective.get(m.stat());
            effective.set(m.stat(), cur + m.value());
        }
        for (StatModifier m : modifiers) {
            if (m.kind() != StatModifier.Kind.PERCENT) continue;
            double cur = effective.get(m.stat());
            effective.set(m.stat(), cur * (1.0 + m.value() / 100.0));
        }

        Bukkit.getPluginManager().callEvent(new StatRecalcEvent(bukkit, effective));
    }

    private List<StatModifier> collectStatusModifiers() {
        try {
            if (RpgServices.statusEffects() instanceof CoreStatusEffectService svc) {
                return svc.modifiersFor(bukkit.getUniqueId());
            }
        } catch (IllegalStateException ignored) {
            // status-effect service not registered yet — no modifiers
        }
        return List.of();
    }

    public void setBaseStat(Stat stat, double value) {
        base.set(stat, value);
    }

    public void clearBaseStats() {
        base.clear();
    }
}
