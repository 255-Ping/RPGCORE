package com.github._255_ping.rpg.parties;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.parties.Party;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;

public final class RpgPartiesPlugin extends JavaPlugin implements Listener {

    private PartyManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new PartyManager(this);
        RpgServices.setParties(manager);

        Objects.requireNonNull(getCommand("party"), "command 'party' missing").setExecutor(new PartyCommand(manager));
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, manager::cleanExpiredInvites, 200L, 200L);

        getLogger().info("rpg-parties v" + getPluginMeta().getVersion() + " enabled.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Optional<Party> opt = manager.partyOf(event.getPlayer());
        opt.ifPresent(party -> manager.removeMember((CoreParty) party, event.getPlayer()));
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-parties disabled.");
    }
}
