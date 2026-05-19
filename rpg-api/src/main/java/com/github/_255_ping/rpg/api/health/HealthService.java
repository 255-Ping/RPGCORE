package com.github._255_ping.rpg.api.health;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface HealthService {
    double currentHp(LivingEntity entity);
    double maxHp(LivingEntity entity);
    void setCurrentHp(LivingEntity entity, double hp);
    void heal(LivingEntity entity, double amount);
    void damage(LivingEntity entity, double amount, String source);
    void syncDisplay(Player player);
    boolean inCombat(Player player);
    void markInCombat(Player player);
}
