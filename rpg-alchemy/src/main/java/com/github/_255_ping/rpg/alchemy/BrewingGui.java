package com.github._255_ping.rpg.alchemy;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Brewing GUI: player drops ingredients into the 3 input slots (10,11,12) and clicks a recipe
 * tile to brew. We resolve the first recipe whose inputs match the contents (matters: id +
 * minimum amount). On confirm, inputs are consumed and the output potion is given to the player.
 *
 * <p><b>Per-player isolation</b>: every call to {@link #open} creates a brand-new
 * {@link Inventory} object for that specific player. Two players clicking the same station
 * block simultaneously each get their own independent inventory — they cannot see or interfere
 * with each other's ingredients.
 */
public final class BrewingGui implements Listener {

    private static final int[] INPUT_SLOTS = {10, 11, 12};

    private final RpgAlchemyPlugin plugin;
    private final AlchemyRegistry registry;
    private final PotionItemFactory potionItems;
    private final Map<UUID, Boolean> open = new HashMap<>();

    public BrewingGui(RpgAlchemyPlugin plugin, AlchemyRegistry registry, PotionItemFactory potionItems) {
        this.plugin = plugin;
        this.registry = registry;
        this.potionItems = potionItems;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(player, 36,
                Component.text("Brewing Station").color(NamedTextColor.LIGHT_PURPLE));
        ItemStack pane = paneItem();
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
        for (int slot : INPUT_SLOTS) inv.setItem(slot, null);
        refreshRecipes(inv);
        player.openInventory(inv);
        open.put(player.getUniqueId(), true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (open.get(p.getUniqueId()) == null) return;
        int raw = e.getRawSlot();
        int topSize = e.getView().getTopInventory().getSize();

        // Shift-click from bottom inventory → route to first free input slot.
        if (raw >= topSize) {
            if (!e.isShiftClick()) return;
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            e.setCancelled(true);
            Inventory top = e.getView().getTopInventory();
            for (int slot : INPUT_SLOTS) {
                ItemStack existing = top.getItem(slot);
                if (existing == null || existing.getType().isAir()) {
                    top.setItem(slot, clicked.clone());
                    clicked.setAmount(0);
                    plugin.getServer().getScheduler().runTask(plugin, () -> refreshRecipes(top));
                    return;
                }
            }
            return;
        }

        // Inputs: 10..12 — allow drop. Recipes: 19..25.
        if (isInputSlot(raw)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> refreshRecipes(e.getView().getTopInventory()));
            return;
        }
        if (raw >= 19 && raw <= 25) {
            e.setCancelled(true);
            tryBrew(p, e.getView().getTopInventory(), raw - 19);
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (open.remove(p.getUniqueId()) == null) return;
        // Return any ingredients still in input slots.
        for (int slot : INPUT_SLOTS) {
            ItemStack stack = e.getInventory().getItem(slot);
            if (stack != null && !stack.getType().isAir()) {
                HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(stack);
                for (ItemStack drop : overflow.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), drop);
                }
            }
        }
    }

    private boolean isInputSlot(int raw) {
        for (int s : INPUT_SLOTS) if (raw == s) return true;
        return false;
    }

    private void refreshRecipes(Inventory inv) {
        // Show all recipes; only highlight ones whose inputs the player satisfies right now.
        java.util.List<BrewRecipeDef> recipes = new java.util.ArrayList<>(registry.allRecipes());
        for (int i = 0; i < 7; i++) {
            int slot = 19 + i;
            if (i >= recipes.size()) { inv.setItem(slot, paneItem()); continue; }
            BrewRecipeDef r = recipes.get(i);
            ItemStack tile = recipeTile(r, slotsSatisfy(inv, r.inputs()));
            inv.setItem(slot, tile);
        }
    }

    private ItemStack recipeTile(BrewRecipeDef r, boolean satisfied) {
        Material mat = satisfied ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(satisfied ? "Brew: " : "Recipe: ")
                    .append(Component.text(r.id())).color(NamedTextColor.WHITE)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            java.util.List<Component> lore = new java.util.ArrayList<>();
            lore.add(Component.text("Inputs:").color(NamedTextColor.GRAY));
            for (BrewRecipeDef.Ingredient in : r.inputs()) {
                lore.add(Component.text("  " + in.amount() + "x " + in.itemId()).color(NamedTextColor.GRAY));
            }
            lore.add(Component.text("Output: " + r.output().amount() + "x " + r.output().itemId())
                    .color(NamedTextColor.AQUA));
            lore.add(Component.text("Level " + r.requiredLevel() + "+").color(NamedTextColor.YELLOW));
            meta.lore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void tryBrew(Player p, Inventory inv, int recipeIdx) {
        if (!p.hasPermission("rpg.alchemy.use.brew")) return;
        List<BrewRecipeDef> recipes = new java.util.ArrayList<>(registry.allRecipes());
        if (recipeIdx >= recipes.size()) return;
        BrewRecipeDef r = recipes.get(recipeIdx);
        int level = RpgServices.skills().level(p, BuiltinSkill.ALCHEMY.id());
        if (level < r.requiredLevel()) {
            p.sendMessage(plugin.messages().get("brew.requires-level",
                    java.util.Map.of("level", r.requiredLevel())));
            return;
        }
        if (!slotsSatisfy(inv, r.inputs())) {
            p.sendMessage(plugin.messages().get("brew.missing-ingredients"));
            return;
        }
        consume(inv, r.inputs());
        ItemStack output = buildOutput(r.output());
        if (output == null) {
            p.sendMessage(plugin.messages().get("brew.unknown-recipe"));
            return;
        }
        HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(output);
        for (ItemStack drop : overflow.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), drop);
        }
        long xp = plugin.getConfig().getLong("xp.per-brew", 30);
        if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.ALCHEMY.id(), xp);
        p.sendMessage(plugin.messages().get("brew.success",
                java.util.Map.of("item", r.output().itemId())));
        refreshRecipes(inv);
    }

    private boolean slotsSatisfy(Inventory inv, List<BrewRecipeDef.Ingredient> inputs) {
        // Count by item id across input slots.
        Map<String, Integer> have = new HashMap<>();
        for (int slot : INPUT_SLOTS) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.getType().isAir()) continue;
            String key = identifierOf(stack);
            have.merge(key.toLowerCase(Locale.ROOT), stack.getAmount(), Integer::sum);
        }
        for (BrewRecipeDef.Ingredient in : inputs) {
            int needed = in.amount();
            int got = have.getOrDefault(in.itemId().toLowerCase(Locale.ROOT), 0);
            if (got < needed) return false;
        }
        return true;
    }

    private void consume(Inventory inv, List<BrewRecipeDef.Ingredient> inputs) {
        Map<String, Integer> remaining = new HashMap<>();
        for (BrewRecipeDef.Ingredient in : inputs) {
            remaining.merge(in.itemId().toLowerCase(Locale.ROOT), in.amount(), Integer::sum);
        }
        for (int slot : INPUT_SLOTS) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.getType().isAir()) continue;
            String key = identifierOf(stack).toLowerCase(Locale.ROOT);
            int need = remaining.getOrDefault(key, 0);
            if (need <= 0) continue;
            int take = Math.min(need, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) inv.setItem(slot, null);
            remaining.put(key, need - take);
        }
    }

    private ItemStack buildOutput(BrewRecipeDef.Ingredient out) {
        Optional<PotionDef> potion = registry.potion(out.itemId());
        if (potion.isPresent()) {
            ItemStack stack = potionItems.build(potion.get());
            stack.setAmount(out.amount());
            return stack;
        }
        Optional<RpgItem> custom = RpgServices.items().get(out.itemId());
        if (custom.isPresent()) {
            ItemStack stack = custom.get().toItemStack();
            stack.setAmount(out.amount());
            return stack;
        }
        Material mat = Material.matchMaterial(out.itemId());
        return mat == null ? null : new ItemStack(mat, out.amount());
    }

    private String identifierOf(ItemStack stack) {
        Optional<String> potionId = potionItems.idOf(stack);
        if (potionId.isPresent()) return potionId.get();
        Optional<RpgItem> custom = RpgServices.items().from(stack);
        if (custom.isPresent()) return custom.get().id();
        return stack.getType().getKey().getKey();
    }

    private static ItemStack paneItem() {
        ItemStack stack = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
