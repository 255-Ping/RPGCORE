package com.github._255_ping.rpg.core.recipes;

import com.github._255_ping.rpg.api.items.RpgItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Loads admin-defined crafting recipes from {@code plugins/rpg-core/recipes/crafting/*.yml} and
 * registers them with Bukkit. Vanilla recipes are still suppressed by the vanilla-suppression
 * listener; our registered recipes survive because their {@link NamespacedKey} namespace is
 * {@code rpg-core}, which the suppression listener now ignores.
 *
 * <p>Schema (matches {@code docs/content/recipes.md}):
 * <pre>
 * super_diamond_sword:
 *   Shape:
 *   - "DDD"
 *   - "DDD"
 *   - " S "
 *   Ingredients:
 *     D: { item: super_diamond, amount: 1 }
 *     S: { item: oak_stick, amount: 1 }
 *   Output: { item: super_diamond_sword, amount: 1 }
 * </pre>
 * Shapeless recipes use {@code Shapeless: true} and an Ingredients list.
 */
public final class RecipeLoader {

    private final JavaPlugin plugin;
    private final File craftingDir;
    private final List<NamespacedKey> registeredKeys = new ArrayList<>();

    public RecipeLoader(JavaPlugin plugin, File craftingDir) {
        this.plugin = plugin;
        this.craftingDir = craftingDir;
    }

    public void reload() {
        unregisterAll();
        if (!craftingDir.isDirectory()) return;
        File[] files = craftingDir.listFiles((d, n) -> n.endsWith(".yml"));
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
                        plugin.getLogger().warning("Skipping recipe '" + id + "' in "
                                + f.getName() + ": " + ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to parse " + f.getName() + ": " + ex.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + registeredKeys.size() + " custom crafting recipes.");
    }

    public void unregisterAll() {
        for (NamespacedKey k : registeredKeys) {
            Bukkit.removeRecipe(k);
        }
        registeredKeys.clear();
        // Defensive sweep: also remove any rpg-core recipes that we may have orphaned
        // (e.g., after a hot-reload that lost track of keys).
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe r = it.next();
            if (r instanceof org.bukkit.Keyed keyed
                    && plugin.getName().toLowerCase(Locale.ROOT).equals(keyed.getKey().getNamespace())) {
                it.remove();
            }
        }
    }

    private void registerOne(String id, ConfigurationSection s) {
        NamespacedKey key = new NamespacedKey(plugin, id);
        ItemStack output = buildOutput(s.getConfigurationSection("Output"));
        if (output == null) throw new IllegalArgumentException("missing Output");

        boolean shapeless = s.getBoolean("Shapeless", false);
        if (shapeless) {
            ShapelessRecipe r = new ShapelessRecipe(key, output);
            for (Object o : s.getList("Ingredients", List.of())) {
                if (o instanceof Map<?, ?> m) {
                    String item = String.valueOf(m.get("item"));
                    Object amt = m.get("amount");
                    int amount = amt instanceof Number n ? n.intValue() : 1;
                    Material mat = resolveMaterial(item);
                    if (mat == null) throw new IllegalArgumentException("unknown item: " + item);
                    r.addIngredient(amount, mat);
                }
            }
            Bukkit.addRecipe(r);
        } else {
            List<String> shape = s.getStringList("Shape");
            if (shape.isEmpty() || shape.size() > 3) {
                throw new IllegalArgumentException("Shape must be 1-3 rows");
            }
            ShapedRecipe r = new ShapedRecipe(key, output);
            r.shape(shape.toArray(new String[0]));
            ConfigurationSection ings = s.getConfigurationSection("Ingredients");
            if (ings == null) throw new IllegalArgumentException("missing Ingredients");
            for (String ch : ings.getKeys(false)) {
                if (ch.length() != 1) throw new IllegalArgumentException("Ingredient key must be 1 char: " + ch);
                ConfigurationSection ing = ings.getConfigurationSection(ch);
                if (ing == null) continue;
                String item = ing.getString("item");
                Material mat = resolveMaterial(item);
                if (mat == null) throw new IllegalArgumentException("unknown item: " + item);
                r.setIngredient(ch.charAt(0), mat);
            }
            Bukkit.addRecipe(r);
        }
        registeredKeys.add(key);
    }

    private ItemStack buildOutput(ConfigurationSection s) {
        if (s == null) return null;
        String item = s.getString("item");
        int amount = s.getInt("amount", 1);
        if (item == null) return null;
        Optional<RpgItem> custom = com.github._255_ping.rpg.api.RpgServices.items().get(item);
        if (custom.isPresent()) {
            ItemStack stack = custom.get().toItemStack();
            stack.setAmount(amount);
            return stack;
        }
        Material mat = Material.matchMaterial(item);
        return mat == null ? null : new ItemStack(mat, amount);
    }

    private static Material resolveMaterial(String id) {
        Optional<RpgItem> custom = com.github._255_ping.rpg.api.RpgServices.items().get(id);
        if (custom.isPresent()) return custom.get().material();
        return Material.matchMaterial(id);
    }
}
