package com.github._255_ping.rpg.cooking;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.gui.GuiConfig;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.persistence.DataStore;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Cooking GUI — 54-slot layout (6 rows):
 * <pre>
 *   Row 0 (0–8):   background / progress bar (when a timed craft is active)
 *   Row 1 (9–17):  ingredient slots centred at 12, 13, 14; rest background
 *   Rows 2–4 (18–44): recipe tiles (27 per page)
 *   Row 5 (45–53): nav bar  ← PREV (45) | bg | bg | bg | ❌ Close (49) | bg | bg | bg | NEXT → (53)
 * </pre>
 *
 * <p>When {@code CookTicks > 0} on a recipe, clicking it starts a timed craft:
 * ingredients are consumed immediately, a progress bar fills row 0, and the
 * output is delivered on completion. If the player closes the GUI mid-craft the
 * state is saved to DataStore and restored the next time they open any cooking
 * station.
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
    // Progress bar: slot 4 = info item (CLOCK); slots 0–3 and 5–8 = 8 fill panes
    private static final int   PROGRESS_INFO_SLOT = 4;
    private static final int[] PROGRESS_FILL_SLOTS = {0, 1, 2, 3, 5, 6, 7, 8};
    // DataStore repository name for in-progress cooking crafts
    private static final String REPO = "cooking_craft";
    // Task fires every 4 ticks — close enough to a tick-accurate timer while being efficient
    private static final int TASK_INTERVAL_TICKS = 4;

    private final RpgCookingPlugin plugin;
    private final CookingRegistry registry;
    private final Map<UUID, Boolean>       open         = new HashMap<>();
    private final Map<UUID, Integer>       page         = new HashMap<>();
    private final Map<UUID, CraftProgress> activeCrafts = new HashMap<>();
    private final Map<UUID, BukkitTask>    craftTasks   = new HashMap<>();

    public CookingGui(RpgCookingPlugin plugin, CookingRegistry registry) {
        this.plugin   = plugin;
        this.registry = registry;
    }

    // -------------------------------------------------------------------------
    // Open / close
    // -------------------------------------------------------------------------

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, 54,
                Component.text("Cooking Station").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        GuiConfig gui = RpgServices.guiConfig();
        gui.fillAll(inv);

        open.put(p.getUniqueId(), true);
        page.put(p.getUniqueId(), 0);

        // Restore any saved craft progress from a previous session
        Optional<Map<String, Object>> saved = getRepo().get(p.getUniqueId().toString());
        if (saved.isPresent()) {
            Map<String, Object> data = saved.get();
            String recipeId  = (String) data.get("recipe_id");
            int    elapsed   = data.get("elapsed_ticks") instanceof Number n ? n.intValue() : 0;
            CookRecipeDef r  = registry.get(recipeId).orElse(null);
            if (r != null && r.cookTicks() > 0) {
                CraftProgress cp = new CraftProgress(recipeId, elapsed, r.cookTicks());
                activeCrafts.put(p.getUniqueId(), cp);
                showLockedIngredients(inv, r);
                updateProgressBar(inv, cp);
                startCraftTask(p, inv);
            } else {
                // Recipe no longer exists or became instant — discard saved state
                getRepo().delete(p.getUniqueId().toString());
                plugin.getLogger().warning("Discarded saved cooking craft for " + p.getName()
                        + ": recipe '" + recipeId + "' missing or no longer timed.");
                for (int slot : INPUT_SLOTS) inv.setItem(slot, null);
            }
        } else {
            for (int slot : INPUT_SLOTS) inv.setItem(slot, null);
        }

        refresh(inv, p);
        p.openInventory(inv);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (open.remove(p.getUniqueId()) == null) return;
        page.remove(p.getUniqueId());

        CraftProgress cp = activeCrafts.remove(p.getUniqueId());
        BukkitTask task  = craftTasks.remove(p.getUniqueId());
        if (task != null) task.cancel();

        if (cp != null) {
            // Save progress so it can be resumed next time the player opens a station
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("recipe_id",     cp.recipeId);
            data.put("elapsed_ticks", cp.elapsedTicks);
            getRepo().save(p.getUniqueId().toString(), data);
            // Ingredients were consumed at craft start — do NOT return them
        } else {
            // No active craft: return whatever the player left in the input slots
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
    }

    // -------------------------------------------------------------------------
    // Click handling
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (open.get(p.getUniqueId()) == null) return;
        int raw     = e.getRawSlot();
        int topSize = e.getView().getTopInventory().getSize();
        Inventory top = e.getView().getTopInventory();

        // Bottom inventory: shift-click routes to first free input slot (locked during craft)
        if (raw >= topSize) {
            if (!e.isShiftClick()) return;
            if (activeCrafts.containsKey(p.getUniqueId())) {
                e.setCancelled(true);
                return; // input slots locked while crafting
            }
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
                page.put(p.getUniqueId(), pg + 1);
                refresh(top, p);
            }
            return;
        }

        // Progress bar (row 0) — always locked
        if (raw <= 8) {
            e.setCancelled(true);
            return;
        }

        // Input slots — allow interaction unless craft is active
        if (isInputSlot(raw)) {
            if (activeCrafts.containsKey(p.getUniqueId())) {
                e.setCancelled(true);
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> refresh(top, p));
            return;
        }

        // Recipe tile click
        if (raw >= RECIPE_START && raw < RECIPE_START + RECIPES_PER_PAGE) {
            e.setCancelled(true);
            if (activeCrafts.containsKey(p.getUniqueId())) {
                // A craft is already running — ignore the click silently
                return;
            }
            int pg  = page.getOrDefault(p.getUniqueId(), 0);
            int idx = pg * RECIPES_PER_PAGE + (raw - RECIPE_START);
            tryCook(p, top, idx);
            return;
        }

        // Everything else in the top inventory is locked
        e.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // Craft logic
    // -------------------------------------------------------------------------

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

        if (r.cookTicks() <= 0) {
            // Instant craft — deliver output immediately
            consume(inv, r.inputs());
            ItemStack output = buildOutput(r.output());
            if (output == null) { p.sendMessage(plugin.messages().get("cook.unknown-output")); return; }
            HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(output);
            for (ItemStack drop : overflow.values()) p.getWorld().dropItemNaturally(p.getLocation(), drop);
            long xp = plugin.getConfig().getLong("xp.per-cook", 20);
            if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.COOKING.id(), xp);
            p.sendMessage(plugin.messages().get("cook.success", Map.of("item", r.output().itemId())));
            refresh(inv, p);
        } else {
            // Timed craft — consume ingredients, show progress bar, start timer
            startCraft(p, inv, r);
        }
    }

    /**
     * Consumes ingredients, locks the input display, and starts the craft timer.
     * Ingredients are consumed upfront so the player can't pick them back up.
     */
    private void startCraft(Player p, Inventory inv, CookRecipeDef r) {
        consume(inv, r.inputs());
        showLockedIngredients(inv, r);

        CraftProgress cp = new CraftProgress(r.id(), 0, r.cookTicks());
        activeCrafts.put(p.getUniqueId(), cp);
        updateProgressBar(inv, cp);

        int secsTotal = Math.max(1, (r.cookTicks() + 19) / 20);
        p.sendMessage(plugin.messages().get("cook.started",
                Map.of("item", r.output().itemId(), "time", secsTotal)));

        startCraftTask(p, inv);
    }

    /**
     * Starts the repeating timer task for the active craft. Safe to call when
     * restoring a saved craft on GUI open — activeCrafts must already have an
     * entry for the player before calling this.
     */
    private void startCraftTask(Player p, Inventory inv) {
        UUID uuid = p.getUniqueId();
        // Defensive: cancel any stale task (shouldn't normally happen)
        BukkitTask old = craftTasks.remove(uuid);
        if (old != null) old.cancel();

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            CraftProgress cp = activeCrafts.get(uuid);
            if (cp == null) {
                // GUI was closed and onClose already cleaned up; shouldn't reach here
                craftTasks.remove(uuid);
                return;
            }
            cp.elapsedTicks = Math.min(cp.elapsedTicks + TASK_INTERVAL_TICKS, cp.totalTicks);
            updateProgressBar(inv, cp);
            if (cp.isDone()) completeCraft(p, inv, cp);
        }, TASK_INTERVAL_TICKS, TASK_INTERVAL_TICKS);

        craftTasks.put(uuid, task);
    }

    /**
     * Called when the craft timer reaches its target. Delivers the output item,
     * awards XP, plays a completion sound, and restores the GUI to idle state.
     */
    private void completeCraft(Player p, Inventory inv, CraftProgress cp) {
        UUID uuid = p.getUniqueId();

        // Cancel the task and clear state before doing anything else
        BukkitTask task = craftTasks.remove(uuid);
        if (task != null) task.cancel();
        activeCrafts.remove(uuid);
        getRepo().delete(uuid.toString());

        // Restore row 0 to background and clear locked ingredient display
        GuiConfig gui = RpgServices.guiConfig();
        for (int i = 0; i <= 8; i++) inv.setItem(i, gui.backgroundItem());
        for (int slot : INPUT_SLOTS) inv.setItem(slot, null);

        // Look up recipe (may have been removed by admin during the craft)
        CookRecipeDef r = registry.get(cp.recipeId).orElse(null);
        if (r == null) {
            plugin.getLogger().warning("Cook craft completed for " + p.getName()
                    + " but recipe '" + cp.recipeId + "' no longer exists — output lost.");
            refresh(inv, p);
            return;
        }

        // Deliver output
        ItemStack output = buildOutput(r.output());
        if (output == null) {
            p.sendMessage(plugin.messages().get("cook.unknown-output"));
            refresh(inv, p);
            return;
        }
        HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(output);
        for (ItemStack drop : overflow.values()) p.getWorld().dropItemNaturally(p.getLocation(), drop);

        // XP and feedback
        long xp = plugin.getConfig().getLong("xp.per-cook", 20);
        if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.COOKING.id(), xp);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        p.sendMessage(plugin.messages().get("cook.success", Map.of("item", r.output().itemId())));

        refresh(inv, p);
    }

    // -------------------------------------------------------------------------
    // Progress bar rendering
    // -------------------------------------------------------------------------

    /** Renders the 9-slot progress bar in row 0. Slot 4 = clock info; 0–3 and 5–8 = fill panes. */
    private static void updateProgressBar(Inventory inv, CraftProgress cp) {
        int secsRemaining = Math.max(0, (cp.totalTicks - cp.elapsedTicks + 19) / 20);
        // fillCount = how many of the 8 fill slots should be lit
        int fillCount = cp.totalTicks > 0
                ? (int) Math.round(8.0 * Math.min(cp.elapsedTicks, cp.totalTicks) / cp.totalTicks)
                : 8;
        for (int i = 0; i < PROGRESS_FILL_SLOTS.length; i++) {
            inv.setItem(PROGRESS_FILL_SLOTS[i], progressPane(i < fillCount));
        }
        inv.setItem(PROGRESS_INFO_SLOT, craftInfoItem("Cooking: " + cp.recipeId, secsRemaining));
    }

    private static ItemStack progressPane(boolean lit) {
        ItemStack stack = new ItemStack(lit ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack craftInfoItem(String label, int secsRemaining) {
        ItemStack stack = new ItemStack(Material.CLOCK);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(label)
                    .color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("⏱ " + secsRemaining + "s remaining")
                            .color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** Places locked ingredient display items in the input slots (visual only — already consumed). */
    private void showLockedIngredients(Inventory inv, CookRecipeDef r) {
        for (int slot : INPUT_SLOTS) inv.setItem(slot, null);
        for (int i = 0; i < r.inputs().size() && i < INPUT_SLOTS.length; i++) {
            ItemStack display = buildIngredientDisplay(r.inputs().get(i));
            if (display != null) inv.setItem(INPUT_SLOTS[i], display);
        }
    }

    // -------------------------------------------------------------------------
    // Recipe list rendering
    // -------------------------------------------------------------------------

    /**
     * Refreshes rows 2–4 (recipe tiles) and the nav bar. Does not touch row 0
     * (progress bar) or the input slots, so it is safe to call during a craft.
     */
    private void refresh(Inventory inv, Player p) {
        List<CookRecipeDef> recipes = new java.util.ArrayList<>(registry.all());
        int totalPages = Math.max(1, (recipes.size() + RECIPES_PER_PAGE - 1) / RECIPES_PER_PAGE);
        int pg = Math.min(page.getOrDefault(p.getUniqueId(), 0), totalPages - 1);
        page.put(p.getUniqueId(), pg);

        int start = pg * RECIPES_PER_PAGE;
        for (int i = 0; i < RECIPES_PER_PAGE; i++) {
            int slot      = RECIPE_START + i;
            int recipeIdx = start + i;
            if (recipeIdx >= recipes.size()) { inv.setItem(slot, paneItem()); continue; }
            CookRecipeDef r = recipes.get(recipeIdx);
            inv.setItem(slot, recipeTile(r, slotsSatisfy(inv, r.inputs())));
        }

        placeNavBar(inv, pg, totalPages);
    }

    private void placeNavBar(Inventory inv, int pg, int totalPages) {
        RpgServices.guiConfig().placeNavBar(inv);
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
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
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
            if (r.cookTicks() > 0) {
                int secs = Math.max(1, (r.cookTicks() + 19) / 20);
                lore.add(Component.text("⏱ " + secs + "s cook time")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            if (satisfied) {
                lore.add(Component.empty());
                lore.add(LEGACY.deserialize("&8▶ &7Left-click to cook").decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean isInputSlot(int raw) {
        for (int s : INPUT_SLOTS) if (raw == s) return true;
        return false;
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
            String id   = identifierOf(stack).toLowerCase(Locale.ROOT);
            int    need = remaining.getOrDefault(id, 0);
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

    private ItemStack buildIngredientDisplay(CookRecipeDef.Ingredient ing) {
        Optional<RpgItem> custom = RpgServices.items().get(ing.itemId());
        if (custom.isPresent()) {
            ItemStack s = custom.get().toItemStack();
            s.setAmount(ing.amount());
            return s;
        }
        Material mat = Material.matchMaterial(ing.itemId());
        return mat == null ? null : new ItemStack(mat, ing.amount());
    }

    private String identifierOf(ItemStack stack) {
        Optional<RpgItem> custom = RpgServices.items().from(stack);
        if (custom.isPresent()) return custom.get().id();
        return stack.getType().getKey().getKey();
    }

    private static ItemStack paneItem() {
        return RpgServices.guiConfig().backgroundItem();
    }

    private DataStore.Repository getRepo() {
        return RpgServices.dataStore().repository(REPO);
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    /** Mutable in-progress craft state for a single player. */
    private static final class CraftProgress {
        final String recipeId;
        final int    totalTicks;
        int          elapsedTicks;

        CraftProgress(String recipeId, int elapsedTicks, int totalTicks) {
            this.recipeId     = recipeId;
            this.elapsedTicks = elapsedTicks;
            this.totalTicks   = totalTicks;
        }

        boolean isDone() { return elapsedTicks >= totalTicks; }
    }
}
