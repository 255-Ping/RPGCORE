package com.github._255_ping.rpg.api.loot;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public interface LootTable {
    String id();
    Attribution attribution();
    RollMode rollMode();
    Map<Player, List<ItemStack>> roll(LootContext context);
}
