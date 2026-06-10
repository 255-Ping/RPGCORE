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
import org.bukkit.event.inventory.InventoryCloseEvent;
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
 * Places five phantom navigation buttons in the player's 2×2 crafting grid + output slot
 * whenever they open their survival inventory (E key). The buttons are not real items — they
 * are set into the crafting inventory server-side and cleared on close so they never persist.
 *
 * <h3>Why InventoryOpenEvent isn't used</h3>
 * <p>When a player presses E, the client opens the inventory locally without sending an open
 * packet to the server. Bukkit's {@code InventoryOpenEvent} with {@code InventoryType.CRAFTING}
 * therefore never fires for the E-key open. Instead, a 2-tick repeating task polls
 * {@code getOpenInventory().getTopInventory()} for all online players and initialises buttons
 * on the first tick it sees a CRAFTING-type top inventory for a session that hasn't been
 * set up yet.
 *
 * <h3>Slot assignment (InventoryType.CRAFTING)</h3>
 * <pre>
 *   Slot 0: output slot → Settings GUI
 *   Slot 1: top-left    → Profile GUI
 *   Slot 2: top-right   → Skills GUI
 *   Slot 3: bottom-left → Social GUI
 *   Slot 4: bottom-right→ Adventure GUI
 * </pre>
 */
public final class CraftingNavListener implements Listener {

    private static final String NAV_KEY = "rpg_nav_btn";

    private final JavaPlugin    plugin;
    private final NamespacedKey navKey;

    private final ProfileGui    profileGui;
    private final SkillsGui     skillsGui;
    private final SocialGui     socialGui;
    private final AdventureGui  adventureGui;
    private final SettingsGui   settingsGui;

    /** Original crafting-slot contents saved per player session. Null entries = slot was empty. */
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

        // Poll every 2 ticks to detect when a player opens their crafting inventory (E key).
        // InventoryOpenEvent does not fire for E-key opens — the client handles it locally.
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0L, 2L);
    }

    // ── Per-tick detection ─────────────────────────────────────────────────────

    private void tick() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Inventory top = p.getOpenInventory().getTopInventory();
            UUID id = p.getUniqueId();

            if (top.getType() == InventoryType.CRAFTING) {
                if (!savedCrafting.containsKey(id)) {
                    placeButtons(p, top);
                }
            } else if (savedCrafting.containsKey(id)) {
                // Inventory changed away from crafting without firing InventoryCloseEvent
                // (e.g. another plugin force-opened a GUI) — clean up the stale session.
                savedCrafting.remove(id);
            }
        }
    }

    private void placeButtons(Player player, Inventory crafting) {
        UUID id = player.getUniqueId();

        // Save whatever was originally in the crafting slots (almost always all null).
        ItemStack[] saved = new ItemStack[5];
        for (int i = 0; i < 5; i++) {
            ItemStack existing = crafting.getItem(i);
            if (existing != null && !existing.getType().isAir() && !isNavButton(existing)) {
                saved[i] = existing.clone();
            }
        }
        savedCrafting.put(id, saved);

        // Set input slots first (1-4), then output slot (0) last.
        // Setting inputs can trigger a server-side recipe check that would clear slot 0;
        // setting slot 0 last ensures our button isn't overwritten by a null result.
        crafting.setItem(1, buildProfileButton(player));
        crafting.setItem(2, buildSkillsButton());
        crafting.setItem(3, buildSocialButton());
        crafting.setItem(4, buildAdventureButton());
        crafting.setItem(0, buildSettingsButton());

        // Force the client to sync — setItem alone updates server state but may not
        // send the slot-update packet needed for the client to render the changes.
        player.updateInventory();
    }

    // ── Click handler ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getType() != InventoryType.CRAFTING) return;
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().getType() != InventoryType.CRAFTING) return;

        int slot = event.getSlot();
        if (slot < 0 || slot > 4) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !isNavButton(clicked)) return;

        event.setCancelled(true);

        switch (slot) {
            case 0 -> {
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin, () -> settingsGui.open(player));
            }
            case 1 -> {
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin, () -> profileGui.open(player, player));
            }
            case 2 -> {
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin, () -> skillsGui.open(player, player));
            }
            case 3 -> {
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin, () -> socialGui.open(player));
            }
            case 4 -> {
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin, () -> adventureGui.open(player));
            }
            default -> { }
        }
    }

    // ── Close handler ──────────────────────────────────────────────────────────

    /**
     * Clears nav buttons directly from the crafting slots before Bukkit can return them to the
     * player's main inventory. Bukkit moves crafting items to the player inventory only AFTER all
     * close-event handlers run, so nulling the slots here prevents the buttons from landing there.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getType() != InventoryType.CRAFTING) return;

        UUID id = player.getUniqueId();
        ItemStack[] saved = savedCrafting.remove(id);
        if (saved == null) return;

        Inventory crafting = event.getInventory();

        // Remove nav buttons before Bukkit returns crafting items to the player.
        for (int i = 0; i < 5; i++) {
            ItemStack item = crafting.getItem(i);
            if (item != null && isNavButton(item)) {
                crafting.setItem(i, null);
            }
        }

        // Restore any items the player had originally (almost always nothing).
        for (int i = 0; i < 5; i++) {
            if (saved[i] != null && !saved[i].getType().isAir()) {
                crafting.setItem(i, saved[i]);
            }
        }
    }

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
