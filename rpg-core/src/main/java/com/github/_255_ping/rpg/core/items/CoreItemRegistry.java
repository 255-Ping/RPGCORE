package com.github._255_ping.rpg.core.items;

import com.github._255_ping.rpg.api.items.ItemRegistry;
import com.github._255_ping.rpg.api.items.RpgItem;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CoreItemRegistry implements ItemRegistry {

    private final ConcurrentMap<String, RpgItem> byId = new ConcurrentHashMap<>();
    private final NamespacedKey itemIdKey;

    public CoreItemRegistry(NamespacedKey itemIdKey) {
        this.itemIdKey = itemIdKey;
    }

    @Override
    public void register(RpgItem item) {
        byId.put(item.id(), item);
    }

    @Override
    public Optional<RpgItem> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<RpgItem> from(ItemStack stack) {
        if (stack == null) return Optional.empty();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return Optional.empty();
        String id = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (id == null) return Optional.empty();
        return get(id);
    }

    @Override
    public Collection<RpgItem> all() {
        return byId.values();
    }

    public NamespacedKey idKey() {
        return itemIdKey;
    }

    public void clear() {
        byId.clear();
    }
}
