package com.github._255_ping.rpg.api.mobs;

import com.github._255_ping.rpg.api.stats.Stat;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;

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

    /**
     * Kill XP awarded to the killer. {@code 0} means use the combat addon's configured
     * {@code default-kill-xp} value instead. Set {@code XP: N} in the mob YAML.
     */
    long xp();

    LivingEntity spawn(Location loc);

    /**
     * Returns the boss bar display config for this mob, if one is defined.
     * Present when the mob YAML has a {@code BossBar:} section.
     * {@code rpg-bossbar} reads this to decide which mobs get proximity bars.
     */
    default Optional<BossBarDef> bossBar() { return Optional.empty(); }
}
