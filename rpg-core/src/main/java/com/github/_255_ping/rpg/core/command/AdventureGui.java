package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.core.achievement.AchievementGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 54-slot Adventure hub GUI opened by {@code /adventure}.
 *
 * <h3>Layout</h3>
 * <pre>
 * Slot  4: ⚑ Adventure   (COMPASS, title item)
 * Slot 12: 📜 Quests      (GRAY_DYE, coming-soon placeholder)
 * Slot 13: 💰 Economy     (EMERALD, live — opens WalletGui nested)
 * Slot 14: 🏆 Achievements (DIAMOND, live — opens AchievementGui nested)
 * Slot 49: ❌ Close
 * </pre>
 */
public final class AdventureGui implements Listener {

    private static final int SLOT_TITLE        =  4;
    private static final int SLOT_QUESTS       = 12;
    private static final int SLOT_ECONOMY      = 13;
    private static final int SLOT_ACHIEVEMENTS = 14;

    private final JavaPlugin      plugin;
    private final WalletGui       walletGui;
    private final AchievementGui  achievementGui;

    private final Map<UUID, Boolean>   viewers     = new HashMap<>();
    private final Map<Inventory, UUID> invToViewer = new HashMap<>();

    public AdventureGui(JavaPlugin plugin, WalletGui walletGui, AchievementGui achievementGui) {
        this.plugin         = plugin;
        this.walletGui      = walletGui;
        this.achievementGui = achievementGui;
    }

    // ── Public API ──────────────────────────────────────────────────────────────

    public void open(Player viewer) {
        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("⚑ Adventure", NamedTextColor.GREEN));

        inv.setItem(SLOT_TITLE,        buildTitle());
        inv.setItem(SLOT_QUESTS,       buildButton(Material.GRAY_DYE,   "📜 Quests",       NamedTextColor.DARK_GRAY,  "Quest journal.",          true));
        inv.setItem(SLOT_ECONOMY,      buildButton(Material.EMERALD,    "💰 Economy",       NamedTextColor.GREEN,      "View your wallet.",        false));
        inv.setItem(SLOT_ACHIEVEMENTS, buildButton(Material.DIAMOND,    "🏆 Achievements",  NamedTextColor.GOLD,       "View your achievements.",  false));

        RpgServices.guiConfig().fillBackground(inv);
        RpgServices.guiConfig().placeNavBar(inv);

        UUID viewerId = viewer.getUniqueId();
        viewers.put(viewerId, true);
        invToViewer.put(inv, viewerId);
        viewer.openInventory(inv);
    }

    // ── Events ──────────────────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        UUID viewerId = viewer.getUniqueId();
        if (!viewers.containsKey(viewerId)) return;
        if (!invToViewer.containsKey(event.getInventory())) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (RpgServices.guiConfig().isCloseButton(clicked)) {
            viewer.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        switch (slot) {
            case SLOT_ECONOMY -> {
                viewer.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> walletGui.open(viewer, () -> open(viewer)));
            }
            case SLOT_ACHIEVEMENTS -> {
                viewer.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> achievementGui.open(viewer, viewer, () -> open(viewer)));
            }
            default -> { /* grayed-out slots do nothing */ }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player viewer)) return;
        UUID viewerId = viewer.getUniqueId();
        if (!viewers.containsKey(viewerId)) return;
        Inventory inv = event.getInventory();
        if (!invToViewer.containsKey(inv)) return;
        viewers.remove(viewerId);
        invToViewer.remove(inv);
    }

    // ── Builders ────────────────────────────────────────────────────────────────

    private static ItemStack buildTitle() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text("⚑ Adventure", NamedTextColor.GREEN, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Quests, economy, and achievements.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildButton(Material mat, String name, NamedTextColor color,
                                         String description, boolean comingSoon) {
        Material display = comingSoon ? Material.GRAY_DYE : mat;
        ItemStack item = new ItemStack(display);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (comingSoon) {
            meta.displayName(Component.text(name, NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH)
                    .decoration(TextDecoration.BOLD, false).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Coming soon!", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        } else {
            meta.displayName(Component.text(name, color, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text(description, NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Click to open!", NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }
}
