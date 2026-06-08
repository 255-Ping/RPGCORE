package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.api.RpgServices;
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
 * 54-slot Social hub GUI opened by the crafting-nav Social button or {@code /social}.
 *
 * <h3>Layout</h3>
 * <pre>
 * Slot  4: ✦ Social        (NETHER_STAR, decorative title)
 * Slot 11: 👥 Friends      (placeholder — Friends system not yet built)
 * Slot 13: ⚑ Party         (IRON_SWORD, live — /party command)
 * Slot 15: 🛡 Guild         (placeholder — Guild GUI not yet built)
 * Slot 22: ✉ Mail          (placeholder — Mail system not yet built)
 * Slot 49: ❌ Close
 * </pre>
 *
 * <p>All placeholders use {@code GRAY_DYE} + strikethrough name + {@code &8[Not yet available]}.
 * When a system ships, swap the placeholder for the live button in-place.
 */
public final class SocialGui implements Listener {

    private static final int SLOT_TITLE   =  4;
    private static final int SLOT_FRIENDS = 11;
    private static final int SLOT_PARTY   = 13;
    private static final int SLOT_GUILD   = 15;
    private static final int SLOT_MAIL    = 22;

    private final JavaPlugin plugin;

    private final Map<UUID, Boolean>   viewers     = new HashMap<>();
    private final Map<Inventory, UUID> invToViewer = new HashMap<>();

    public SocialGui(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Public API ──────────────────────────────────────────────────────────────

    public void open(Player viewer) {
        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("⚑ Social", NamedTextColor.AQUA));

        inv.setItem(SLOT_TITLE,   buildTitle());
        inv.setItem(SLOT_FRIENDS, buildPlaceholder("👥 Friends",  "&7Manage and interact with your friends."));
        inv.setItem(SLOT_PARTY,   buildLive(Material.IRON_SWORD,  "⚑ Party",   NamedTextColor.GREEN,      "View and manage your party."));
        inv.setItem(SLOT_GUILD,   buildPlaceholder("🛡 Guild",    "&7Guild features and management."));
        inv.setItem(SLOT_MAIL,    buildPlaceholder("✉ Mail",      "&7Send and receive player mail."));

        RpgServices.guiConfig().fillBackground(inv);
        RpgServices.guiConfig().placeNavBar(inv);

        UUID id = viewer.getUniqueId();
        viewers.put(id, true);
        invToViewer.put(inv, id);
        viewer.openInventory(inv);
    }

    // ── Events ──────────────────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        UUID id = viewer.getUniqueId();
        if (!viewers.containsKey(id)) return;
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
            case SLOT_PARTY -> {
                // /party with no args opens PartyGui when rpg-parties is loaded
                viewer.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> viewer.performCommand("party"));
            }
            // Placeholders — no action
            default -> { }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player viewer)) return;
        UUID id = viewer.getUniqueId();
        if (!viewers.containsKey(id)) return;
        Inventory inv = event.getInventory();
        if (!invToViewer.containsKey(inv)) return;
        viewers.remove(id);
        invToViewer.remove(inv);
    }

    // ── Builders ────────────────────────────────────────────────────────────────

    private static ItemStack buildTitle() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text("⚑ Social", NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Friends, party, guild, and mail.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildLive(Material mat, String name, NamedTextColor color, String desc) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text(name, color, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(desc, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Click to open!", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildPlaceholder(String name, String desc) {
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text(name, NamedTextColor.DARK_GRAY, TextDecoration.STRIKETHROUGH)
                .decoration(TextDecoration.BOLD, false).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Not yet available.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }
}
