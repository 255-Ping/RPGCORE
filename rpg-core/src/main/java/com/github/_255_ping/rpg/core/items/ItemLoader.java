package com.github._255_ping.rpg.core.items;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityInvocation;
import com.github._255_ping.rpg.api.abilities.ItemAbilityBinding;
import com.github._255_ping.rpg.api.abilities.PlayerAbilityTrigger;
import com.github._255_ping.rpg.api.items.BuiltinItemType;
import com.github._255_ping.rpg.api.items.CustomItemType;
import com.github._255_ping.rpg.api.items.ItemType;
import com.github._255_ping.rpg.api.items.Rarity;
import com.github._255_ping.rpg.api.stats.Stat;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Scans {@code plugins/rpg-core/items/} for *.yml files and registers each top-level
 * key as an item. Bad entries log a warning and are skipped.
 */
public final class ItemLoader {

    private final File folder;
    private final CoreItemRegistry registry;
    private final NamespacedKey itemIdKey;
    private final Logger logger;

    public ItemLoader(File folder, CoreItemRegistry registry, NamespacedKey itemIdKey, Logger logger) {
        this.folder = folder;
        this.registry = registry;
        this.itemIdKey = itemIdKey;
        this.logger = logger;
    }

    public void loadAll() {
        registry.clear();
        if (!folder.isDirectory()) return;
        File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                loadFile(f);
            } catch (Exception ex) {
                logger.warning("Failed to parse items file " + f.getName() + ": " + ex.getMessage());
            }
        }
    }

    private void loadFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(id);
            if (section == null) {
                logger.warning("item '" + id + "' in " + file.getName() + " is not a section, skipping");
                continue;
            }
            try {
                CoreRpgItem item = parse(id, section);
                registry.register(item);
            } catch (Exception ex) {
                logger.warning("Skipping item '" + id + "' in " + file.getName() + ": " + ex.getMessage());
            }
        }
    }

    private CoreRpgItem parse(String id, ConfigurationSection s) {
        String materialName = s.getString("MinecraftItem");
        if (materialName == null) throw new IllegalArgumentException("missing MinecraftItem");
        Material material = Material.matchMaterial(materialName);
        if (material == null) throw new IllegalArgumentException("unknown MinecraftItem: " + materialName);

        String typeName = s.getString("Type", "MATERIAL");
        ItemType type = parseType(typeName);

        String displayName = s.getString("DisplayName");
        int customModelData = s.getInt("CustomModelData", 0);

        List<String> lore = s.getStringList("Lore");

        String rarityStr = s.getString("Rarity");
        Rarity rarity = rarityStr == null ? null : new Rarity(stripColor(rarityStr).toLowerCase(Locale.ROOT), rarityStr);

        Map<Stat, Double> stats = new HashMap<>();
        ConfigurationSection statsSec = s.getConfigurationSection("Stats");
        if (statsSec != null) {
            for (String statId : statsSec.getKeys(false)) {
                Optional<Stat> stat = RpgServices.stats().get(statId);
                if (stat.isEmpty()) {
                    logger.warning("item '" + id + "' references unknown stat '" + statId + "'");
                    continue;
                }
                stats.put(stat.get(), statsSec.getDouble(statId));
            }
        }

        List<ItemAbilityBinding> triggeredAbilities = new ArrayList<>();
        for (String line : s.getStringList("Abilities")) {
            try {
                triggeredAbilities.add(parseAbilityLine(id, line));
            } catch (Exception ex) {
                logger.warning("item '" + id + "' has bad ability line '" + line + "': " + ex.getMessage());
            }
        }

        List<CoreRpgItem.ConsumeEffect> consumeEffects = new ArrayList<>();
        ConfigurationSection consumeSec = s.getConfigurationSection("OnConsume");
        if (consumeSec != null) {
            for (Object entry : consumeSec.getList("Effects", List.of())) {
                if (entry instanceof java.util.Map<?, ?> m) {
                    String effectId = String.valueOf(m.get("effect"));
                    int level = m.get("level") instanceof Number n ? n.intValue() : 1;
                    int duration = m.get("duration") instanceof Number n ? n.intValue() : 200;
                    consumeEffects.add(new CoreRpgItem.ConsumeEffect(effectId, level, duration));
                }
            }
        }

        int attackCooldown  = s.getInt("AttackCooldown", 0);
        int itemCooldown    = s.getInt("ItemCooldown", 0);
        String ammoType     = s.getString("AmmoType");
        boolean infiniteAmmo = s.getBoolean("InfiniteAmmo", false);
        String projectileType = s.getString("ProjectileType", "ARROW");
        boolean tradeable   = s.getBoolean("Tradeable", true);
        String setId        = s.getString("SetId");

        return new CoreRpgItem(id, displayName, type, rarity, material, customModelData,
                stats, triggeredAbilities, lore, consumeEffects,
                attackCooldown, itemCooldown, ammoType, infiniteAmmo, projectileType,
                tradeable, setId, itemIdKey);
    }

    /**
     * Parses a single line from an item's {@code Abilities:} list into an {@link ItemAbilityBinding}.
     *
     * <ul>
     *   <li>Lines starting with {@code ~trigger } bind to that trigger, e.g.
     *       {@code "~on_hit drain{amount=5}"}.</li>
     *   <li>Lines without a {@code ~} prefix default to {@link PlayerAbilityTrigger#RIGHT_CLICK}
     *       (backwards compatible with pre-trigger YAML).</li>
     * </ul>
     */
    private static ItemAbilityBinding parseAbilityLine(String itemId, String line) {
        if (line.startsWith("~")) {
            int space = line.indexOf(' ');
            if (space < 0) {
                throw new IllegalArgumentException(
                        "trigger line must be '~<trigger> <effects...>', got: '" + line + "'");
            }
            String triggerStr = line.substring(1, space);
            String effectStr  = line.substring(space + 1).trim();
            PlayerAbilityTrigger trigger = PlayerAbilityTrigger.parse(triggerStr);
            List<AbilityInvocation> invocations = AbilityDsl.parse(effectStr);
            return new ItemAbilityBinding(trigger, invocations);
        }
        // No prefix — legacy right_click binding.
        List<AbilityInvocation> invocations = AbilityDsl.parse(line);
        return new ItemAbilityBinding(PlayerAbilityTrigger.RIGHT_CLICK, invocations);
    }

    private static ItemType parseType(String s) {
        try {
            return BuiltinItemType.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return new CustomItemType(s.toLowerCase(Locale.ROOT));
        }
    }

    private static String stripColor(String s) {
        return s.replaceAll("&[0-9a-fA-Fk-orK-OR]", "");
    }
}
