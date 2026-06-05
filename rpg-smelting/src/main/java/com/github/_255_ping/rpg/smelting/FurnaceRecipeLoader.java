package com.github._255_ping.rpg.smelting;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.items.RpgItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registers a vanilla {@link FurnaceRecipe} for each {@link SmeltRecipeDef} so the
 * same items can also be smelted in a regular furnace (when
 * {@code features.vanilla-furnace-recipes} is enabled in config).
 *
 * <p>The {@code VanillaSuppressionListener} in rpg-core now allows any non-minecraft
 * recipe namespace through, so these recipes work even with vanilla smelting suppressed.
 */
public final class FurnaceRecipeLoader {

    private final JavaPlugin plugin;
    private final List<NamespacedKey> registered = new ArrayList<>();

    public FurnaceRecipeLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(SmeltRecipeDef def) {
        Material inputMat = resolveMaterial(def.input().itemId());
        ItemStack output  = buildOutput(def.output());
        if (inputMat == null || output == null) {
            plugin.getLogger().warning("Skipping vanilla furnace recipe '" + def.id()
                    + "': could not resolve input or output.");
            return;
        }

        // Use at least 1 tick for vanilla recipe (0-tick furnace recipes are invalid in Bukkit).
        int cookTicks = Math.max(1, def.smeltTicks());

        NamespacedKey key = new NamespacedKey(plugin, "smelt_" + def.id());
        FurnaceRecipe recipe = new FurnaceRecipe(key, output,
                new RecipeChoice.MaterialChoice(inputMat), def.vanillaXp(), cookTicks);
        Bukkit.addRecipe(recipe);
        registered.add(key);
    }

    public void unregisterAll() {
        for (NamespacedKey k : registered) Bukkit.removeRecipe(k);
        registered.clear();
    }

    private Material resolveMaterial(String id) {
        try {
            Optional<RpgItem> custom = RpgServices.items().get(id);
            if (custom.isPresent()) return custom.get().material();
        } catch (Exception ignored) {}
        return Material.matchMaterial(id);
    }

    private ItemStack buildOutput(SmeltRecipeDef.Ingredient out) {
        try {
            Optional<RpgItem> custom = RpgServices.items().get(out.itemId());
            if (custom.isPresent()) {
                ItemStack s = custom.get().toItemStack();
                s.setAmount(out.amount());
                return s;
            }
        } catch (Exception ignored) {}
        Material m = Material.matchMaterial(out.itemId());
        return m == null ? null : new ItemStack(m, out.amount());
    }
}
