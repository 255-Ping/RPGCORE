package com.github._255_ping.rpg.core.sets;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityInvocation;
import com.github._255_ping.rpg.api.abilities.ItemAbilityBinding;
import com.github._255_ping.rpg.api.abilities.PlayerAbilityTrigger;
import com.github._255_ping.rpg.api.sets.SetBonus;
import com.github._255_ping.rpg.api.stats.Stat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads armor set definitions from {@code plugins/rpg-core/sets/*.yml}.
 *
 * <p>YAML schema:
 * <pre>
 * berserker_set:
 *   Name: "Berserker's Set"
 *   Pieces:
 *     - berserker_helmet
 *     - berserker_chestplate
 *     - berserker_leggings
 *     - berserker_boots
 *   Bonuses:
 *     2:
 *       Stats:
 *         ferocity: 25
 *       Abilities:
 *         - "~on_hit particles{type=crit}"
 *     4:
 *       Stats:
 *         ferocity: 75
 *         damage: 50
 *       Abilities:
 *         - "~on_hit drain{amount=10}"
 *       Scale: 0.5    # optional — multiplies all numeric params in Abilities at load time
 * </pre>
 *
 * <p>Both approaches can coexist: use {@code Scale} for tiers that share the same shape with
 * weaker numbers; write explicit {@code Abilities} for tiers that need entirely different effects.
 */
public final class ArmorSetLoader {

    private final File folder;
    private final CoreArmorSetRegistry registry;
    private final Logger logger;

    public ArmorSetLoader(File folder, CoreArmorSetRegistry registry, Logger logger) {
        this.folder = folder;
        this.registry = registry;
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
                logger.warning("Failed to parse sets file " + f.getName() + ": " + ex.getMessage());
            }
        }
    }

    private void loadFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(id);
            if (section == null) {
                logger.warning("set '" + id + "' in " + file.getName() + " is not a section, skipping");
                continue;
            }
            try {
                ArmorSetDefImpl def = parse(id, section);
                registry.register(def);
            } catch (Exception ex) {
                logger.warning("Skipping set '" + id + "' in " + file.getName() + ": " + ex.getMessage());
            }
        }
    }

    private ArmorSetDefImpl parse(String id, ConfigurationSection s) {
        String name = s.getString("Name", id);
        List<String> pieces = s.getStringList("Pieces");
        if (pieces.isEmpty()) throw new IllegalArgumentException("set '" + id + "' has no Pieces list");

        Map<Integer, SetBonus> bonuses = new LinkedHashMap<>();
        ConfigurationSection bonusesSec = s.getConfigurationSection("Bonuses");
        if (bonusesSec != null) {
            for (String key : bonusesSec.getKeys(false)) {
                int threshold;
                try {
                    threshold = Integer.parseInt(key);
                } catch (NumberFormatException ex) {
                    logger.warning("set '" + id + "' has non-integer bonus key '" + key + "', skipping");
                    continue;
                }
                ConfigurationSection bonusSec = bonusesSec.getConfigurationSection(key);
                if (bonusSec == null) continue;
                try {
                    bonuses.put(threshold, parseBonus(id, threshold, bonusSec));
                } catch (Exception ex) {
                    logger.warning("set '" + id + "' bonus[" + key + "]: " + ex.getMessage());
                }
            }
        }

        return new ArmorSetDefImpl(id, name, pieces, bonuses);
    }

    private SetBonus parseBonus(String setId, int threshold, ConfigurationSection s) {
        // Stats
        Map<Stat, Double> stats = new HashMap<>();
        ConfigurationSection statsSec = s.getConfigurationSection("Stats");
        if (statsSec != null) {
            for (String statId : statsSec.getKeys(false)) {
                try {
                    Stat stat = RpgServices.stats().get(statId)
                            .orElseThrow(() -> new IllegalArgumentException("unknown stat: " + statId));
                    stats.put(stat, statsSec.getDouble(statId));
                } catch (Exception ex) {
                    logger.warning("set '" + setId + "' bonus[" + threshold + "] stat '" + statId + "': "
                            + ex.getMessage());
                }
            }
        }

        // Optional scale factor — multiplies all numeric params in ability lines at load time.
        double scale = s.getDouble("Scale", 1.0);

        // Abilities — same ~trigger syntax as item Abilities: lists.
        // Active click triggers (right_click etc.) are silently filtered out — set bonuses
        // only support proc/passive triggers.
        List<ItemAbilityBinding> abilities = new ArrayList<>();
        for (String line : s.getStringList("Abilities")) {
            try {
                ItemAbilityBinding binding = parseAbilityLine(setId, threshold, line);
                if (binding.trigger().isActive()) {
                    logger.warning("set '" + setId + "' bonus[" + threshold + "]: "
                            + "active trigger '" + binding.trigger() + "' is not supported on set bonuses "
                            + "(only on_hit / on_hurt / on_jump / passive). Line ignored: '" + line + "'");
                    continue;
                }
                abilities.add(scale == 1.0 ? binding : scaleBinding(binding, scale));
            } catch (Exception ex) {
                logger.warning("set '" + setId + "' bonus[" + threshold + "] ability line '"
                        + line + "': " + ex.getMessage());
            }
        }

        return new SetBonus(stats, abilities);
    }

    /**
     * Parses a single ability line (same syntax as item Abilities entries).
     * Lines must have a {@code ~trigger} prefix; a bare effect string defaults to {@code passive}.
     */
    private static ItemAbilityBinding parseAbilityLine(String setId, int threshold, String line) {
        if (line.startsWith("~")) {
            int space = line.indexOf(' ');
            if (space < 0) throw new IllegalArgumentException(
                    "trigger line must be '~<trigger> <effects...>', got: '" + line + "'");
            String triggerStr = line.substring(1, space);
            String effectStr  = line.substring(space + 1).trim();
            PlayerAbilityTrigger trigger = PlayerAbilityTrigger.parse(triggerStr);
            return new ItemAbilityBinding(trigger, AbilityDsl.parse(effectStr));
        }
        // No prefix on a set bonus: default to passive (unlike items, which default to right_click).
        return new ItemAbilityBinding(PlayerAbilityTrigger.PASSIVE, AbilityDsl.parse(line));
    }

    /**
     * Returns a copy of {@code binding} with all numeric parameters multiplied by {@code scale}.
     * Non-numeric parameters (e.g. {@code type=crit}) are passed through unchanged.
     * This implements the "Scale: 0.5 → 50% of the ability" admin convenience feature.
     */
    static ItemAbilityBinding scaleBinding(ItemAbilityBinding binding, double scale) {
        List<AbilityInvocation> scaled = binding.invocations().stream()
                .map(inv -> scaleInvocation(inv, scale))
                .toList();
        return new ItemAbilityBinding(binding.trigger(), scaled);
    }

    private static AbilityInvocation scaleInvocation(AbilityInvocation inv, double scale) {
        Map<String, String> scaledParams = new LinkedHashMap<>();
        inv.params().forEach((k, v) -> {
            if (v == null || v.isEmpty()) {
                scaledParams.put(k, v);
                return;
            }
            try {
                double num = Double.parseDouble(v);
                // Format cleanly — drop ".0" for whole numbers
                double scaled = num * scale;
                scaledParams.put(k, scaled == Math.floor(scaled) && !Double.isInfinite(scaled)
                        ? Long.toString((long) scaled)
                        : String.valueOf(scaled));
            } catch (NumberFormatException ignored) {
                scaledParams.put(k, v); // non-numeric (e.g. type=crit) — leave as-is
            }
        });
        return new AbilityInvocation(inv.effectName(), scaledParams);
    }
}
