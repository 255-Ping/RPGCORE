package com.github._255_ping.rpg.core.abilities;

import com.github._255_ping.rpg.api.abilities.PlayerAbilityTrigger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Fires {@link PlayerAbilityTrigger#ON_LOGIN} bindings when a player joins the server.
 *
 * <p>The event fires after the player's inventory is fully loaded, so item-based ability
 * bindings (armor, main-hand) are available when this triggers. The target is {@code null}
 * since login has no inherent target entity.
 */
public final class PlayerLoginAbilityListener implements Listener {

    private final PassiveAbilityFirer firer;

    public PlayerLoginAbilityListener(PassiveAbilityFirer firer) {
        this.firer = firer;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        firer.fire(event.getPlayer(), PlayerAbilityTrigger.ON_LOGIN, null);
    }
}
