package com.github._255_ping.rpg.core.items;

import com.github._255_ping.rpg.api.RpgServices;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class ConsumableItemListener implements Listener {

    private final NamespacedKey itemIdKey;

    public ConsumableItemListener(NamespacedKey itemIdKey) {
        this.itemIdKey = itemIdKey;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack stack = event.getItem();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        String itemId = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (itemId == null) return;

        RpgServices.items().get(itemId).ifPresent(item -> {
            if (!(item instanceof CoreRpgItem core)) return;
            for (CoreRpgItem.ConsumeEffect effect : core.consumeEffects()) {
                try {
                    RpgServices.statusEffects().apply(
                            event.getPlayer(), effect.effectId(), effect.level(),
                            effect.durationTicks(), "item:" + itemId);
                } catch (Exception ignored) {
                    // unknown effect id — skip silently
                }
            }
        });
    }
}
