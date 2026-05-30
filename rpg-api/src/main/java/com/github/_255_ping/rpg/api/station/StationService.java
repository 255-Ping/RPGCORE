package com.github._255_ping.rpg.api.station;

import com.github._255_ping.rpg.api.blocks.Block;
import org.bukkit.entity.Player;
import java.util.function.BiConsumer;

/**
 * Central registry for custom-block station handlers. Addons call
 * {@link #register} in their {@code onEnable} to claim a {@code StationType}
 * string. rpg-core's {@code BlockInteractListener} calls {@link #open} when a
 * player right-clicks a custom block whose {@code stationType()} matches.
 */
public interface StationService {

    /**
     * Register a handler for the given station-type string (case-insensitive).
     * Replaces any prior handler for the same type.
     */
    void register(String stationType, BiConsumer<Player, Block> handler);

    /**
     * Dispatch a right-click interaction to the registered handler.
     *
     * @return {@code true} if a handler was found and called (caller should
     *         cancel the original {@code PlayerInteractEvent}), {@code false}
     *         if no handler is registered for the type.
     */
    boolean open(String stationType, Player player, Block block);
}
