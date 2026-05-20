package com.github._255_ping.rpg.alchemy;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Copies the brewing station block definition into rpg-core's blocks/ folder on enable.
 * Idempotent — leaves admin edits alone.
 */
public final class StationBlockInstaller {

    private static final String CORE_FILENAME = "rpg-alchemy-stations.yml";

    public static void installInto(JavaPlugin self) {
        Plugin core = Bukkit.getPluginManager().getPlugin("rpg-core");
        if (core == null) {
            self.getLogger().warning("rpg-core not loaded; skipping station block install.");
            return;
        }
        File blocksDir = new File(core.getDataFolder(), "blocks");
        if (!blocksDir.isDirectory() && !blocksDir.mkdirs()) {
            self.getLogger().warning("Could not create " + blocksDir + "; station blocks not installed.");
            return;
        }
        File target = new File(blocksDir, CORE_FILENAME);
        if (target.exists()) return;
        try (InputStream in = self.getResource("blocks-example.yml")) {
            if (in == null) return;
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            self.getLogger().info("Installed brewing station to " + target.getName() + ".");
            try {
                core.getClass().getMethod("reloadAll").invoke(core);
            } catch (Exception ignored) {
                self.getLogger().info("Run /rpg reload to load it.");
            }
        } catch (Exception ex) {
            self.getLogger().warning("Failed to install brewing station: " + ex.getMessage());
        }
    }
}
