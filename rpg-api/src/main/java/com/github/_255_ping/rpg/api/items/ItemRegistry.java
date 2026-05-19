package com.github._255_ping.rpg.api.items;

import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Optional;

public interface ItemRegistry {
    void register(RpgItem item);
    Optional<RpgItem> get(String id);
    Optional<RpgItem> from(ItemStack stack);
    Collection<RpgItem> all();
}
