package com.github._255_ping.rpg.npcs;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.economy.Economy;
import com.github._255_ping.rpg.api.items.RpgItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class NpcInteractListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private final NpcManager manager;
    private final Map<UUID, String> openShop = new HashMap<>();

    public NpcInteractListener(NpcManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent e) {
        Optional<NpcDef> opt = manager.fromEntity(e.getRightClicked());
        if (opt.isEmpty()) return;
        // Always cancel the vanilla interaction for NPC entities — prevents villager trade GUI.
        e.setCancelled(true);
        if (!e.getPlayer().hasPermission("rpg.npcs.use")) return;
        NpcDef def = opt.get();
        switch (def.behaviorType()) {
            case DIALOGUE -> sendDialogue(e.getPlayer(), def);
            case SHOP -> openShop(e.getPlayer(), def);
            case QUEST -> handoffQuest(e.getPlayer(), def);
        }
    }

    private void sendDialogue(Player p, NpcDef def) {
        for (String line : def.dialogueLines()) {
            p.sendMessage(LEGACY.deserialize(line));
        }
    }

    private void openShop(Player p, NpcDef def) {
        Inventory inv = Bukkit.createInventory(p, 27, Component.text(def.displayName())
                .color(NamedTextColor.GOLD));
        int i = 0;
        for (NpcDef.ShopEntry entry : def.shopItems()) {
            if (i >= 27) break;
            Optional<RpgItem> item = RpgServices.items().get(entry.itemId());
            ItemStack stack = item.map(RpgItem::toItemStack).orElseGet(() -> {
                Material mat = Material.matchMaterial(entry.itemId());
                return mat == null ? new ItemStack(Material.BARRIER) : new ItemStack(mat);
            });
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                java.util.List<Component> lore = meta.lore() != null ? new java.util.ArrayList<>(meta.lore()) : new java.util.ArrayList<>();
                lore.add(Component.empty());
                if (entry.buy() > 0) lore.add(Component.text("Buy: " + (long) entry.buy()).color(NamedTextColor.GREEN));
                if (entry.sell() > 0) lore.add(Component.text("Sell (shift+click): " + (long) entry.sell()).color(NamedTextColor.YELLOW));
                meta.lore(lore);
                stack.setItemMeta(meta);
            }
            inv.setItem(i, stack);
            i++;
        }
        p.openInventory(inv);
        openShop.put(p.getUniqueId(), def.id());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onShopClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String npcId = openShop.get(p.getUniqueId());
        if (npcId == null) return;
        if (e.getRawSlot() >= e.getView().getTopInventory().getSize()) return; // bottom
        e.setCancelled(true);
        Optional<NpcDef> opt = manager.get(npcId);
        if (opt.isEmpty()) return;
        NpcDef def = opt.get();
        if (e.getRawSlot() >= def.shopItems().size()) return;
        NpcDef.ShopEntry entry = def.shopItems().get(e.getRawSlot());
        try {
            Economy economy = RpgServices.economy();
            if (e.isShiftClick()) {
                // sell-back path — check whether the player has the item.
                Optional<RpgItem> rpg = RpgServices.items().get(entry.itemId());
                Material mat = rpg.isPresent() ? rpg.get().material() : Material.matchMaterial(entry.itemId());
                if (mat == null) return;
                if (!p.getInventory().contains(mat)) {
                    p.sendMessage(Component.text("You don't have that item.").color(NamedTextColor.RED));
                    return;
                }
                p.getInventory().removeItem(new ItemStack(mat, 1));
                economy.deposit(p, BigDecimal.valueOf(entry.sell()));
                p.sendMessage(Component.text("Sold for " + (long) entry.sell()).color(NamedTextColor.YELLOW));
            } else {
                if (economy.balance(p).compareTo(BigDecimal.valueOf(entry.buy())) < 0) {
                    p.sendMessage(Component.text("Not enough currency.").color(NamedTextColor.RED));
                    return;
                }
                if (!economy.withdraw(p, BigDecimal.valueOf(entry.buy()))) {
                    p.sendMessage(Component.text("Not enough currency.").color(NamedTextColor.RED));
                    return;
                }
                Optional<RpgItem> rpg = RpgServices.items().get(entry.itemId());
                ItemStack stack = rpg.map(RpgItem::toItemStack).orElseGet(() -> {
                    Material mat = Material.matchMaterial(entry.itemId());
                    return mat == null ? new ItemStack(Material.BARRIER) : new ItemStack(mat);
                });
                HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(stack);
                for (ItemStack drop : overflow.values()) p.getWorld().dropItemNaturally(p.getLocation(), drop);
                p.sendMessage(Component.text("Purchased.").color(NamedTextColor.GREEN));
            }
        } catch (IllegalStateException ex) {
            p.sendMessage(Component.text("Economy is not available.").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onShopClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) openShop.remove(p.getUniqueId());
    }

    private void handoffQuest(Player p, NpcDef def) {
        if (def.questId() == null) {
            p.sendMessage(Component.text(def.displayName() + " has nothing to say.").color(NamedTextColor.GRAY));
            return;
        }
        // Quest service is provided by rpg-quests via Bukkit ServicesManager — soft dep.
        QuestHandoffBridge bridge = Bukkit.getServer().getServicesManager()
                .load(QuestHandoffBridge.class);
        if (bridge == null) {
            p.sendMessage(Component.text("Quest system is not loaded.").color(NamedTextColor.RED));
            return;
        }
        bridge.handoff(p, def.questId());
    }
}
