package com.github._255_ping.rpg.core.player;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.api.stats.Stat;
import com.github._255_ping.rpg.api.stats.StatRecalcEvent;
import com.github._255_ping.rpg.core.status.CoreStatusEffectService;
import com.github._255_ping.rpg.core.status.StatModifier;
import com.github._255_ping.rpg.core.stats.MutableStatHolder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CoreRpgPlayer implements RpgPlayer {

    private final Player bukkit;
    private final MutableStatHolder base = new MutableStatHolder();
    /** Permanent bonus stats accumulated from milestone rewards. Applied in recalculation after base. */
    private final MutableStatHolder bonusStats = new MutableStatHolder();
    /** Temporary flat bonuses from active armor-set tiers. Updated by ArmorSetListener before recalc. */
    private final MutableStatHolder setBonusStats = new MutableStatHolder();
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
    public void add(Stat stat, double amount) {
        bonusStats.add(stat, amount);
    }

    public Map<Stat, Double> bonusStats() { return bonusStats.snapshot(); }

    public void setBonusStats(Map<Stat, Double> stats) {
        bonusStats.clear();
        stats.forEach(bonusStats::set);
    }

    @Override
    public void recalculateStats() {
        effective.clear();

        // Layer 1: base stats from config
        for (Map.Entry<Stat, Double> e : base.snapshot().entrySet()) {
            effective.set(e.getKey(), e.getValue());
        }

        // Layer 1.5: permanent bonus stats (milestone rewards, etc.)
        for (Map.Entry<Stat, Double> e : bonusStats.snapshot().entrySet()) {
            double cur = effective.get(e.getKey());
            effective.set(e.getKey(), cur + e.getValue());
        }

        // Layer 2: equipment (armor slots + main hand)
        for (Map.Entry<Stat, Double> e : collectEquipmentStats().entrySet()) {
            double cur = effective.get(e.getKey());
            effective.set(e.getKey(), cur + e.getValue());
        }

        // Layer 2.5: armor-set bonus stats (flat, set by ArmorSetListener before recalc)
        for (Map.Entry<Stat, Double> e : setBonusStats.snapshot().entrySet()) {
            double cur = effective.get(e.getKey());
            effective.set(e.getKey(), cur + e.getValue());
        }

        // Layer 3: accessory bag (only counts when rpg-accessories is loaded)
        for (Map.Entry<Stat, Double> e : collectAccessoryStats().entrySet()) {
            double cur = effective.get(e.getKey());
            effective.set(e.getKey(), cur + e.getValue());
        }

        // Layer 4: status-effect modifiers (flat then percent)
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

    private Map<Stat, Double> collectEquipmentStats() {
        Map<Stat, Double> out = new HashMap<>();
        PlayerInventory inv = bukkit.getInventory();
        addItemStats(inv.getHelmet(), out);
        addItemStats(inv.getChestplate(), out);
        addItemStats(inv.getLeggings(), out);
        addItemStats(inv.getBoots(), out);
        addItemStats(inv.getItemInMainHand(), out);
        return out;
    }

    private void addItemStats(ItemStack stack, Map<Stat, Double> out) {
        if (stack == null) return;
        Optional<RpgItem> item;
        try {
            item = RpgServices.items().from(stack);
        } catch (IllegalStateException ex) {
            return;
        }
        if (item.isEmpty()) return;
        for (Map.Entry<Stat, Double> e : item.get().stats().entrySet()) {
            out.merge(e.getKey(), e.getValue(), Double::sum);
        }
    }

    private Map<Stat, Double> collectAccessoryStats() {
        try {
            return RpgServices.accessories().aggregateStats(bukkit);
        } catch (IllegalStateException ex) {
            return Map.of();
        }
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

    /**
     * Replaces the entire set-bonus stat layer.  Called by {@code ArmorSetListener} whenever
     * the player's active set tiers change, before {@code EquipmentListener} triggers recalc.
     */
    public void setSetBonusStats(Map<Stat, Double> stats) {
        setBonusStats.clear();
        stats.forEach(setBonusStats::set);
    }
}
