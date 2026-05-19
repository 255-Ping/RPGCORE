package com.github._255_ping.rpg.api.items;

import com.github._255_ping.rpg.api.abilities.AbilityInvocation;
import com.github._255_ping.rpg.api.stats.Stat;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public interface RpgItem {
    String id();
    String displayName();
    ItemType type();
    Rarity rarity();
    Material material();
    int customModelData();
    Map<Stat, Double> stats();
    List<AbilityInvocation> abilities();
    List<String> extraLore();

    ItemStack toItemStack();
}
