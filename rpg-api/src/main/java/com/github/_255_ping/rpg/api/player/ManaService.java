package com.github._255_ping.rpg.api.player;

import org.bukkit.entity.Player;

public interface ManaService {
    boolean consume(Player player, double amount);
    void restore(Player player, double amount);
    double get(Player player);
    void setRegenRate(Player player, double perSecond);
}
