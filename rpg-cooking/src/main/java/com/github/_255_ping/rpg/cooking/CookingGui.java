package com.github._255_ping.rpg.cooking;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.gui.GuiConfig;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
 * Cooking GUI — 54-slot layout (6 rows):
 * <pre>
 *   Row 0 (0–8):   background
 *   Row 1 (9–17):  ingredient slots centred at 12, 13, 14; rest background
 *   Rows 2–4 (18–44): recipe tiles (27 per page)
 *   Row 5 (45–53): nav bar  ← PREV (45) | bg | bg | bg | ❌ Close (49) | bg | bg | bg | NEXT → (53)
 * </pre>
 *
 * <p><b>Per-player isolation</b>: every call to {@link #open} creates a brand-new
 * {@link Inventory} object for that specific player.
 */
public final class CookingGui implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    // Ingredient slots: row 1, centred (matches brewing station layout)
    private static final int[] INPUT_SLOTS    = {12, 13, 14};
    // Recipe tiles fill rows 2–4
    private static final int RECIPE_START     = 18;
    private static final int RECIPES_PER_PAGE = 27; // rows 2–4
    // Nav bar
    private static final int NAV_PREV  = 45;
    private static final int NAV_CLOSE = GuiConfig.CLOSE_SLOT; // 49
    private static final int NAV_NEXT  = 53;

    private final RpgCookingPlugin plugin;
    private final CookingRegistry registry;
    private final Map<UUID, Boolean> open = new HashMap<>();
    private final Map<UUID, Integer> page  = new HashMap<>();

    public CookingGui(RpgCookingPlugin plugin, CookingRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, 54,
                Component.text("Cooking Station").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        GuiConfig gui = RpgServices.guiConfig();
        gui.fillAll(inv);
        for (int slot : INPUT_SLOTS) inv.setItem(slot, null);
        open.put(p.getUniqueId(), true);
        page.put(p.getUniqueId(), 0);
        refresh(inv, p);
        p.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (open.get(p.getUniqueId()) == null) return;
        int raw = e.getRawSlot();
        int topSize = e.getView().getTopInventory().getSize();
        Inventory top = e.getView().getTopInventory();

        // Bottom inventory: shift-click routes to first free input slot
        if (raw >= topSize) {
            if (!e.isShiftClick()) return;
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            e.setCancelled(true);
            for (int slot : INPUT_SLOTS) {
                ItemStack ex = top.getItem(slot);
                if (ex == null || ex.getType().isAir()) {
                    top.setItem(slot, clicked.clone());
                    clicked.setAmount(0);
                    plugin.getServer().getScheduler().runTask(plugin, () -> refresh(top, p));
                    return;
                }
            }
            return;
        }

        // Nav bar (row 5): close, prev, next — block all other nav-bar slots
        if (raw >= 45) {
            e.setCancelled(true);
            if (RpgServices.guiConfig().isCloseButton(top.getItem(raw))) {
                p.closeInventory();
            } else if (raw == NAV_PREV) {
                int pg = page.getOrDefault(p.getUniqueId(), 0);
                if (pg > 0) { page.put(p.getUniqueId(), pg - 1); refresh(top, p); }
            } else if (raw == NAV_NEXT) {
                int pg = page.getOrDefault(p.getUniqueId(), 0);
                page.put(p.getUniqueId(), pg + 1); // refresh clamps
                refresh(top, p);
            }
            return;
        }

        // Input slots — allow interaction, defer refresh
        if (isInputSlot(raw)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> refresh(top, p));
            return;
        }

        // Recipe tile click
        if (raw >= RECIPE_START && raw < RECIPE_START + RECIPES_PER_PAGE) {
            e.setCancelled(true);
            int pg = page.getOrDefault(p.getUniqueId(), 0);
            int idx = pg * RECIPES_PER_PAGE + (raw - RECIPE_START);
            tryCook(p, top, idx);
            return;
        }

        // Everything else in the top inventory is locked
        e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (open.remove(p.getUniqueId()) == null) return;
        page.remove(p.getUniqueId());
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

    private void refresh(Inventory inv, Player p) {
        List<CookRecipeDef> recipes = new java.util.ArrayList<>(registry.all());
        int totalPages = Math.max(1, (recipes.size() + RECIPES_PER_PAGE - 1) / RECIPES_PER_PAGE);
        int pg = Math.min(page.getOrDefault(p.getUniqueId(), 0), totalPages - 1);
        page.put(p.getUniqueId(), pg);

        int start = pg * RECIPES_PER_PAGE;
        for (int i = 0; i < RECIPES_PER_PAGE; i++) {
            int slot = RECIPE_START + i;
            int recipeIdx = start + i;
            if (recipeIdx >= recipes.size()) { inv.setItem(slot, paneItem()); continue; }
            CookRecipeDef r = recipes.get(recipeIdx);
            inv.setItem(slot, recipeTile(r, slotsSatisfy(inv, r.inputs())));
        }

        placeNavBar(inv, pg, totalPages);
    }

    private void placeNavBar(Inventory inv, int pg, int totalPages) {
        RpgServices.guiConfig().placeNavBar(inv); // border row + close at 49
        if (totalPages > 1) {
            inv.setItem(NAV_PREV, pg > 0
                    ? pageArrow("&7← Previous Page", pg, totalPages)
                    : RpgServices.guiConfig().borderItem());
            inv.setItem(NAV_NEXT, pg < totalPages - 1
                    ? pageArrow("&7Next Page →", pg, totalPages)
                    : RpgServices.guiConfig().borderItem());
        }
    }

    private static ItemStack pageArrow(String label, int pg, int total) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize(label).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(LEGACY.deserialize("&8Page &7" + (pg + 1) + " &8/ &7" + total)
                    .decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack recipeTile(CookRecipeDef r, boolean satisfied) {
        Material mat = satisfied ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text((satisfied ? "Cook: " : "Recipe: ") + r.id())
                    .color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new java.util.ArrayList<>();
            lore.add(Component.text("Inputs:").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            for (CookRecipeDef.Ingredient in : r.inputs()) {
                lore.add(Component.text("  " + in.amount() + "x " + in.itemId())
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.text("Output: " + r.output().amount() + "x " + r.output().itemId())
                    .color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Level " + r.requiredLevel() + "+")
                    .color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            if (satisfied) {
                lore.add(Component.empty());
                lore.add(LEGACY.deserialize("&8▶ &7Left-click to cook").decoration(TextDecoration.ITALIC, false));
            }
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
            p.sendMessage(plugin.messages().get("cook.requires-level", Map.of("level", r.requiredLevel())));
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
        for (ItemStack drop : overflow.values()) p.getWorld().dropItemNaturally(p.getLocation(), drop);
        long xp = plugin.getConfig().getLong("xp.per-cook", 20);
        if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.COOKING.id(), xp);
        p.sendMessage(plugin.messages().get("cook.success", Map.of("item", r.output().itemId())));
        refresh(inv, p);
    }

    private boolean slotsSatisfy(Inventory inv, List<CookRecipeDef.Ingredient> inputs) {
        Map<String, Integer> have = new HashMap<>();
        for (int slot : INPUT_SLOTS) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.getType().isAir()) continue;
            have.merge(identifierOf(stack).toLowerCase(Locale.ROOT), stack.getAmount(), Integer::sum);
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
        return RpgServices.guiConfig().backgroundItem();
    }
}
