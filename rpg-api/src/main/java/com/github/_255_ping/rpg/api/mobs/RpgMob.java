package com.github._255_ping.rpg.api.mobs;

import com.github._255_ping.rpg.api.stats.Stat;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public interface RpgMob {
    String id();
    String displayName();
    EntityType baseType();
    double maxHealth();
    double damage();
    double defense();
    Map<Stat, Double> stats();
    ItemStack helmet();
    ItemStack chestplate();
    ItemStack leggings();
    ItemStack boots();
    ItemStack mainHand();
    ItemStack offHand();
    String customHeadTexture();

    LivingEntity spawn(Location loc);
}
