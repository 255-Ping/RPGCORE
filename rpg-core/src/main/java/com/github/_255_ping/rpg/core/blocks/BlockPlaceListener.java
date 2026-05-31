package com.github._255_ping.rpg.core.blocks;

import com.github._255_ping.rpg.api.RpgServices;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Automatically registers custom-block locations when an admin places an item
 * obtained via {@code /rpg block give}.
 *
 * <p>The item is tagged with {@link #BLOCK_ITEM_KEY} in its PDC (set in
 * {@code RpgCommand.handleBlockGive}). On a successful place event the location
 * is tagged in {@link CoreBlockRegistry} and the block persistence is saved, so
 * the block is immediately treated as a custom block without needing
 * {@code /rpg block convert}.
 *
 * <p>Only fires for players with {@code rpg.admin} — normal players cannot hold
 * or place these items anyway (they carry the admin lore), but the permission
 * guard adds a safety net.
 */
public final class BlockPlaceListener implements Listener {

    /** PDC key set on items produced by {@code /rpg block give}. */
    public static final String KEY = "rpg_block_id";

    private final CoreBlockRegistry registry;
    private final BlockPersistence persistence;
    private final NamespacedKey blockItemKey;

    public BlockPlaceListener(CoreBlockRegistry registry, BlockPersistence persistence,
                              NamespacedKey blockItemKey) {
        this.registry = registry;
        this.persistence = persistence;
        this.blockItemKey = blockItemKey;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        // Only admins in creative can place (consistent with break permission).
        if (!player.hasPermission("rpg.admin")) return;
        if (player.getGameMode() != GameMode.CREATIVE) return;

        ItemStack item = event.getItemInHand();
        if (!item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String blockId = meta.getPersistentDataContainer()
                .get(blockItemKey, PersistentDataType.STRING);
        if (blockId == null) return;

        // Verify the block definition still exists.
        if (RpgServices.blocks().get(blockId).isEmpty()) return;

        registry.tagLocation(event.getBlock().getLocation(), blockId);
        persistence.save();
        player.sendActionBar(net.kyori.adventure.text.Component.text(
                "§aRegistered §e" + blockId + " §aat this location."));
    }
}
