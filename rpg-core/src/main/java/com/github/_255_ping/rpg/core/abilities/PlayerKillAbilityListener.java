package com.github._255_ping.rpg.core.abilities;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.PlayerAbilityTrigger;
import com.github._255_ping.rpg.core.mobs.DamagerTracker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Fires {@link PlayerAbilityTrigger#ON_KILL} bindings when a player kills an RPG mob.
 *
 * <p>Uses {@link DamagerTracker} to resolve the last-hit player, because the vanilla
 * {@code entity.getKiller()} is not set when our custom damage pipeline cancels the vanilla
 * event and applies HP reduction directly.
 *
 * <p>Listens at {@link EventPriority#NORMAL} so the tracker data is still available before
 * {@link com.github._255_ping.rpg.core.mobs.MobLootListener} clears it at HIGH priority.
 */
public final class PlayerKillAbilityListener implements Listener {

    private final PassiveAbilityFirer firer;
    private final DamagerTracker tracker;

    public PlayerKillAbilityListener(PassiveAbilityFirer firer, DamagerTracker tracker) {
        this.firer = firer;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity dead)) return;
        // Only fire for RPG mobs; skip plain vanilla entities.
        try {
            if (RpgServices.mobs().from(dead).isEmpty()) return;
        } catch (IllegalStateException ex) {
            return;
        }

        Player killer = tracker.lastHitter(dead.getUniqueId());
        if (killer == null || !killer.isOnline()) return;

        // Fire with null target — the mob is dead and targeting it is not meaningful.
        firer.fire(killer, PlayerAbilityTrigger.ON_KILL, null);
    }
}
