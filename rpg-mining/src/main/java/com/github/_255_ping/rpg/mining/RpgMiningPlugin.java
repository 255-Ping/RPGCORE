package com.github._255_ping.rpg.mining;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.blocks.RpgBlockBreakEvent;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class RpgMiningPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("rpg-mining v" + getPluginMeta().getVersion() + " enabled.");
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
