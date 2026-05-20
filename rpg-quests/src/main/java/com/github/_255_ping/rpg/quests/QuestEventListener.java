package com.github._255_ping.rpg.quests;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.blocks.RpgBlockBreakEvent;
import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class QuestEventListener implements Listener {

    private final QuestManager manager;

    public QuestEventListener(QuestManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        manager.load(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        manager.unload(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent e) {
        if (e.getEntity().getKiller() == null) return;
        Player killer = e.getEntity().getKiller();
        String target = identifyMob(e.getEntity());
        manager.progressFor(killer, QuestObjective.Type.KILL_MOB, target);
    }

    /**
     * Fall-back kill detection via PostDamageEvent for sources that don't bubble through
     * EntityDeathEvent's killer slot (abilities, etc.).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(PostDamageEvent e) {
        if (!(e.context().attacker() instanceof Player p)) return;
        Entity victim = e.context().victim();
        if (victim == null) return;
        if (victim instanceof org.bukkit.entity.LivingEntity le && le.getHealth() - e.dealtDamage() <= 0) {
            manager.progressFor(p, QuestObjective.Type.KILL_MOB, identifyMob(victim));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(RpgBlockBreakEvent e) {
        manager.progressFor(e.player(), QuestObjective.Type.MINE_BLOCK, e.block().id());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        ItemStack stack = e.getItem().getItemStack();
        String id = identifyItem(stack);
        if (id != null) {
            manager.progressFor(p, QuestObjective.Type.COLLECT_ITEM, id, stack.getAmount());
        }
    }

    private static String identifyMob(Entity ent) {
        try {
            if (ent instanceof org.bukkit.entity.LivingEntity le) {
                Optional<RpgMob> rpg = RpgServices.mobs().from(le);
                if (rpg.isPresent()) return rpg.get().id();
            }
        } catch (IllegalStateException ignored) {}
        return ent.getType().getKey().getKey();
    }

    private static String identifyItem(ItemStack stack) {
        try {
            Optional<RpgItem> rpg = RpgServices.items().from(stack);
            if (rpg.isPresent()) return rpg.get().id();
        } catch (IllegalStateException ignored) {}
        return stack.getType().getKey().getKey();
    }
}
