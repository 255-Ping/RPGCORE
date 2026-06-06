package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.skills.Skill;
import com.github._255_ping.rpg.api.skills.SkillsService;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 54-slot GUI showing all registered skills for a player, with level,
 * XP total, XP to next level, and a progress bar.
 *
 * <p>Layout:
 * <ul>
 *   <li>Slot 4: header item (EXPERIENCE_BOTTLE, viewer's name).</li>
 *   <li>Slots 9-44: one item per skill (rows 1-4, up to 36 skills).</li>
 *   <li>Row 5 (slots 45-53): standard nav bar.</li>
 * </ul>
 */
public final class SkillsGui implements Listener {

    private static final int SLOT_HEADER    = 4;
    private static final int CONTENT_START  = 9;
    private static final int CONTENT_END    = 44;

    /** Skill ID → icon material. Falls back to BOOK for unknown IDs. */
    private static final Map<String, Material> SKILL_ICONS = Map.ofEntries(
            Map.entry("combat",      Material.DIAMOND_SWORD),
            Map.entry("mining",      Material.DIAMOND_PICKAXE),
            Map.entry("foraging",    Material.IRON_AXE),
            Map.entry("farming",     Material.WHEAT),
            Map.entry("fishing",     Material.FISHING_ROD),
            Map.entry("cooking",     Material.FURNACE),
            Map.entry("alchemy",     Material.BREWING_STAND),
            Map.entry("enchanting",  Material.ENCHANTING_TABLE),
            Map.entry("smithing",    Material.ANVIL),
            Map.entry("smelting",    Material.BLAST_FURNACE),
            Map.entry("woodcutting", Material.OAK_LOG),
            Map.entry("herbalism",   Material.SUNFLOWER),
            Map.entry("agility",     Material.FEATHER),
            Map.entry("arcane",      Material.BLAZE_POWDER)
    );

    private final Map<UUID, UUID> openViews   = new HashMap<>();   // viewer → target
    private final Map<Inventory, UUID> invToViewer = new HashMap<>();
    private final Map<UUID, Runnable>  backCallbacks = new HashMap<>();

    private final JavaPlugin plugin;

    public SkillsGui(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Open ──────────────────────────────────────────────────────────────────

    public void open(Player viewer, Player target) {
        open(viewer, target, null);
    }

    public void open(Player viewer, Player target, Runnable onBack) {
        SkillsService svc = RpgServices.skills();
        List<Skill> skills = new ArrayList<>(RpgServices.skillRegistry().all());
        skills.sort(Comparator.comparing(Skill::displayName));

        Component title = Component.text(target.getName() + "'s Skills", NamedTextColor.GREEN);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Header
        inv.setItem(SLOT_HEADER, buildHeader(target, svc, skills));

        // Skill items
        int slot = CONTENT_START;
        for (Skill skill : skills) {
            if (slot > CONTENT_END) break;
            inv.setItem(slot, buildSkillItem(skill, target, svc));
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
        if (clicked == null || clicked.getType() == Material.AIR) return;

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

    // ── Builders ─────────────────────────────────────────────────────────────

    private static ItemStack buildHeader(Player target, SkillsService svc, List<Skill> skills) {
        long totalXp = skills.stream().mapToLong(s -> svc.totalXp(target, s.id())).sum();
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text(target.getName() + "'s Skills", NamedTextColor.GREEN, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Total XP: " + totalXp, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                Component.text("Skills: " + skills.size(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildSkillItem(Skill skill, Player target, SkillsService svc) {
        Material mat = SKILL_ICONS.getOrDefault(skill.id().toLowerCase(Locale.ROOT), Material.BOOK);
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        int  level  = svc.level(target, skill.id());
        int  max    = svc.maxLevel(skill.id());
        long total  = svc.totalXp(target, skill.id());
        long toNext = svc.xpToNext(target, skill.id());

        meta.displayName(Component.text(skill.displayName(), NamedTextColor.YELLOW, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Level: " + level + " / " + max, NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));

        // XP progress bar (20 chars wide)
        if (level < max && toNext > 0) {
            // percentage of progress toward next level — approximate from totalXp
            long nextLvlXp = total + toNext;
            long prevLvlXp = level > 0 ? nextLvlXp - toNext : 0;
            long inLevel = total - prevLvlXp;
            int filled = toNext > 0 ? (int) Math.min(20, inLevel * 20 / toNext) : 20;
            String bar = "§a" + "█".repeat(filled) + "§8" + "░".repeat(20 - filled);
            lore.add(Component.text(bar).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("XP to next: " + toNext, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        } else if (level >= max) {
            lore.add(Component.text("§a" + "█".repeat(20)).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("MAX LEVEL", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.text("Total XP: " + total, NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }
}
