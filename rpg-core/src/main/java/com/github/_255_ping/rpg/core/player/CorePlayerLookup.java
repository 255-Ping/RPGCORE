package com.github._255_ping.rpg.core.player;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CorePlayerLookup implements RpgServices.PlayerLookup {

    private final Map<UUID, CoreRpgPlayer> players = new ConcurrentHashMap<>();

    @Override
    public RpgPlayer get(Player player) {
        return players.computeIfAbsent(player.getUniqueId(), k -> new CoreRpgPlayer(player));
    }

    public CoreRpgPlayer getOrCreate(Player player) {
        return players.computeIfAbsent(player.getUniqueId(), k -> new CoreRpgPlayer(player));
    }

    public void remove(Player player) {
        players.remove(player.getUniqueId());
    }
}
