package com.github._255_ping.rpg.core.recipes;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.items.RpgItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Loads admin-defined furnace recipes from {@code plugins/rpg-core/recipes/smelting/*.yml} and
 * registers them with Bukkit. The vanilla-suppression listener cancels vanilla smelting via
 * {@code FurnaceSmeltEvent}, but our admin-defined recipes are explicitly allowed to run when
 * they match our namespace.
 *
 * <p>Schema:
 * <pre>
 * iron_ingot_from_dust:
 *   Input: { item: iron_dust, amount: 1 }
 *   Output: { item: iron_ingot, amount: 1 }
 *   Experience: 0.7
 *   CookTicks: 200
 * </pre>
 */
public final class SmeltingLoader {

    private final JavaPlugin plugin;
    private final File dir;
    private final List<NamespacedKey> registeredKeys = new ArrayList<>();

    public SmeltingLoader(JavaPlugin plugin, File dir) {
        this.plugin = plugin;
        this.dir = dir;
    }

    public void reload() {
        unregisterAll();
        if (!dir.isDirectory()) return;
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
                for (String id : y.getKeys(false)) {
                    ConfigurationSection s = y.getConfigurationSection(id);
                    if (s == null) continue;
                    try {
                        registerOne(id.toLowerCase(Locale.ROOT), s);
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Skipping smelting recipe '" + id + "' in "
                                + f.getName() + ": " + ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to parse " + f.getName() + ": " + ex.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + registeredKeys.size() + " custom smelting recipes.");
    }

    public void unregisterAll() {
        for (NamespacedKey k : registeredKeys) Bukkit.removeRecipe(k);
        registeredKeys.clear();
    }

    public boolean owns(NamespacedKey key) {
        return registeredKeys.contains(key);
    }

    private void registerOne(String id, ConfigurationSection s) {
        ConfigurationSection inSec = s.getConfigurationSection("Input");
        ConfigurationSection outSec = s.getConfigurationSection("Output");
        if (inSec == null || outSec == null) throw new IllegalArgumentException("missing Input/Output");

        Material inputMat = resolveMaterial(inSec.getString("item"));
        if (inputMat == null) throw new IllegalArgumentException("unknown input: " + inSec.getString("item"));
        ItemStack output = buildOutput(outSec);
        if (output == null) throw new IllegalArgumentException("unknown output: " + outSec.getString("item"));

        float xp = (float) s.getDouble("Experience", 0.1);
        int cookTicks = s.getInt("CookTicks", 200);

        NamespacedKey key = new NamespacedKey(plugin, "smelt_" + id);
        FurnaceRecipe recipe = new FurnaceRecipe(key, output, new RecipeChoice.MaterialChoice(inputMat), xp, cookTicks);
        Bukkit.addRecipe(recipe);
        registeredKeys.add(key);
    }

    private ItemStack buildOutput(ConfigurationSection s) {
        String item = s.getString("item");
        int amount = s.getInt("amount", 1);
        if (item == null) return null;
        Optional<RpgItem> custom = RpgServices.items().get(item);
        if (custom.isPresent()) {
            ItemStack stack = custom.get().toItemStack();
            stack.setAmount(amount);
            return stack;
        }
        Material mat = Material.matchMaterial(item);
        return mat == null ? null : new ItemStack(mat, amount);
    }

    private static Material resolveMaterial(String id) {
        if (id == null) return null;
        Optional<RpgItem> custom = RpgServices.items().get(id);
        if (custom.isPresent()) return custom.get().material();
        return Material.matchMaterial(id);
    }
}
