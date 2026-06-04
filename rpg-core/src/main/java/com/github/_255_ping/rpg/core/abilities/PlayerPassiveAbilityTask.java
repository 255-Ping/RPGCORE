package com.github._255_ping.rpg.core.abilities;

import com.github._255_ping.rpg.api.abilities.PlayerAbilityTrigger;
import org.bukkit.Server;

/**
 * Repeating task that fires {@link PlayerAbilityTrigger#PASSIVE} bindings on every online player.
 * The interval is controlled by {@code abilities.passive-interval-ticks} in {@code config.yml}
 * (default: 20 ticks = once per second).
 */
public final class PlayerPassiveAbilityTask implements Runnable {

    private final Server server;
    private final PassiveAbilityFirer firer;

    public PlayerPassiveAbilityTask(Server server, PassiveAbilityFirer firer) {
        this.server = server;
        this.firer = firer;
    }

    @Override
    public void run() {
        for (var player : server.getOnlinePlayers()) {
            firer.fire(player, PlayerAbilityTrigger.PASSIVE, null);
        }
    }
}
