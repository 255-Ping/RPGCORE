package com.github._255_ping.rpg.api.blocks;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fires when a player successfully breaks an RPG-tagged custom block. Fired by rpg-core's
 * BlockBreakHandler just before the location is untagged and drops are rolled, so listeners
 * can read the live state. Cancelling aborts the break (item drops + respawn skipped).
 */
public class RpgBlockBreakEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Block block;
    private final Location location;
    private boolean cancelled;

    public RpgBlockBreakEvent(Player player, Block block, Location location) {
        this.player = player;
        this.block = block;
        this.location = location;
    }

    public Player player() { return player; }
    public Block block() { return block; }
    public Location location() { return location; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
