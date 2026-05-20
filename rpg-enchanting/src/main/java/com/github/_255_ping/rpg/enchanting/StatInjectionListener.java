package com.github._255_ping.rpg.enchanting;

import com.github._255_ping.rpg.api.stats.Stat;
import com.github._255_ping.rpg.api.stats.StatRecalcEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Injects enchant/reforge/upgrade stats into the player's stat holder on every recalc.
 * Runs at {@link EventPriority#HIGH} so it lands after rpg-core's base population.
 */
public final class StatInjectionListener implements Listener {

    private final EnchantRegistry registry;
    private final ItemModifier modifier;

    public StatInjectionListener(EnchantRegistry registry, ItemModifier modifier) {
        this.registry = registry;
        this.modifier = modifier;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRecalc(StatRecalcEvent e) {
        Player p = e.getPlayer();
        contributeFor(e, p.getInventory().getHelmet());
        contributeFor(e, p.getInventory().getChestplate());
        contributeFor(e, p.getInventory().getLeggings());
        contributeFor(e, p.getInventory().getBoots());
        contributeFor(e, p.getInventory().getItemInMainHand());
    }

    private void contributeFor(StatRecalcEvent e, ItemStack stack) {
        if (stack == null) return;
        Map<Stat, Double> contributions = modifier.contributedStats(stack, registry);
        for (Map.Entry<Stat, Double> entry : contributions.entrySet()) {
            try {
                e.holder().add(entry.getKey(), entry.getValue());
            } catch (UnsupportedOperationException ex) {
                // Holder not mutable in this context; skip silently.
                return;
            }
        }
    }
}
