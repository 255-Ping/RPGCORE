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

        // Layer 1: base stats from config
        for (Map.Entry<Stat, Double> e : base.snapshot().entrySet()) {
            effective.set(e.getKey(), e.getValue());
        }

        // Layer 2: equipment (armor slots + main hand) — accessory bag arrives with rpg-accessories
        for (Map.Entry<Stat, Double> e : collectEquipmentStats().entrySet()) {
            double cur = effective.get(e.getKey());
            effective.set(e.getKey(), cur + e.getValue());
        }

        // Layer 3: status-effect modifiers (flat then percent)
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
