package com.github._255_ping.rpg.enchanting;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Copies the bundled station block definitions ({@code blocks-example.yml}) into rpg-core's
 * {@code blocks/} data folder on enable, so the admin doesn't have to copy them by hand.
 *
 * <p>Idempotent: if {@code blocks/rpg-enchanting-stations.yml} already exists in rpg-core's data
 * folder, we don't touch it (the admin may have edited it). If it doesn't, we copy our resource
 * over and ask core to reload its block defs so the IDs become available immediately.
 */
public final class StationBlockInstaller {

    private static final String CORE_FILENAME = "rpg-enchanting-stations.yml";

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
            if (in == null) {
                self.getLogger().warning("blocks-example.yml not bundled in jar.");
                return;
            }
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            self.getLogger().info("Installed station blocks to " + target.getName() + ". "
                    + "Run /rpg reload to make them available.");
            triggerReload(self, core);
        } catch (Exception ex) {
            self.getLogger().warning("Failed to install station blocks: " + ex.getMessage());
        }
    }

    private static void triggerReload(JavaPlugin self, Plugin core) {
        // Call the reloadAll() method on RpgCorePlugin reflectively, so we don't need to depend on
        // core's concrete class. If we can't, the admin can `/rpg reload` themselves.
        try {
            core.getClass().getMethod("reloadAll").invoke(core);
            self.getLogger().info("rpg-core blocks reloaded; station blocks now available.");
        } catch (Exception ex) {
            self.getLogger().info("Run /rpg reload to load the new station blocks.");
        }
    }
}
