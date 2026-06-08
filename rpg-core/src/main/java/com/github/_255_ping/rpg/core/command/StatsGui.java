package com.github._255_ping.rpg.core.command;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import com.github._255_ping.rpg.api.stats.Stat;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 54-slot (6-row) inventory GUI for the {@code /stats [player]} command.
 *
 * <h3>Layout</h3>
 * <pre>
 * Row 0: [Helmet ] [bg][bg] [Combat   ] [Survival] [Caster ] [bg][bg][bg]
 * Row 1: [Chest  ] [bg][bg] [Mobility ] [Loot    ] [Wisdom ] [bg][bg][bg]
 * Row 2: [Legs   ] [bg][bg] [Skills   ] [bg      ] [bg     ] [bg][bg][bg]
 * Row 3: [Boots  ] [bg][bg] [bg       ] [bg      ] [bg     ] [bg][bg][bg]
 * Row 4: [Weapon ] [Off][Pet][bg      ] [bg      ] [bg     ] [bg][bg   ][bg]
 * Row 5: [nav bar — Close at centre]
 * </pre>
 *
 * <p>Gear slots show the target player's actual equipped items (read-only — all clicks cancelled).
 * Category items list every non-zero stat in that category in their lore.
 *
 * <p>All clicks inside the GUI are cancelled. The inventory is purely informational.
 */
public final class StatsGui implements Listener {

    // ── Slot constants ────────────────────────────────────────────────────────

    // Left column — gear
    private static final int SLOT_HELMET    = 0;
    private static final int SLOT_CHEST     = 9;
    private static final int SLOT_LEGS      = 18;
    private static final int SLOT_BOOTS     = 27;
    private static final int SLOT_WEAPON    = 36;
    private static final int SLOT_OFFHAND   = 37;
    private static final int SLOT_PET       = 38;  // placeholder — pets not yet implemented

    // Centre columns — stat categories (cols 3, 4, 5)
    private static final int SLOT_COMBAT    = 3;
    private static final int SLOT_SURVIVAL  = 4;
    private static final int SLOT_CASTER    = 5;
    private static final int SLOT_MOBILITY  = 12;
    private static final int SLOT_LOOT      = 13;
    private static final int SLOT_WISDOM    = 14;
    private static final int SLOT_SKILLS    = 21;  // gathering / mining / foraging / farming / fishing / enchanting

    // Stat groups shown in each category slot
    private static final Map<Integer, String[]> SLOT_GROUPS = Map.of(
            SLOT_COMBAT,   new String[]{"combat"},
            SLOT_SURVIVAL, new String[]{"survival"},
            SLOT_CASTER,   new String[]{"caster"},
            SLOT_MOBILITY, new String[]{"mobility"},
            SLOT_LOOT,     new String[]{"loot"},
            SLOT_WISDOM,   new String[]{"wisdom"},
            SLOT_SKILLS,   new String[]{"gathering", "mining", "foraging", "farming", "fishing", "enchanting", "pets"}
    );

    // Category display definitions: slot → (material, displayName, colour)
    private record CategoryDef(Material mat, String name, String color) {}
    private static final Map<Integer, CategoryDef> CATEGORY_DEFS = Map.of(
            SLOT_COMBAT,   new CategoryDef(Material.DIAMOND_SWORD,    "Combat",   "&c"),
            SLOT_SURVIVAL, new CategoryDef(Material.GOLDEN_APPLE,     "Survival", "&a"),
            SLOT_CASTER,   new CategoryDef(Material.BLAZE_POWDER,     "Caster",   "&b"),
            SLOT_MOBILITY, new CategoryDef(Material.FEATHER,          "Mobility", "&f"),
            SLOT_LOOT,     new CategoryDef(Material.CHEST,            "Loot",     "&6"),
            SLOT_WISDOM,   new CategoryDef(Material.BOOK,             "Wisdom",   "&3"),
            SLOT_SKILLS,   new CategoryDef(Material.IRON_PICKAXE,     "Skills",   "&e")
    );

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final Component GUI_TITLE_SUFFIX = LEGACY.deserialize("&r&7's Stats");

    /** One-line description for each stat, shown as a grey tooltip line in the lore. */
    private static final Map<String, String> STAT_DESC = Map.ofEntries(
            Map.entry("damage",              "Raw damage dealt with weapons."),
            Map.entry("strength",            "Increases melee damage."),
            Map.entry("crit_chance",         "Chance to deal a critical hit."),
            Map.entry("crit_damage",         "Bonus damage multiplier on critical hits."),
            Map.entry("ability_damage",      "Increases damage dealt by abilities."),
            Map.entry("attack_speed",        "Bonus attack speed over base."),
            Map.entry("ferocity",            "Chance to strike twice instantly."),
            Map.entry("lifesteal",           "% of damage dealt restored as health."),
            Map.entry("knockback",           "Knocks enemies further on hit."),
            Map.entry("ammo_usage_reduction","Chance to not consume ammo on each shot."),
            Map.entry("projectile_speed",    "Increases arrow and projectile velocity."),
            Map.entry("auto_loot",           "Automatically picks up drops in range."),
            Map.entry("max_health",          "Maximum health points."),
            Map.entry("health_regen",        "Health restored per second out of combat."),
            Map.entry("vitality",            "% increase to effective max health."),
            Map.entry("defense",             "Reduces incoming physical damage."),
            Map.entry("true_defense",        "Reduces all damage, including magic."),
            Map.entry("max_mana",            "Maximum mana pool size."),
            Map.entry("mana_regen",          "Mana restored per second."),
            Map.entry("intelligence",        "Boosts ability damage and mana pool."),
            Map.entry("cooldown_reduction",  "% reduction to ability cooldown durations."),
            Map.entry("speed",               "Movement speed bonus over base."),
            Map.entry("swing_range",         "Extra melee reach beyond the default 3 blocks."),
            Map.entry("magic_find",          "Increases chance of rare loot drops."),
            Map.entry("breaking_power",      "Required tier to break custom blocks."),
            Map.entry("mining_speed",        "Speed bonus when mining custom blocks."),
            Map.entry("mining_fortune",      "Bonus resources from custom mining."),
            Map.entry("foraging_speed",      "Speed bonus when chopping custom logs."),
            Map.entry("foraging_fortune",    "Bonus wood from custom foraging."),
            Map.entry("farming_fortune",     "Bonus crops from custom farming blocks."),
            Map.entry("fishing_speed",       "Faster fishing timers."),
            Map.entry("fishing_fortune",     "Bonus fish and treasure from fishing."),
            Map.entry("sea_creature_chance", "Chance to hook a sea creature while fishing."),
            Map.entry("combat_wisdom",       "% bonus Combat XP per kill."),
            Map.entry("mining_wisdom",       "% bonus Mining XP per block broken."),
            Map.entry("foraging_wisdom",     "% bonus Foraging XP per log."),
            Map.entry("farming_wisdom",      "% bonus Farming XP per crop."),
            Map.entry("fishing_wisdom",      "% bonus Fishing XP per catch."),
            Map.entry("cooking_wisdom",      "% bonus Cooking XP per recipe."),
            Map.entry("alchemy_wisdom",      "% bonus Alchemy XP per potion."),
            Map.entry("enchanting_wisdom",   "% bonus Enchanting XP per enchant.")
    );

    // ── State tracking ─────────────────────────────────────────────────────────

    /** viewer UUID → target UUID for open stats GUIs */
    private final Map<UUID, UUID> openViews = new HashMap<>();
    /** inventory reference → viewer UUID, for click routing */
    private final Map<Inventory, UUID> invToViewer = new HashMap<>();
    /** viewer UUID → back callback (null = top-level, no Back button) */
    private final Map<UUID, Runnable> backCallbacks = new HashMap<>();

    private final JavaPlugin plugin;

    public StatsGui(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Opens the stats GUI as a top-level screen (Close only). */
    public void open(Player viewer, Player target) {
        open(viewer, target, null);
    }

    /**
     * Opens the stats GUI, optionally as a nested screen.
     *
     * @param onBack when non-null, a ← Back button is added; clicking it closes the GUI and
     *               runs {@code onBack} on the next tick to reopen the parent.
     */
    public void open(Player viewer, Player target, Runnable onBack) {
        Component title = Component.text(target.getName(), NamedTextColor.GOLD,
                TextDecoration.BOLD).append(GUI_TITLE_SUFFIX);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        RpgPlayer rp = RpgServices.player(target);
        Map<Stat, Double> snap = rp.snapshot();

        // ── Gear column ────────────────────────────────────────────────────
        ItemStack[] armor = target.getInventory().getArmorContents();
        // armor[3]=helmet, [2]=chest, [1]=legs, [0]=boots
        placeGear(inv, SLOT_HELMET,  armor[3], Material.LEATHER_HELMET,   "&7Helmet");
        placeGear(inv, SLOT_CHEST,   armor[2], Material.LEATHER_CHESTPLATE,"&7Chestplate");
        placeGear(inv, SLOT_LEGS,    armor[1], Material.LEATHER_LEGGINGS,  "&7Leggings");
        placeGear(inv, SLOT_BOOTS,   armor[0], Material.LEATHER_BOOTS,     "&7Boots");
        placeGear(inv, SLOT_WEAPON,  target.getInventory().getItemInMainHand(), Material.STICK, "&7Weapon");
        ItemStack offhand = target.getInventory().getItemInOffHand();
        if (offhand.getType() != Material.AIR) {
            inv.setItem(SLOT_OFFHAND, offhand.clone());
        } else {
            inv.setItem(SLOT_OFFHAND, emptySlotItem(Material.SHIELD, "&8Offhand (empty)"));
        }
        inv.setItem(SLOT_PET, emptySlotItem(Material.BARRIER, "&8Pet &7(coming soon)"));

        // ── Stat category items ────────────────────────────────────────────
        for (Map.Entry<Integer, CategoryDef> e : CATEGORY_DEFS.entrySet()) {
            int slot = e.getKey();
            CategoryDef def = e.getValue();
            String[] groups = SLOT_GROUPS.get(slot);
            ItemStack item = buildCategoryItem(def, groups, snap);
            inv.setItem(slot, item);
        }

        // ── Background + nav bar ───────────────────────────────────────────
        RpgServices.guiConfig().fillBackground(inv);
        if (onBack != null) {
            RpgServices.guiConfig().placeNavBarNested(inv);
        } else {
            RpgServices.guiConfig().placeNavBar(inv);
        }

        // ── Track and open ─────────────────────────────────────────────────
        UUID viewerId = viewer.getUniqueId();
        openViews.put(viewerId, target.getUniqueId());
        invToViewer.put(inv, viewerId);
        if (onBack != null) backCallbacks.put(viewerId, onBack);
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

        // Nav bar buttons
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

        // No clickable action slots on this read-only screen.
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
        backCallbacks.remove(viewerId);
    }

    // ── Builders ───────────────────────────────────────────────────────────────

    /** Place the actual gear item if present, otherwise a dim placeholder. */
    private static void placeGear(Inventory inv, int slot, ItemStack item, Material placeholder, String emptyLabel) {
        if (item != null && item.getType() != Material.AIR) {
            inv.setItem(slot, item.clone());
        } else {
            inv.setItem(slot, emptySlotItem(placeholder, emptyLabel + " &8(empty)"));
        }
    }

    private static ItemStack buildCategoryItem(CategoryDef def, String[] groups, Map<Stat, Double> snap) {
        // Collect stats belonging to any of these groups, sorted by display name.
        SortedMap<String, Double> grouped = new TreeMap<>();
        Map<String, Stat> statById = new HashMap<>();
        for (Map.Entry<Stat, Double> e : snap.entrySet()) {
            if (e.getValue() == 0.0) continue;
            Stat s = e.getKey();
            for (String g : groups) {
                if (g.equals(s.group())) {
                    grouped.put(s.id(), e.getValue());
                    statById.put(s.id(), s);
                    break;
                }
            }
        }

        ItemStack item = new ItemStack(def.mat());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(def.color() + "&l" + def.name()).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (grouped.isEmpty()) {
            lore.add(LEGACY.deserialize("&8No active stats.").decoration(TextDecoration.ITALIC, false));
        } else {
            for (Map.Entry<String, Double> se : grouped.entrySet()) {
                Stat s = statById.get(se.getKey());
                if (s == null) continue;
                lore.add(LEGACY.deserialize(
                        "  " + s.colorCode() + s.displayName() + " &7: &f" + formatVal(se.getValue(), s.percent())
                ).decoration(TextDecoration.ITALIC, false));
                String desc = STAT_DESC.get(s.id());
                if (desc != null) {
                    lore.add(LEGACY.deserialize("    &8" + desc)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack emptySlotItem(Material mat, String label) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(label).decoration(TextDecoration.ITALIC, false));
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

    private static String formatVal(double v, boolean percent) {
        String num;
        if (v == Math.floor(v) && !Double.isInfinite(v)) num = Long.toString((long) v);
        else num = String.format("%.1f", v);
        return num + (percent ? "%" : "");
    }
}
