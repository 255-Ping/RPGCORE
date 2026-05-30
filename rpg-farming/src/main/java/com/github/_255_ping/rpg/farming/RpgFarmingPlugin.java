package com.github._255_ping.rpg.farming;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class RpgFarmingPlugin extends JavaPlugin implements Listener, org.bukkit.command.CommandExecutor {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        java.util.Objects.requireNonNull(getCommand("farming"), "command 'farming' missing").setExecutor(this);
        getLogger().info("rpg-farming v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command,
                             String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rpg.farming.admin.reload")) {
                sender.sendMessage("§cNo permission."); return true;
            }
            reloadConfig();
            sender.sendMessage("§arpg-farming reloaded.");
            return true;
        }
        sender.sendMessage("§7Usage: §e/farming reload");
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        String key = event.getBlock().getType().name().toLowerCase(Locale.ROOT);
        long base = getConfig().getLong("xp-per-block." + key, -1);
        if (base < 0) base = getConfig().getLong("default-xp", -1);
        if (base <= 0) return;

        // For age-based crops (wheat, carrots, etc.) only award at max age.
        BlockData data = event.getBlock().getBlockData();
        if (data instanceof Ageable age && age.getAge() != age.getMaximumAge()) return;

        double wisdom = RpgServices.player(event.getPlayer()).get(BuiltinStat.FARMING_WISDOM);
        long amount = Math.round(base * (1.0 + wisdom / 100.0));
        if (amount <= 0) return;
        RpgServices.skills().awardXp(event.getPlayer(), BuiltinSkill.FARMING.id(), amount);
    }

    /**
     * Apply FARMING_FORTUNE to vanilla drops from mature crops and configured blocks.
     *
     * <p>Fortune formula: every 100 points = +1 guaranteed extra multiply.
     * Fractional part is probabilistic (50 fortune → 50 % chance of ×2 vs ×1).
     * Runs at NORMAL priority so silk-touch / other modifiers resolve first.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFarmingDrops(BlockDropItemEvent event) {
        String key = event.getBlock().getType().name().toLowerCase(Locale.ROOT);
        if (!isFarmingBlock(key)) return;

        // Fortune only applies to mature crops.
        BlockData data = event.getBlock().getBlockData();
        if (data instanceof Ageable age && age.getAge() != age.getMaximumAge()) return;

        double fortune = RpgServices.player(event.getPlayer()).get(BuiltinStat.FARMING_FORTUNE);
        if (fortune <= 0) return;

        int guaranteed = (int) (fortune / 100.0);
        double chance   = (fortune % 100.0) / 100.0;
        int extra = guaranteed + (ThreadLocalRandom.current().nextDouble() < chance ? 1 : 0);
        if (extra <= 0) return;

        for (Item item : event.getItems()) {
            ItemStack is = item.getItemStack();
            is.setAmount(Math.min(is.getAmount() * (1 + extra), is.getType().getMaxStackSize()));
            item.setItemStack(is);
        }
    }

    /** True if this block key is tracked by the farming system. */
    private boolean isFarmingBlock(String key) {
        return getConfig().contains("xp-per-block." + key);
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-farming disabled.");
    }
}
