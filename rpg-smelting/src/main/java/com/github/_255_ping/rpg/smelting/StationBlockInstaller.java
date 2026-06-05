package com.github._255_ping.rpg.smelting;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Installs the smelting station block definition into rpg-core's blocks/ directory. */
public final class StationBlockInstaller {

    private static final String CORE_FILENAME = "rpg-smelting-stations.yml";

    public static void installInto(JavaPlugin self) {
        Plugin core = Bukkit.getPluginManager().getPlugin("rpg-core");
        if (core == null) return;
        File blocksDir = new File(core.getDataFolder(), "blocks");
        if (!blocksDir.isDirectory() && !blocksDir.mkdirs()) return;
        File target = new File(blocksDir, CORE_FILENAME);
        if (target.exists()) return;
        try (InputStream in = self.getResource("blocks-example.yml")) {
            if (in == null) return;
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            self.getLogger().info("Installed smelting station block to " + target.getName() + ".");
            try { core.getClass().getMethod("reloadAll").invoke(core); }
            catch (Exception ignored) {
                self.getLogger().info("Run /rpg reload to load it.");
            }
        } catch (Exception ex) {
            self.getLogger().warning("Failed to install smelting station: " + ex.getMessage());
        }
    }

    private StationBlockInstaller() {}
}
