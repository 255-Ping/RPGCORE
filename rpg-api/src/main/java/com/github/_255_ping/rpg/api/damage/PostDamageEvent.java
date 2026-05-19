package com.github._255_ping.rpg.api.damage;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PostDamageEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final DamageContext context;
    private final double dealtDamage;

    public PostDamageEvent(DamageContext context, double dealtDamage) {
        this.context = context;
        this.dealtDamage = dealtDamage;
    }

    public DamageContext context() { return context; }
    public double dealtDamage() { return dealtDamage; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
