package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.loot.LootContext;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * On RPG mob death: cancel vanilla drops, look up our loot table, attribute via the
 * damager tracker, and drop the rolled items at the corpse.
 */
public final class MobLootListener implements Listener {

    private final DamagerTracker tracker;

    public MobLootListener(DamagerTracker tracker) {
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Optional<RpgMob> opt = RpgServices.mobs().from(victim);
        if (opt.isEmpty()) return;
        if (!(opt.get() instanceof CoreRpgMob def)) return;

        // Cancel vanilla drops + XP — our loot table is the source of truth.
        event.getDrops().clear();
        event.setDroppedExp(0);

        CoreLootTable table = def.lootTable();
        if (table == null) {
            tracker.takeFor(victim.getUniqueId()); // still clear the tracker entry
            return;
        }

        Player killer = tracker.lastHitter(victim.getUniqueId());
        Map<Player, Double> damagers = tracker.takeFor(victim.getUniqueId());
        LootContext ctx = new LootContext(victim, damagers, killer);

        Map<Player, List<ItemStack>> rolled = table.roll(ctx);
        if (victim.getWorld() == null) return;
        for (List<ItemStack> stacks : rolled.values()) {
            for (ItemStack s : stacks) {
                if (s == null) continue;
                victim.getWorld().dropItemNaturally(victim.getLocation(), s);
            }
        }
    }
}
