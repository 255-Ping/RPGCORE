package com.github._255_ping.rpg.enchanting;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.stats.Stat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads/writes enchant/reforge/upgrade state on an ItemStack via PDC, and rewrites the
 * lore so the player sees what's on the item.
 *
 * <p>PDC layout on the item meta:
 * <ul>
 *   <li>{@code rpg_enchants} — String, format {@code "id1:lvl1,id2:lvl2,..."}</li>
 *   <li>{@code rpg_reforge} — String, single reforge id (or absent)</li>
 *   <li>{@code rpg_upgrades} — String, format {@code "id1:tier1,id2:tier2,..."}</li>
 * </ul>
 */
public final class ItemModifier {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final NamespacedKey enchantsKey;
    private final NamespacedKey reforgeKey;
    private final NamespacedKey upgradesKey;

    public ItemModifier(NamespacedKey enchantsKey, NamespacedKey reforgeKey, NamespacedKey upgradesKey) {
        this.enchantsKey = enchantsKey;
        this.reforgeKey = reforgeKey;
        this.upgradesKey = upgradesKey;
    }

    public NamespacedKey enchantsKey() { return enchantsKey; }
    public NamespacedKey reforgeKey() { return reforgeKey; }
    public NamespacedKey upgradesKey() { return upgradesKey; }

    public Map<String, Integer> enchants(ItemStack stack) {
        return parseLevelMap(read(stack, enchantsKey));
    }

    public Optional<String> reforge(ItemStack stack) {
        String r = read(stack, reforgeKey);
        return (r == null || r.isEmpty()) ? Optional.empty() : Optional.of(r);
    }

    public Map<String, Integer> upgrades(ItemStack stack) {
        return parseLevelMap(read(stack, upgradesKey));
    }

    public void setEnchant(ItemStack stack, String id, int level) {
        Map<String, Integer> map = new LinkedHashMap<>(enchants(stack));
        if (level <= 0) map.remove(id); else map.put(id, level);
        write(stack, enchantsKey, encodeLevelMap(map));
    }

    public void setReforge(ItemStack stack, String id) {
        write(stack, reforgeKey, id == null ? "" : id);
    }

    public void addUpgradeTier(ItemStack stack, String id, int maxTier) {
        Map<String, Integer> map = new LinkedHashMap<>(upgrades(stack));
        int cur = map.getOrDefault(id, 0);
        if (cur >= maxTier) return;
        map.put(id, cur + 1);
        write(stack, upgradesKey, encodeLevelMap(map));
    }

    public void rewriteLore(ItemStack stack, EnchantRegistry registry) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        java.util.List<Component> lore = new java.util.ArrayList<>();
        Optional<RpgItem> base = RpgServices.items().from(stack);
        // Re-render the original item lines (stats, base lore, abilities, rarity).
        // Easiest path: rebuild from base.toItemStack() and copy that lore as the starting point.
        if (base.isPresent()) {
            ItemStack fresh = base.get().toItemStack();
            ItemMeta freshMeta = fresh.getItemMeta();
            if (freshMeta != null && freshMeta.lore() != null) {
                lore.addAll(freshMeta.lore());
            }
        }

        Optional<String> reforgeId = reforge(stack);
        Map<String, Integer> enchMap = enchants(stack);
        Map<String, Integer> upMap = upgrades(stack);

        if (!enchMap.isEmpty()) {
            lore.add(Component.empty());
            lore.add(noItalic(LEGACY.deserialize("&5Enchantments:")));
            for (Map.Entry<String, Integer> e : enchMap.entrySet()) {
                EnchantDef def = registry.enchant(e.getKey()).orElse(null);
                String name = def != null ? def.displayName() : e.getKey();
                lore.add(noItalic(LEGACY.deserialize("  " + name + " " + roman(e.getValue()))));
            }
        }
        if (reforgeId.isPresent()) {
            ReforgeDef def = registry.reforge(reforgeId.get()).orElse(null);
            if (def != null) {
                lore.add(Component.empty());
                lore.add(noItalic(LEGACY.deserialize("&dReforge: " + def.displayName())));
            }
        }
        if (!upMap.isEmpty()) {
            lore.add(Component.empty());
            lore.add(noItalic(LEGACY.deserialize("&6Upgrades:")));
            for (Map.Entry<String, Integer> e : upMap.entrySet()) {
                UpgradeDef def = registry.upgrade(e.getKey()).orElse(null);
                String name = def != null ? def.displayName() : e.getKey();
                lore.add(noItalic(LEGACY.deserialize("  " + name + " &7x" + e.getValue())));
            }
        }
        meta.lore(lore);
        stack.setItemMeta(meta);
    }

    /** Compute combined stat additions from all enchant/reforge/upgrade tags on this item. */
    public Map<Stat, Double> contributedStats(ItemStack stack, EnchantRegistry registry) {
        Map<Stat, Double> out = new LinkedHashMap<>();
        Optional<RpgItem> base = RpgServices.items().from(stack);
        String rarityId = base.map(b -> b.rarity() == null ? "common" : b.rarity().id()).orElse("common");

        for (Map.Entry<String, Integer> e : enchants(stack).entrySet()) {
            EnchantDef d = registry.enchant(e.getKey()).orElse(null);
            if (d == null) continue;
            addByStatId(out, d.statsAtLevel(e.getValue()));
        }
        reforge(stack).flatMap(registry::reforge).ifPresent(r -> addByStatId(out, r.statsFor(rarityId)));
        for (Map.Entry<String, Integer> e : upgrades(stack).entrySet()) {
            UpgradeDef d = registry.upgrade(e.getKey()).orElse(null);
            if (d == null) continue;
            Map<String, Double> perTier = d.statsPerTier();
            int tier = e.getValue();
            for (Map.Entry<String, Double> sd : perTier.entrySet()) {
                addStatById(out, sd.getKey(), sd.getValue() * tier);
            }
        }
        return out;
    }

    private static void addByStatId(Map<Stat, Double> out, Map<String, Double> in) {
        for (Map.Entry<String, Double> e : in.entrySet()) {
            addStatById(out, e.getKey(), e.getValue());
        }
    }

    private static void addStatById(Map<Stat, Double> out, String statId, double value) {
        Stat stat = RpgServices.stats().get(statId).orElse(null);
        if (stat == null) return;
        out.merge(stat, value, Double::sum);
    }

    private static String read(ItemStack stack, NamespacedKey key) {
        if (stack == null) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(key, PersistentDataType.STRING) ? pdc.get(key, PersistentDataType.STRING) : null;
    }

    private static void write(ItemStack stack, NamespacedKey key, String value) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        if (value == null || value.isEmpty()) {
            meta.getPersistentDataContainer().remove(key);
        } else {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        }
        stack.setItemMeta(meta);
    }

    /** Package-private for testing — round-trips with {@link #encodeLevelMap}. */
    static Map<String, Integer> parseLevelMap(String raw) {
        if (raw == null || raw.isEmpty()) return Map.of();
        Map<String, Integer> out = new LinkedHashMap<>();
        for (String pair : raw.split(",")) {
            int colon = pair.lastIndexOf(':');
            if (colon < 0) continue;
            try {
                out.put(pair.substring(0, colon), Integer.parseInt(pair.substring(colon + 1)));
            } catch (NumberFormatException ignored) {}
        }
        return out;
    }

    /** Package-private for testing — round-trips with {@link #parseLevelMap}. */
    static String encodeLevelMap(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (!first) sb.append(',');
            sb.append(e.getKey()).append(':').append(e.getValue());
            first = false;
        }
        return sb.toString();
    }

    private static Component noItalic(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> Integer.toString(n);
        };
    }

    @SuppressWarnings("unused")
    public List<String> debug(ItemStack stack) {
        return List.of("enchants=" + enchants(stack), "reforge=" + reforge(stack), "upgrades=" + upgrades(stack));
    }
}
