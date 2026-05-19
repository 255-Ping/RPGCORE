package com.github._255_ping.rpg.api.skills;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class SkillXpAwardEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final String skillId;
    private long amount;
    private boolean cancelled;

    public SkillXpAwardEvent(Player player, String skillId, long amount) {
        super(player);
        this.skillId = skillId;
        this.amount = amount;
    }

    public String skillId() { return skillId; }
    public long amount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
