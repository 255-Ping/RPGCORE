package com.github._255_ping.rpg.core.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Delivers and protects the persistent Main Menu Item.
 *
 * <ul>
 *   <li>Gives the item to the player in the configured hotbar slot on join
 *       (only if they don't already have it in that slot).</li>
 *   <li>Prevents the player from dropping or moving it out of its slot.</li>
 *   <li>Right-click → opens {@link MainMenuGui}.</li>
 * </ul>
 *
 * <p>Config keys read via constructor parameters (resolved in {@link
 * com.github._255_ping.rpg.core.RpgCorePlugin}):
 * <pre>
 * main-menu:
 *   enabled: true
 *   slot: 8          # hotbar slot 0-8
 *   material: COMPASS
 *   name: "§6✦ Menu §6✦"
 * </pre>
 */
public final class MainMenuListener implements Listener {

    /** PDC tag stamped on the item so we can identify it by data, not just appearance. */
    public static final String PDC_KEY = "rpg_main_menu";

    private final JavaPlugin   plugin;
    private final MainMenuGui  gui;
    private final NamespacedKey itemKey;

    private final boolean  enabled;
    private final int      slot;
    private final Material material;
    private final String   displayName;

    public MainMenuListener(JavaPlugin plugin, MainMenuGui gui, NamespacedKey itemKey,
                            boolean enabled, int slot, Material material, String displayName) {
        this.plugin      = plugin;
        this.gui         = gui;
        this.itemKey     = itemKey;
        this.enabled     = enabled;
        this.slot        = Math.max(0, Math.min(8, slot));
        this.material    = material;
        this.displayName = displayName;
    }

    // ── Item delivery ─────────────────────────────────────────────────────────

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        // Defer by one tick — inventory is fully initialised on the next tick after join.
        plugin.getServer().getScheduler().runTask(plugin, () -> ensureHasItem(player));
    }

    /** Gives or replaces the item in the configured slot if it's missing or wrong. */
    public void ensureHasItem(Player player) {
        if (!enabled) return;
        ItemStack current = player.getInventory().getItem(slot);
        if (current != null && isMenuItem(current)) return;   // already there
        player.getInventory().setItem(slot, buildItem());
    }

    // ── Drop prevention ───────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (!enabled) return;
        if (isMenuItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    // ── Move-out-of-slot prevention ───────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enabled) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        // Check both the cursor and the slot item — player might shift-click or swap.
        ItemStack current = event.getCurrentItem();
        ItemStack cursor  = event.getCursor();

        boolean touchingMenuItem =
                (current != null && isMenuItem(current)) ||
                (cursor  != null && isMenuItem(cursor));

        if (touchingMenuItem) {
            event.setCancelled(true);
        }
    }

    // ── Right-click to open ───────────────────────────────────────────────────

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled) return;
        // Only fire on main hand right-click
        if (event.getHand() != EquipmentSlot.HAND) return;
        var action = event.getAction();
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Player    player = event.getPlayer();
        ItemStack held   = player.getInventory().getItemInMainHand();
        if (!isMenuItem(held)) return;

        event.setCancelled(true);
        gui.open(player);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Checks whether an item is our main menu item by its PDC tag.
     * Robust against material changes, renames, and enchants.
     */
    public boolean isMenuItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    /** Builds a fresh main menu item with the configured appearance. */
    public ItemStack buildItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        // Use legacy color codes from config if present; wrap in Adventure component otherwise.
        meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand()
                .deserialize(displayName)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(List.of(
                Component.text("Right-click to open the menu.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
        meta.addItemFlags(
                org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP
        );
        item.setItemMeta(meta);
        return item;
    }
}
