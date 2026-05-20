package com.github._255_ping.rpg.foraging;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        String key = event.getBlock().getType().name().toLowerCase(Locale.ROOT);
        long base = getConfig().getLong("xp-per-block." + key, -1);
        if (base < 0) {
            // Not a configured log — only award if this is a log/stem material we didn't list explicitly.
            if (!key.endsWith("_log") && !key.endsWith("_stem")) return;
            base = getConfig().getLong("default-xp", 3);
        }
        if (base <= 0) return;
        double wisdom = RpgServices.player(event.getPlayer()).get(BuiltinStat.FORAGING_WISDOM);
        long amount = Math.round(base * (1.0 + wisdom / 100.0));
        if (amount <= 0) return;
        RpgServices.skills().awardXp(event.getPlayer(), BuiltinSkill.FORAGING.id(), amount);
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-foraging disabled.");
    }
}
