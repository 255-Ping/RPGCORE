package com.github._255_ping.rpg.core.abilities;

import com.github._255_ping.rpg.api.abilities.PlayerAbilityTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Fires {@link PlayerAbilityTrigger#ON_HURT} bindings when a player receives damage.
 */
public final class PlayerHurtAbilityListener implements Listener {

    private final PassiveAbilityFirer firer;

    public PlayerHurtAbilityListener(PassiveAbilityFirer firer) {
        this.firer = firer;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHurt(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        firer.fire(player, PlayerAbilityTrigger.ON_HURT, null);
    }
}
