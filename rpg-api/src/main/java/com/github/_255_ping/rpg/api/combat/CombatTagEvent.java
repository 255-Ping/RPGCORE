package com.github._255_ping.rpg.api.combat;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player transitions from out-of-combat to in-combat. Not re-fired while combat tag
 * is refreshed; addons that want every tag-extension event can listen and combine with their own
 * timer.
 */
public class CombatTagEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final long durationSeconds;

    public CombatTagEvent(Player player, long durationSeconds) {
        super(player);
        this.durationSeconds = durationSeconds;
    }

    public long durationSeconds() { return durationSeconds; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
