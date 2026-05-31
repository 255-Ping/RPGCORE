package com.github._255_ping.rpg.npcs;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.economy.Economy;
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
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GUI for the BANKER NPC behavior.
 * Layout (27 slots, 3 rows):
 *   Row 0: balance info pane
 *   Row 1: deposit amounts  (100 | 1k | 10k | 100k | All)
 *   Row 2: withdraw amounts (100 | 1k | 10k | 100k | All)
 */
public final class BankerGui implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    /** How bank balances are persisted: key = "bank.<npcId>.<playerUuid>". */
    private static final String REPO = "npc_bank";

    private final JavaPlugin plugin;
    /** Players currently with the banker GUI open: uuid → npcId */
    private final Map<UUID, String> openBank = new ConcurrentHashMap<>();
    /** Players in chat-entry mode: uuid → (npcId, action "deposit"|"withdraw") */
    private final Map<UUID, ChatEntry> chatEntry = new ConcurrentHashMap<>();

    private record ChatEntry(String npcId, String action) {}

    public BankerGui(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player p, NpcDef def) {
        NpcDef.BankerData data = def.bankerData();
        String bankName = (data != null && data.bankName() != null) ? data.bankName() : def.displayName();
        BigDecimal savings = getBalance(p.getUniqueId(), def.id());
        BigDecimal wallet;
        try {
            wallet = RpgServices.economy().balance(p);
        } catch (IllegalStateException ex) {
            p.sendMessage(Component.text("Economy unavailable.").color(NamedTextColor.RED));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27,
            Component.text("⚑ " + stripColor(bankName)).color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));

        // Row 0: info pane (remaining slots filled by guiConfig below)
        setInfo(inv, 4, "&6Bank Balance: &e" + fmt(savings), "&7Wallet: &f" + fmt(wallet));

        // Row 1: deposit
        setAmount(inv, 9,  Material.LIME_STAINED_GLASS_PANE, "&aDeposit &f100",       "deposit", 100);
        setAmount(inv, 10, Material.LIME_STAINED_GLASS_PANE, "&aDeposit &f1,000",     "deposit", 1_000);
        setAmount(inv, 11, Material.LIME_STAINED_GLASS_PANE, "&aDeposit &f10,000",    "deposit", 10_000);
        setAmount(inv, 12, Material.LIME_STAINED_GLASS_PANE, "&aDeposit &f100,000",   "deposit", 100_000);
        setAction(inv, 13, Material.LIME_DYE,               "&aDeposit All",          "deposit_all");

        // Row 2: withdraw
        setAmount(inv, 18, Material.RED_STAINED_GLASS_PANE, "&cWithdraw &f100",       "withdraw", 100);
        setAmount(inv, 19, Material.RED_STAINED_GLASS_PANE, "&cWithdraw &f1,000",     "withdraw", 1_000);
        setAmount(inv, 20, Material.RED_STAINED_GLASS_PANE, "&cWithdraw &f10,000",    "withdraw", 10_000);
        setAmount(inv, 21, Material.RED_STAINED_GLASS_PANE, "&cWithdraw &f100,000",   "withdraw", 100_000);
        setAction(inv, 22, Material.RED_DYE,                "&cWithdraw All",          "withdraw_all");

        // Fill all remaining empty slots with the configured background pane
        RpgServices.guiConfig().fillBackground(inv);

        p.openInventory(inv);
        openBank.put(p.getUniqueId(), def.id());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String npcId = openBank.get(p.getUniqueId());
        if (npcId == null) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        String action = getAction(e.getCurrentItem());
        if (action == null) return;

        BigDecimal savings = getBalance(p.getUniqueId(), npcId);
        BigDecimal wallet;
        try {
            wallet = RpgServices.economy().balance(p);
        } catch (IllegalStateException ex) {
            return;
        }

        switch (action) {
            case "deposit_all" -> transact(p, npcId, "deposit", wallet);
            case "withdraw_all" -> transact(p, npcId, "withdraw", savings);
            default -> {
                if (action.startsWith("deposit:") || action.startsWith("withdraw:")) {
                    String[] parts = action.split(":");
                    transact(p, npcId, parts[0], new BigDecimal(parts[1]));
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) openBank.remove(p.getUniqueId());
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent e) {
        ChatEntry entry = chatEntry.remove(e.getPlayer().getUniqueId());
        if (entry == null) return;
        e.setCancelled(true);
        String input = e.getMessage().trim();
        try {
            BigDecimal amount = new BigDecimal(input.replace(",", ""));
            Player p = e.getPlayer();
            plugin.getServer().getScheduler().runTask(plugin, () -> transact(p, entry.npcId(), entry.action(), amount));
        } catch (NumberFormatException ignored) {
            e.getPlayer().sendMessage(Component.text("Invalid amount.").color(NamedTextColor.RED));
        }
    }

    private void transact(Player p, String npcId, String action, BigDecimal amount) {
        amount = amount.setScale(2, RoundingMode.FLOOR);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            p.sendMessage(Component.text("Amount must be positive.").color(NamedTextColor.RED));
            return;
        }
        Economy eco;
        try { eco = RpgServices.economy(); } catch (IllegalStateException ex) { return; }

        BigDecimal savings = getBalance(p.getUniqueId(), npcId);
        if ("deposit".equals(action)) {
            BigDecimal wallet = eco.balance(p);
            BigDecimal actual = amount.min(wallet);
            if (actual.compareTo(BigDecimal.ZERO) <= 0) {
                p.sendMessage(Component.text("Not enough coins.").color(NamedTextColor.RED));
                return;
            }
            eco.withdraw(p, actual);
            setBalance(p.getUniqueId(), npcId, savings.add(actual));
            p.sendMessage(Component.text("Deposited " + fmt(actual) + ". Savings: " + fmt(savings.add(actual))).color(NamedTextColor.GREEN));
        } else {
            BigDecimal actual = amount.min(savings);
            if (actual.compareTo(BigDecimal.ZERO) <= 0) {
                p.sendMessage(Component.text("No savings to withdraw.").color(NamedTextColor.RED));
                return;
            }
            eco.deposit(p, actual);
            setBalance(p.getUniqueId(), npcId, savings.subtract(actual));
            p.sendMessage(Component.text("Withdrew " + fmt(actual) + ". Savings: " + fmt(savings.subtract(actual))).color(NamedTextColor.GREEN));
        }
    }

    /** Accrue daily interest for a given banker NPC on all accounts. Called by plugin scheduler. */
    public void accrueInterest(String npcId, double dailyInterestPercent) {
        if (dailyInterestPercent <= 0) return;
        double rate = dailyInterestPercent / 100.0;
        var repo = RpgServices.dataStore().repository(REPO);
        String prefix = npcId + ":";
        for (String key : repo.keys()) {
            if (!key.startsWith(prefix)) continue;
            repo.get(key).ifPresent(data -> {
                double current = ((Number) data.getOrDefault("balance", 0.0)).doubleValue();
                if (current <= 0) return;
                double interest = current * rate;
                data.put("balance", current + interest);
                repo.save(key, data);
            });
        }
    }

    // ---- persistence helpers ----

    private BigDecimal getBalance(UUID player, String npcId) {
        try {
            return RpgServices.dataStore().repository(REPO)
                .get(npcId + ":" + player)
                .map(d -> BigDecimal.valueOf(((Number) d.getOrDefault("balance", 0.0)).doubleValue()))
                .orElse(BigDecimal.ZERO);
        } catch (IllegalStateException ex) {
            return BigDecimal.ZERO;
        }
    }

    private void setBalance(UUID player, String npcId, BigDecimal balance) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("balance", balance.doubleValue());
            RpgServices.dataStore().repository(REPO).save(npcId + ":" + player, data);
        } catch (IllegalStateException ignored) {}
    }

    // ---- inventory helpers ----

    private void setInfo(Inventory inv, int slot, String... lines) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(lines[0]).decoration(TextDecoration.ITALIC, false));
        if (lines.length > 1) {
            meta.lore(List.of(LEGACY.deserialize(lines[1]).decoration(TextDecoration.ITALIC, false)));
        }
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private void setAmount(Inventory inv, int slot, Material mat, String label, String actionType, double amount) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(label).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                LEGACY.deserialize("&8▶ &7Left-click to " + actionType)
                        .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey("rpg", "banker_action"),
            org.bukkit.persistence.PersistentDataType.STRING,
            actionType + ":" + (long) amount);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private void setAction(Inventory inv, int slot, Material mat, String label, String action) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(label).decoration(TextDecoration.ITALIC, false));
        String verb = action.startsWith("deposit") ? "deposit all" : "withdraw all";
        meta.lore(List.of(
                Component.empty(),
                LEGACY.deserialize("&8▶ &7Left-click to " + verb)
                        .decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey("rpg", "banker_action"),
            org.bukkit.persistence.PersistentDataType.STRING,
            action);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private String getAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        var key = new org.bukkit.NamespacedKey("rpg", "banker_action");
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
    }

    private static String fmt(BigDecimal val) {
        long v = val.longValue();
        if (v >= 1_000_000_000) return String.format("%.1fB", v / 1_000_000_000.0);
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000) return String.format("%.1fK", v / 1_000.0);
        return String.valueOf(v);
    }

    private static String stripColor(String s) {
        return s.replaceAll("&[0-9a-fk-or]", "");
    }
}
