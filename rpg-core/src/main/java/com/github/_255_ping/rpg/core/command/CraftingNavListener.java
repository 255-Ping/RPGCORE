package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.core.achievement.AchievementGui;
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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Places five phantom navigation buttons in the player's 2×2 crafting grid + output slot whenever
 * they open their survival inventory. These are not real items — they are set into the crafting
 * inventory server-side and cleared on close so they never persist in the player's real inventory.
 *
 * <h3>Slot assignment (InventoryType.CRAFTING slots)</h3>
 * <pre>
 *   Slot 0: output  → Settings GUI
 *   Slot 1: top-left    → Profile GUI
 *   Slot 2: top-right   → Skills GUI
 *   Slot 3: bottom-left → Social GUI
 *   Slot 4: bottom-right→ Adventure GUI
 * </pre>
 *
 * <p>If the player already has items in those crafting slots (unusual but possible), they are
 * saved in {@code savedCrafting} and returned directly to the player's main inventory on close.
 */
public final class CraftingNavListener implements Listener {

    /** PDC key stamped on every nav-button item so we can identify them after close. */
    private static final String NAV_KEY = "rpg_nav_btn";

    private final JavaPlugin   plugin;
    private final NamespacedKey navKey;

    // GUIs opened by each slot
    private final ProfileGui   profileGui;
    private final SkillsGui    skillsGui;
    private final SocialGui    socialGui;
    private final AdventureGui adventureGui;
    private final SettingsGui  settingsGui;

    /** Crafting-slot backups per player (null entries = slot was empty). */
    private final Map<UUID, ItemStack[]> savedCrafting = new HashMap<>();

    public CraftingNavListener(JavaPlugin plugin,
                               ProfileGui profileGui,
                               SkillsGui skillsGui,
                               SocialGui socialGui,
                               AdventureGui adventureGui,
                               SettingsGui settingsGui) {
        this.plugin       = plugin;
        this.navKey       = new NamespacedKey(plugin, NAV_KEY);
        this.profileGui   = profileGui;
        this.skillsGui    = skillsGui;
        this.socialGui    = socialGui;
        this.adventureGui = adventureGui;
        this.settingsGui  = settingsGui;
    }

    // ── Inventory open ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getType() != InventoryType.CRAFTING) return;

        UUID id = player.getUniqueId();
        Inventory crafting = event.getInventory();

        // One-tick defer — inventory is fully open on the next tick.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (player.getOpenInventory().getTopInventory().getType() != InventoryType.CRAFTING) return;

            // Save whatever was in the crafting slots (usually all null)
            ItemStack[] saved = new ItemStack[5];
            for (int i = 0; i < 5; i++) {
                ItemStack existing = crafting.getItem(i);
                if (existing != null && !existing.getType().isAir()) {
                    saved[i] = existing.clone();
                }
            }
            savedCrafting.put(id, saved);

            // Place nav buttons
            crafting.setItem(0, buildSettingsButton());
            crafting.setItem(1, buildProfileButton(player));
            crafting.setItem(2, buildSkillsButton());
            crafting.setItem(3, buildSocialButton());
            crafting.setItem(4, buildAdventureButton());

            // Force-sync the crafting grid to the client.  Without this the server
            // changes the slot state but never sends the update packet, so the player
            // never sees the buttons appear.
            player.updateInventory();
        });
    }

    // ── Click handler ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getType() != InventoryType.CRAFTING) return;
        // Only handle clicks in the crafting top inventory (slots 0-4)
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().getType() != InventoryType.CRAFTING) return;

        int slot = event.getSlot();
        if (slot < 0 || slot > 4) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !isNavButton(clicked)) return;

        event.setCancelled(true);

        switch (slot) {
            case 0 -> {                          // Settings
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> settingsGui.open(player));
            }
            case 1 -> {                          // Profile
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> profileGui.open(player, player));
            }
            case 2 -> {                          // Skills
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> skillsGui.open(player, player));
            }
            case 3 -> {                          // Social
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> socialGui.open(player));
            }
            case 4 -> {                          // Adventure
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> adventureGui.open(player));
            }
            default -> { }
        }
    }

    // ── Close handler ──────────────────────────────────────────────────────────

    /**
     * Cleans up phantom nav buttons. We null out the nav-button slots directly in the crafting
     * inventory before the close event finishes — Bukkit returns crafting items to the player's
     * main inventory only AFTER all close-event handlers run, so nulling here means the buttons
     * are never returned and never land in the player's real inventory.
     *
     * <p>Original items (saved on open) are restored to the crafting slots so Bukkit can return
     * those back to the player correctly in the unlikely case they had items there.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getType() != InventoryType.CRAFTING) return;

        UUID id = player.getUniqueId();
        ItemStack[] saved = savedCrafting.remove(id);
        if (saved == null) return; // wasn't a managed session

        Inventory crafting = event.getInventory();

        // Clear nav buttons directly from the crafting slots.  Bukkit hasn't moved them to the
        // player's main inventory yet at this point, so nulling here prevents them from landing
        // there at all.
        for (int i = 0; i < 5; i++) {
            ItemStack item = crafting.getItem(i);
            if (item != null && isNavButton(item)) {
                crafting.setItem(i, null);
            }
        }

        // Restore whatever was originally in those crafting slots (almost always all null).
        // Bukkit will then return these back to the player's main inventory as normal.
        for (int i = 0; i < 5; i++) {
            if (saved[i] != null && !saved[i].getType().isAir()) {
                crafting.setItem(i, saved[i]);
            }
        }
    }

    /** Drain the savedCrafting map when a player disconnects to prevent memory leaks. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        savedCrafting.remove(event.getPlayer().getUniqueId());
    }

    // ── Button builders ────────────────────────────────────────────────────────

    private ItemStack buildProfileButton(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (item.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(player);
            meta.displayName(Component.text("⚑ Profile", NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("View your stats, skills, and profile.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            tagAndFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildSkillsButton() {
        return navButton(Material.DIAMOND_SWORD, "✦ Skills", NamedTextColor.GREEN,
                "View your skill levels and progress.");
    }

    private ItemStack buildSocialButton() {
        return navButton(Material.IRON_SWORD, "⚑ Social", NamedTextColor.AQUA,
                "Friends, party, guild, and mail.");
    }

    private ItemStack buildAdventureButton() {
        return navButton(Material.WRITABLE_BOOK, "📜 Adventure", NamedTextColor.GOLD,
                "Quests, economy, and achievements.");
    }

    private ItemStack buildSettingsButton() {
        return navButton(Material.COMPARATOR, "⚙ Settings", NamedTextColor.GRAY,
                "Adjust your player settings.");
    }

    private ItemStack navButton(Material mat, String name, NamedTextColor color, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text(name, color, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(lore, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        tagAndFlag(meta);
        item.setItemMeta(meta);
        return item;
    }

    private void tagAndFlag(ItemMeta meta) {
        meta.getPersistentDataContainer().set(navKey, PersistentDataType.BYTE, (byte) 1);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
    }

    private boolean isNavButton(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(navKey, PersistentDataType.BYTE);
    }
}
