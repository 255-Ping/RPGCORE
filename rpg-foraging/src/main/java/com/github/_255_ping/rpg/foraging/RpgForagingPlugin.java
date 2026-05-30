package com.github._255_ping.rpg.foraging;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
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

public final class RpgForagingPlugin extends JavaPlugin implements Listener, org.bukkit.command.CommandExecutor {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        java.util.Objects.requireNonNull(getCommand("foraging"), "command 'foraging' missing").setExecutor(this);
        getLogger().info("rpg-foraging v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command,
                             String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rpg.foraging.admin.reload")) {
                sender.sendMessage("§cNo permission."); return true;
            }
            reloadConfig();
            sender.sendMessage("§arpg-foraging reloaded.");
            return true;
        }
        sender.sendMessage("§7Usage: §e/foraging reload");
        return true;
    }

    /** Award foraging XP when a log or configured block is broken. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        String key = event.getBlock().getType().name().toLowerCase(Locale.ROOT);
        long base = getConfig().getLong("xp-per-block." + key, -1);
        if (base < 0) {
            // Not explicitly configured — award default XP for any log or nether stem.
            if (!key.endsWith("_log") && !key.endsWith("_stem")) return;
            base = getConfig().getLong("default-xp", 3);
        }
        if (base <= 0) return;
        double wisdom = RpgServices.player(event.getPlayer()).get(BuiltinStat.FORAGING_WISDOM);
        long amount = Math.round(base * (1.0 + wisdom / 100.0));
        if (amount <= 0) return;
        RpgServices.skills().awardXp(event.getPlayer(), BuiltinSkill.FORAGING.id(), amount);
    }

    /**
     * Apply FORAGING_FORTUNE to vanilla drops from logs and configured blocks.
     *
     * <p>Fortune formula: every 100 points = +1 guaranteed extra multiply.
     * Fractional part is probabilistic (50 fortune → 50 % chance of ×2 vs ×1).
     * Runs at NORMAL priority so silk-touch / other modifiers resolve first.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onForagingDrops(BlockDropItemEvent event) {
        String key = event.getBlock().getType().name().toLowerCase(Locale.ROOT);
        if (!isForagingBlock(key)) return;

        double fortune = RpgServices.player(event.getPlayer()).get(BuiltinStat.FORAGING_FORTUNE);
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

    /** True if this block key is tracked by the foraging system. */
    private boolean isForagingBlock(String key) {
        if (getConfig().contains("xp-per-block." + key)) return true;
        return key.endsWith("_log") || key.endsWith("_stem");
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-foraging disabled.");
    }
}
