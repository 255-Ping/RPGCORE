package com.github._255_ping.rpg.core.wand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class WandListener implements Listener {

    public static final String WAND_PDC_VALUE = "rpg-selection-wand";
    private final JavaPlugin plugin;
    private final CoreWandService service;
    private final NamespacedKey wandKey;

    public WandListener(JavaPlugin plugin, CoreWandService service) {
        this.plugin = plugin;
        this.service = service;
        this.wandKey = new NamespacedKey(plugin, "wand");
    }

    public ItemStack newWand() {
        ItemStack stack = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Selection Wand").color(NamedTextColor.LIGHT_PURPLE));
            meta.lore(java.util.List.of(
                    Component.text("Left-click block: corner 1").color(NamedTextColor.GRAY),
                    Component.text("Right-click block: corner 2").color(NamedTextColor.GRAY),
                    Component.text("/rpg wand <mode> to switch mode").color(NamedTextColor.DARK_GRAY)));
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.STRING, WAND_PDC_VALUE);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public boolean isWand(ItemStack stack) {
        if (stack == null) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        return WAND_PDC_VALUE.equals(meta.getPersistentDataContainer().get(wandKey, PersistentDataType.STRING));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
        if (!isWand(hand)) return;
        Player p = e.getPlayer();
        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            service.setCorner1(p, e.getClickedBlock().getLocation());
            p.sendMessage(Component.text("Corner 1 set at "
                            + format(e.getClickedBlock().getLocation()) + " (mode: " + service.modeOf(p) + ")")
                    .color(NamedTextColor.AQUA));
            e.setCancelled(true);
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            service.setCorner2(p, e.getClickedBlock().getLocation());
            p.sendMessage(Component.text("Corner 2 set at "
                            + format(e.getClickedBlock().getLocation()) + " (mode: " + service.modeOf(p) + ")")
                    .color(NamedTextColor.AQUA));
            e.setCancelled(true);
        }
    }

    private static String format(org.bukkit.Location l) {
        return l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }
}
