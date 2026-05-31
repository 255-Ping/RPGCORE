package com.github._255_ping.rpg.cooking;

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
 * Cooking GUI: 3 ingredient slots + recipe tiles. Clicking a tile cooks instantly if the player
 * meets the level requirement and has the required inputs.
 *
 * <p><b>Per-player isolation</b>: every call to {@link #open} creates a brand-new
 * {@link Inventory} object for that specific player. Two players clicking the same station
 * block simultaneously each get their own independent inventory — they cannot see or interfere
 * with each other's ingredients.
 */
public final class CookingGui implements Listener {

    // Ingredient slots: row 0, middle three (slot 4, 5, 6 in a 36-slot GUI)
    private static final int[] INPUT_SLOTS = {4, 5, 6};
    // Recipe tiles start at slot 9 (second row) and fill forward
    private static final int RECIPE_START = 9;

    private final RpgCookingPlugin plugin;
    private final CookingRegistry registry;
    private final Map<UUID, Boolean> open = new HashMap<>();

    public CookingGui(RpgCookingPlugin plugin, CookingRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, 36,
                Component.text("Cooking Station").color(NamedTextColor.GOLD));
        ItemStack pane = paneItem();
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
        for (int slot : INPUT_SLOTS) inv.setItem(slot, null);
        refresh(inv);
        p.openInventory(inv);
        open.put(p.getUniqueId(), true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (open.get(p.getUniqueId()) == null) return;
        int raw = e.getRawSlot();
        int topSize = e.getView().getTopInventory().getSize();
        Inventory top = e.getView().getTopInventory();

        // Shift-click from player's bottom inventory → route into first free input slot
        if (raw >= topSize && e.isShiftClick()) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            e.setCancelled(true);
            for (int slot : INPUT_SLOTS) {
                ItemStack existing = top.getItem(slot);
                if (existing == null || existing.getType().isAir()) {
                    top.setItem(slot, clicked.clone());
                    clicked.setAmount(0);
                    plugin.getServer().getScheduler().runTask(plugin, () -> refresh(top));
                    return;
                }
            }
            // No free slot — do nothing (item stays in player inventory)
            return;
        }

        if (raw >= topSize) return; // other bottom-inventory interactions: ignore

        if (isInputSlot(raw)) {
            // Allow the click; defer refresh so the inventory state settles first
            plugin.getServer().getScheduler().runTask(plugin, () -> refresh(top));
            return;
        }

        // Recipe tile click
        if (raw >= RECIPE_START) {
            e.setCancelled(true);
            int idx = raw - RECIPE_START;
            tryCook(p, top, idx);
            return;
        }

        // Everything else in the top inventory is a pane
        e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (open.remove(p.getUniqueId()) == null) return;
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

    private void refresh(Inventory inv) {
        List<CookRecipeDef> recipes = new java.util.ArrayList<>(registry.all());
        int maxTiles = inv.getSize() - RECIPE_START;
        for (int i = 0; i < maxTiles; i++) {
            int slot = RECIPE_START + i;
            if (i >= recipes.size()) { inv.setItem(slot, paneItem()); continue; }
            CookRecipeDef r = recipes.get(i);
            inv.setItem(slot, recipeTile(r, slotsSatisfy(inv, r.inputs())));
        }
    }

    private ItemStack recipeTile(CookRecipeDef r, boolean satisfied) {
        Material mat = satisfied ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text((satisfied ? "Cook: " : "Recipe: ") + r.id())
                    .color(NamedTextColor.WHITE)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            java.util.List<Component> lore = new java.util.ArrayList<>();
            lore.add(Component.text("Inputs:").color(NamedTextColor.GRAY));
            for (CookRecipeDef.Ingredient in : r.inputs()) {
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

    private void tryCook(Player p, Inventory inv, int idx) {
        if (!p.hasPermission("rpg.cooking.use")) return;
        List<CookRecipeDef> recipes = new java.util.ArrayList<>(registry.all());
        if (idx >= recipes.size()) return;
        CookRecipeDef r = recipes.get(idx);
        int level = RpgServices.skills().level(p, BuiltinSkill.COOKING.id());
        if (level < r.requiredLevel()) {
            p.sendMessage(plugin.messages().get("cook.requires-level",
                    Map.of("level", r.requiredLevel())));
            return;
        }
        if (!slotsSatisfy(inv, r.inputs())) {
            p.sendMessage(plugin.messages().get("cook.missing-ingredients"));
            return;
        }
        consume(inv, r.inputs());
        ItemStack output = buildOutput(r.output());
        if (output == null) {
            p.sendMessage(plugin.messages().get("cook.unknown-output"));
            return;
        }
        HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(output);
        for (ItemStack drop : overflow.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), drop);
        }
        long xp = plugin.getConfig().getLong("xp.per-cook", 20);
        if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.COOKING.id(), xp);
        p.sendMessage(plugin.messages().get("cook.success", Map.of("item", r.output().itemId())));
        refresh(inv);
    }

    private boolean slotsSatisfy(Inventory inv, List<CookRecipeDef.Ingredient> inputs) {
        Map<String, Integer> have = new HashMap<>();
        for (int slot : INPUT_SLOTS) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.getType().isAir()) continue;
            String id = identifierOf(stack).toLowerCase(Locale.ROOT);
            have.merge(id, stack.getAmount(), Integer::sum);
        }
        for (CookRecipeDef.Ingredient in : inputs) {
            if (have.getOrDefault(in.itemId().toLowerCase(Locale.ROOT), 0) < in.amount()) return false;
        }
        return true;
    }

    private void consume(Inventory inv, List<CookRecipeDef.Ingredient> inputs) {
        Map<String, Integer> remaining = new HashMap<>();
        for (CookRecipeDef.Ingredient in : inputs) {
            remaining.merge(in.itemId().toLowerCase(Locale.ROOT), in.amount(), Integer::sum);
        }
        for (int slot : INPUT_SLOTS) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.getType().isAir()) continue;
            String id = identifierOf(stack).toLowerCase(Locale.ROOT);
            int need = remaining.getOrDefault(id, 0);
            if (need <= 0) continue;
            int take = Math.min(need, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) inv.setItem(slot, null);
            remaining.put(id, need - take);
        }
    }

    private ItemStack buildOutput(CookRecipeDef.Ingredient out) {
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
        Optional<RpgItem> custom = RpgServices.items().from(stack);
        if (custom.isPresent()) return custom.get().id();
        return stack.getType().getKey().getKey();
    }

    private static ItemStack paneItem() {
        ItemStack stack = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
