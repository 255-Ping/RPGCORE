package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.achievement.AchievementDef;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 54-slot profile GUI opened by {@code /profile [player]}.
 *
 * <h3>Layout</h3>
 * <pre>
 * Row 0: [Head(0)]  [bg] [Combat(2)] [Mining(3)] [Foraging(4)] [Fishing(5)] [bg] [SkillAvg(7)] [Balance(8)]
 * Row 1: [bg]       [bg] [Farming(11)] [Cooking(12)] [Alchemy(13)] [Enchanting(14)] [bg] [bg] [bg]
 * Row 2: [bg]       [bg] [Ach1(20)] [Ach2(21)] [Ach3(22)] [bg] [bg] [bg] [bg]
 * Row 3: [bg only — visual separator]
 * Row 4: [bg] ...   [AH(42)] [Stats(43)] [Trade(44) — shown when viewing another player]
 * Row 5: [nav bar — Close at slot 49]
 * </pre>
 *
 * <p>The Stats button drills into {@link StatsGui} with a Back callback that reopens this screen.
 * Economy and achievement data degrade gracefully if those services aren't loaded.
 */
public final class ProfileGui implements Listener {

    // ── Slot constants ────────────────────────────────────────────────────────

    private static final int SLOT_HEAD       = 0;
    // Row 0 skills
    private static final int SLOT_COMBAT     = 2;
    private static final int SLOT_MINING     = 3;
    private static final int SLOT_FORAGING   = 4;
    private static final int SLOT_FISHING    = 5;
    private static final int SLOT_BALANCE    = 8;
    // Row 1 skills
    private static final int SLOT_FARMING    = 11;
    private static final int SLOT_COOKING    = 12;
    private static final int SLOT_ALCHEMY    = 13;
    private static final int SLOT_ENCHANTING = 14;
    // Row 2 – recent achievements (first 3 unlocked, definition order)
    private static final int SLOT_ACH_1     = 20;
    private static final int SLOT_ACH_2     = 21;
    private static final int SLOT_ACH_3     = 22;
    // Row 0 – skill average (slot between fishing and balance, both bg previously)
    private static final int SLOT_SKILL_AVG = 7;
    // Row 4 – action buttons
    private static final int SLOT_AH        = 42;
    private static final int SLOT_STATS     = 43;
    private static final int SLOT_TRADE     = 44;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final Component GUI_TITLE_SUFFIX = LEGACY.deserialize("&r&7's Profile");

    // ── State tracking ────────────────────────────────────────────────────────

    /** viewer UUID → target UUID */
    private final Map<UUID, UUID> openViews   = new HashMap<>();
    /** inventory reference → viewer UUID */
    private final Map<Inventory, UUID> invToViewer = new HashMap<>();

    private final JavaPlugin plugin;
    private final StatsGui   statsGui;

    public ProfileGui(JavaPlugin plugin, StatsGui statsGui) {
        this.plugin   = plugin;
        this.statsGui = statsGui;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Opens the profile GUI for {@code target} as seen by {@code viewer}. */
    public void open(Player viewer, Player target) {
        Component title = Component.text(target.getName(), NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(GUI_TITLE_SUFFIX);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // ── Player head (slot 0) ───────────────────────────────────────────
        inv.setItem(SLOT_HEAD, buildHead(target));

        // ── Skill level items ──────────────────────────────────────────────
        inv.setItem(SLOT_COMBAT,     buildSkillItem("combat",     target, Material.DIAMOND_SWORD,    "&c⚔ Combat"));
        inv.setItem(SLOT_MINING,     buildSkillItem("mining",     target, Material.IRON_PICKAXE,     "&7⛏ Mining"));
        inv.setItem(SLOT_FORAGING,   buildSkillItem("foraging",   target, Material.OAK_LOG,          "&a🌲 Foraging"));
        inv.setItem(SLOT_FISHING,    buildSkillItem("fishing",    target, Material.FISHING_ROD,      "&b🎣 Fishing"));
        inv.setItem(SLOT_FARMING,    buildSkillItem("farming",    target, Material.WHEAT,            "&e🌾 Farming"));
        inv.setItem(SLOT_COOKING,    buildSkillItem("cooking",    target, Material.FURNACE,          "&6🍳 Cooking"));
        inv.setItem(SLOT_ALCHEMY,    buildSkillItem("alchemy",    target, Material.BLAZE_POWDER,     "&d⚗ Alchemy"));
        inv.setItem(SLOT_ENCHANTING, buildSkillItem("enchanting", target, Material.ENCHANTING_TABLE, "&3✦ Enchanting"));

        // ── Skill average (slot 7) ────────────────────────────────────────
        inv.setItem(SLOT_SKILL_AVG, buildSkillAverageItem(target));

        // ── Balance (slot 8) ───────────────────────────────────────────────
        inv.setItem(SLOT_BALANCE, buildBalanceItem(target));

        // ── Recent achievements (first 3 unlocked, in definition order) ───
        List<AchievementDef> recentUnlocked = collectUnlocked(target, 3);
        int[] achSlots = { SLOT_ACH_1, SLOT_ACH_2, SLOT_ACH_3 };
        for (int i = 0; i < achSlots.length; i++) {
            inv.setItem(achSlots[i],
                    i < recentUnlocked.size()
                            ? buildAchievementItem(recentUnlocked.get(i))
                            : buildEmptyAchSlot());
        }

        // ── Action buttons (row 4) ─────────────────────────────────────────
        inv.setItem(SLOT_AH, makeButton(Material.GOLD_BLOCK, "&8🏪 Auction House",
                List.of("&8Coming soon.")));
        inv.setItem(SLOT_STATS, makeButton(Material.BOOK, "&e📊 View Stats",
                List.of("&7Open the detailed stat breakdown", "&7for &e" + target.getName() + "&7.")));
        if (!viewer.getUniqueId().equals(target.getUniqueId())) {
            inv.setItem(SLOT_TRADE, makeButton(Material.IRON_SWORD, "&6⚔ Trade",
                    List.of("&7Send a trade request to", "&e" + target.getName() + "&7.")));
        }

        // ── Background fill + nav bar ──────────────────────────────────────
        RpgServices.guiConfig().fillBackground(inv);
        RpgServices.guiConfig().placeNavBar(inv);

        // ── Track and open ─────────────────────────────────────────────────
        UUID viewerId = viewer.getUniqueId();
        openViews.put(viewerId, target.getUniqueId());
        invToViewer.put(inv, viewerId);
        viewer.openInventory(inv);
    }

    // ── Event handlers ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOW)
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

        int slot = event.getRawSlot();

        if (slot == SLOT_STATS) {
            UUID targetId = openViews.get(viewerId);
            Player target = Bukkit.getPlayer(targetId);
            if (target == null) { viewer.closeInventory(); return; }
            viewer.closeInventory();
            // Open StatsGui; clicking Back in StatsGui reopens this profile screen.
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    statsGui.open(viewer, target,
                            () -> plugin.getServer().getScheduler().runTask(plugin,
                                    () -> open(viewer, target))));
        } else if (slot == SLOT_TRADE) {
            UUID targetId = openViews.get(viewerId);
            if (targetId != null && !viewerId.equals(targetId)) {
                Player target = Bukkit.getPlayer(targetId);
                if (target != null) {
                    viewer.closeInventory();
                    viewer.performCommand("trade " + target.getName());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player viewer)) return;
        UUID viewerId = viewer.getUniqueId();
        if (!openViews.containsKey(viewerId)) return;
        Inventory inv = event.getInventory();
        if (!invToViewer.containsKey(inv)) return;
        openViews.remove(viewerId);
        invToViewer.remove(inv);
    }

    // ── Item builders ──────────────────────────────────────────────────────────

    private static ItemStack buildHead(Player target) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(LEGACY.deserialize("&b&l" + target.getName())
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        // Party membership — degrade gracefully if rpg-parties is absent
        try {
            RpgServices.parties().partyOf(target).ifPresentOrElse(
                    p -> lore.add(LEGACY.deserialize("&7Party: &f" + p.members().size() + " members")
                                        .decoration(TextDecoration.ITALIC, false)),
                    () -> lore.add(LEGACY.deserialize("&8No party")
                                         .decoration(TextDecoration.ITALIC, false)));
        } catch (IllegalStateException ignored) {
            lore.add(LEGACY.deserialize("&8Party: unavailable")
                           .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        skull.setItemMeta(meta);
        return skull;
    }

    private static ItemStack buildSkillItem(String skillId, Player target,
                                            Material mat, String label) {
        int level;
        try {
            level = RpgServices.skills().level(target, skillId);
        } catch (IllegalStateException ignored) {
            level = 0;
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(label).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                LEGACY.deserialize("&7Level: &f" + level).decoration(TextDecoration.ITALIC, false)));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    /** Shows the average skill level across all 8 skills with per-skill breakdown in lore. */
    private static ItemStack buildSkillAverageItem(Player target) {
        String[] skillIds   = {"combat","mining","foraging","fishing","farming","cooking","alchemy","enchanting"};
        String[] skillLabels = {"⚔ Combat","⛏ Mining","🌲 Foraging","🎣 Fishing","🌾 Farming","🍳 Cooking","⚗ Alchemy","✦ Enchanting"};

        int total = 0;
        int[] levels = new int[skillIds.length];
        for (int i = 0; i < skillIds.length; i++) {
            try {
                levels[i] = RpgServices.skills().level(target, skillIds[i]);
            } catch (IllegalStateException ignored) {}
            total += levels[i];
        }
        int avg = total / skillIds.length;

        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize("&a✦ Skill Average &7(Lv. &f" + avg + "&7)")
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (int i = 0; i < skillIds.length; i++) {
            lore.add(LEGACY.deserialize("&7" + skillLabels[i] + ": &f" + levels[i])
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildBalanceItem(Player target) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize("&6Balance").decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        try {
            BigDecimal balance = RpgServices.economy().balance(target);
            String formatted = RpgServices.currencies().primary()
                    .map(c -> c.format(balance))
                    .orElseGet(() -> "$" + String.format("%,.0f", balance.doubleValue()));
            lore.add(LEGACY.deserialize("&f" + formatted).decoration(TextDecoration.ITALIC, false));
        } catch (IllegalStateException ignored) {
            lore.add(LEGACY.deserialize("&8Economy not available").decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Returns the first {@code max} achievements that {@code target} has unlocked,
     * in the order they appear in the achievement registry. Returns an empty list
     * if the achievement service is unavailable.
     */
    private static List<AchievementDef> collectUnlocked(Player target, int max) {
        List<AchievementDef> out = new ArrayList<>();
        try {
            for (AchievementDef def : RpgServices.achievements().all()) {
                if (RpgServices.achievements().isUnlocked(target, def.id())) {
                    out.add(def);
                    if (out.size() >= max) break;
                }
            }
        } catch (IllegalStateException ignored) {}
        return out;
    }

    private static ItemStack buildAchievementItem(AchievementDef def) {
        Material mat = Material.NETHER_STAR;
        if (def.icon() != null) {
            Material parsed = Material.matchMaterial(def.icon());
            if (parsed != null) mat = parsed;
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize("&6✦ " + def.title())
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(LEGACY.deserialize("&7" + def.description())
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("&a✔ Unlocked").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildEmptyAchSlot() {
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize("&8— No achievement —")
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeButton(Material mat, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(name).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String l : loreLines) {
            lore.add(LEGACY.deserialize(l).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }
}
