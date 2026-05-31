package com.github._255_ping.rpg.enchanting;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.gui.GuiConfig;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Two-mode GUI for the enchanting station block.
 *
 * <h3>ENCHANTING mode</h3>
 * <ul>
 *   <li>Slot 11 — target item</li>
 *   <li>Slots 19–25 (row 2) and 28–34 (row 3) — up to 14 enchantment buttons, one per
 *       applicable enchant for the item; click to apply (costs currency + skill level)</li>
 * </ul>
 *
 * <h3>ANVIL mode</h3>
 * <ul>
 *   <li>Slot 11 — target item</li>
 *   <li>Slot 13 — reforge stone or upgrade book (physical item consumed on apply)</li>
 *   <li>Slot 15 — Apply button; shows compatibility status and what will happen</li>
 *   <li>Slot 22 — contextual hint label</li>
 * </ul>
 *
 * <p>Reforge stones and upgrade books are physical {@link ItemStack}s obtained via
 * {@code /enchanting give reforge|upgrade}. Placing a stone/book in slot 13 alongside
 * a compatible item and clicking Apply consumes the stone/book (and any required reagent)
 * and modifies the target item.
 *
 * <p>Per-player isolation: every {@link #open} call creates a fresh {@link Inventory}
 * so two players at the same station never share state.
 */
public final class StationGui implements Listener {

    public enum Mode { ENCHANTING, ANVIL }

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    // Shared slot
    private static final int TARGET_SLOT     = 11;   // item to enchant/reforge/upgrade

    // Enchanting mode — enchant option slots (rows 2 and 3, excluding border cols)
    private static final int[] ENCHANT_SLOTS = {19, 20, 21, 22, 23, 24, 25,
                                                 28, 29, 30, 31, 32, 33, 34};

    // Anvil mode
    private static final int ANVIL_STONE_SLOT   = 13;   // reforge stone or upgrade book
    private static final int ANVIL_APPLY_SLOT   = 15;   // apply button
    private static final int ANVIL_HINT_SLOT    = 22;   // contextual hint label

    private final RpgEnchantingPlugin plugin;
    private final EnchantRegistry registry;
    private final ItemModifier modifier;
    private final Map<UUID, Mode> open = new HashMap<>();

    public StationGui(RpgEnchantingPlugin plugin, EnchantRegistry registry, ItemModifier modifier) {
        this.plugin = plugin;
        this.registry = registry;
        this.modifier = modifier;
    }

    // ── Open ─────────────────────────────────────────────────────────────────

    public void open(Player player, Mode mode) {
        String title = mode == Mode.ENCHANTING ? "✦ Enchanting Table" : "⚒ Custom Anvil";
        Inventory inv = player.getServer().createInventory(player, 45,
                Component.text(title).color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
        GuiConfig gui = RpgServices.guiConfig();
        gui.fillAll(inv);
        inv.setItem(TARGET_SLOT, null);    // leave open for player's item
        if (mode == Mode.ANVIL) {
            inv.setItem(ANVIL_STONE_SLOT, null);  // leave open for stone/book
            inv.setItem(ANVIL_APPLY_SLOT, buildApplyButton(null, null));
            inv.setItem(ANVIL_HINT_SLOT, hintItem("&7Place your item in the &dleft slot&7,\nthen a reforge stone or upgrade book in the &dcenter slot&7."));
        }
        player.openInventory(inv);
        open.put(player.getUniqueId(), mode);
    }

    // ── Events ───────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Mode mode = open.get(p.getUniqueId());
        if (mode == null) return;

        int raw     = e.getRawSlot();
        int topSize = e.getView().getTopInventory().getSize();
        Inventory top = e.getView().getTopInventory();

        // ── Bottom inventory ──────────────────────────────────────────────────
        if (raw >= topSize) {
            if (!e.isShiftClick()) return;   // normal bottom-inv interaction — allow
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            // Shift-click fills TARGET_SLOT first, then ANVIL_STONE_SLOT (anvil only)
            if (isSlotEmpty(top, TARGET_SLOT)) {
                e.setCancelled(true);
                top.setItem(TARGET_SLOT, clicked.clone());
                clicked.setAmount(0);
            } else if (mode == Mode.ANVIL && isSlotEmpty(top, ANVIL_STONE_SLOT)) {
                e.setCancelled(true);
                top.setItem(ANVIL_STONE_SLOT, clicked.clone());
                clicked.setAmount(0);
            } else {
                return;   // both slots full — do nothing
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> refresh(p, mode));
            return;
        }

        // ── Top inventory ─────────────────────────────────────────────────────

        // Target slot — allow normal interaction, refresh after
        if (raw == TARGET_SLOT) {
            plugin.getServer().getScheduler().runTask(plugin, () -> refresh(p, mode));
            return;
        }

        // Anvil stone slot — allow normal interaction, refresh after
        if (mode == Mode.ANVIL && raw == ANVIL_STONE_SLOT) {
            plugin.getServer().getScheduler().runTask(plugin, () -> refresh(p, mode));
            return;
        }

        // Anvil apply button
        if (mode == Mode.ANVIL && raw == ANVIL_APPLY_SLOT) {
            e.setCancelled(true);
            tryApplyFromStone(p);
            return;
        }

        // Enchanting mode — enchant slots
        if (mode == Mode.ENCHANTING) {
            for (int i = 0; i < ENCHANT_SLOTS.length; i++) {
                if (raw == ENCHANT_SLOTS[i]) {
                    e.setCancelled(true);
                    tryApplyEnchant(p, i);
                    refresh(p, mode);
                    return;
                }
            }
        }

        // Everything else in top inv is locked
        e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        Mode mode = open.remove(p.getUniqueId());
        if (mode == null) return;
        // Return any items left in the input slots
        returnSlot(p, e.getInventory(), TARGET_SLOT);
        if (mode == Mode.ANVIL) returnSlot(p, e.getInventory(), ANVIL_STONE_SLOT);
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    private void refresh(Player p, Mode mode) {
        Inventory inv = p.getOpenInventory().getTopInventory();
        if (mode == Mode.ENCHANTING) {
            refreshEnchanting(p, inv);
        } else {
            refreshAnvil(p, inv);
        }
    }

    private void refreshEnchanting(Player p, Inventory inv) {
        // Clear all enchant slots first
        for (int slot : ENCHANT_SLOTS) inv.setItem(slot, paneItem());

        ItemStack target = inv.getItem(TARGET_SLOT);
        if (target == null || target.getType().isAir()) return;

        Optional<RpgItem> base = RpgServices.items().from(target);
        String itemType = base.map(b -> b.type().id()).orElse("");

        int slotIdx = 0;
        for (EnchantDef def : registry.allEnchants()) {
            if (slotIdx >= ENCHANT_SLOTS.length) break;
            if (!appliesTo(def.appliesTo(), itemType)) continue;
            int cur = modifier.enchants(target).getOrDefault(def.id(), 0);
            inv.setItem(ENCHANT_SLOTS[slotIdx], buildEnchantSlot(def, cur));
            slotIdx++;
        }
    }

    private void refreshAnvil(Player p, Inventory inv) {
        ItemStack target = inv.getItem(TARGET_SLOT);
        ItemStack stone  = inv.getItem(ANVIL_STONE_SLOT);
        inv.setItem(ANVIL_APPLY_SLOT, buildApplyButton(target, stone));
        // Update hint to reflect current state
        if (target == null || target.getType().isAir()) {
            inv.setItem(ANVIL_HINT_SLOT, hintItem("&7Place your item in the &dleft slot&7."));
        } else if (stone == null || stone.getType().isAir()) {
            inv.setItem(ANVIL_HINT_SLOT, hintItem("&7Place a reforge stone or upgrade book\nin the &dcenter slot&7."));
        } else {
            inv.setItem(ANVIL_HINT_SLOT, hintItem("&7Click &aApply&7 to confirm."));
        }
    }

    // ── Apply — enchanting (click-from-list) ──────────────────────────────────

    private void tryApplyEnchant(Player p, int slotIdx) {
        if (!p.hasPermission("rpg.enchanting.use.enchant")) {
            p.sendMessage(plugin.messages().get("no-permission")); return;
        }
        Inventory inv = p.getOpenInventory().getTopInventory();
        ItemStack target = inv.getItem(TARGET_SLOT);
        if (target == null || target.getType().isAir()) return;

        Optional<RpgItem> base = RpgServices.items().from(target);
        String itemType = base.map(b -> b.type().id()).orElse("");

        int j = 0;
        for (EnchantDef def : registry.allEnchants()) {
            if (!appliesTo(def.appliesTo(), itemType)) continue;
            if (j == slotIdx) {
                int cur = modifier.enchants(target).getOrDefault(def.id(), 0);
                if (cur >= def.maxLevel()) {
                    p.sendMessage(plugin.messages().get("enchant.at-max")); return;
                }
                if (!checkAndCharge(p, def.currencyCost(), def.requiredSkillLevel())) return;
                modifier.setEnchant(target, def.id(), cur + 1);
                modifier.rewriteLore(target, registry);
                modifier.rewriteName(target, registry);
                long xp = plugin.getConfig().getLong("xp.per-enchant", 25);
                if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.ENCHANTING.id(), xp);
                p.sendMessage(plugin.messages().get("enchant.applied"));
                return;
            }
            j++;
        }
    }

    // ── Apply — anvil (physical stone / book) ─────────────────────────────────

    private void tryApplyFromStone(Player p) {
        if (!p.hasPermission("rpg.enchanting.use.anvil")) {
            p.sendMessage(plugin.messages().get("no-permission")); return;
        }
        Inventory inv = p.getOpenInventory().getTopInventory();
        ItemStack target = inv.getItem(TARGET_SLOT);
        ItemStack stone  = inv.getItem(ANVIL_STONE_SLOT);

        if (target == null || target.getType().isAir()) return;
        if (stone == null || stone.getType().isAir()) {
            p.sendMessage(plugin.messages().get("stone.missing")); return;
        }

        Optional<String> reforgeId = modifier.reforgeIdFromStone(stone);
        if (reforgeId.isPresent()) {
            applyReforgeStone(p, inv, target, stone, reforgeId.get()); return;
        }
        Optional<String> upgradeId = modifier.upgradeIdFromBook(stone);
        if (upgradeId.isPresent()) {
            applyUpgradeBook(p, inv, target, stone, upgradeId.get()); return;
        }
        p.sendMessage(plugin.messages().get("stone.not-a-stone"));
    }

    private void applyReforgeStone(Player p, Inventory inv, ItemStack target,
                                   ItemStack stone, String reforgeId) {
        ReforgeDef def = registry.reforge(reforgeId).orElse(null);
        if (def == null) { p.sendMessage(plugin.messages().get("stone.unknown")); return; }

        Optional<RpgItem> base = RpgServices.items().from(target);
        String itemType = base.map(b -> b.type().id()).orElse("");
        if (!appliesTo(def.appliesTo(), itemType)) {
            p.sendMessage(plugin.messages().get("stone.incompatible")); return;
        }
        // Skill level check (currency is embedded in the stone cost — not charged here)
        if (!checkSkillLevel(p, def.requiredSkillLevel())) return;
        // Reagent check
        if (def.reagent() != null && !consumeReagent(p, def.reagent())) return;

        // Consume the stone
        stone.setAmount(stone.getAmount() - 1);

        modifier.setReforge(target, def.id());
        modifier.rewriteLore(target, registry);
        modifier.rewriteName(target, registry);
        long xp = plugin.getConfig().getLong("xp.per-reforge", 15);
        if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.ENCHANTING.id(), xp);
        p.sendMessage(plugin.messages().get("reforge.applied"));
        refreshAnvil(p, inv);
    }

    private void applyUpgradeBook(Player p, Inventory inv, ItemStack target,
                                  ItemStack stone, String upgradeId) {
        UpgradeDef def = registry.upgrade(upgradeId).orElse(null);
        if (def == null) { p.sendMessage(plugin.messages().get("stone.unknown")); return; }

        Optional<RpgItem> base = RpgServices.items().from(target);
        String itemType = base.map(b -> b.type().id()).orElse("");
        if (!appliesTo(def.appliesTo(), itemType)) {
            p.sendMessage(plugin.messages().get("stone.incompatible")); return;
        }
        int cur = modifier.upgrades(target).getOrDefault(def.id(), 0);
        if (cur >= def.maxTier()) {
            p.sendMessage(plugin.messages().get("upgrade.at-max")); return;
        }
        if (!checkSkillLevel(p, def.requiredSkillLevel())) return;
        if (def.reagent() != null && !consumeReagent(p, def.reagent())) return;

        // Consume the book
        stone.setAmount(stone.getAmount() - 1);

        modifier.addUpgradeTier(target, def.id(), def.maxTier());
        modifier.rewriteLore(target, registry);
        modifier.rewriteName(target, registry);
        long xp = plugin.getConfig().getLong("xp.per-upgrade", 40);
        if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.ENCHANTING.id(), xp);
        p.sendMessage(plugin.messages().get("upgrade.applied"));
        refreshAnvil(p, inv);
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    /** Enchant option slot — shows current level, next level, cost, and click hint. */
    private ItemStack buildEnchantSlot(EnchantDef def, int curLevel) {
        boolean maxed = curLevel >= def.maxLevel();
        Material mat = maxed ? Material.RED_DYE : Material.LIME_DYE;
        String lvlText = curLevel == 0 ? "" : " &7(" + roman(curLevel) + " → " + (maxed ? "&cMAX" : roman(curLevel + 1)) + "&7)";
        String name = (maxed ? "&8" : "&a") + def.displayName() + lvlText;

        List<Component> lore = new ArrayList<>();
        for (String desc : def.description()) {
            lore.add(ni(LEGACY.deserialize("  &8" + desc)));
        }
        if (!def.description().isEmpty()) lore.add(Component.empty());
        if (def.currencyCost() > 0) {
            lore.add(ni(LEGACY.deserialize("&7Cost: &e$" + (long) def.currencyCost())));
        }
        if (def.requiredSkillLevel() > 1) {
            lore.add(ni(LEGACY.deserialize("&7Requires Enchanting Lv &e" + def.requiredSkillLevel())));
        }
        lore.add(Component.empty());
        lore.add(ni(LEGACY.deserialize(maxed ? "&cAlready at max level" : "&8▶ &7Click to enchant")));
        return buildWithLore(mat, name, lore);
    }

    /**
     * Apply button for the anvil. Shows what will happen based on the current stone/target.
     * Null arguments produce a greyed-out "no item" placeholder.
     */
    private ItemStack buildApplyButton(ItemStack target, ItemStack stone) {
        if (target == null || target.getType().isAir()
                || stone == null || stone.getType().isAir()) {
            return simple(Material.GRAY_DYE, "&8Apply");
        }

        Optional<String> reforgeId = modifier.reforgeIdFromStone(stone);
        Optional<String> upgradeId = modifier.upgradeIdFromBook(stone);
        if (reforgeId.isEmpty() && upgradeId.isEmpty()) {
            return simpleWithHint(Material.BARRIER, "&cInvalid", "&cNot a reforge stone or upgrade book");
        }

        Optional<RpgItem> base = RpgServices.items().from(target);
        String itemType = base.map(b -> b.type().id()).orElse("");
        String rarityId  = base.map(b -> b.rarity() == null ? "common" : b.rarity().id()).orElse("common");

        if (reforgeId.isPresent()) {
            ReforgeDef def = registry.reforge(reforgeId.get()).orElse(null);
            if (def == null) return simple(Material.BARRIER, "&cUnknown reforge");
            if (!appliesTo(def.appliesTo(), itemType))
                return simpleWithHint(Material.RED_DYE, "&cIncompatible",
                        "&c" + def.displayName() + " can't be applied to this item type");
            // Build preview lore
            List<Component> lore = new ArrayList<>();
            Map<String, Double> stats = def.statsFor(rarityId);
            if (!stats.isEmpty()) {
                lore.add(ni(LEGACY.deserialize("&7Stat changes:")));
                for (Map.Entry<String, Double> e : stats.entrySet()) {
                    var stat = RpgServices.stats().get(e.getKey()).orElse(null);
                    if (stat == null) continue;
                    lore.add(ni(LEGACY.deserialize("  " + stat.colorCode() + stat.displayName()
                            + ": " + fmtVal(e.getValue(), stat.percent()))));
                }
            }
            if (def.reagent() != null) lore.add(ni(LEGACY.deserialize("&7Reagent: &f" + def.reagent())));
            if (def.requiredSkillLevel() > 1)
                lore.add(ni(LEGACY.deserialize("&7Requires Enchanting Lv &e" + def.requiredSkillLevel())));
            lore.add(Component.empty());
            lore.add(ni(LEGACY.deserialize("&8▶ &7Click to apply")));
            return buildWithLore(Material.LIME_DYE, "&aReforge: &d" + def.displayName(), lore);
        }

        // Upgrade book path
        UpgradeDef def = registry.upgrade(upgradeId.get()).orElse(null);
        if (def == null) return simple(Material.BARRIER, "&cUnknown upgrade");
        if (!appliesTo(def.appliesTo(), itemType))
            return simpleWithHint(Material.RED_DYE, "&cIncompatible",
                    "&c" + def.displayName() + " can't be applied to this item type");
        int cur = modifier.upgrades(target).getOrDefault(def.id(), 0);
        if (cur >= def.maxTier())
            return simpleWithHint(Material.RED_DYE, "&cMax tier reached",
                    "&c" + def.displayName() + " is already at max tier (" + def.maxTier() + ")");

        List<Component> lore = new ArrayList<>();
        if (!def.statsPerTier().isEmpty()) {
            lore.add(ni(LEGACY.deserialize("&7Stats this tier:")));
            for (Map.Entry<String, Double> e : def.statsPerTier().entrySet()) {
                var stat = RpgServices.stats().get(e.getKey()).orElse(null);
                if (stat == null) continue;
                lore.add(ni(LEGACY.deserialize("  " + stat.colorCode() + stat.displayName()
                        + ": " + fmtVal(e.getValue(), stat.percent()))));
            }
        }
        lore.add(ni(LEGACY.deserialize("&7Tier: &e" + cur + " &7→ &e" + (cur + 1) + " &8/ " + def.maxTier())));
        if (def.reagent() != null) lore.add(ni(LEGACY.deserialize("&7Reagent: &f" + def.reagent())));
        if (def.requiredSkillLevel() > 1)
            lore.add(ni(LEGACY.deserialize("&7Requires Enchanting Lv &e" + def.requiredSkillLevel())));
        lore.add(Component.empty());
        lore.add(ni(LEGACY.deserialize("&8▶ &7Click to apply")));
        return buildWithLore(Material.LIME_DYE, "&aUpgrade: &6" + def.displayName(), lore);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean checkAndCharge(Player p, double currencyCost, int requiredLevel) {
        if (!checkSkillLevel(p, requiredLevel)) return false;
        if (!plugin.getConfig().getBoolean("charge-currency", true) || currencyCost <= 0) return true;
        try {
            var economy = RpgServices.economy();
            java.math.BigDecimal cost = java.math.BigDecimal.valueOf(currencyCost);
            if (economy.balance(p).compareTo(cost) < 0) {
                p.sendMessage(plugin.messages().get("not-enough-currency")); return false;
            }
            if (!economy.withdraw(p, cost)) {
                p.sendMessage(plugin.messages().get("not-enough-currency")); return false;
            }
            return true;
        } catch (IllegalStateException ex) {
            return true; // economy not loaded — allow free
        }
    }

    private boolean checkSkillLevel(Player p, int required) {
        if (required <= 1) return true;
        int level = RpgServices.skills().level(p, BuiltinSkill.ENCHANTING.id());
        if (level < required) {
            p.sendMessage(plugin.messages().get("requires-level", Map.of("level", required)));
            return false;
        }
        return true;
    }

    /**
     * Finds and removes one stack of the given RPG item id from the player's inventory.
     * Sends a message and returns false if the item is not found.
     */
    private boolean consumeReagent(Player p, String reagentId) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            Optional<RpgItem> opt = RpgServices.items().from(item);
            if (opt.isPresent() && opt.get().id().equals(reagentId)) {
                item.setAmount(item.getAmount() - 1);
                return true;
            }
        }
        p.sendMessage(plugin.messages().get("stone.reagent-missing", Map.of("reagent", reagentId)));
        return false;
    }

    private static boolean appliesTo(List<String> list, String itemType) {
        if (list == null || list.isEmpty() || list.contains("any")) return true;
        return list.contains(itemType.toLowerCase());
    }

    private static void returnSlot(Player p, Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item == null || item.getType() == Material.AIR) return;
        HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(item);
        for (ItemStack s : overflow.values()) p.getWorld().dropItemNaturally(p.getLocation(), s);
    }

    private static boolean isSlotEmpty(Inventory inv, int slot) {
        ItemStack s = inv.getItem(slot);
        return s == null || s.getType().isAir();
    }

    private static ItemStack paneItem() {
        return RpgServices.guiConfig().backgroundItem();
    }

    private static ItemStack simple(Material mat, String legacyName) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(ni(LEGACY.deserialize(legacyName)));
            meta.lore(List.of());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack simpleWithHint(Material mat, String legacyName, String hint) {
        ItemStack stack = simple(mat, legacyName);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.lore(List.of(Component.empty(), ni(LEGACY.deserialize(hint))));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack buildWithLore(Material mat, String legacyName, List<Component> lore) {
        ItemStack stack = simple(mat, legacyName);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && !lore.isEmpty()) {
            meta.lore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack hintItem(String legacyText) {
        return simple(Material.NETHER_STAR, legacyText);
    }

    private static Component ni(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }

    private static String fmtVal(double v, boolean percent) {
        String num = (v == Math.floor(v) && !Double.isInfinite(v))
                ? Long.toString((long) v) : String.format("%.1f", v);
        return (v >= 0 ? "+" : "") + num + (percent ? "%" : "");
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> Integer.toString(n);
        };
    }
}
