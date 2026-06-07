package com.github._255_ping.rpg.accessories;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.accessories.AccessoryService;
import com.github._255_ping.rpg.api.items.BuiltinItemType;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.persistence.DataStore;
import com.github._255_ping.rpg.api.stats.Stat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player accessory bag inventory. ACCESSORY items inside contribute their stats via
 * {@link #aggregateStats}. Persistence is via DataStore — the inventory is serialized to
 * a base64 ItemStack blob on close / quit and restored on first open / join.
 */
public final class AccessoryBagService implements AccessoryService {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final String REPO = "accessory-bags";

    private final RpgAccessoriesPlugin plugin;
    private final Map<UUID, Inventory> bags = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> tiers = new ConcurrentHashMap<>();

    public AccessoryBagService(RpgAccessoriesPlugin plugin) {
        this.plugin = plugin;
    }

    public Inventory openBag(Player player) {
        Inventory bag = bags.computeIfAbsent(player.getUniqueId(), k -> {
            int tier = currentTier(player.getUniqueId());
            int rows = rowsForTier(tier);
            int size = rows * 9;
            String title = plugin.getConfig().getString("bag.title", "&5&lAccessory Bag");
            Inventory inv = Bukkit.createInventory(player, size, LEGACY.deserialize(title));
            loadInto(player.getUniqueId(), inv);
            return inv;
        });
        // Always (re)place the upgrade button in the last slot — last slot is reserved for UI.
        refreshUpgradeButton(player, bag);
        return bag;
    }

    /**
     * Returns the in-memory bag for the given player, or {@code null} if the bag has not been
     * opened yet. Callers should call {@link #openBag} first to ensure the bag exists.
     */
    public Inventory getBag(UUID id) {
        return bags.get(id);
    }

    /**
     * Places (or refreshes) the upgrade button in the last slot of the bag.
     * The last slot of every bag inventory is reserved as a UI element and is never saved.
     */
    public void refreshUpgradeButton(Player player, Inventory bag) {
        bag.setItem(bag.getSize() - 1, buildUpgradeButton(player));
    }

    private ItemStack buildUpgradeButton(Player player) {
        int current  = currentTier(player.getUniqueId());
        double cost  = upgradeCost(current);

        if (cost < 0) {
            // Max tier — show a placeholder
            ItemStack btn = new ItemStack(Material.BARRIER);
            ItemMeta  meta = btn.getItemMeta();
            meta.displayName(Component.text("Max Tier", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD,   true));
            meta.lore(List.of(
                Component.text("Your bag is fully expanded.", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Tier " + current + "  —  " + contentSlots(current) + " accessory slots",
                        NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            btn.setItemMeta(meta);
            return btn;
        }

        int nextTier     = current + 1;
        int curSlots     = contentSlots(current);
        int nextSlots    = contentSlots(nextTier);

        ItemStack btn  = new ItemStack(Material.NETHER_STAR);
        ItemMeta  meta = btn.getItemMeta();
        meta.displayName(Component.text("✦ Upgrade Bag", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD,   true));
        meta.lore(List.of(
            Component.text("Expand your accessory bag.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("Current: ", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Tier " + current + " (" + curSlots + " slots)",
                            NamedTextColor.WHITE)),
            Component.text("Next:    ", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Tier " + nextTier + " (" + nextSlots + " slots)",
                            NamedTextColor.GREEN)),
            Component.empty(),
            Component.text("Cost: ", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text((long) cost + " coins", NamedTextColor.GOLD)),
            Component.empty(),
            Component.text("Click to upgrade!", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false)
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        btn.setItemMeta(meta);
        return btn;
    }

    /**
     * Usable accessory slots for a given tier: all slots minus the 1 reserved for the
     * upgrade button in the last position.
     */
    private int contentSlots(int tier) {
        return Math.max(0, rowsForTier(tier) * 9 - 1);
    }

    /** Returns the current tier for this player; defaults to 1 if not stored. */
    public int currentTier(UUID id) {
        Integer cached = tiers.get(id);
        if (cached != null) return cached;
        int tier = 1;
        try {
            Optional<Map<String, Object>> opt = RpgServices.dataStore().repository(REPO).get(id.toString());
            if (opt.isPresent()) {
                Object t = opt.get().get("tier");
                if (t instanceof Number n) tier = n.intValue();
            }
        } catch (Exception ignored) {}
        tiers.put(id, tier);
        return tier;
    }

    public void setTier(UUID id, int tier) {
        tiers.put(id, tier);
    }

    public int rowsForTier(int tier) {
        List<?> raw = plugin.getConfig().getList("tiers");
        if (raw == null) return 3;
        for (Object o : raw) {
            if (o instanceof Map<?, ?> m) {
                Object t = m.get("tier");
                if (t instanceof Number n && n.intValue() == tier) {
                    Object rows = m.get("rows");
                    return rows instanceof Number rn
                            ? Math.max(1, Math.min(6, rn.intValue()))
                            : 3;
                }
            }
        }
        return 3;
    }

    /** Returns the cost to advance to (currentTier + 1), or -1 if no next tier defined. */
    public double upgradeCost(int currentTier) {
        List<?> raw = plugin.getConfig().getList("tiers");
        if (raw == null) return -1;
        int next = currentTier + 1;
        for (Object o : raw) {
            if (o instanceof Map<?, ?> m) {
                Object t = m.get("tier");
                if (t instanceof Number n && n.intValue() == next) {
                    Object cost = m.get("cost");
                    return cost instanceof Number cn ? cn.doubleValue() : -1;
                }
            }
        }
        return -1;
    }

    public boolean isBagInventory(Inventory inv) {
        return bags.containsValue(inv);
    }

    public void save(UUID id) {
        Inventory bag = bags.get(id);
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("schema-version", 2);
            data.put("tier", currentTier(id));
            if (bag != null) {
                // Snapshot contents and null the last slot (upgrade button) — never persisted.
                ItemStack[] contents = bag.getContents();
                contents[contents.length - 1] = null;
                data.put("contents", serialize(contents));
            }
            RpgServices.dataStore().repository(REPO).save(id.toString(), data);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to save accessory bag for " + id + ": " + ex.getMessage());
        }
    }

    public void release(UUID id) {
        save(id);
        bags.remove(id);
    }

    public void saveAll() {
        for (UUID id : bags.keySet()) save(id);
    }

    @Override
    public Map<Stat, Double> aggregateStats(Player player) {
        return applyFamilyStacking(collectAccessories(player));
    }

    /**
     * Collects all RPG ACCESSORY items currently active for the player.
     * Skips the last bag slot (always the upgrade button, never an accessory).
     */
    private List<RpgItem> collectAccessories(Player player) {
        List<RpgItem> items = new ArrayList<>();

        Inventory bag = bags.get(player.getUniqueId());
        if (bag != null) {
            // Last slot is the upgrade-button UI element — skip it.
            int contentSlots = bag.getSize() - 1;
            for (int i = 0; i < contentSlots; i++) {
                toAccessory(bag.getItem(i)).ifPresent(items::add);
            }
        }

        if (plugin.getConfig().getBoolean("inventory-accessories.enabled", false)) {
            for (ItemStack stack : player.getInventory().getContents()) {
                toAccessory(stack).ifPresent(items::add);
            }
        }

        return items;
    }

    /**
     * Applies family stacking rules and returns the final stat map.
     *
     * <p>Items without a family always contribute. Items that share a family follow their
     * declared {@code Stacking} rule:
     * <ul>
     *   <li>{@code highest} — only the item with the largest combined absolute stat value counts.</li>
     *   <li>{@code sum} / {@code independent} — every copy in the family contributes.</li>
     * </ul>
     */
    private static Map<Stat, Double> applyFamilyStacking(List<RpgItem> items) {
        List<RpgItem> noFamily = new ArrayList<>();
        Map<String, List<RpgItem>> byFamily = new LinkedHashMap<>();

        for (RpgItem item : items) {
            String fam = item.family();
            if (fam == null || fam.isBlank()) {
                noFamily.add(item);
            } else {
                byFamily.computeIfAbsent(fam, k -> new ArrayList<>()).add(item);
            }
        }

        Map<Stat, Double> out = new HashMap<>();
        for (RpgItem item : noFamily) addStats(item, out);

        for (List<RpgItem> group : byFamily.values()) {
            String mode = group.get(0).accessoryStacking();
            if ("sum".equals(mode) || "independent".equals(mode)) {
                for (RpgItem item : group) addStats(item, out);
            } else {
                // "highest" — pick the copy with the greatest combined stat magnitude.
                group.stream()
                     .max(Comparator.comparingDouble(
                             it -> it.stats().values().stream().mapToDouble(Math::abs).sum()))
                     .ifPresent(best -> addStats(best, out));
            }
        }
        return out;
    }

    private static Optional<RpgItem> toAccessory(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return Optional.empty();
        return RpgServices.items().from(stack)
                .filter(it -> it.type() == BuiltinItemType.ACCESSORY);
    }

    private static void addStats(RpgItem item, Map<Stat, Double> out) {
        for (Map.Entry<Stat, Double> e : item.stats().entrySet()) {
            out.merge(e.getKey(), e.getValue(), Double::sum);
        }
    }

    private void loadInto(UUID id, Inventory inv) {
        Optional<Map<String, Object>> opt = RpgServices.dataStore().repository(REPO).get(id.toString());
        if (opt.isEmpty()) return;
        Object raw = opt.get().get("contents");
        if (!(raw instanceof String encoded) || encoded.isEmpty()) return;
        try {
            ItemStack[] contents = deserialize(encoded);
            int limit = Math.min(contents.length, inv.getSize());
            for (int i = 0; i < limit; i++) inv.setItem(i, contents[i]);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to load accessory bag for " + id + ": " + ex.getMessage());
        }
    }

    private static String serialize(ItemStack[] items) throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(bos)) {
            out.writeInt(items.length);
            for (ItemStack item : items) out.writeObject(item);
            out.flush();
            return java.util.Base64.getEncoder().encodeToString(bos.toByteArray());
        }
    }

    private static ItemStack[] deserialize(String encoded) throws Exception {
        byte[] data = java.util.Base64.getDecoder().decode(encoded);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             BukkitObjectInputStream in = new BukkitObjectInputStream(bis)) {
            int len = in.readInt();
            ItemStack[] items = new ItemStack[len];
            for (int i = 0; i < len; i++) items[i] = (ItemStack) in.readObject();
            return items;
        }
    }

    @SuppressWarnings("unused")
    private static void touchDataStore(DataStore ignored) {}
}
