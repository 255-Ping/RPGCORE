package com.github._255_ping.rpg.api.regions;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class RegionLeaveEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Region region;

    public RegionLeaveEvent(Player player, Region region) {
        super(player);
        this.region = region;
    }

    public Region region() { return region; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
