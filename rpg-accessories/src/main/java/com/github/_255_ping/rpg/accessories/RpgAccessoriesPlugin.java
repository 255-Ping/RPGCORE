package com.github._255_ping.rpg.accessories;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.items.BuiltinItemType;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;

public final class RpgAccessoriesPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private AccessoryBagService bagService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        bagService = new AccessoryBagService(this);
        RpgServices.setAccessories(bagService);

        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("accessories"), "command 'accessories' missing").setExecutor(this);

        getLogger().info("rpg-accessories v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cPlayers only."); return true;
        }
        if (!sender.hasPermission("rpg.accessories.open")) {
            sender.sendMessage("§cNo permission."); return true;
        }
        p.openInventory(bagService.openBag(p));
        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Lazy-load on first /bag open; here we just touch the bag so its inventory exists
        // for stat aggregation on the player's first recalc.
        bagService.openBag(event.getPlayer());
        RpgPlayer rp = RpgServices.player(event.getPlayer());
        rp.recalculateStats();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        bagService.release(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(InventoryClickEvent event) {
        if (!bagService.isBagInventory(event.getInventory())) return;
        // Only ACCESSORY items can enter the bag.
        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            // Click is putting cursor → bag (top inventory click)
            if (event.getClickedInventory() == event.getInventory() && !isAccessory(cursor)) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player p) {
                    p.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                            .legacyAmpersand().deserialize("&cOnly accessories go in the bag."));
                }
                return;
            }
        }
        // Shift-click from player inventory → bag: only allow accessories.
        if (event.isShiftClick()
                && event.getClickedInventory() != event.getInventory()
                && event.getCurrentItem() != null
                && !isAccessory(event.getCurrentItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!bagService.isBagInventory(event.getInventory())) return;
        bagService.save(event.getPlayer().getUniqueId());
        if (event.getPlayer() instanceof Player p) RpgServices.player(p).recalculateStats();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClickRecalc(InventoryClickEvent event) {
        if (!bagService.isBagInventory(event.getInventory())) return;
        if (!(event.getWhoClicked() instanceof Player p)) return;
        // Defer recalc one tick so the inventory mutation has settled.
        getServer().getScheduler().runTask(this, () -> RpgServices.player(p).recalculateStats());
    }

    @Override
    public void onDisable() {
        if (bagService != null) bagService.saveAll();
        getLogger().info("rpg-accessories disabled.");
    }

    private static boolean isAccessory(ItemStack stack) {
        Optional<RpgItem> opt = RpgServices.items().from(stack);
        return opt.isPresent() && opt.get().type() == BuiltinItemType.ACCESSORY;
    }
}
