package com.github._255_ping.rpg.core.blocks;

import com.github._255_ping.rpg.api.blocks.RequiredToolType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public final class BlockLoader {

    private final File folder;
    private final CoreBlockRegistry registry;
    private final Logger logger;

    public BlockLoader(File folder, CoreBlockRegistry registry, Logger logger) {
        this.folder = folder;
        this.registry = registry;
        this.logger = logger;
    }

    public void loadAll() {
        registry.clearDefinitions();
        if (!folder.isDirectory()) return;
        File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                loadFile(f);
            } catch (Exception ex) {
                logger.warning("Failed to parse blocks file " + f.getName() + ": " + ex.getMessage());
            }
        }
    }

    private void loadFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(id);
            if (section == null) {
                logger.warning("block '" + id + "' in " + file.getName() + " is not a section, skipping");
                continue;
            }
            try {
                registry.register(parse(id, section));
            } catch (Exception ex) {
                logger.warning("Skipping block '" + id + "' in " + file.getName() + ": " + ex.getMessage());
            }
        }
    }

    private CoreBlock parse(String id, ConfigurationSection s) {
        String matName = s.getString("MinecraftBlock");
        if (matName == null) throw new IllegalArgumentException("missing MinecraftBlock");
        Material material = Material.matchMaterial(matName);
        if (material == null) throw new IllegalArgumentException("unknown MinecraftBlock: " + matName);

        double toughness = s.getDouble("Toughness", 100);
        int requiredPower = s.getInt("RequiredPower", 0);
        RequiredToolType tool = parseTool(s.getString("RequiredToolType", "any"));
        int respawnTicks = s.getInt("RespawnTicks", 0);

        String placeholderName = s.getString("RespawnPlaceholder", matName);
        Material placeholder = Material.matchMaterial(placeholderName);
        if (placeholder == null) placeholder = material;

        boolean interactable = s.getBoolean("Interactable", false);
        String stationType = s.getString("StationType", "");

        List<String> dropSpecs = s.getStringList("Drops");
        long xp = s.getLong("XP", 0);
        String hologramText = s.getString("Hologram", "");
        double hologramYOffset = s.getDouble("HologramYOffset", 1.2);

        return new CoreBlock(id, material, toughness, requiredPower, tool,
                respawnTicks, placeholder, interactable, stationType, dropSpecs, xp,
                hologramText, hologramYOffset);
    }

    private static RequiredToolType parseTool(String s) {
        try {
            return RequiredToolType.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return RequiredToolType.ANY;
        }
    }
}
