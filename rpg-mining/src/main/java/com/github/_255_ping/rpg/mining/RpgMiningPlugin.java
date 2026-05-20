package com.github._255_ping.rpg.mining;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.blocks.RpgBlockBreakEvent;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class RpgMiningPlugin extends JavaPlugin implements Listener, org.bukkit.command.CommandExecutor {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        java.util.Objects.requireNonNull(getCommand("mining"), "command 'mining' missing").setExecutor(this);
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

    @Override
    public void onDisable() {
        getLogger().info("rpg-mining disabled.");
    }
}
