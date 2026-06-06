package com.github._255_ping.rpg.core.achievement;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.achievement.AchievementDef;
import com.github._255_ping.rpg.api.achievement.AchievementService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 54-slot chest GUI showing all achievements, grouped by category.
 *
 * <p>Unlocked achievements show their icon; locked ones show a grey stained-glass pane
 * with "???" as the title. A star icon in the top-right corner shows overall progress.
 *
 * <p>The GUI is read-only — all clicks are cancelled.
 */
public final class AchievementGui implements Listener {

    private static final int SIZE = 54;
    private static final String TITLE_PREFIX = "Achievements";

    /** UUIDs of players currently viewing an achievement GUI. */
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    // ── Open ──────────────────────────────────────────────────────────────────

    /**
     * Opens the achievement GUI for the given player, viewing {@code target}'s achievements.
     * Typically {@code viewer == target}, but ops can view other players.
     */
    public void open(Player viewer, Player target) {
        AchievementService svc;
        try {
            svc = RpgServices.achievements();
        } catch (IllegalStateException ex) {
            viewer.sendMessage(Component.text("Achievement system not available.", NamedTextColor.RED));
            return;
        }

        List<AchievementDef> all = new ArrayList<>(svc.all());
        long total = all.size();
        long done  = all.stream().filter(d -> svc.isUnlocked(target, d.id())).count();

        String title = TITLE_PREFIX + (viewer == target ? "" : " — " + target.getName())
                + " (" + done + "/" + total + ")";
        Inventory inv = Bukkit.createInventory(null, SIZE, Component.text(title, NamedTextColor.GOLD));

        // Fill with achievements (up to 53 slots; last slot = summary star)
        int slot = 0;
        for (AchievementDef def : all) {
            if (slot >= SIZE - 1) break;
            boolean unlocked = svc.isUnlocked(target, def.id());
            inv.setItem(slot, buildItem(def, unlocked, svc, target));
            slot++;
        }

        // Summary item in last slot
        inv.setItem(SIZE - 1, buildSummary(done, total));

        viewers.add(viewer.getUniqueId());
        viewer.openInventory(inv);
    }

    // ── Events ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!viewers.contains(player.getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        viewers.remove(event.getPlayer().getUniqueId());
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private ItemStack buildItem(AchievementDef def, boolean unlocked, AchievementService svc, Player target) {
        Material mat;
        try {
            mat = Material.valueOf(def.icon().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            mat = Material.BOOK;
        }

        ItemStack item;
        ItemMeta meta;

        if (unlocked) {
            item = new ItemStack(mat);
            meta = item.getItemMeta();
            if (meta == null) return item;
            meta.displayName(Component.text("✔ " + def.title(), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(def.description(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Category: " + def.category(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("✔ Unlocked", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            if (!def.reward().isEmpty()) {
                lore.add(Component.empty());
                lore.add(Component.text("Reward collected", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        } else {
            // Locked — show as grey glass pane, hide details
            item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            meta = item.getItemMeta();
            if (meta == null) return item;
            meta.displayName(Component.text("???", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Category: " + def.category(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            // Show counter progress for COUNTER achievements
            if (def.isCounter()) {
                long cur = svc.getCounter(target, def.counterKey());
                lore.add(Component.text("Progress: " + cur + " / " + def.target(), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text(def.description(), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
        }
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildSummary(long done, long total) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        int pct = total > 0 ? (int) (done * 100 / total) : 0;
        meta.displayName(Component.text("Progress: " + done + "/" + total + " (" + pct + "%)", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        // Simple ASCII bar
        int filled = total > 0 ? (int) (done * 20 / total) : 0;
        String bar = "§a" + "█".repeat(filled) + "§8" + "░".repeat(20 - filled);
        lore.add(Component.text(bar).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }
}
