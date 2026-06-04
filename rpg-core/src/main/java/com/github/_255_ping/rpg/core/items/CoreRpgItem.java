package com.github._255_ping.rpg.core.items;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityInvocation;
import com.github._255_ping.rpg.api.abilities.ItemAbilityBinding;
import com.github._255_ping.rpg.api.abilities.PlayerAbilityTrigger;
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
import org.bukkit.attribute.Attribute;
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
    private final List<ItemAbilityBinding> triggeredAbilities;
    private final List<String> extraLore;
    private final List<ConsumeEffect> consumeEffects;
    private final int attackCooldownTicks;
    private final int itemCooldownTicks;
    private final String ammoType;
    private final boolean infiniteAmmo;
    private final String projectileType;
    private final boolean tradeable;
    private final String setId;
    private final NamespacedKey itemIdKey;

    public CoreRpgItem(String id, String displayName, ItemType type, Rarity rarity,
                       Material material, int customModelData,
                       Map<Stat, Double> stats, List<ItemAbilityBinding> triggeredAbilities,
                       List<String> extraLore, List<ConsumeEffect> consumeEffects,
                       int attackCooldownTicks, int itemCooldownTicks,
                       String ammoType, boolean infiniteAmmo, String projectileType,
                       boolean tradeable, String setId, NamespacedKey itemIdKey) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.rarity = rarity;
        this.material = material;
        this.customModelData = customModelData;
        this.stats = Map.copyOf(stats);
        this.triggeredAbilities = List.copyOf(triggeredAbilities);
        this.extraLore = List.copyOf(extraLore);
        this.consumeEffects = List.copyOf(consumeEffects);
        this.attackCooldownTicks = attackCooldownTicks;
        this.itemCooldownTicks = itemCooldownTicks;
        this.ammoType = ammoType;
        this.infiniteAmmo = infiniteAmmo;
        this.projectileType = projectileType != null ? projectileType : "ARROW";
        this.tradeable = tradeable;
        this.setId = setId;
        this.itemIdKey = itemIdKey;
    }

    @Override public String id() { return id; }
    @Override public String displayName() { return displayName; }
    @Override public ItemType type() { return type; }
    @Override public Rarity rarity() { return rarity; }
    @Override public Material material() { return material; }
    @Override public int customModelData() { return customModelData; }
    @Override public Map<Stat, Double> stats() { return stats; }

    @Override
    public List<ItemAbilityBinding> triggeredAbilities() { return triggeredAbilities; }

    /** Backwards compat — returns invocations from RIGHT_CLICK bindings only. */
    @Override
    public List<AbilityInvocation> abilities() {
        return triggeredAbilities.stream()
                .filter(b -> b.trigger() == PlayerAbilityTrigger.RIGHT_CLICK)
                .flatMap(b -> b.invocations().stream())
                .toList();
    }

    @Override public List<String> extraLore() { return extraLore; }
    public List<ConsumeEffect> consumeEffects() { return consumeEffects; }
    @Override public int attackCooldownTicks() { return attackCooldownTicks; }
    @Override public int itemCooldownTicks() { return itemCooldownTicks; }
    @Override public String ammoType() { return ammoType; }
    @Override public boolean infiniteAmmo() { return infiniteAmmo; }
    @Override public String projectileType() { return projectileType; }
    @Override public boolean tradeable() { return tradeable; }
    @Override public String setId() { return setId; }

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

        // Abilities — grouped by trigger for clean lore presentation.
        if (!triggeredAbilities.isEmpty()) {
            if (!lore.isEmpty()) lore.add(Component.empty());
            for (ItemAbilityBinding binding : triggeredAbilities) {
                renderAbilityBinding(binding, lore);
            }
        }

        if (!tradeable) {
            if (!lore.isEmpty()) lore.add(Component.empty());
            lore.add(noItalic(LEGACY.deserialize("&c✘ Not Tradeable")));
        }
        if (rarity != null) {
            if (!lore.isEmpty()) lore.add(Component.empty());
            lore.add(noItalic(LEGACY.deserialize(rarity.coloredDisplay())));
        }
        if (!lore.isEmpty()) meta.lore(lore);

        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, id);

        // Remove vanilla attribute modifiers so they don't interfere with our own stat system.
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);
        meta.removeAttributeModifier(Attribute.ARMOR);
        meta.removeAttributeModifier(Attribute.ARMOR_TOUGHNESS);
        meta.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Renders a single ability binding into lore lines.
     * Active triggers (click-based) show as "&5Ability: &d[name] &8([trigger hint])".
     * Passive/proc triggers show as "&2Passive: &a[name] &8([trigger hint])".
     */
    private void renderAbilityBinding(ItemAbilityBinding binding, List<Component> lore) {
        // Each binding may contain multiple invocations (one line in AbilitySequence per invocation).
        // We show one lore entry per invocation that has a known display name.
        for (AbilityInvocation inv : binding.invocations()) {
            String abilityId = inv.effectName();
            String abilityDisplayName;
            List<String> description;
            try {
                abilityDisplayName = RpgServices.abilities().abilityDisplayName(abilityId);
                description = RpgServices.abilities().abilityDescription(abilityId);
            } catch (IllegalStateException ex) {
                abilityDisplayName = abilityId;
                description = List.of();
            }

            // Skip built-in effect primitives (mana_cost, cooldown, particles, sound, delay) —
            // they're pipeline plumbing, not standalone abilities worth showing in lore.
            if (isPlumbingEffect(abilityId) && description.isEmpty()) continue;

            boolean isActive = binding.trigger().isActive();
            String prefix = isActive ? "&5Ability: &d" : "&2Passive: &a";
            String hint = " &8(" + binding.trigger().loreHint() + ")";
            lore.add(noItalic(LEGACY.deserialize(prefix + abilityDisplayName + hint)));
            for (String line : description) {
                lore.add(noItalic(LEGACY.deserialize("  &7" + line)));
            }
        }
    }

    /** Returns true for low-level pipeline effects that shouldn't appear as standalone lore entries. */
    private static boolean isPlumbingEffect(String id) {
        return switch (id) {
            case "mana_cost", "cooldown", "delay", "particles", "sound" -> true;
            default -> false;
        };
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
