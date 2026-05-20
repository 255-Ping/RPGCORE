package com.github._255_ping.rpg.enchanting;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.economy.Economy;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Two-mode GUI:
 * <ul>
 *   <li><b>Enchanting</b> (slot 11 = item, slot 13 = options panel, slot 15 = result)</li>
 *   <li><b>Anvil</b> (slot 11 = item, slots 19-25 = reforge options, slots 28-34 = upgrade options)</li>
 * </ul>
 * On close, any item still in slot 11 is returned to the player's inventory.
 */
public final class StationGui implements Listener {

    public enum Mode { ENCHANTING, ANVIL }

    private static final int INPUT_SLOT = 11;
    private static final int RESULT_SLOT = 15;

    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final EnchantRegistry registry;
    private final ItemModifier modifier;
    private final Map<UUID, Mode> open = new HashMap<>();

    public StationGui(org.bukkit.plugin.java.JavaPlugin plugin, EnchantRegistry registry, ItemModifier modifier) {
        this.plugin = plugin;
        this.registry = registry;
        this.modifier = modifier;
    }

    public void open(Player player, Mode mode) {
        String title = mode == Mode.ENCHANTING ? "Enchanting Table" : "Custom Anvil";
        Inventory inv = Bukkit.createInventory(player, 45, Component.text(title).color(NamedTextColor.DARK_PURPLE));
        // Border fill
        ItemStack pane = paneItem();
        for (int i = 0; i < inv.getSize(); i++) {
            if (i == INPUT_SLOT) continue;
            if (mode == Mode.ENCHANTING && i == RESULT_SLOT) continue;
            if (i == 22) continue; // info panel
            inv.setItem(i, pane);
        }
        inv.setItem(22, infoItem(mode));
        player.openInventory(inv);
        open.put(player.getUniqueId(), mode);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Mode mode = open.get(p.getUniqueId());
        if (mode == null) return;

        int raw = e.getRawSlot();
        if (raw >= e.getView().getTopInventory().getSize()) return; // bottom inv — let normal click

        // Slots that are interactive:
        if (raw == INPUT_SLOT) {
            // Allow placing/picking up an item there. After change, refresh result.
            plugin.getServer().getScheduler().runTask(plugin, () -> refresh(p, mode));
            return;
        }
        if (mode == Mode.ENCHANTING && raw == RESULT_SLOT) {
            // Confirm enchant (apply next-level of an admin-chosen enchant — for v1, pick the first
            // enchant whose AppliesTo matches the item type).
            e.setCancelled(true);
            tryApplyEnchant(p);
            refresh(p, mode);
            return;
        }
        if (mode == Mode.ANVIL && raw >= 19 && raw <= 25) {
            e.setCancelled(true);
            // pick reforge by index from allReforges()
            tryApplyReforge(p, raw - 19);
            refresh(p, mode);
            return;
        }
        if (mode == Mode.ANVIL && raw >= 28 && raw <= 34) {
            e.setCancelled(true);
            tryApplyUpgrade(p, raw - 28);
            refresh(p, mode);
            return;
        }
        // Everything else in top inv is locked.
        e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        Mode mode = open.remove(p.getUniqueId());
        if (mode == null) return;
        ItemStack remaining = e.getInventory().getItem(INPUT_SLOT);
        if (remaining != null && remaining.getType() != Material.AIR) {
            HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(remaining);
            for (ItemStack stack : overflow.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), stack);
            }
        }
    }

    private void refresh(Player p, Mode mode) {
        Inventory inv = p.getOpenInventory().getTopInventory();
        if (mode == Mode.ENCHANTING) {
            ItemStack target = inv.getItem(INPUT_SLOT);
            if (target == null || target.getType().isAir()) {
                inv.setItem(RESULT_SLOT, paneItem());
                return;
            }
            EnchantDef def = findApplicableEnchant(target);
            if (def == null) {
                inv.setItem(RESULT_SLOT, simple(Material.BARRIER, "&cNo enchant available"));
                return;
            }
            int curLevel = modifier.enchants(target).getOrDefault(def.id(), 0);
            int nextLevel = Math.min(def.maxLevel(), curLevel + 1);
            String label = "&aApply " + def.displayName() + " " + nextLevel + " &7(" + (long) def.currencyCost() + ")";
            inv.setItem(RESULT_SLOT, simple(Material.LIME_DYE, label));
        } else {
            // Anvil: populate reforge + upgrade option slots based on the input item.
            ItemStack target = inv.getItem(INPUT_SLOT);
            for (int i = 19; i <= 25; i++) inv.setItem(i, paneItem());
            for (int i = 28; i <= 34; i++) inv.setItem(i, paneItem());
            if (target == null || target.getType().isAir()) return;
            Optional<RpgItem> base = RpgServices.items().from(target);
            String itemType = base.map(b -> b.type().id()).orElse("");
            int rIdx = 0;
            for (ReforgeDef def : registry.allReforges()) {
                if (rIdx >= 7) break;
                if (!appliesTo(def.appliesTo(), itemType)) continue;
                inv.setItem(19 + rIdx, simple(Material.PINK_DYE,
                        "&dReforge: " + def.displayName() + " &7(" + (long) def.currencyCost() + ")"));
                rIdx++;
            }
            int uIdx = 0;
            for (UpgradeDef def : registry.allUpgrades()) {
                if (uIdx >= 7) break;
                if (!appliesTo(def.appliesTo(), itemType)) continue;
                inv.setItem(28 + uIdx, simple(Material.ORANGE_DYE,
                        "&6Upgrade: " + def.displayName() + " &7(" + (long) def.currencyCost() + ")"));
                uIdx++;
            }
        }
    }

    // ----- Apply paths -----

    private void tryApplyEnchant(Player p) {
        if (!p.hasPermission("rpg.enchanting.use.enchant")) {
            p.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
            return;
        }
        Inventory inv = p.getOpenInventory().getTopInventory();
        ItemStack target = inv.getItem(INPUT_SLOT);
        if (target == null || target.getType().isAir()) return;
        EnchantDef def = findApplicableEnchant(target);
        if (def == null) return;
        int curLevel = modifier.enchants(target).getOrDefault(def.id(), 0);
        if (curLevel >= def.maxLevel()) {
            p.sendMessage(Component.text("Enchant is already at max level.").color(NamedTextColor.YELLOW));
            return;
        }
        if (!checkAndCharge(p, def.currencyCost(), def.requiredSkillLevel())) return;

        modifier.setEnchant(target, def.id(), curLevel + 1);
        modifier.rewriteLore(target, registry);

        long xp = plugin.getConfig().getLong("xp.per-enchant", 25);
        if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.ENCHANTING.id(), xp);
        p.sendMessage(Component.text("Enchant applied.").color(NamedTextColor.GREEN));
    }

    private void tryApplyReforge(Player p, int idx) {
        if (!p.hasPermission("rpg.enchanting.use.anvil")) return;
        Inventory inv = p.getOpenInventory().getTopInventory();
        ItemStack target = inv.getItem(INPUT_SLOT);
        if (target == null || target.getType().isAir()) return;
        Optional<RpgItem> base = RpgServices.items().from(target);
        String itemType = base.map(b -> b.type().id()).orElse("");

        int j = 0;
        for (ReforgeDef def : registry.allReforges()) {
            if (!appliesTo(def.appliesTo(), itemType)) continue;
            if (j == idx) {
                if (!checkAndCharge(p, def.currencyCost(), def.requiredSkillLevel())) return;
                modifier.setReforge(target, def.id());
                modifier.rewriteLore(target, registry);
                long xp = plugin.getConfig().getLong("xp.per-reforge", 15);
                if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.ENCHANTING.id(), xp);
                p.sendMessage(Component.text("Reforge applied.").color(NamedTextColor.GREEN));
                return;
            }
            j++;
        }
    }

    private void tryApplyUpgrade(Player p, int idx) {
        if (!p.hasPermission("rpg.enchanting.use.anvil")) return;
        Inventory inv = p.getOpenInventory().getTopInventory();
        ItemStack target = inv.getItem(INPUT_SLOT);
        if (target == null || target.getType().isAir()) return;
        Optional<RpgItem> base = RpgServices.items().from(target);
        String itemType = base.map(b -> b.type().id()).orElse("");

        int j = 0;
        for (UpgradeDef def : registry.allUpgrades()) {
            if (!appliesTo(def.appliesTo(), itemType)) continue;
            if (j == idx) {
                int cur = modifier.upgrades(target).getOrDefault(def.id(), 0);
                if (cur >= def.maxTier()) {
                    p.sendMessage(Component.text("Already at max tier.").color(NamedTextColor.YELLOW));
                    return;
                }
                if (!checkAndCharge(p, def.currencyCost(), def.requiredSkillLevel())) return;
                modifier.addUpgradeTier(target, def.id(), def.maxTier());
                modifier.rewriteLore(target, registry);
                long xp = plugin.getConfig().getLong("xp.per-upgrade", 40);
                if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.ENCHANTING.id(), xp);
                p.sendMessage(Component.text("Upgrade applied.").color(NamedTextColor.GREEN));
                return;
            }
            j++;
        }
    }

    private boolean checkAndCharge(Player p, double currencyCost, int requiredLevel) {
        int skillLevel = RpgServices.skills().level(p, BuiltinSkill.ENCHANTING.id());
        if (skillLevel < requiredLevel) {
            p.sendMessage(Component.text("Requires Enchanting level " + requiredLevel + ".")
                    .color(NamedTextColor.YELLOW));
            return false;
        }
        if (!plugin.getConfig().getBoolean("charge-currency", true) || currencyCost <= 0) {
            return true;
        }
        try {
            Economy economy = RpgServices.economy();
            BigDecimal cost = BigDecimal.valueOf(currencyCost);
            if (economy.balance(p).compareTo(cost) < 0) {
                p.sendMessage(Component.text("Not enough currency.").color(NamedTextColor.RED));
                return false;
            }
            if (!economy.withdraw(p, cost)) {
                p.sendMessage(Component.text("Not enough currency.").color(NamedTextColor.RED));
                return false;
            }
            return true;
        } catch (IllegalStateException ex) {
            return true; // economy not present; allow free
        }
    }

    private EnchantDef findApplicableEnchant(ItemStack stack) {
        Optional<RpgItem> base = RpgServices.items().from(stack);
        String itemType = base.map(b -> b.type().id()).orElse("");
        for (EnchantDef def : registry.allEnchants()) {
            if (appliesTo(def.appliesTo(), itemType)) {
                int cur = modifier.enchants(stack).getOrDefault(def.id(), 0);
                if (cur < def.maxLevel()) return def;
            }
        }
        return null;
    }

    private static boolean appliesTo(java.util.List<String> list, String itemType) {
        if (list.isEmpty()) return true;
        if (list.contains("any")) return true;
        return list.contains(itemType.toLowerCase());
    }

    private static ItemStack paneItem() {
        ItemStack stack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack simple(Material mat, String legacyName) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacyAmpersand().deserialize(legacyName)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack infoItem(Mode mode) {
        String text = mode == Mode.ENCHANTING
                ? "&dPlace an item in the slot, then click the result to enchant."
                : "&dPlace an item, then click a reforge or upgrade option.";
        return simple(Material.NETHER_STAR, text);
    }
}
