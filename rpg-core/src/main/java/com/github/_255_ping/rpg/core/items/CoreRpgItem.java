package com.github._255_ping.rpg.core.items;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityInvocation;
import com.github._255_ping.rpg.api.abilities.ItemAbilityBinding;
import com.github._255_ping.rpg.api.abilities.PlayerAbilityTrigger;
import com.github._255_ping.rpg.api.items.ItemType;
import com.github._255_ping.rpg.api.sets.SetBonus;
import com.github._255_ping.rpg.api.items.Rarity;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.stats.Stat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    /**
     * Ordered list of stat IDs for lore display. Stats present on this item are shown
     * in the order they appear here; stats not listed appear after, sorted alphabetically.
     * Loaded from {@code plugins/rpg-core/stat-order.yml} per item type.
     */
    private final List<String> statOrder;

    public CoreRpgItem(String id, String displayName, ItemType type, Rarity rarity,
                       Material material, int customModelData,
                       Map<Stat, Double> stats, List<ItemAbilityBinding> triggeredAbilities,
                       List<String> extraLore, List<ConsumeEffect> consumeEffects,
                       int attackCooldownTicks, int itemCooldownTicks,
                       String ammoType, boolean infiniteAmmo, String projectileType,
                       boolean tradeable, String setId, NamespacedKey itemIdKey,
                       List<String> statOrder) {
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
        this.statOrder = statOrder != null ? List.copyOf(statOrder) : List.of();
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

        // Item type label — dark gray, italic (e.g. "Sword", "Armor", "Wand")
        if (type != null) {
            lore.add(Component.text(capitalize(type.id()))
                    .color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, true));
            lore.add(Component.empty());
        }

        // Stats — ordered by statOrder config; unlisted stats follow alphabetically.
        if (!stats.isEmpty()) {
            List<Map.Entry<Stat, Double>> statEntries = new ArrayList<>(stats.entrySet());
            statEntries.sort(Comparator
                    .<Map.Entry<Stat, Double>, Integer>comparing(
                            e -> {
                                int idx = statOrder.indexOf(e.getKey().id());
                                return idx < 0 ? Integer.MAX_VALUE : idx;
                            })
                    .thenComparing(e -> e.getKey().id()));
            for (Map.Entry<Stat, Double> e : statEntries) {
                Stat stat = e.getKey();
                double value = e.getValue();
                String line = stat.colorCode() + stat.displayName() + ": " + formatValue(value, stat.percent());
                lore.add(noItalic(LEGACY.deserialize(line)));
            }
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

        // Set membership lore — shows set name and each tier's bonuses.
        // Rendered before rarity so the set block sits just above the rarity line.
        if (setId != null && !setId.isBlank()) {
            try {
                RpgServices.armorSets().get(setId).ifPresent(def -> {
                    if (!lore.isEmpty()) lore.add(Component.empty());
                    lore.add(noItalic(LEGACY.deserialize("&6" + def.name())));
                    def.bonuses().entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .forEach(entry -> {
                                int threshold = entry.getKey();
                                SetBonus bonus = entry.getValue();
                                StringBuilder sb = new StringBuilder("  &8(");
                                sb.append(threshold).append('/').append(def.totalPieces()).append(") ");
                                // Stats
                                boolean first = true;
                                for (Map.Entry<Stat, Double> se : bonus.stats().entrySet()) {
                                    if (!first) sb.append("&8, ");
                                    sb.append("&f").append(formatValue(se.getValue(), se.getKey().percent()))
                                            .append(" &7").append(se.getKey().displayName());
                                    first = false;
                                }
                                // Ability trigger hints
                                if (!bonus.abilities().isEmpty()) {
                                    if (!first) sb.append(" ");
                                    Map<PlayerAbilityTrigger, Long> counts = bonus.abilities().stream()
                                            .collect(java.util.stream.Collectors.groupingBy(
                                                    ItemAbilityBinding::trigger,
                                                    java.util.stream.Collectors.counting()));
                                    sb.append("&8| ");
                                    sb.append(counts.keySet().stream()
                                            .map(t -> "&7" + t.loreHint())
                                            .collect(java.util.stream.Collectors.joining("&8, ")));
                                }
                                lore.add(noItalic(LEGACY.deserialize(sb.toString())));
                            });
                });
            } catch (IllegalStateException ignored) {
                // ArmorSetRegistry not yet bootstrapped — skip set lore on this render
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
     *
     * <p>An invocation is only rendered when it has <em>explicit</em> metadata registered
     * via {@link com.github._255_ping.rpg.api.abilities.AbilityRegistry#registerMeta}.
     * Raw DSL primitives (beam, damage, heal, knockback, …) return their own ID as the
     * display name and have no description — they are treated as invisible plumbing just
     * like mana_cost/cooldown, letting the item's manual {@code Lore:} block speak for
     * itself without duplication.
     */
    private void renderAbilityBinding(ItemAbilityBinding binding, List<Component> lore) {
        // Scan the whole chain once for a cooldown{ticks=N} invocation so we can
        // append the time to every ability line rendered from this binding.
        double cooldownSecs = extractCooldownSeconds(binding.invocations());

        for (AbilityInvocation inv : binding.invocations()) {
            String abilityId = inv.effectName();

            // Hard-skip pipeline mechanics (mana gate, cooldown, delay, vfx) — these are
            // never standalone abilities worth advertising, even if meta is registered.
            if (isPlumbingEffect(abilityId)) continue;

            String abilityDisplayName;
            List<String> description;
            try {
                abilityDisplayName = RpgServices.abilities().abilityDisplayName(abilityId);
                description = RpgServices.abilities().abilityDescription(abilityId);
            } catch (IllegalStateException ex) {
                abilityDisplayName = abilityId;
                description = List.of();
            }

            // Skip raw DSL primitives that have no registered display name or description.
            // AbilityRegistry.abilityDisplayName() returns the raw ID as fallback, so
            // "displayName equals id" means "no one called registerMeta() for this effect."
            // Items that describe their abilities in manual Lore: entries rely on this to
            // avoid duplicate lore lines (e.g. "Ability: beam (Right-click)" after a manual
            // "Right-click: Solar Beam (30 mana)" line).
            boolean hasDisplayMeta = !abilityDisplayName.equals(abilityId) || !description.isEmpty();
            if (!hasDisplayMeta) continue;

            boolean isActive = binding.trigger().isActive();
            String prefix = isActive ? "&5Ability: &d" : "&2Passive: &a";
            String hint = cooldownSecs > 0
                    ? " &8(" + binding.trigger().loreHint() + " | &b" + formatSeconds(cooldownSecs) + " cd&8)"
                    : " &8(" + binding.trigger().loreHint() + ")";
            lore.add(noItalic(LEGACY.deserialize(prefix + abilityDisplayName + hint)));
            for (String line : description) {
                lore.add(noItalic(LEGACY.deserialize("  &7" + line)));
            }
        }
    }

    /**
     * Scans a chain for a {@code cooldown{ticks=N}} invocation and returns the
     * equivalent duration in seconds, or {@code -1} if no cooldown is present.
     */
    private static double extractCooldownSeconds(List<AbilityInvocation> invocations) {
        for (AbilityInvocation inv : invocations) {
            if ("cooldown".equals(inv.effectName())) {
                String raw = inv.params().get("ticks");
                if (raw != null) {
                    try { return Double.parseDouble(raw) / 20.0; }
                    catch (NumberFormatException ignored) {}
                }
            }
        }
        return -1;
    }

    /** Formats a duration: whole seconds show as {@code "5s"}, fractional as {@code "3.5s"}. */
    private static String formatSeconds(double secs) {
        if (secs == Math.floor(secs) && !Double.isInfinite(secs)) return (long) secs + "s";
        return String.format("%.1fs", secs);
    }

    /**
     * Returns true for low-level pipeline effects that are never shown in lore regardless
     * of registered metadata — they describe the cast mechanics, not the ability itself.
     */
    private static boolean isPlumbingEffect(String id) {
        return switch (id) {
            case "mana_cost", "cooldown", "delay", "particles", "sound" -> true;
            default -> false;
        };
    }

    private static Component noItalic(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(java.util.Locale.ROOT);
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
