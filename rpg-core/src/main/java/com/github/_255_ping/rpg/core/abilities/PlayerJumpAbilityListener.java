package com.github._255_ping.rpg.core.abilities;

import com.github._255_ping.rpg.api.abilities.PlayerAbilityTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;

/**
 * Fires {@link PlayerAbilityTrigger#ON_JUMP} bindings when a player jumps.
 */
public final class PlayerJumpAbilityListener implements Listener {

    private final PassiveAbilityFirer firer;

    public PlayerJumpAbilityListener(PassiveAbilityFirer firer) {
        this.firer = firer;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJump(PlayerJumpEvent event) {
        firer.fire(event.getPlayer(), PlayerAbilityTrigger.ON_JUMP, null);
    }
}
