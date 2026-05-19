package com.github._255_ping.rpg.farming;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class RpgFarmingPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("rpg-farming v" + getPluginMeta().getVersion() + " enabled.");
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

    @Override
    public void onDisable() {
        getLogger().info("rpg-farming disabled.");
    }
}
