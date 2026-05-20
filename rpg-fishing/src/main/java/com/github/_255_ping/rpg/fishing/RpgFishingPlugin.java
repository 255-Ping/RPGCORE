package com.github._255_ping.rpg.fishing;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class RpgFishingPlugin extends JavaPlugin implements Listener, org.bukkit.command.CommandExecutor {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        java.util.Objects.requireNonNull(getCommand("fishing"), "command 'fishing' missing").setExecutor(this);
        getLogger().info("rpg-fishing v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command,
                             String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rpg.fishing.admin.reload")) {
                sender.sendMessage("§cNo permission."); return true;
            }
            reloadConfig();
            sender.sendMessage("§arpg-fishing reloaded.");
            return true;
        }
        sender.sendMessage("§7Usage: §e/fishing reload");
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        long base = getConfig().getLong("xp-per-catch", 10);
        if (base <= 0) return;
        double wisdom = RpgServices.player(event.getPlayer()).get(BuiltinStat.FISHING_WISDOM);
        long amount = Math.round(base * (1.0 + wisdom / 100.0));
        if (amount <= 0) return;
        RpgServices.skills().awardXp(event.getPlayer(), BuiltinSkill.FISHING.id(), amount);
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-fishing disabled.");
    }
}
