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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hub / main menu GUI — 54-slot, opened by right-clicking the persistent Main Menu Item.
 *
 * <p>Top-level screen: no Back button, Close at slot 49 only.
 * Each feature button opens the relevant GUI as a <em>nested</em> screen (Back → Main Menu,
 * Close always present).
 *
 * <h3>Layout</h3>
 * <pre>
 * Slot  4: ✦ RPGCORE ✦  (NETHER_STAR, title/logo)
 *
 * Slot 10: ⚔ Stats      — opens StatsGui nested
 * Slot 11: ✦ Skills     — opens SkillsGui nested
 * Slot 12: 📜 Quests    — grayed placeholder
 * Slot 13: 🏆 Achievements — opens AchievementGui nested
 * Slot 14: ⚑ Party      — grayed placeholder
 * Slot 15: 🛡 Guild      — grayed placeholder
 *
 * Slot 20: ✉ Mail       — grayed placeholder
 * Slot 21: 💰 Economy   — opens WalletGui nested
 *
 * Slot 49: ❌ Close
 * </pre>
 */
public final class MainMenuGui implements Listener {

    // ── Slot layout ─────────────────────────────────────────────────────────────

    private static final int SLOT_LOGO         =  4;
    private static final int SLOT_STATS        = 10;
    private static final int SLOT_SKILLS       = 11;
    private static final int SLOT_QUESTS       = 12;
    private static final int SLOT_ACHIEVEMENTS = 13;
    private static final int SLOT_PARTY        = 14;
    private static final int SLOT_GUILD        = 15;
    private static final int SLOT_MAIL         = 20;
    private static final int SLOT_ECONOMY      = 21;

    // ── Dependencies ─────────────────────────────────────────────────────────────

    private final JavaPlugin      plugin;
    private final StatsGui        statsGui;
    private final SkillsGui       skillsGui;
    private final AchievementGui  achievementGui;
    private final WalletGui       walletGui;

    // ── State tracking ───────────────────────────────────────────────────────────

    /** viewer UUID → true (open) */
    private final Map<UUID, Boolean>   viewers     = new HashMap<>();
    private final Map<Inventory, UUID> invToViewer = new HashMap<>();

    public MainMenuGui(JavaPlugin plugin,
                       StatsGui statsGui,
                       SkillsGui skillsGui,
                       AchievementGui achievementGui,
                       WalletGui walletGui) {
        this.plugin         = plugin;
        this.statsGui       = statsGui;
        this.skillsGui      = skillsGui;
        this.achievementGui = achievementGui;
        this.walletGui      = walletGui;
    }

    // ── Open ─────────────────────────────────────────────────────────────────────

    public void open(Player viewer) {
        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("✦ RPGCORE ✦", NamedTextColor.GOLD));

        // Logo
        inv.setItem(SLOT_LOGO, buildLogo());

        // Feature buttons
        inv.setItem(SLOT_STATS,        buildHead(viewer, "⚔ Stats",        "View your character stats & gear."));
        inv.setItem(SLOT_SKILLS,       buildButton(Material.DIAMOND_SWORD,  "✦ Skills",        NamedTextColor.GREEN,  "View your skill levels and XP.", false));
        inv.setItem(SLOT_QUESTS,       buildButton(Material.WRITABLE_BOOK,  "📜 Quests",        NamedTextColor.YELLOW, "Quest journal.", true));
        inv.setItem(SLOT_ACHIEVEMENTS, buildButton(Material.DIAMOND,        "🏆 Achievements",  NamedTextColor.GOLD,   "View your achievements.", false));
        inv.setItem(SLOT_PARTY,        buildButton(Material.IRON_SWORD,     "⚑ Party",          NamedTextColor.AQUA,   "Party management.", true));
        inv.setItem(SLOT_GUILD,        buildButton(Material.SHIELD,         "🛡 Guild",          NamedTextColor.LIGHT_PURPLE, "Guild features.", true));
        inv.setItem(SLOT_MAIL,         buildButton(Material.PAPER,          "✉ Mail",           NamedTextColor.WHITE,  "Player mail.", true));
        inv.setItem(SLOT_ECONOMY,      buildButton(Material.EMERALD,        "💰 Economy",        NamedTextColor.GREEN,  "View your balance.", false));

        // Nav bar — top-level: Close only (slot 49), no Back
        RpgServices.guiConfig().fillBackground(inv);
        RpgServices.guiConfig().placeNavBar(inv);

        UUID viewerId = viewer.getUniqueId();
        viewers.put(viewerId, true);
        invToViewer.put(inv, viewerId);
        viewer.openInventory(inv);
    }

    // ── Events ───────────────────────────────────────────────────────────────────

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
            case SLOT_STATS -> {
                viewer.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> statsGui.open(viewer, viewer, () -> open(viewer)));
            }
            case SLOT_SKILLS -> {
                viewer.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> skillsGui.open(viewer, viewer, () -> open(viewer)));
            }
            case SLOT_ACHIEVEMENTS -> {
                viewer.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> achievementGui.open(viewer, viewer, () -> open(viewer)));
            }
            case SLOT_ECONOMY -> {
                viewer.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> walletGui.open(viewer, () -> open(viewer)));
            }
            // grayed-out stubs — no action
            default -> { }
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

    // ── Builders ─────────────────────────────────────────────────────────────────

    private static ItemStack buildLogo() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text("✦ RPGCORE ✦", NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Welcome to RPGCORE.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Use the buttons below to navigate.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    /** Player skull button for the Stats slot — shows the viewer's own head. */
    private static ItemStack buildHead(Player player, String name, String description) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (!(item.getItemMeta() instanceof SkullMeta skull)) return item;
        skull.setOwningPlayer(player);
        skull.displayName(Component.text(name, NamedTextColor.YELLOW, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        skull.lore(List.of(
                Component.text(description, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Click to open!", NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        skull.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(skull);
        return item;
    }

    private static ItemStack buildButton(Material mat, String name, NamedTextColor color,
                                         String description, boolean comingSoon) {
        Material display = comingSoon ? Material.GRAY_DYE : mat;
        NamedTextColor col = comingSoon ? NamedTextColor.DARK_GRAY : color;

        ItemStack item = new ItemStack(display);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        Component displayName = comingSoon
                ? Component.text(name, NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH)
                        .decoration(TextDecoration.BOLD, false)
                        .decoration(TextDecoration.ITALIC, false)
                : Component.text(name, col, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false);

        meta.displayName(displayName);
        if (comingSoon) {
            meta.lore(List.of(
                    Component.text("Coming soon!", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        } else {
            meta.lore(List.of(
                    Component.text(description, NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Click to open!", NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false)
            ));
        }
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }
}
