package com.github._255_ping.rpg.api.stats;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class StatRecalcEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final StatHolder holder;

    public StatRecalcEvent(Player player, StatHolder holder) {
        super(player);
        this.holder = holder;
    }

    public StatHolder holder() {
        return holder;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
