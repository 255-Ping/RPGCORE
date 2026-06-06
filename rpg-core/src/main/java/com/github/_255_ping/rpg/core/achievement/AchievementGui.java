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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 54-slot chest GUI showing all achievements.
 *
 * <p>Layout:
 * <ul>
 *   <li>Slot 4: progress summary (NETHER_STAR, done/total).</li>
 *   <li>Slots 9-44: achievement items (up to 36, rows 1-4).</li>
 *   <li>Row 5 (slots 45-53): standard nav bar (Back if nested, Close always).</li>
 * </ul>
 *
 * <p>Locked achievements show a GRAY_STAINED_GLASS_PANE with "???" unless the category
 * can be hinted. COUNTER achievements show their current progress even while locked.
 * All clicks are cancelled — the GUI is read-only.
 *
 * <p>Supports nested navigation: pass a non-null {@code onBack} to {@link #open} and a
 * ← Back button appears at slot 45; clicking it closes and reopens the parent GUI.
 */
public final class AchievementGui implements Listener {

    private static final int SLOT_SUMMARY = 4;
    private static final int CONTENT_START = 9;   // row 1
    private static final int CONTENT_END   = 44;  // row 4 last slot (inclusive)

    /** viewer UUID → target UUID */
    private final Map<UUID, UUID> openViews = new HashMap<>();
    /** inventory → viewer UUID */
    private final Map<Inventory, UUID> invToViewer = new HashMap<>();
    /** viewer UUID → back runnable (null = top-level) */
    private final Map<UUID, Runnable> backCallbacks = new HashMap<>();

    private final JavaPlugin plugin;

    public AchievementGui(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Open ──────────────────────────────────────────────────────────────────

    /** Opens as a top-level screen (Close only). */
    public void open(Player viewer, Player target) {
        open(viewer, target, null);
    }

    /**
     * Opens the achievement GUI.
     *
     * @param onBack when non-null, shows ← Back at slot 45; clicking it reopens the parent.
     */
    public void open(Player viewer, Player target, Runnable onBack) {
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

        String titleStr = target.getName() + "'s Achievements (" + done + "/" + total + ")";
        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(titleStr, NamedTextColor.GOLD));

        // Summary item at slot 4
        inv.setItem(SLOT_SUMMARY, buildSummary(done, total));

        // Achievement items in rows 1-4 (slots 9-44)
        int slot = CONTENT_START;
        for (AchievementDef def : all) {
            if (slot > CONTENT_END) break;
            boolean unlocked = svc.isUnlocked(target, def.id());
            inv.setItem(slot, buildItem(def, unlocked, svc, target));
            slot++;
        }

        // Nav bar
        RpgServices.guiConfig().fillBackground(inv);
        if (onBack != null) {
            RpgServices.guiConfig().placeNavBarNested(inv);
        } else {
            RpgServices.guiConfig().placeNavBar(inv);
        }

        UUID viewerId = viewer.getUniqueId();
        openViews.put(viewerId, target.getUniqueId());
        invToViewer.put(inv, viewerId);
        if (onBack != null) backCallbacks.put(viewerId, onBack);
        viewer.openInventory(inv);
    }

    // ── Events ────────────────────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        UUID viewerId = viewer.getUniqueId();
        if (!openViews.containsKey(viewerId)) return;
        if (!invToViewer.containsKey(event.getInventory())) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == org.bukkit.Material.AIR) return;

        if (RpgServices.guiConfig().isCloseButton(clicked)) {
            viewer.closeInventory();
            return;
        }
        if (RpgServices.guiConfig().isBackButton(clicked)) {
            Runnable cb = backCallbacks.get(viewerId);
            viewer.closeInventory();
            if (cb != null) plugin.getServer().getScheduler().runTask(plugin, cb);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player viewer)) return;
        UUID viewerId = viewer.getUniqueId();
        if (!openViews.containsKey(viewerId)) return;
        Inventory inv = event.getInventory();
        if (!invToViewer.containsKey(inv)) return;
        openViews.remove(viewerId);
        invToViewer.remove(inv);
        backCallbacks.remove(viewerId);
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private ItemStack buildItem(AchievementDef def, boolean unlocked,
                                AchievementService svc, Player target) {
        ItemStack item;
        ItemMeta  meta;

        if (unlocked) {
            item = new ItemStack(Material.LIME_DYE);
            meta = item.getItemMeta();
            if (meta == null) return item;
            meta.displayName(Component.text("✔ " + def.title(), NamedTextColor.GREEN, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(def.description(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Category: " + def.category(), NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            if (!def.reward().isEmpty()) {
                lore.add(Component.empty());
                StringBuilder rb = new StringBuilder("Reward: ");
                if (def.reward().money().compareTo(java.math.BigDecimal.ZERO) > 0)
                    rb.append("$").append(def.reward().money().toPlainString());
                if (def.reward().xp() > 0) rb.append(" ").append(def.reward().xp()).append(" XP");
                lore.add(Component.text(rb.toString(), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        } else {
            item = new ItemStack(Material.GRAY_DYE);
            meta = item.getItemMeta();
            if (meta == null) return item;
            meta.displayName(Component.text("???", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Category: " + def.category(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            if (def.isCounter()) {
                long cur = svc.getCounter(target, def.counterKey());
                lore.add(Component.text("Progress: " + cur + " / " + def.target(), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text(def.description(), NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
        }

        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildSummary(long done, long total) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        int pct    = total > 0 ? (int) (done * 100 / total) : 0;
        int filled = total > 0 ? (int) (done * 20  / total) : 0;
        String bar = "§a" + "█".repeat(filled) + "§8" + "░".repeat(20 - filled);

        meta.displayName(Component.text("Progress: " + done + "/" + total + " (" + pct + "%)",
                NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(bar).decoration(TextDecoration.ITALIC, false)));
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }
}
