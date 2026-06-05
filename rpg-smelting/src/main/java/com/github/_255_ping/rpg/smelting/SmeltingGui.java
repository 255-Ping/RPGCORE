package com.github._255_ping.rpg.smelting;

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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Smelting GUI — 54-slot layout (6 rows):
 * <pre>
 *   Row 0 (0–8):   background / progress bar (when a timed smelt is active)
 *   Row 1 (9–17):  single input slot at centre (13); rest background
 *   Rows 2–4 (18–44): recipe tiles (27 per page)
 *   Row 5 (45–53): nav bar — ← PREV (45) | ... | ❌ Close (49) | ... | NEXT → (53)
 * </pre>
 *
 * <p>When {@code SmeltTicks > 0} on a recipe, clicking it starts a timed smelt:
 * the input item is consumed immediately, a progress bar fills row 0, and the
 * output is delivered on completion. Closing mid-smelt saves state to DataStore;
 * reopening any smelting station restores it.
 */
public final class SmeltingGui implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    // Single input slot in the centre of row 1
    private static final int INPUT_SLOT            = 13;
    // Recipe tile area
    private static final int RECIPE_START          = 18;
    private static final int RECIPES_PER_PAGE      = 27;
    // Nav slots
    private static final int NAV_PREV              = 45;
    private static final int NAV_CLOSE             = GuiConfig.CLOSE_SLOT; // 49
    private static final int NAV_NEXT              = 53;
    // Progress bar: slot 4 = furnace info item; 0-3 and 5-8 = 8 fill panes
    private static final int   PROGRESS_INFO_SLOT  = 4;
    private static final int[] PROGRESS_FILL_SLOTS = {0, 1, 2, 3, 5, 6, 7, 8};
    // DataStore key
    private static final String REPO               = "smelting_craft";
    // Timer fires every 4 ticks
    private static final int TASK_INTERVAL_TICKS   = 4;

    private final RpgSmeltingPlugin plugin;
    private final SmeltingRegistry  registry;
    private final Map<UUID, Boolean>       open         = new HashMap<>();
    private final Map<UUID, Integer>       page         = new HashMap<>();
    private final Map<UUID, SmeltProgress> activeSmelt  = new HashMap<>();
    private final Map<UUID, BukkitTask>    smeltTasks   = new HashMap<>();

    public SmeltingGui(RpgSmeltingPlugin plugin, SmeltingRegistry registry) {
        this.plugin   = plugin;
        this.registry = registry;
    }

    // ── Open / close ──────────────────────────────────────────────────────

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(p, 54,
                Component.text("Smelting Station")
                        .color(NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD));
        GuiConfig gui = RpgServices.guiConfig();
        gui.fillAll(inv);

        open.put(p.getUniqueId(), true);
        page.put(p.getUniqueId(), 0);

        // Restore any saved smelt progress
        Optional<Map<String, Object>> saved = getRepo().get(p.getUniqueId().toString());
        if (saved.isPresent()) {
            Map<String, Object> data = saved.get();
            String recipeId = (String) data.get("recipe_id");
            int    elapsed  = data.get("elapsed_ticks") instanceof Number n ? n.intValue() : 0;
            SmeltRecipeDef r = registry.get(recipeId).orElse(null);
            if (r != null && r.smeltTicks() > 0) {
                SmeltProgress sp = new SmeltProgress(recipeId, elapsed, r.smeltTicks());
                activeSmelt.put(p.getUniqueId(), sp);
                showLockedInput(inv, r);
                updateProgressBar(inv, sp);
                startSmeltTask(p, inv);
            } else {
                getRepo().delete(p.getUniqueId().toString());
                inv.setItem(INPUT_SLOT, null);
            }
        } else {
            inv.setItem(INPUT_SLOT, null);
        }

        refresh(inv, p);
        p.openInventory(inv);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (open.remove(p.getUniqueId()) == null) return;
        page.remove(p.getUniqueId());

        SmeltProgress sp = activeSmelt.remove(p.getUniqueId());
        BukkitTask task  = smeltTasks.remove(p.getUniqueId());
        if (task != null) task.cancel();

        if (sp != null) {
            // Save progress; ingredients consumed at craft start, do NOT return them
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("recipe_id",     sp.recipeId);
            data.put("elapsed_ticks", sp.elapsedTicks);
            getRepo().save(p.getUniqueId().toString(), data);
        } else {
            // Return any item the player left in the input slot
            ItemStack stack = e.getInventory().getItem(INPUT_SLOT);
            if (stack != null && !stack.getType().isAir()) {
                HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(stack);
                for (ItemStack drop : overflow.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), drop);
                }
            }
        }
    }

    // ── Click handling ────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (open.get(p.getUniqueId()) == null) return;
        int raw     = e.getRawSlot();
        int topSize = e.getView().getTopInventory().getSize();
        Inventory top = e.getView().getTopInventory();

        // Bottom inventory: shift-click into the single input slot
        if (raw >= topSize) {
            if (!e.isShiftClick()) return;
            if (activeSmelt.containsKey(p.getUniqueId())) { e.setCancelled(true); return; }
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            e.setCancelled(true);
            ItemStack ex = top.getItem(INPUT_SLOT);
            if (ex == null || ex.getType().isAir()) {
                top.setItem(INPUT_SLOT, clicked.clone());
                clicked.setAmount(0);
                plugin.getServer().getScheduler().runTask(plugin, () -> refresh(top, p));
            }
            return;
        }

        // Nav bar
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

        // Progress bar row — always locked
        if (raw <= 8) { e.setCancelled(true); return; }

        // Input slot
        if (raw == INPUT_SLOT) {
            if (activeSmelt.containsKey(p.getUniqueId())) { e.setCancelled(true); return; }
            plugin.getServer().getScheduler().runTask(plugin, () -> refresh(top, p));
            return;
        }

        // Recipe tiles
        if (raw >= RECIPE_START && raw < RECIPE_START + RECIPES_PER_PAGE) {
            e.setCancelled(true);
            if (activeSmelt.containsKey(p.getUniqueId())) return; // already smelting
            int pg  = page.getOrDefault(p.getUniqueId(), 0);
            int idx = pg * RECIPES_PER_PAGE + (raw - RECIPE_START);
            trySmelt(p, top, idx);
            return;
        }

        e.setCancelled(true);
    }

    // ── Smelt logic ───────────────────────────────────────────────────────

    private void trySmelt(Player p, Inventory inv, int idx) {
        if (!p.hasPermission("rpg.smelting.use")) return;
        List<SmeltRecipeDef> recipes = new java.util.ArrayList<>(registry.all());
        if (idx >= recipes.size()) return;
        SmeltRecipeDef r = recipes.get(idx);

        int level = RpgServices.skills().level(p, BuiltinSkill.MINING.id()); // smelting uses mining skill
        if (level < r.requiredLevel()) {
            p.sendMessage(plugin.messages().get("smelt.requires-level", Map.of("level", r.requiredLevel())));
            return;
        }
        if (!inputSatisfies(inv, r.input())) {
            p.sendMessage(plugin.messages().get("smelt.missing-ingredients"));
            return;
        }

        if (r.smeltTicks() <= 0) {
            // Instant smelt
            consumeInput(inv, r.input());
            ItemStack output = buildOutput(r.output());
            if (output == null) { p.sendMessage(plugin.messages().get("smelt.unknown-output")); return; }
            HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(output);
            for (ItemStack drop : overflow.values()) p.getWorld().dropItemNaturally(p.getLocation(), drop);
            long xp = plugin.getConfig().getLong("xp.per-smelt", 20);
            if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.MINING.id(), xp);
            p.sendMessage(plugin.messages().get("smelt.success", Map.of("item", r.output().itemId())));
            refresh(inv, p);
        } else {
            startSmelt(p, inv, r);
        }
    }

    private void startSmelt(Player p, Inventory inv, SmeltRecipeDef r) {
        consumeInput(inv, r.input());
        showLockedInput(inv, r);

        SmeltProgress sp = new SmeltProgress(r.id(), 0, r.smeltTicks());
        activeSmelt.put(p.getUniqueId(), sp);
        updateProgressBar(inv, sp);

        int secsTotal = Math.max(1, (r.smeltTicks() + 19) / 20);
        p.sendMessage(plugin.messages().get("smelt.started",
                Map.of("item", r.output().itemId(), "time", secsTotal)));

        startSmeltTask(p, inv);
    }

    private void startSmeltTask(Player p, Inventory inv) {
        UUID uuid = p.getUniqueId();
        BukkitTask old = smeltTasks.remove(uuid);
        if (old != null) old.cancel();

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            SmeltProgress sp = activeSmelt.get(uuid);
            if (sp == null) { smeltTasks.remove(uuid); return; }
            sp.elapsedTicks = Math.min(sp.elapsedTicks + TASK_INTERVAL_TICKS, sp.totalTicks);
            updateProgressBar(inv, sp);
            if (sp.isDone()) completeSmelt(p, inv, sp);
        }, TASK_INTERVAL_TICKS, TASK_INTERVAL_TICKS);

        smeltTasks.put(uuid, task);
    }

    private void completeSmelt(Player p, Inventory inv, SmeltProgress sp) {
        UUID uuid = p.getUniqueId();

        BukkitTask task = smeltTasks.remove(uuid);
        if (task != null) task.cancel();
        activeSmelt.remove(uuid);
        getRepo().delete(uuid.toString());

        GuiConfig gui = RpgServices.guiConfig();
        for (int i = 0; i <= 8; i++) inv.setItem(i, gui.backgroundItem());
        inv.setItem(INPUT_SLOT, null);

        SmeltRecipeDef r = registry.get(sp.recipeId).orElse(null);
        if (r == null) {
            plugin.getLogger().warning("Smelt completed for " + p.getName()
                    + " but recipe '" + sp.recipeId + "' no longer exists — output lost.");
            refresh(inv, p);
            return;
        }

        ItemStack output = buildOutput(r.output());
        if (output == null) {
            p.sendMessage(plugin.messages().get("smelt.unknown-output"));
            refresh(inv, p);
            return;
        }
        HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(output);
        for (ItemStack drop : overflow.values()) p.getWorld().dropItemNaturally(p.getLocation(), drop);

        long xp = plugin.getConfig().getLong("xp.per-smelt", 20);
        if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.MINING.id(), xp);
        p.playSound(p.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, 1.0f, 1.5f);
        p.sendMessage(plugin.messages().get("smelt.success", Map.of("item", r.output().itemId())));

        refresh(inv, p);
    }

    // ── Progress bar ──────────────────────────────────────────────────────

    private static void updateProgressBar(Inventory inv, SmeltProgress sp) {
        int secsRemaining = Math.max(0, (sp.totalTicks - sp.elapsedTicks + 19) / 20);
        int fillCount = sp.totalTicks > 0
                ? (int) Math.round(8.0 * Math.min(sp.elapsedTicks, sp.totalTicks) / sp.totalTicks)
                : 8;
        for (int i = 0; i < PROGRESS_FILL_SLOTS.length; i++) {
            inv.setItem(PROGRESS_FILL_SLOTS[i], progressPane(i < fillCount));
        }
        inv.setItem(PROGRESS_INFO_SLOT, smeltInfoItem("Smelting: " + sp.recipeId, secsRemaining));
    }

    private static ItemStack progressPane(boolean lit) {
        ItemStack stack = new ItemStack(lit ? Material.ORANGE_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty().decoration(TextDecoration.ITALIC, false));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack smeltInfoItem(String label, int secsRemaining) {
        ItemStack stack = new ItemStack(Material.FURNACE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(label)
                    .color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("⏱ " + secsRemaining + "s remaining")
                            .color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void showLockedInput(Inventory inv, SmeltRecipeDef r) {
        inv.setItem(INPUT_SLOT, buildIngredientDisplay(r.input()));
    }

    // ── Recipe list rendering ─────────────────────────────────────────────

    private void refresh(Inventory inv, Player p) {
        List<SmeltRecipeDef> recipes = new java.util.ArrayList<>(registry.all());
        int totalPages = Math.max(1, (recipes.size() + RECIPES_PER_PAGE - 1) / RECIPES_PER_PAGE);
        int pg = Math.min(page.getOrDefault(p.getUniqueId(), 0), totalPages - 1);
        page.put(p.getUniqueId(), pg);

        int start = pg * RECIPES_PER_PAGE;
        for (int i = 0; i < RECIPES_PER_PAGE; i++) {
            int slot      = RECIPE_START + i;
            int recipeIdx = start + i;
            if (recipeIdx >= recipes.size()) { inv.setItem(slot, paneItem()); continue; }
            SmeltRecipeDef r = recipes.get(recipeIdx);
            inv.setItem(slot, recipeTile(r, inputSatisfies(inv, r.input())));
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
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack recipeTile(SmeltRecipeDef r, boolean satisfied) {
        Material mat = satisfied ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text((satisfied ? "Smelt: " : "Recipe: ") + r.id())
                    .color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new java.util.ArrayList<>();
            lore.add(Component.text("Input: " + r.input().amount() + "x " + r.input().itemId())
                    .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Output: " + r.output().amount() + "x " + r.output().itemId())
                    .color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Level " + r.requiredLevel() + "+")
                    .color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            if (r.smeltTicks() > 0) {
                int secs = Math.max(1, (r.smeltTicks() + 19) / 20);
                lore.add(Component.text("⏱ " + secs + "s smelt time")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            if (satisfied) {
                lore.add(Component.empty());
                lore.add(LEGACY.deserialize("&8▶ &7Left-click to smelt").decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private boolean inputSatisfies(Inventory inv, SmeltRecipeDef.Ingredient input) {
        ItemStack stack = inv.getItem(INPUT_SLOT);
        if (stack == null || stack.getType().isAir()) return false;
        String id = identifierOf(stack);
        return id.equalsIgnoreCase(input.itemId()) && stack.getAmount() >= input.amount();
    }

    private void consumeInput(Inventory inv, SmeltRecipeDef.Ingredient input) {
        ItemStack stack = inv.getItem(INPUT_SLOT);
        if (stack == null) return;
        stack.setAmount(stack.getAmount() - input.amount());
        if (stack.getAmount() <= 0) inv.setItem(INPUT_SLOT, null);
    }

    private ItemStack buildOutput(SmeltRecipeDef.Ingredient out) {
        Optional<RpgItem> custom = RpgServices.items().get(out.itemId());
        if (custom.isPresent()) {
            ItemStack s = custom.get().toItemStack();
            s.setAmount(out.amount());
            return s;
        }
        Material mat = Material.matchMaterial(out.itemId());
        return mat == null ? null : new ItemStack(mat, out.amount());
    }

    private ItemStack buildIngredientDisplay(SmeltRecipeDef.Ingredient ing) {
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

    // ── Inner type ────────────────────────────────────────────────────────

    private static final class SmeltProgress {
        final String recipeId;
        final int    totalTicks;
        int          elapsedTicks;

        SmeltProgress(String recipeId, int elapsedTicks, int totalTicks) {
            this.recipeId     = recipeId;
            this.elapsedTicks = elapsedTicks;
            this.totalTicks   = totalTicks;
        }

        boolean isDone() { return elapsedTicks >= totalTicks; }
    }
}
