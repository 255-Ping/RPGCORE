package com.github._255_ping.rpg.enchanting;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityInvocation;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.api.stats.Stat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
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
 *
 * <p>Lore section order produced by {@link #rewriteLore}:
 * <ol>
 *   <li>Stats — base value + bonus in &amp;8dark-grey parentheses</li>
 *   <li>Enchantments — one per line with optional description below</li>
 *   <li>Extra lore + abilities from the base item definition</li>
 *   <li>Not-Tradeable indicator (if applicable)</li>
 *   <li>Upgrades — name and current tier</li>
 *   <li>Rarity</li>
 * </ol>
 *
 * <p>Always call {@link #rewriteName} alongside {@link #rewriteLore} so the reforge
 * prefix on the item display name stays in sync.
 */
public final class ItemModifier {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final NamespacedKey enchantsKey;
    private final NamespacedKey reforgeKey;
    private final NamespacedKey upgradesKey;
    /** PDC key that marks a physical item as a reforge stone; value = reforge id. */
    private final NamespacedKey reforgeStoneKey;
    /** PDC key that marks a physical item as an upgrade book; value = upgrade id. */
    private final NamespacedKey upgradeBookKey;

    public ItemModifier(NamespacedKey enchantsKey, NamespacedKey reforgeKey, NamespacedKey upgradesKey,
                        NamespacedKey reforgeStoneKey, NamespacedKey upgradeBookKey) {
        this.enchantsKey = enchantsKey;
        this.reforgeKey = reforgeKey;
        this.upgradesKey = upgradesKey;
        this.reforgeStoneKey = reforgeStoneKey;
        this.upgradeBookKey = upgradeBookKey;
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

    // ── Physical stones / books ───────────────────────────────────────────────

    /**
     * Creates a physical reforge stone {@link ItemStack} tagged with the given reforge id.
     * Give to players via the admin {@code /enchanting give} command or loot tables.
     */
    public org.bukkit.inventory.ItemStack createReforgeStone(ReforgeDef def) {
        org.bukkit.inventory.ItemStack stone = new org.bukkit.inventory.ItemStack(org.bukkit.Material.AMETHYST_SHARD);
        ItemMeta meta = stone.getItemMeta();
        if (meta == null) return stone;
        meta.displayName(noItalic(LEGACY.deserialize("&d" + def.displayName() + " &5Reforge Stone")));
        List<Component> lore = new ArrayList<>();
        // Show stats for each defined rarity tier
        if (!def.statsByRarity().isEmpty()) {
            for (Map.Entry<String, Map<String, Double>> rarityEntry : def.statsByRarity().entrySet()) {
                if (rarityEntry.getValue().isEmpty()) continue;
                lore.add(noItalic(LEGACY.deserialize("&8" + capitalize(rarityEntry.getKey()) + ":")));
                for (Map.Entry<String, Double> statEntry : rarityEntry.getValue().entrySet()) {
                    Stat stat = RpgServices.stats().get(statEntry.getKey()).orElse(null);
                    if (stat == null) continue;
                    lore.add(noItalic(LEGACY.deserialize("  " + stat.colorCode() + stat.displayName()
                            + ": " + fmtVal(statEntry.getValue(), stat.percent()))));
                }
            }
            lore.add(Component.empty());
        }
        if (def.appliesTo() != null && !def.appliesTo().isEmpty() && !def.appliesTo().contains("any")) {
            lore.add(noItalic(LEGACY.deserialize("&7Applies to: &f" + String.join(", ", def.appliesTo()))));
        }
        if (def.reagent() != null) {
            lore.add(noItalic(LEGACY.deserialize("&7Reagent: &f" + def.reagent())));
        }
        if (def.requiredSkillLevel() > 1) {
            lore.add(noItalic(LEGACY.deserialize("&7Requires Enchanting Lv &e" + def.requiredSkillLevel())));
        }
        lore.add(Component.empty());
        lore.add(noItalic(LEGACY.deserialize("&8▶ &7Place in anvil to reforge an item")));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(reforgeStoneKey, PersistentDataType.STRING, def.id());
        stone.setItemMeta(meta);
        return stone;
    }

    /**
     * Creates a physical upgrade book {@link ItemStack} tagged with the given upgrade id.
     */
    public org.bukkit.inventory.ItemStack createUpgradeBook(UpgradeDef def) {
        org.bukkit.inventory.ItemStack book = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta == null) return book;
        meta.displayName(noItalic(LEGACY.deserialize("&6" + def.displayName() + " &eUpgrade Book")));
        List<Component> lore = new ArrayList<>();
        if (!def.statsPerTier().isEmpty()) {
            lore.add(noItalic(LEGACY.deserialize("&8Stats per tier:")));
            for (Map.Entry<String, Double> e : def.statsPerTier().entrySet()) {
                Stat stat = RpgServices.stats().get(e.getKey()).orElse(null);
                if (stat == null) continue;
                lore.add(noItalic(LEGACY.deserialize("  " + stat.colorCode() + stat.displayName()
                        + ": " + fmtVal(e.getValue(), stat.percent()))));
            }
            lore.add(Component.empty());
        }
        lore.add(noItalic(LEGACY.deserialize("&7Max Tier: &e" + def.maxTier())));
        if (def.appliesTo() != null && !def.appliesTo().isEmpty() && !def.appliesTo().contains("any")) {
            lore.add(noItalic(LEGACY.deserialize("&7Applies to: &f" + String.join(", ", def.appliesTo()))));
        }
        if (def.reagent() != null) {
            lore.add(noItalic(LEGACY.deserialize("&7Reagent: &f" + def.reagent())));
        }
        if (def.requiredSkillLevel() > 1) {
            lore.add(noItalic(LEGACY.deserialize("&7Requires Enchanting Lv &e" + def.requiredSkillLevel())));
        }
        lore.add(Component.empty());
        lore.add(noItalic(LEGACY.deserialize("&8▶ &7Place in anvil to upgrade an item")));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(upgradeBookKey, PersistentDataType.STRING, def.id());
        book.setItemMeta(meta);
        return book;
    }

    /** Returns the reforge id if this stack is a reforge stone, else empty. */
    public Optional<String> reforgeIdFromStone(org.bukkit.inventory.ItemStack stack) {
        if (stack == null) return Optional.empty();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return Optional.empty();
        String val = meta.getPersistentDataContainer().get(reforgeStoneKey, PersistentDataType.STRING);
        return val == null ? Optional.empty() : Optional.of(val);
    }

    /** Returns the upgrade id if this stack is an upgrade book, else empty. */
    public Optional<String> upgradeIdFromBook(org.bukkit.inventory.ItemStack stack) {
        if (stack == null) return Optional.empty();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return Optional.empty();
        String val = meta.getPersistentDataContainer().get(upgradeBookKey, PersistentDataType.STRING);
        return val == null ? Optional.empty() : Optional.of(val);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ── Lore rewrite ──────────────────────────────────────────────────────────

    /**
     * Rebuilds the item lore from scratch using the base item definition plus the current
     * enchant/reforge/upgrade PDC state. Section order: stats → enchants → lore/abilities
     * → upgrades → rarity.
     */
    public void rewriteLore(ItemStack stack, EnchantRegistry registry) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        Optional<RpgItem> baseOpt = RpgServices.items().from(stack);
        if (baseOpt.isEmpty()) return;
        RpgItem base = baseOpt.get();

        Map<Stat, Double> bonusMap = contributedStats(stack, registry);
        Map<String, Integer> enchMap = enchants(stack);
        Map<String, Integer> upMap = upgrades(stack);

        List<Component> lore = new ArrayList<>();

        // ── 1. Stats — base + bonus with (+x) indicator ───────────────────────
        List<Map.Entry<Stat, Double>> baseEntries = new ArrayList<>(base.stats().entrySet());
        // BREAKING_POWER first (gathering tools), then alphabetical by display name
        baseEntries.sort(Comparator.comparingInt(e -> e.getKey() == BuiltinStat.BREAKING_POWER ? 0 : 1));
        for (Map.Entry<Stat, Double> e : baseEntries) {
            Stat stat = e.getKey();
            double baseVal = e.getValue();
            double bonus = bonusMap.getOrDefault(stat, 0.0);
            double total = baseVal + bonus;
            String line;
            if (bonus != 0.0) {
                line = stat.colorCode() + stat.displayName() + ": " + fmtVal(total, stat.percent())
                     + " &8(" + fmtVal(bonus, stat.percent()) + ")";
            } else {
                line = stat.colorCode() + stat.displayName() + ": " + fmtVal(baseVal, stat.percent());
            }
            lore.add(noItalic(LEGACY.deserialize(line)));
        }
        // Pure-bonus stats (not on base item — added entirely by enchant/reforge/upgrade)
        for (Map.Entry<Stat, Double> e : bonusMap.entrySet()) {
            if (base.stats().containsKey(e.getKey())) continue;
            double bonus = e.getValue();
            if (bonus == 0.0) continue;
            Stat stat = e.getKey();
            String line = stat.colorCode() + stat.displayName() + ": " + fmtVal(bonus, stat.percent())
                        + " &8(" + fmtVal(bonus, stat.percent()) + ")";
            lore.add(noItalic(LEGACY.deserialize(line)));
        }

        // ── 2. Enchantments (between stats and lore) ──────────────────────────
        if (!enchMap.isEmpty()) {
            if (!lore.isEmpty()) lore.add(Component.empty());
            for (Map.Entry<String, Integer> e : enchMap.entrySet()) {
                EnchantDef def = registry.enchant(e.getKey()).orElse(null);
                String name = def != null ? def.displayName() : e.getKey();
                lore.add(noItalic(LEGACY.deserialize("&5" + name + " " + roman(e.getValue()))));
                if (def != null) {
                    for (String desc : def.description()) {
                        lore.add(noItalic(LEGACY.deserialize("  &8" + desc)));
                    }
                }
            }
        }

        // ── 3. Extra lore + abilities ──────────────────────────────────────────
        List<String> extraLore = base.extraLore();
        List<AbilityInvocation> abilities = base.abilities();
        if (!extraLore.isEmpty() || !abilities.isEmpty()) {
            if (!lore.isEmpty()) lore.add(Component.empty());
            for (String l : extraLore) {
                lore.add(noItalic(LEGACY.deserialize(l)));
            }
            if (!abilities.isEmpty()) {
                if (!extraLore.isEmpty()) lore.add(Component.empty());
                for (AbilityInvocation inv : abilities) {
                    String abilityId = inv.effectName();
                    String dispName;
                    List<String> desc;
                    try {
                        dispName = RpgServices.abilities().abilityDisplayName(abilityId);
                        desc = RpgServices.abilities().abilityDescription(abilityId);
                    } catch (IllegalStateException ex) {
                        dispName = abilityId;
                        desc = List.of();
                    }
                    lore.add(noItalic(LEGACY.deserialize("&5Ability: &d" + dispName)));
                    for (String dl : desc) {
                        lore.add(noItalic(LEGACY.deserialize("  &7" + dl)));
                    }
                }
            }
        }

        // ── 4. Not-tradeable indicator ─────────────────────────────────────────
        if (!base.tradeable()) {
            if (!lore.isEmpty()) lore.add(Component.empty());
            lore.add(noItalic(LEGACY.deserialize("&c✘ Not Tradeable")));
        }

        // ── 5. Upgrades — below enchants/lore, above rarity ───────────────────
        if (!upMap.isEmpty()) {
            if (!lore.isEmpty()) lore.add(Component.empty());
            for (Map.Entry<String, Integer> e : upMap.entrySet()) {
                UpgradeDef def = registry.upgrade(e.getKey()).orElse(null);
                String name = def != null ? def.displayName() : e.getKey();
                lore.add(noItalic(LEGACY.deserialize("&6" + name + " &7(Tier " + e.getValue() + ")")));
            }
        }

        // ── 6. Rarity ──────────────────────────────────────────────────────────
        if (base.rarity() != null) {
            if (!lore.isEmpty()) lore.add(Component.empty());
            lore.add(noItalic(LEGACY.deserialize(base.rarity().coloredDisplay())));
        }

        meta.lore(lore);
        stack.setItemMeta(meta);
    }

    /**
     * Rewrites the item's display name: prepends the reforge's display name if a reforge
     * is applied, or resets to the base item name otherwise. Always call alongside
     * {@link #rewriteLore}.
     */
    public void rewriteName(ItemStack stack, EnchantRegistry registry) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        Optional<RpgItem> baseOpt = RpgServices.items().from(stack);
        if (baseOpt.isEmpty()) return;
        String baseName = baseOpt.get().displayName();
        if (baseName == null || baseName.isEmpty()) baseName = baseOpt.get().id();

        Optional<String> reforgeId = reforge(stack);
        if (reforgeId.isPresent()) {
            ReforgeDef def = registry.reforge(reforgeId.get()).orElse(null);
            if (def != null) {
                meta.displayName(noItalic(LEGACY.deserialize(def.displayName() + " " + baseName)));
                stack.setItemMeta(meta);
                return;
            }
        }
        // No reforge (or unknown reforge) — reset to base name
        meta.displayName(noItalic(LEGACY.deserialize(baseName)));
        stack.setItemMeta(meta);
    }

    // ── Stat contributions ────────────────────────────────────────────────────

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

    // ── Private helpers ───────────────────────────────────────────────────────

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

    private static String fmtVal(double v, boolean percent) {
        String num;
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            num = Long.toString((long) v);
        } else {
            num = String.format("%.1f", v);
        }
        String sign = v >= 0 ? "+" : "";
        return sign + num + (percent ? "%" : "");
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
