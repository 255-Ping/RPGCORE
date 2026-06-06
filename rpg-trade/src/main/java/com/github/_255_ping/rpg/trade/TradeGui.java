package com.github._255_ping.rpg.trade;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.economy.Economy;
import com.github._255_ping.rpg.api.gui.GuiConfig;
import com.github._255_ping.rpg.api.items.RpgItem;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the trade inventory GUI for both players.
 *
 * <p>Layout (54 slots, 6 rows):
 * <pre>
 * Row 0:  [ Your Offer label ][ panes ][ divider ][ panes ][ Their Offer label ]
 * Row 1:  [ border ][ Y0 ][ Y1 ][ Y2 ][ divider ][ T0 ][ T1 ][ T2 ][ border ]
 * Row 2:  [ border ][ Y3 ][ Y4 ][ Y5 ][ divider ][ T3 ][ T4 ][ T5 ][ border ]
 * Row 3:  [ border ][ Y6 ][ Y7 ][ Y8 ][ divider ][ T6 ][ T7 ][ T8 ][ border ]
 * Row 4:  [ pane ][ COINS btn ][ panes ][ CONFIRM ][ panes ][ THEIR COINS ][ pane ]
 * Row 5:  [ black border row ]
 * </pre>
 *
 * <p>Each player has their own independent {@link Inventory} object.
 * Clicking your own item slots (left 3×3) interactively adds/removes items.
 * The other player's slots (right 3×3) are display-only.
 */
public final class TradeGui implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    // ── Slot constants ───────────────────────────────────────────────────────

    /** Your 3×3 offer slots (rows 1–3, cols 1–3). Indices map to session.mySlots(). */
    static final int[] OWN_SLOTS   = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    /** Their 3×3 display slots (rows 1–3, cols 5–7). */
    static final int[] THEIR_SLOTS = {14, 15, 16, 23, 24, 25, 32, 33, 34};

    private static final Set<Integer> OWN_SET   = Set.of(10,11,12,19,20,21,28,29,30);
    private static final Set<Integer> THEIR_SET = Set.of(14,15,16,23,24,25,32,33,34);
    private static final Set<Integer> DIVIDERS  = Set.of(4,13,22,31);

    private static final int COIN_SLOT       = 37;
    private static final int CONFIRM_SLOT    = 40;
    private static final int THEIR_COIN_SLOT = 43;
    private static final int CANCEL_SLOT     = GuiConfig.CLOSE_SLOT; // 49

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final RpgTradePlugin plugin;
    private final TradeManager manager;
    private final Messages messages;

    public TradeGui(RpgTradePlugin plugin, TradeManager manager, Messages messages) {
        this.plugin   = plugin;
        this.manager  = manager;
        this.messages = messages;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /** Create both inventory objects and open the GUI for both players. */
    public void open(TradeSession session) {
        Player a = Bukkit.getPlayer(session.uuidA);
        Player b = Bukkit.getPlayer(session.uuidB);
        if (a == null || b == null) return;

        String titleA = "&a&lTrade &7with &e" + session.nameB;
        String titleB = "&a&lTrade &7with &e" + session.nameA;

        session.invA = Bukkit.createInventory(a, 54, LEGACY.deserialize(titleA)
                .decoration(TextDecoration.ITALIC, false));
        session.invB = Bukkit.createInventory(b, 54, LEGACY.deserialize(titleB)
                .decoration(TextDecoration.ITALIC, false));

        populate(session.invA, session, session.uuidA);
        populate(session.invB, session, session.uuidB);

        a.openInventory(session.invA);
        b.openInventory(session.invB);
    }

    // ── Event handlers ───────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (!manager.inCoinMode(uuid)) return;
        e.setCancelled(true);
        manager.exitCoinMode(uuid);

        String input = e.getMessage().trim();
        if (input.equalsIgnoreCase("cancel")) return;

        TradeSession session = manager.getSession(uuid);
        if (session == null || session.state == TradeSession.State.DONE) return;

        long amount;
        try {
            amount = Long.parseLong(input.replace(",", "").replace("_", ""));
        } catch (NumberFormatException ex) {
            e.getPlayer().sendMessage(messages.get("coins-invalid"));
            return;
        }
        if (amount < 0) {
            e.getPlayer().sendMessage(messages.get("coins-negative"));
            return;
        }

        long max = plugin.getConfig().getLong("trade.max-coins", 0);
        if (max > 0 && amount > max) {
            e.getPlayer().sendMessage(messages.get("coins-exceed-max", Map.of("max", max)));
            return;
        }

        final long finalAmount = amount;
        Bukkit.getScheduler().runTask(plugin, () -> {
            TradeSession s = manager.getSession(uuid);
            if (s == null || s.state == TradeSession.State.DONE) return;
            s.setMyCoins(uuid, finalAmount);
            resetConfirm(s, uuid);
            refresh(s);
            e.getPlayer().sendMessage(messages.get("coins-set", Map.of("amount", finalAmount)));
        });
    }

    @SuppressWarnings("deprecation") // setCursor is deprecated in Paper 26 but still works correctly for custom GUIs
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        TradeSession session = manager.getSession(p.getUniqueId());
        if (session == null || session.state == TradeSession.State.DONE) return;

        // Make sure this is the player's trade inventory (not some other GUI)
        Inventory topInv = e.getView().getTopInventory();
        if (!topInv.equals(session.myInv(p.getUniqueId()))) return;

        e.setCancelled(true);

        int raw = e.getRawSlot();
        int topSize = topInv.getSize();
        ClickType click = e.getClick();

        // ── Bottom inventory: shift-click routes into own item slots ────────
        if (raw >= topSize) {
            if (!e.isShiftClick()) return;
            ItemStack item = e.getCurrentItem();
            if (item == null || item.getType().isAir()) return;
            if (!isTradeable(item)) {
                p.sendMessage(messages.get("not-tradeable"));
                return;
            }
            ItemStack[] mySlots = session.mySlots(p.getUniqueId());
            for (int i = 0; i < OWN_SLOTS.length; i++) {
                if (mySlots[i] == null || mySlots[i].getType().isAir()) {
                    mySlots[i] = item.clone();
                    item.setAmount(0);
                    resetConfirm(session, p.getUniqueId());
                    refresh(session);
                    return;
                }
            }
            return; // no free slot
        }

        // ── Top inventory clicks ─────────────────────────────────────────────

        // Cancel / close button — equivalent to closing the inventory (triggers cancelAndReturn via onClose).
        if (raw == CANCEL_SLOT) {
            p.closeInventory();
            return;
        }

        // Confirm button
        if (raw == CONFIRM_SLOT) {
            handleConfirm(session, p);
            return;
        }

        // Coin button
        if (raw == COIN_SLOT) {
            try { RpgServices.economy(); } catch (IllegalStateException ex) {
                p.sendMessage(messages.get("coins-no-economy"));
                return;
            }
            manager.enterCoinMode(p.getUniqueId());
            p.sendMessage(messages.get("coins-prompt"));
            return;
        }

        // Their display slots: read-only
        if (THEIR_SET.contains(raw)) return;

        // Everything else in the top (dividers, borders, headers, their coin display): locked
        if (!OWN_SET.contains(raw)) return;

        // ── Own item slot interaction ────────────────────────────────────────
        int idx = ownSlotIndex(raw);
        if (idx < 0) return;

        ItemStack[] mySlots = session.mySlots(p.getUniqueId());
        ItemStack cursor = (e.getCursor() == null || e.getCursor().getType().isAir())
                ? null : e.getCursor().clone();
        ItemStack inSlot = (mySlots[idx] == null || mySlots[idx].getType().isAir())
                ? null : mySlots[idx].clone();

        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            // Return slot item to player inventory
            if (inSlot != null) {
                Map<Integer, ItemStack> overflow = p.getInventory().addItem(inSlot);
                for (ItemStack drop : overflow.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), drop);
                }
                mySlots[idx] = null;
                resetConfirm(session, p.getUniqueId());
                refresh(session);
            }
            return;
        }

        if (click == ClickType.LEFT) {
            if (cursor != null && !isTradeable(cursor)) {
                p.sendMessage(messages.get("not-tradeable"));
                return;
            }
            if (cursor != null && inSlot != null && cursor.isSimilar(inSlot)) {
                // Stack
                int max = inSlot.getMaxStackSize();
                int total = inSlot.getAmount() + cursor.getAmount();
                if (total <= max) {
                    inSlot.setAmount(total);
                    mySlots[idx] = inSlot;
                    e.setCursor(null);
                } else {
                    inSlot.setAmount(max);
                    mySlots[idx] = inSlot;
                    cursor.setAmount(total - max);
                    e.setCursor(cursor);
                }
            } else {
                // Swap cursor ↔ slot
                mySlots[idx] = cursor;
                e.setCursor(inSlot);
            }
            resetConfirm(session, p.getUniqueId());
            refresh(session);
            return;
        }

        if (click == ClickType.RIGHT) {
            if (cursor != null) {
                // Place one from cursor into slot (if compatible)
                if (!isTradeable(cursor)) {
                    p.sendMessage(messages.get("not-tradeable"));
                    return;
                }
                if (inSlot == null) {
                    ItemStack placed = cursor.clone();
                    placed.setAmount(1);
                    mySlots[idx] = placed;
                    if (cursor.getAmount() > 1) {
                        cursor.setAmount(cursor.getAmount() - 1);
                        e.setCursor(cursor);
                    } else {
                        e.setCursor(null);
                    }
                    resetConfirm(session, p.getUniqueId());
                    refresh(session);
                } else if (inSlot.isSimilar(cursor) && inSlot.getAmount() < inSlot.getMaxStackSize()) {
                    inSlot.setAmount(inSlot.getAmount() + 1);
                    mySlots[idx] = inSlot;
                    if (cursor.getAmount() > 1) {
                        cursor.setAmount(cursor.getAmount() - 1);
                        e.setCursor(cursor);
                    } else {
                        e.setCursor(null);
                    }
                    resetConfirm(session, p.getUniqueId());
                    refresh(session);
                }
            } else if (inSlot != null) {
                // Pick up half
                int half = (int) Math.ceil(inSlot.getAmount() / 2.0);
                ItemStack picked = inSlot.clone();
                picked.setAmount(half);
                e.setCursor(picked);
                inSlot.setAmount(inSlot.getAmount() - half);
                mySlots[idx] = inSlot.getAmount() <= 0 ? null : inSlot;
                resetConfirm(session, p.getUniqueId());
                refresh(session);
            }
        }
        // All other click types: blocked (already cancelled)
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        TradeSession session = manager.getSession(p.getUniqueId());
        if (session == null) return;
        if (session.state == TradeSession.State.DONE) {
            manager.removeSession(p.getUniqueId());
            manager.exitCoinMode(p.getUniqueId());
            return;
        }
        Player other = Bukkit.getPlayer(session.otherUuid(p.getUniqueId()));
        cancelAndReturn(session, p, other);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        TradeSession session = manager.getSession(p.getUniqueId());
        if (session == null) return;
        if (session.state == TradeSession.State.DONE) return;
        Player other = Bukkit.getPlayer(session.otherUuid(p.getUniqueId()));
        cancelAndReturn(session, p, other);
    }

    // ── Confirm / countdown logic ────────────────────────────────────────────

    private void handleConfirm(TradeSession session, Player clicker) {
        UUID uuid = clicker.getUniqueId();

        if (session.state == TradeSession.State.COUNTDOWN) {
            // Click during countdown → cancel
            Player other = Bukkit.getPlayer(session.otherUuid(uuid));
            cancelAndReturn(session, clicker, other);
            return;
        }

        switch (session.state) {
            case OPEN -> session.state = session.isA(uuid)
                    ? TradeSession.State.CONFIRMED_A
                    : TradeSession.State.CONFIRMED_B;
            case CONFIRMED_A -> {
                if (!session.isA(uuid)) startCountdown(session); // B confirms
                // If A clicks again, no-op (already confirmed)
            }
            case CONFIRMED_B -> {
                if (session.isA(uuid)) startCountdown(session); // A confirms
            }
            default -> { /* DONE — ignore */ }
        }
        refresh(session);
    }

    private void startCountdown(TradeSession session) {
        int seconds = plugin.getConfig().getInt("trade.countdown-seconds", 5);
        session.countdownLeft = seconds;
        session.state = TradeSession.State.COUNTDOWN;
        refresh(session);

        session.countdownTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            session.countdownLeft--;
            if (session.countdownLeft <= 0) {
                Bukkit.getScheduler().cancelTask(session.countdownTaskId);
                session.countdownTaskId = -1;
                Player a = Bukkit.getPlayer(session.uuidA);
                Player b = Bukkit.getPlayer(session.uuidB);
                if (a == null || b == null) {
                    cancelAndReturn(session, a, b);
                } else {
                    executeSwap(session, a, b);
                }
            } else {
                refresh(session);
            }
        }, 20L, 20L).getTaskId();
    }

    // ── Trade execution ──────────────────────────────────────────────────────

    private void executeSwap(TradeSession session, Player a, Player b) {
        session.state = TradeSession.State.DONE;
        cancelCountdown(session);
        manager.removeSession(session.uuidA);
        manager.removeSession(session.uuidB);
        manager.exitCoinMode(session.uuidA);
        manager.exitCoinMode(session.uuidB);

        // Coin transfer
        if (session.coinsA > 0 || session.coinsB > 0) {
            Economy eco;
            try { eco = RpgServices.economy(); } catch (IllegalStateException ex) {
                // No economy — return everything and cancel
                giveItems(a, session.slotsA);
                giveItems(b, session.slotsB);
                a.closeInventory(); b.closeInventory();
                a.sendMessage(messages.get("trade-failed-coins"));
                b.sendMessage(messages.get("trade-failed-coins"));
                return;
            }
            boolean okA = session.coinsA <= 0 || eco.withdraw(a, BigDecimal.valueOf(session.coinsA));
            if (!okA) {
                giveItems(a, session.slotsA); giveItems(b, session.slotsB);
                a.closeInventory(); b.closeInventory();
                a.sendMessage(messages.get("trade-failed-coins"));
                b.sendMessage(messages.get("trade-failed-coins"));
                return;
            }
            boolean okB = session.coinsB <= 0 || eco.withdraw(b, BigDecimal.valueOf(session.coinsB));
            if (!okB) {
                if (session.coinsA > 0) eco.deposit(a, BigDecimal.valueOf(session.coinsA));
                giveItems(a, session.slotsA); giveItems(b, session.slotsB);
                a.closeInventory(); b.closeInventory();
                a.sendMessage(messages.get("trade-failed-coins"));
                b.sendMessage(messages.get("trade-failed-coins"));
                return;
            }
            if (session.coinsA > 0) eco.deposit(b, BigDecimal.valueOf(session.coinsA));
            if (session.coinsB > 0) eco.deposit(a, BigDecimal.valueOf(session.coinsB));
        }

        // Item swap: A gets B's items, B gets A's items
        giveItems(a, session.slotsB);
        giveItems(b, session.slotsA);

        a.closeInventory();
        b.closeInventory();
        a.sendMessage(messages.get("trade-complete"));
        b.sendMessage(messages.get("trade-complete"));
    }

    void cancelAndReturn(TradeSession session, Player closer, Player other) {
        if (session == null || session.state == TradeSession.State.DONE) return;
        session.state = TradeSession.State.DONE;
        cancelCountdown(session);
        manager.removeSession(session.uuidA);
        manager.removeSession(session.uuidB);
        manager.exitCoinMode(session.uuidA);
        manager.exitCoinMode(session.uuidB);

        // Return items to their owners
        Player a = Bukkit.getPlayer(session.uuidA);
        Player b = Bukkit.getPlayer(session.uuidB);
        if (a != null) giveItems(a, session.slotsA);
        if (b != null) giveItems(b, session.slotsB);

        // Close the other player's GUI and notify
        if (other != null && other.isOnline()) {
            other.closeInventory();
            if (closer != null) {
                other.sendMessage(messages.get("trade-cancelled-by-other",
                        Map.of("player", closer.getName())));
            } else {
                other.sendMessage(messages.get("trade-cancelled"));
            }
        }
        if (closer != null) {
            closer.sendMessage(messages.get("trade-cancelled"));
        }
    }

    // ── GUI rendering ────────────────────────────────────────────────────────

    /** Full initial populate — run once when opening the GUI. */
    private void populate(Inventory inv, TradeSession session, UUID viewer) {
        GuiConfig gui = RpgServices.guiConfig();
        gui.fillAll(inv);
        gui.placeNavBar(inv); // border bottom row + ❌ Cancel at slot 49

        // Dividers (col 4)
        ItemStack div = divider();
        for (int d : DIVIDERS) inv.setItem(d, div);

        // Header labels (row 0)
        inv.setItem(1, headerLabel("&a&lYour Offer"));
        inv.setItem(6, headerLabel("&e&l" + session.theirName(viewer) + "'s Offer"));

        // Your item slots start as empty (null = air, player can fill them)
        for (int s : OWN_SLOTS) inv.setItem(s, null);

        // Dynamic content
        refreshFor(inv, session, viewer);
    }

    /** Update only the dynamic slots (items, coins, confirm) — both players. */
    private void refresh(TradeSession session) {
        Player a = Bukkit.getPlayer(session.uuidA);
        Player b = Bukkit.getPlayer(session.uuidB);
        if (a != null && session.invA != null) refreshFor(session.invA, session, session.uuidA);
        if (b != null && session.invB != null) refreshFor(session.invB, session, session.uuidB);
    }

    private void refreshFor(Inventory inv, TradeSession session, UUID viewer) {
        // Your items
        ItemStack[] myItems = session.mySlots(viewer);
        for (int i = 0; i < OWN_SLOTS.length; i++) {
            inv.setItem(OWN_SLOTS[i], myItems[i]);
        }
        // Their items (display)
        ItemStack[] theirItems = session.theirSlots(viewer);
        for (int i = 0; i < THEIR_SLOTS.length; i++) {
            inv.setItem(THEIR_SLOTS[i], theirItems[i]);
        }
        // Coins
        inv.setItem(COIN_SLOT, coinButton(session.myCoins(viewer), manager.inCoinMode(viewer)));
        inv.setItem(THEIR_COIN_SLOT, coinDisplay(session.theirCoins(viewer), session.theirName(viewer)));
        // Confirm
        inv.setItem(CONFIRM_SLOT, confirmButton(session, viewer));
    }

    // ── Item builders ────────────────────────────────────────────────────────

    private static ItemStack headerLabel(String legacyText) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize(legacyText).decoration(TextDecoration.ITALIC, false));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack divider() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack coinButton(long coins, boolean inputMode) {
        Material mat = inputMode ? Material.YELLOW_DYE : Material.SUNFLOWER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = inputMode ? "&eTyping amount..." : "&6Offer Coins: &e" + coins;
            meta.displayName(LEGACY.deserialize(name).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.empty(),
                    LEGACY.deserialize("&8▶ &7Left-click to set coin amount")
                            .decoration(TextDecoration.ITALIC, false)));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack coinDisplay(long coins, String otherName) {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize("&6" + otherName + " offers: &e" + coins + " coins")
                    .decoration(TextDecoration.ITALIC, false));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack confirmButton(TradeSession session, UUID viewer) {
        return switch (session.state) {
            case COUNTDOWN -> {
                ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(LEGACY.deserialize(
                            "&c✗ Cancel (&e" + session.countdownLeft + "s&c)")
                            .decoration(TextDecoration.ITALIC, false));
                    meta.lore(List.of(
                            Component.empty(),
                            LEGACY.deserialize("&8▶ &7Left-click to cancel the trade")
                                    .decoration(TextDecoration.ITALIC, false)));
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                    item.setItemMeta(meta);
                }
                yield item;
            }
            default -> {
                boolean confirmed = session.hasConfirmed(viewer);
                Material mat = confirmed
                        ? Material.YELLOW_STAINED_GLASS_PANE
                        : Material.GREEN_STAINED_GLASS_PANE;
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    String label = confirmed ? "&e⌛ Waiting for other player..." : "&a✔ Confirm Trade";
                    meta.displayName(LEGACY.deserialize(label).decoration(TextDecoration.ITALIC, false));
                    if (!confirmed) {
                        meta.lore(List.of(
                                Component.empty(),
                                LEGACY.deserialize("&8▶ &7Left-click to confirm")
                                        .decoration(TextDecoration.ITALIC, false)));
                    }
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                    item.setItemMeta(meta);
                }
                yield item;
            }
        };
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static boolean isTradeable(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return true;
        Optional<RpgItem> item = RpgServices.items().from(stack);
        return item.map(RpgItem::tradeable).orElse(true);
    }

    /** Reset the confirmation state when a player modifies their offer. */
    private static void resetConfirm(TradeSession session, UUID who) {
        switch (session.state) {
            case COUNTDOWN -> {
                // Cancel countdown — handled by the caller if needed
                if (session.countdownTaskId != -1) {
                    Bukkit.getScheduler().cancelTask(session.countdownTaskId);
                    session.countdownTaskId = -1;
                }
                session.state = TradeSession.State.OPEN;
            }
            case CONFIRMED_A -> { if (session.isA(who)) session.state = TradeSession.State.OPEN; }
            case CONFIRMED_B -> { if (!session.isA(who)) session.state = TradeSession.State.OPEN; }
            default -> { /* OPEN or DONE — nothing to reset */ }
        }
    }

    private static void cancelCountdown(TradeSession session) {
        if (session.countdownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(session.countdownTaskId);
            session.countdownTaskId = -1;
        }
    }

    private static void giveItems(Player p, ItemStack[] items) {
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;
            Map<Integer, ItemStack> overflow = p.getInventory().addItem(item.clone());
            for (ItemStack drop : overflow.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), drop);
            }
        }
    }

    private static int ownSlotIndex(int rawSlot) {
        for (int i = 0; i < OWN_SLOTS.length; i++) {
            if (OWN_SLOTS[i] == rawSlot) return i;
        }
        return -1;
    }
}
