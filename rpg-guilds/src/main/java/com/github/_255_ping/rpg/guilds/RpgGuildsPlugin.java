package com.github._255_ping.rpg.guilds;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.skills.SkillXpAwardEvent;
import com.github._255_ping.rpg.api.stats.Stat;
import com.github._255_ping.rpg.api.stats.StatRecalcEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Objects;

public final class RpgGuildsPlugin extends JavaPlugin implements Listener {

    private GuildManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new GuildManager(this);
        manager.loadAll();
        RpgServices.setGuilds(manager);

        GuildCommand guildCommand = new GuildCommand(this, manager);
        var guildCmd = Objects.requireNonNull(getCommand("guild"), "command 'guild' missing");
        guildCmd.setExecutor(guildCommand);
        guildCmd.setTabCompleter(guildCommand);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, manager::cleanExpiredInvites, 200L, 200L);

        getLogger().info("rpg-guilds v" + getPluginMeta().getVersion()
                + " enabled; loaded " + manager.all().size() + " guilds.");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.saveAll();
        getLogger().info("rpg-guilds disabled.");
    }

    @EventHandler
    public void onSkillXp(SkillXpAwardEvent event) {
        manager.addXpFromSkill(event.getPlayer(), event.amount());
    }

    @EventHandler
    public void onStatRecalc(StatRecalcEvent event) {
        Map<Stat, Double> perks = manager.perkStatsFor(event.getPlayer());
        for (Map.Entry<Stat, Double> entry : perks.entrySet()) {
            event.holder().add(entry.getKey(), entry.getValue());
        }
    }
}
