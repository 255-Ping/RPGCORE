package com.github._255_ping.rpg.alchemy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds a custom-potion ItemStack for a given {@link PotionDef}. We don't register potions as
 * {@code RpgItem}s in the core registry (alchemy is the owner here); instead, we tag the stack
 * with a {@code rpg_potion_id} PDC string so the drink listener can recognize it.
 */
public final class PotionItemFactory {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final NamespacedKey potionIdKey;

    public PotionItemFactory(NamespacedKey potionIdKey) {
        this.potionIdKey = potionIdKey;
    }

    public NamespacedKey idKey() { return potionIdKey; }

    public ItemStack build(PotionDef def) {
        ItemStack stack = new ItemStack(Material.POTION);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.displayName(noItalic(LEGACY.deserialize(def.displayName())));
        if (def.customModelData() > 0) meta.setCustomModelData(def.customModelData());
        List<Component> lore = new ArrayList<>();
        for (PotionDef.EffectSpec eff : def.effects()) {
            int seconds = eff.durationTicks() / 20;
            lore.add(noItalic(LEGACY.deserialize("&7" + eff.id() + " " + roman(eff.level()) + " &8(" + seconds + "s)")));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(potionIdKey, PersistentDataType.STRING, def.id());
        stack.setItemMeta(meta);
        return stack;
    }

    public Optional<String> idOf(ItemStack stack) {
        if (stack == null) return Optional.empty();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return Optional.empty();
        String id = meta.getPersistentDataContainer().get(potionIdKey, PersistentDataType.STRING);
        return id == null ? Optional.empty() : Optional.of(id);
    }

    private static Component noItalic(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            default -> Integer.toString(n);
        };
    }
}
