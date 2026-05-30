package com.github._255_ping.rpg.api.hud;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Priority action bar system. Any plugin can push a message that overrides the HUD's idle
 * format for a given duration. The HUD polls {@link #peek} each tick; if a message is
 * pending it shows that instead of the idle stats line.
 *
 * <p>Access via {@code RpgServices.actionBar()}.
 */
public interface ActionBarService {

    /**
     * Show {@code message} on {@code player}'s action bar for {@code durationTicks} ticks,
     * overriding the idle HUD format. Repeated calls extend/replace the current message.
     */
    void send(Player player, Component message, int durationTicks);

    /**
     * Returns the current priority message for {@code player}, or {@code null} if none is
     * pending (expired or never set). Clears expired entries automatically.
     */
    Component peek(Player player);
}
