package com.github._255_ping.rpg.core.items;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityInvocation;
import com.github._255_ping.rpg.api.items.ItemType;
import com.github._255_ping.rpg.api.items.Rarity;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.api.stats.Stat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class CoreRpgItem implements RpgItem {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    public record ConsumeEffect(String effectId, int level, int durationTicks) {}

    private final String id;
    private final String displayName;
    private final ItemType type;
    private final Rarity rarity;
    private final Material material;
    private final int customModelData;
    private final Map<Stat, Double> stats;
    private final List<AbilityInvocation> abilities;
    private final List<String> extraLore;
    private final List<ConsumeEffect> consumeEffects;
    private final NamespacedKey itemIdKey;

    public CoreRpgItem(String id, String displayName, ItemType type, Rarity rarity,
                       Material material, int customModelData,
                       Map<Stat, Double> stats, List<AbilityInvocation> abilities,
                       List<String> extraLore, NamespacedKey itemIdKey) {
        this(id, displayName, type, rarity, material, customModelData, stats, abilities, extraLore, List.of(), itemIdKey);
    }

    public CoreRpgItem(String id, String displayName, ItemType type, Rarity rarity,
                       Material material, int customModelData,
                       Map<Stat, Double> stats, List<AbilityInvocation> abilities,
                       List<String> extraLore, List<ConsumeEffect> consumeEffects, NamespacedKey itemIdKey) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.rarity = rarity;
        this.material = material;
        this.customModelData = customModelData;
        this.stats = Map.copyOf(stats);
        this.abilities = List.copyOf(abilities);
        this.extraLore = List.copyOf(extraLore);
        this.consumeEffects = List.copyOf(consumeEffects);
        this.itemIdKey = itemIdKey;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return displayName; }
    @Override public ItemType type() { return type; }
    @Override public Rarity rarity() { return rarity; }
    @Override public Material material() { return material; }
    @Override public int customModelData() { return customModelData; }
    @Override public Map<Stat, Double> stats() { return stats; }
    @Override public List<AbilityInvocation> abilities() { return abilities; }
    @Override public List<String> extraLore() { return extraLore; }
    public List<ConsumeEffect> consumeEffects() { return consumeEffects; }

    @Override
    public ItemStack toItemStack() {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        if (displayName != null && !displayName.isEmpty()) {
            meta.displayName(noItalic(LEGACY.deserialize(displayName)));
        } else {
            meta.displayName(noItalic(Component.text(id)));
        }

        if (customModelData != 0) {
            meta.setCustomModelData(customModelData);
        }

        List<Component> lore = new ArrayList<>();

        // Stats: BREAKING_POWER shown first (gathering tools), then all others.
        List<Map.Entry<Stat, Double>> statEntries = new ArrayList<>(stats.entrySet());
        statEntries.sort(Comparator.comparingInt(e -> e.getKey() == BuiltinStat.BREAKING_POWER ? 0 : 1));
        for (Map.Entry<Stat, Double> e : statEntries) {
            Stat stat = e.getKey();
            double value = e.getValue();
            String line = stat.colorCode() + stat.displayName() + ": " + formatValue(value, stat.percent());
            lore.add(noItalic(LEGACY.deserialize(line)));
        }

        if (!stats.isEmpty() && !extraLore.isEmpty()) {
            lore.add(Component.empty());
        }
        for (String l : extraLore) {
            lore.add(noItalic(LEGACY.deserialize(l)));
        }

        // Abilities: show display name + description lines from the ability registry.
        if (!abilities.isEmpty()) {
            if (!lore.isEmpty()) lore.add(Component.empty());
            for (AbilityInvocation inv : abilities) {
                String abilityId = inv.effectName();
                String displayName;
                List<String> description;
                try {
                    displayName = RpgServices.abilities().abilityDisplayName(abilityId);
                    description = RpgServices.abilities().abilityDescription(abilityId);
                } catch (IllegalStateException ex) {
                    displayName = abilityId;
                    description = List.of();
                }
                lore.add(noItalic(LEGACY.deserialize("&5Ability: &d" + displayName)));
                for (String line : description) {
                    lore.add(noItalic(LEGACY.deserialize("  &7" + line)));
                }
            }
        }

        if (rarity != null) {
            if (!lore.isEmpty()) lore.add(Component.empty());
            lore.add(noItalic(LEGACY.deserialize(rarity.coloredDisplay())));
        }
        if (!lore.isEmpty()) meta.lore(lore);

        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, id);

        // Suppress vanilla tooltip lines (attack damage, potion effects, "No Effects", etc.)
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        stack.setItemMeta(meta);
        return stack;
    }

    private static Component noItalic(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    private static String formatValue(double v, boolean percent) {
        String num;
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            num = Long.toString((long) v);
        } else {
            num = String.format("%.1f", v);
        }
        String sign = v >= 0 ? "+" : "";
        return sign + num + (percent ? "%" : "");
    }
}
