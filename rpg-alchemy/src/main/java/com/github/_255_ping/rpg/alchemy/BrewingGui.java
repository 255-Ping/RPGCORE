package com.github._255_ping.rpg.alchemy;

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
import org.bukkit.NamespacedKey;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Brewing GUI — 54-slot layout (6 rows):
 * <pre>
 *   Row 0 (0–8):   background / progress bar (when a timed brew is active)
 *   Row 1 (9–17):  ingredient slots at 12, 13, 14 | arrow at 15 | output slot at 16 | rest background
 *   Rows 2–4 (18–44): recipe tiles (27 per page)
 *   Row 5 (45–53): nav bar  ← PREV (45) | bg | bg | bg | ❌ Close (49) | bg | bg | bg | NEXT → (53)
 * </pre>
 *
 * <p>When {@code BrewTicks > 0} on a recipe, clicking it starts a timed brew.
 * Output lands in the dedicated output slot (16) on completion; closing the GUI
 * auto-collects any pending output.
 *
 * <p>A {@code timestamp_ms} is saved with the craft state so offline time is
 * accounted for when the player reopens the station.
 */
public final class BrewingGui implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private static final int[] INPUT_SLOTS    = {12, 13, 14};
    private static final int ARROW_SLOT       = 15;
    private static final int OUTPUT_SLOT      = 16;
    private static final int RECIPE_START     = 18;
    private static final int RECIPES_PER_PAGE = 27;
    private static final int NAV_PREV         = 45;
    private static final int NAV_CLOSE        = GuiConfig.CLOSE_SLOT; // 49
    private static final int NAV_NEXT         = 53;
    private static final int   PROGRESS_INFO_SLOT  = 4;
    private static final int[] PROGRESS_FILL_SLOTS = {0, 1, 2, 3, 5, 6, 7, 8};
    private static final String REPO               = "brewing_craft";
    private static final int TASK_INTERVAL_TICKS   = 4;

    private final RpgAlchemyPlugin plugin;
    private final AlchemyRegistry  registry;
    private final PotionItemFactory potionItems;
    private final NamespacedKey    outputPlaceholderKey;

    private final Map<UUID, Boolean>      open        = new HashMap<>();
    private final Map<UUID, Integer>      page        = new HashMap<>();
    private final Map<UUID, BrewProgress> activeBrews = new HashMap<>();
    private final Map<UUID, BukkitTask>   brewTasks   = new HashMap<>();

    public BrewingGui(RpgAlchemyPlugin plugin, AlchemyRegistry registry, PotionItemFactory potionItems) {
        this.plugin               = plugin;
        this.registry             = registry;
        this.potionItems          = potionItems;
        this.outputPlaceholderKey = new NamespacedKey(plugin, "brewing_output_placeholder");
    }

    // -------------------------------------------------------------------------
    // Open / close
    // -------------------------------------------------------------------------

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(player, 54,
                Component.text("Brewing Station").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));
        GuiConfig gui = RpgServices.guiConfig();
        gui.fillAll(inv);

        open.put(player.getUniqueId(), true);
        page.put(player.getUniqueId(), 0);

        inv.setItem(ARROW_SLOT, buildArrowItem());
        inv.setItem(OUTPUT_SLOT, buildOutputPlaceholder());

        Optional<Map<String, Object>> saved = getRepo().get(player.getUniqueId().toString());
        if (saved.isPresent()) {
            Map<String, Object> data = saved.get();
            String recipeId  = (String) data.get("recipe_id");
            int    elapsed   = data.get("elapsed_ticks") instanceof Number n ? n.intValue() : 0;
            long   savedMs   = data.get("timestamp_ms")  instanceof Number n ? n.longValue() : 0L;
            BrewRecipeDef r  = registry.recipe(recipeId).orElse(null);

            if (r != null && r.brewTicks() > 0) {
                if (savedMs > 0) {
                    long offlineMs    = System.currentTimeMillis() - savedMs;
                    int  offlineTicks = (int) Math.min(offlineMs / 50L, (long) Integer.MAX_VALUE);
                    elapsed           = Math.min(elapsed + offlineTicks, r.brewTicks());
                }

                if (elapsed >= r.brewTicks()) {
                    // Completed offline
                    getRepo().delete(player.getUniqueId().toString());
                    ItemStack output = buildOutput(r.output());
                    if (output != null) {
                        inv.setItem(OUTPUT_SLOT, output);
                        long xp = plugin.getConfig().getLong("xp.per-brew", 30);
                        if (xp > 0) RpgServices.skills().awardXp(player, BuiltinSkill.ALCHEMY.id(), xp);
                        player.sendMessage(plugin.messages().get("brew.success",
                                Map.of("item", r.output().itemId())));
                    } else {
                        plugin.getLogger().warning("Brew craft completed offline for " + player.getName()
                                + " but output '" + r.output().itemId() + "' could not be built.");
                    }
                    for (int slot : INPUT_SLOTS) inv.setItem(slot, null);
                } else {
                    BrewProgress bp = new BrewProgress(recipeId, elapsed, r.brewTicks());
                    activeBrews.put(player.getUniqueId(), bp);
                    showLockedIngredients(inv, r);
                    updateProgressBar(inv, bp);
                    startBrewTask(player, inv);
                }
            } else {
                getRepo().delete(player.getUniqueId().toString());
                plugin.getLogger().warning("Discarded saved brewing craft for " + player.getName()
                        + ": recipe '" + recipeId + "' missing or no longer timed.");
                for (int slot : INPUT_SLOTS) inv.setItem(slot, null);
            }
        } else {
            for (int slot : INPUT_SLOTS) inv.setItem(slot, null);
        }

        refreshRecipes(inv, player);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (open.remove(p.getUniqueId()) == null) return;
        page.remove(p.getUniqueId());

        // Auto-collect any pending output
        ItemStack pendingOutput = e.getInventory().getItem(OUTPUT_SLOT);
        if (pendingOutput != null && !pendingOutput.getType().isAir() && !isOutputPlaceholder(pendingOutput)) {
            HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(pendingOutput.clone());
            for (ItemStack drop : overflow.values()) p.getWorld().dropItemNaturally(p.getLocation(), drop);
        }

        BrewProgress bp = activeBrews.remove(p.getUniqueId());
        BukkitTask task = brewTasks.remove(p.getUniqueId());
        if (task != null) task.cancel();

        if (bp != null) {
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("recipe_id",     bp.recipeId);
            data.put("elapsed_ticks", bp.elapsedTicks);
            data.put("timestamp_ms",  System.currentTimeMillis());
            getRepo().save(p.getUniqueId().toString(), data);
        } else {
            for (int slot : INPUT_SLOTS) {
                ItemStack stack = e.getInventory().getItem(slot);
                if (stack != null && !stack.getType().isAir()) {
                    HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(stack);
                    for (ItemStack drop : overflow.values()) p.getWorld().dropItemNaturally(p.getLocation(), drop);
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

        if (raw >= topSize) {
            if (!e.isShiftClick()) return;
            if (activeBrews.containsKey(p.getUniqueId())) { e.setCancelled(true); return; }
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            e.setCancelled(true);
            for (int slot : INPUT_SLOTS) {
                ItemStack ex = top.getItem(slot);
                if (ex == null || ex.getType().isAir()) {
                    top.setItem(slot, clicked.clone());
                    clicked.setAmount(0);
                    plugin.getServer().getScheduler().runTask(plugin, () -> refreshRecipes(top, p));
                    return;
                }
            }
            return;
        }

        if (raw >= 45) {
            e.setCancelled(true);
            if (RpgServices.guiConfig().isCloseButton(top.getItem(raw))) {
                p.closeInventory();
            } else if (raw == NAV_PREV) {
                int pg = page.getOrDefault(p.getUniqueId(), 0);
                if (pg > 0) { page.put(p.getUniqueId(), pg - 1); refreshRecipes(top, p); }
            } else if (raw == NAV_NEXT) {
                int pg = page.getOrDefault(p.getUniqueId(), 0);
                page.put(p.getUniqueId(), pg + 1);
                refreshRecipes(top, p);
            }
            return;
        }

        if (raw <= 8) { e.setCancelled(true); return; }

        if (isInputSlot(raw)) {
            if (activeBrews.containsKey(p.getUniqueId())) { e.setCancelled(true); return; }
            plugin.getServer().getScheduler().runTask(plugin, () -> refreshRecipes(top, p));
            return;
        }

        // Output slot — click to collect
        if (raw == OUTPUT_SLOT) {
            e.setCancelled(true);
            ItemStack item = top.getItem(OUTPUT_SLOT);
            if (item != null && !item.getType().isAir() && !isOutputPlaceholder(item)) {
                top.setItem(OUTPUT_SLOT, buildOutputPlaceholder());
                HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(item.clone());
                for (ItemStack drop : overflow.values()) p.getWorld().dropItemNaturally(p.getLocation(), drop);
            }
            return;
        }

        if (raw >= RECIPE_START && raw < RECIPE_START + RECIPES_PER_PAGE) {
            e.setCancelled(true);
            if (activeBrews.containsKey(p.getUniqueId())) return;
            ItemStack pendingOutput = top.getItem(OUTPUT_SLOT);
            if (pendingOutput != null && !pendingOutput.getType().isAir() && !isOutputPlaceholder(pendingOutput)) {
                p.sendMessage(plugin.messages().get("brew.collect-output"));
                return;
            }
            int pg  = page.getOrDefault(p.getUniqueId(), 0);
            int idx = pg * RECIPES_PER_PAGE + (raw - RECIPE_START);
            tryBrew(p, top, idx);
            return;
        }

        e.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // Brew logic
    // -------------------------------------------------------------------------

    private void tryBrew(Player p, Inventory inv, int idx) {
        if (!p.hasPermission("rpg.alchemy.use.brew")) return;
        List<BrewRecipeDef> recipes = new java.util.ArrayList<>(registry.allRecipes());
        if (idx >= recipes.size()) return;
        BrewRecipeDef r = recipes.get(idx);

        int level = RpgServices.skills().level(p, BuiltinSkill.ALCHEMY.id());
        if (level < r.requiredLevel()) {
            p.sendMessage(plugin.messages().get("brew.requires-level", Map.of("level", r.requiredLevel())));
            return;
        }
        if (!slotsSatisfy(inv, r.inputs())) {
            p.sendMessage(plugin.messages().get("brew.missing-ingredients"));
            return;
        }

        if (r.brewTicks() <= 0) {
            consume(inv, r.inputs());
            ItemStack output = buildOutput(r.output());
            if (output == null) { p.sendMessage(plugin.messages().get("brew.unknown-recipe")); return; }
            HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(output);
            for (ItemStack drop : overflow.values()) p.getWorld().dropItemNaturally(p.getLocation(), drop);
            long xp = plugin.getConfig().getLong("xp.per-brew", 30);
            if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.ALCHEMY.id(), xp);
            p.sendMessage(plugin.messages().get("brew.success", Map.of("item", r.output().itemId())));
            refreshRecipes(inv, p);
        } else {
            startBrew(p, inv, r);
        }
    }

    private void startBrew(Player p, Inventory inv, BrewRecipeDef r) {
        consume(inv, r.inputs());
        showLockedIngredients(inv, r);

        BrewProgress bp = new BrewProgress(r.id(), 0, r.brewTicks());
        activeBrews.put(p.getUniqueId(), bp);
        updateProgressBar(inv, bp);

        int secsTotal = Math.max(1, (r.brewTicks() + 19) / 20);
        p.sendMessage(plugin.messages().get("brew.started",
                Map.of("item", r.output().itemId(), "time", secsTotal)));

        startBrewTask(p, inv);
    }

    private void startBrewTask(Player p, Inventory inv) {
        UUID uuid = p.getUniqueId();
        BukkitTask old = brewTasks.remove(uuid);
        if (old != null) old.cancel();

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            BrewProgress bp = activeBrews.get(uuid);
            if (bp == null) { brewTasks.remove(uuid); return; }
            bp.elapsedTicks = Math.min(bp.elapsedTicks + TASK_INTERVAL_TICKS, bp.totalTicks);
            updateProgressBar(inv, bp);
            if (bp.isDone()) completeBrew(p, inv, bp);
        }, TASK_INTERVAL_TICKS, TASK_INTERVAL_TICKS);

        brewTasks.put(uuid, task);
    }

    private void completeBrew(Player p, Inventory inv, BrewProgress bp) {
        UUID uuid = p.getUniqueId();

        BukkitTask task = brewTasks.remove(uuid);
        if (task != null) task.cancel();
        activeBrews.remove(uuid);
        getRepo().delete(uuid.toString());

        GuiConfig gui = RpgServices.guiConfig();
        for (int i = 0; i <= 8; i++) inv.setItem(i, gui.backgroundItem());
        for (int slot : INPUT_SLOTS) inv.setItem(slot, null);

        BrewRecipeDef r = registry.recipe(bp.recipeId).orElse(null);
        if (r == null) {
            plugin.getLogger().warning("Brew completed for " + p.getName()
                    + " but recipe '" + bp.recipeId + "' no longer exists — output lost.");
            refreshRecipes(inv, p);
            return;
        }

        ItemStack output = buildOutput(r.output());
        if (output == null) {
            p.sendMessage(plugin.messages().get("brew.unknown-recipe"));
            refreshRecipes(inv, p);
            return;
        }

        ItemStack existing = inv.getItem(OUTPUT_SLOT);
        if (existing == null || existing.getType().isAir() || isOutputPlaceholder(existing)) {
            inv.setItem(OUTPUT_SLOT, output);
        } else {
            HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(output);
            for (ItemStack drop : overflow.values()) p.getWorld().dropItemNaturally(p.getLocation(), drop);
        }

        long xp = plugin.getConfig().getLong("xp.per-brew", 30);
        if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.ALCHEMY.id(), xp);
        p.playSound(p.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.0f);
        p.sendMessage(plugin.messages().get("brew.success", Map.of("item", r.output().itemId())));

        refreshRecipes(inv, p);
    }

    // -------------------------------------------------------------------------
    // Progress bar rendering
    // -------------------------------------------------------------------------

    private static void updateProgressBar(Inventory inv, BrewProgress bp) {
        int secsRemaining = Math.max(0, (bp.totalTicks - bp.elapsedTicks + 19) / 20);
        int fillCount = bp.totalTicks > 0
                ? (int) Math.round(8.0 * Math.min(bp.elapsedTicks, bp.totalTicks) / bp.totalTicks)
                : 8;
        for (int i = 0; i < PROGRESS_FILL_SLOTS.length; i++) {
            inv.setItem(PROGRESS_FILL_SLOTS[i], progressPane(i < fillCount));
        }
        inv.setItem(PROGRESS_INFO_SLOT, brewInfoItem("Brewing: " + bp.recipeId, secsRemaining));
    }

    private static ItemStack progressPane(boolean lit) {
        ItemStack stack = new ItemStack(lit ? Material.PURPLE_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack brewInfoItem(String label, int secsRemaining) {
        ItemStack stack = new ItemStack(Material.BREWING_STAND);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(label)
                    .color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("⏱ " + secsRemaining + "s remaining")
                            .color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void showLockedIngredients(Inventory inv, BrewRecipeDef r) {
        for (int slot : INPUT_SLOTS) inv.setItem(slot, null);
        for (int i = 0; i < r.inputs().size() && i < INPUT_SLOTS.length; i++) {
            ItemStack display = buildIngredientDisplay(r.inputs().get(i));
            if (display != null) inv.setItem(INPUT_SLOTS[i], display);
        }
    }

    // -------------------------------------------------------------------------
    // Recipe list rendering
    // -------------------------------------------------------------------------

    private void refreshRecipes(Inventory inv, Player p) {
        List<BrewRecipeDef> recipes = new java.util.ArrayList<>(registry.allRecipes());
        int totalPages = Math.max(1, (recipes.size() + RECIPES_PER_PAGE - 1) / RECIPES_PER_PAGE);
        int pg = Math.min(page.getOrDefault(p.getUniqueId(), 0), totalPages - 1);
        page.put(p.getUniqueId(), pg);

        int start = pg * RECIPES_PER_PAGE;
        for (int i = 0; i < RECIPES_PER_PAGE; i++) {
            int slot      = RECIPE_START + i;
            int recipeIdx = start + i;
            if (recipeIdx >= recipes.size()) { inv.setItem(slot, paneItem()); continue; }
            BrewRecipeDef r = recipes.get(recipeIdx);
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

    private ItemStack recipeTile(BrewRecipeDef r, boolean satisfied) {
        Material mat = satisfied ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(satisfied ? "Brew: " : "Recipe: ")
                    .append(Component.text(r.id())).color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new java.util.ArrayList<>();
            lore.add(Component.text("Inputs:").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            for (BrewRecipeDef.Ingredient in : r.inputs()) {
                lore.add(Component.text("  " + in.amount() + "x " + in.itemId())
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.text("Output: " + r.output().amount() + "x " + r.output().itemId())
                    .color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Level " + r.requiredLevel() + "+")
                    .color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            if (r.brewTicks() > 0) {
                int secs = Math.max(1, (r.brewTicks() + 19) / 20);
                lore.add(Component.text("⏱ " + secs + "s brew time")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            if (satisfied) {
                lore.add(Component.empty());
                lore.add(LEGACY.deserialize("&8▶ &7Left-click to brew").decoration(TextDecoration.ITALIC, false));
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

    private boolean slotsSatisfy(Inventory inv, List<BrewRecipeDef.Ingredient> inputs) {
        Map<String, Integer> have = new HashMap<>();
        for (int slot : INPUT_SLOTS) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.getType().isAir()) continue;
            have.merge(identifierOf(stack).toLowerCase(Locale.ROOT), stack.getAmount(), Integer::sum);
        }
        for (BrewRecipeDef.Ingredient in : inputs) {
            if (have.getOrDefault(in.itemId().toLowerCase(Locale.ROOT), 0) < in.amount()) return false;
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
            String key  = identifierOf(stack).toLowerCase(Locale.ROOT);
            int    need = remaining.getOrDefault(key, 0);
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

    private ItemStack buildIngredientDisplay(BrewRecipeDef.Ingredient ing) {
        Optional<PotionDef> potion = registry.potion(ing.itemId());
        if (potion.isPresent()) return potionItems.build(potion.get());
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
        Optional<String> potionId = potionItems.idOf(stack);
        if (potionId.isPresent()) return potionId.get();
        Optional<RpgItem> custom = RpgServices.items().from(stack);
        if (custom.isPresent()) return custom.get().id();
        return stack.getType().getKey().getKey();
    }

    private ItemStack buildArrowItem() {
        ItemStack item = new ItemStack(Material.PURPLE_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("→").color(NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.ITALIC, false));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildOutputPlaceholder() {
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Output Slot")
                    .color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Completed brews appear here.")
                            .color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Click to collect.")
                            .color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
            ));
            meta.getPersistentDataContainer().set(outputPlaceholderKey, PersistentDataType.BYTE, (byte) 1);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isOutputPlaceholder(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return true;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(outputPlaceholderKey, PersistentDataType.BYTE);
    }

    private static ItemStack paneItem() { return RpgServices.guiConfig().backgroundItem(); }

    private DataStore.Repository getRepo() {
        return RpgServices.dataStore().repository(REPO);
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    private static final class BrewProgress {
        final String recipeId;
        final int    totalTicks;
        int          elapsedTicks;

        BrewProgress(String recipeId, int elapsedTicks, int totalTicks) {
            this.recipeId     = recipeId;
            this.elapsedTicks = elapsedTicks;
            this.totalTicks   = totalTicks;
        }

        boolean isDone() { return elapsedTicks >= totalTicks; }
    }
}
