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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Simple 54-slot wallet / economy summary GUI.
 *
 * <p>Layout:
 * <ul>
 *   <li>Slot 4: balance display (EMERALD, shows current balance prominently).</li>
 *   <li>Slot 13: transaction log shortcut ("View /money log in chat").</li>
 *   <li>Slot 22: money earning tips.</li>
 *   <li>Row 5: nav bar.</li>
 * </ul>
 */
public final class WalletGui implements Listener {

    private static final int SLOT_BALANCE = 4;
    private static final int SLOT_LOG     = 13;
    private static final int SLOT_TIPS    = 22;

    private final Map<UUID, Runnable>  backCallbacks = new HashMap<>();
    private final Map<UUID, Boolean>   viewers       = new HashMap<>();   // UUID → true (open)
    private final Map<Inventory, UUID> invToViewer   = new HashMap<>();

    private final JavaPlugin plugin;

    public WalletGui(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Open ──────────────────────────────────────────────────────────────────

    public void open(Player viewer) {
        open(viewer, null);
    }

    public void open(Player viewer, Runnable onBack) {
        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("💰 Wallet", NamedTextColor.GOLD));

        // Balance item
        BigDecimal balance;
        try {
            balance = RpgServices.economy().balance(viewer);
        } catch (IllegalStateException ex) {
            balance = BigDecimal.ZERO;
        }

        // Format using the primary currency's own formatter; fall back to plain "$x.xx".
        // Capture balance as effectively-final so it can be used in lambdas.
        final BigDecimal balanceFinal = balance;
        String balanceFormatted = String.format("$%,.2f", balanceFinal);
        String currencyName     = "Coins";
        try {
            var opt = RpgServices.currencies().primary();
            if (opt.isPresent()) {
                balanceFormatted = opt.get().format(balanceFinal);
                currencyName     = opt.get().displayPlural();
            }
        } catch (IllegalStateException ignored) {}

        inv.setItem(SLOT_BALANCE, buildBalance(balanceFormatted, currencyName));
        inv.setItem(SLOT_LOG,     buildLogShortcut());
        inv.setItem(SLOT_TIPS,    buildTips());

        RpgServices.guiConfig().fillBackground(inv);
        if (onBack != null) {
            RpgServices.guiConfig().placeNavBarNested(inv);
        } else {
            RpgServices.guiConfig().placeNavBar(inv);
        }

        UUID viewerId = viewer.getUniqueId();
        viewers.put(viewerId, true);
        invToViewer.put(inv, viewerId);
        if (onBack != null) backCallbacks.put(viewerId, onBack);
        viewer.openInventory(inv);
    }

    // ── Events ────────────────────────────────────────────────────────────────

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
        if (RpgServices.guiConfig().isBackButton(clicked)) {
            Runnable cb = backCallbacks.get(viewerId);
            viewer.closeInventory();
            if (cb != null) plugin.getServer().getScheduler().runTask(plugin, cb);
            return;
        }

        // Log shortcut — run /money log in chat
        if (event.getRawSlot() == SLOT_LOG) {
            viewer.closeInventory();
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> viewer.performCommand("money log"));
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
        backCallbacks.remove(viewerId);
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private static ItemStack buildBalance(String formatted, String currencyName) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Component.text(formatted, NamedTextColor.GREEN, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Currency: " + currencyName, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Your current balance.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildLogShortcut() {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text("📜 Transaction Log", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to view your recent", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("transactions in chat.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("(/money log)", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildTips() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text("💡 Earning Tips", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("• Kill mobs for currency drops", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Complete quests for rewards", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Trade with other players", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Use /trade <player> to exchange", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }
}
