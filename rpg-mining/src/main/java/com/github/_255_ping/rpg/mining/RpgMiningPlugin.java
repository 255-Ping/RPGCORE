package com.github._255_ping.rpg.mining;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.blocks.RpgBlockBreakEvent;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RpgMiningPlugin extends JavaPlugin implements Listener, org.bukkit.command.CommandExecutor {

    /** Players currently holding a gathering tool — tracked so we can remove the fatigue precisely. */
    private final Set<UUID> fatigueApplied = new HashSet<>();
    private NamespacedKey itemIdKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        java.util.Objects.requireNonNull(getCommand("mining"), "command 'mining' missing").setExecutor(this);
        // Item PDC key matches rpg-core's registration (same plugin name, same key).
        itemIdKey = new NamespacedKey("rpg-core", "item_id");
        getLogger().info("rpg-mining v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command,
                             String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rpg.mining.admin.reload")) {
                sender.sendMessage("§cNo permission."); return true;
            }
            reloadConfig();
            sender.sendMessage("§arpg-mining reloaded.");
            return true;
        }
        sender.sendMessage("§7Usage: §e/mining reload");
        return true;
    }

    // ── XP on custom block break ──────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(RpgBlockBreakEvent event) {
        long base = getConfig().getLong("xp-per-block." + event.block().id(),
                getConfig().getLong("default-xp", 5));
        if (base <= 0) return;
        double wisdom = RpgServices.player(event.player()).get(BuiltinStat.MINING_WISDOM);
        long amount = Math.round(base * (1.0 + wisdom / 100.0));
        if (amount <= 0) return;
        RpgServices.skills().awardXp(event.player(), BuiltinSkill.MINING.id(), amount);
    }

    // ── Mining Fatigue for gathering tools ───────────────────────────────────

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        updateMiningFatigue(player, newItem);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Restore fatigue if player joins already holding a gathering tool.
        updateMiningFatigue(event.getPlayer(), event.getPlayer().getInventory().getItemInMainHand());
    }

    private void updateMiningFatigue(Player player, ItemStack held) {
        if (!getConfig().getBoolean("mining-fatigue.enabled", true)) {
            removeFatigue(player);
            return;
        }
        if (isGatheringTool(held)) {
            if (!fatigueApplied.contains(player.getUniqueId())) {
                int amplifier = getConfig().getInt("mining-fatigue.amplifier", 1);
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.MINING_FATIGUE,
                        Integer.MAX_VALUE, amplifier,
                        false,  // ambient (no particles)
                        false,  // no particles
                        false   // no icon
                ));
                fatigueApplied.add(player.getUniqueId());
            }
        } else {
            removeFatigue(player);
        }
    }

    private void removeFatigue(Player player) {
        if (fatigueApplied.remove(player.getUniqueId())) {
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        }
    }

    private boolean isGatheringTool(ItemStack item) {
        if (item == null || item.getType().isAir() || itemIdKey == null) return false;
        // Check if it's a registered RPG item with mining stats.
        String id = item.getItemMeta() != null
                ? item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING)
                : null;
        if (id == null) return false;
        try {
            Optional<RpgItem> opt = RpgServices.items().get(id);
            if (opt.isEmpty()) return false;
            var stats = opt.get().stats();
            return stats.containsKey(BuiltinStat.MINING_SPEED)
                    || stats.containsKey(BuiltinStat.BREAKING_POWER)
                    || stats.containsKey(BuiltinStat.FORAGING_SPEED);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-mining disabled.");
    }
}
