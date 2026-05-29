package com.github._255_ping.rpg.core.status;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.stats.Stat;
import com.github._255_ping.rpg.api.status.StackingStrategy;
import com.github._255_ping.rpg.api.status.StatusEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Scans the configured folder for *.yml files, parses each top-level key as a status-effect
 * definition, and registers the resulting effects. Bad entries log a warning and are skipped.
 */
public final class StatusEffectLoader {

    private final File folder;
    private final CoreStatusEffectRegistry registry;
    private final Logger logger;

    public StatusEffectLoader(File folder, CoreStatusEffectRegistry registry, Logger logger) {
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
                logger.warning("Failed to parse status-effects file " + f.getName() + ": " + ex.getMessage());
            }
        }
    }

    private void loadFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(id);
            if (section == null) {
                logger.warning("status-effect '" + id + "' in " + file.getName() + " is not a section, skipping");
                continue;
            }
            try {
                StatusEffect effect = parse(id, section);
                registry.register(effect);
            } catch (Exception ex) {
                logger.warning("Skipping status-effect '" + id + "' in " + file.getName() + ": " + ex.getMessage());
            }
        }
    }

    private CoreStatusEffect parse(String id, ConfigurationSection s) {
        String display = s.getString("display", id);
        StatusEffect.Category category = parseCategory(s.getString("category", "neutral"));
        StackingStrategy stacking = parseStacking(s.getString("stacking", "refresh"));
        boolean hidden = s.getBoolean("hidden", false);

        List<StatModifier> modifiers = new ArrayList<>();
        ConfigurationSection mods = s.getConfigurationSection("stat-modifiers");
        if (mods != null) {
            for (String statId : mods.getKeys(false)) {
                ConfigurationSection mod = mods.getConfigurationSection(statId);
                if (mod == null) {
                    logger.warning("status-effect '" + id + "' has malformed modifier for '" + statId + "'");
                    continue;
                }
                Optional<Stat> stat = RpgServices.stats().get(statId);
                if (stat.isEmpty()) {
                    logger.warning("status-effect '" + id + "' references unknown stat '" + statId + "'");
                    continue;
                }
                StatModifier.Kind kind = parseKind(mod.getString("kind", "flat"));
                double value = mod.getDouble("value", 0);
                modifiers.add(new StatModifier(stat.get(), kind, value));
            }
        }

        CoreStatusEffect.TickSpec tickSpec = null;
        ConfigurationSection tick = s.getConfigurationSection("tick");
        if (tick != null) {
            long intervalTicks = tick.getLong("interval-ticks", 20);
            String action = tick.getString("action");
            double amount = tick.getDouble("amount", 0);
            String source = tick.getString("source", id);
            if (action != null && !action.isBlank()) {
                tickSpec = new CoreStatusEffect.TickSpec(intervalTicks, action.toLowerCase(Locale.ROOT), amount, source);
            }
        }

        CoreStatusEffect.HookSpec onApply = parseHookSpec(s.getConfigurationSection("on-apply"));
        CoreStatusEffect.HookSpec onExpire = parseHookSpec(s.getConfigurationSection("on-expire"));

        return new CoreStatusEffect(id, display, category, stacking, hidden, modifiers, tickSpec, onApply, onExpire);
    }

    private static CoreStatusEffect.HookSpec parseHookSpec(ConfigurationSection s) {
        if (s == null) return null;
        CoreStatusEffect.SoundSpec sound = null;
        ConfigurationSection soundSec = s.getConfigurationSection("sound");
        if (soundSec != null) {
            String key = soundSec.getString("key");
            if (key != null && !key.isBlank()) {
                float volume = (float) soundSec.getDouble("volume", 1.0);
                float pitch = (float) soundSec.getDouble("pitch", 1.0);
                sound = new CoreStatusEffect.SoundSpec(key, volume, pitch);
            }
        }
        CoreStatusEffect.ParticleSpec particles = null;
        ConfigurationSection partSec = s.getConfigurationSection("particles");
        if (partSec != null) {
            String type = partSec.getString("type");
            if (type != null && !type.isBlank()) {
                int count = partSec.getInt("count", 5);
                double defaultSpread = partSec.getDouble("spread", 0.3);
                double spreadX = partSec.getDouble("spread-x", defaultSpread);
                double spreadY = partSec.getDouble("spread-y", defaultSpread);
                double spreadZ = partSec.getDouble("spread-z", defaultSpread);
                particles = new CoreStatusEffect.ParticleSpec(type, count, spreadX, spreadY, spreadZ);
            }
        }
        if (sound == null && particles == null) return null;
        return new CoreStatusEffect.HookSpec(sound, particles);
    }

    private static StatusEffect.Category parseCategory(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "buff" -> StatusEffect.Category.BUFF;
            case "debuff" -> StatusEffect.Category.DEBUFF;
            case "neutral" -> StatusEffect.Category.NEUTRAL;
            default -> throw new IllegalArgumentException("Unknown category: " + s);
        };
    }

    private static StackingStrategy parseStacking(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "refresh" -> StackingStrategy.REFRESH;
            case "stack-power", "stack_power" -> StackingStrategy.STACK_POWER;
            case "take-max", "take_max" -> StackingStrategy.TAKE_MAX;
            case "independent" -> StackingStrategy.INDEPENDENT;
            default -> throw new IllegalArgumentException("Unknown stacking: " + s);
        };
    }

    private static StatModifier.Kind parseKind(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "flat" -> StatModifier.Kind.FLAT;
            case "percent", "%" -> StatModifier.Kind.PERCENT;
            default -> throw new IllegalArgumentException("Unknown modifier kind: " + s);
        };
    }
}
