package com.github._255_ping.rpg.core.station;

import com.github._255_ping.rpg.api.blocks.Block;
import com.github._255_ping.rpg.api.station.StationService;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class CoreStationService implements StationService {

    private final Map<String, BiConsumer<Player, Block>> handlers = new ConcurrentHashMap<>();

    @Override
    public void register(String stationType, BiConsumer<Player, Block> handler) {
        handlers.put(stationType.toLowerCase(Locale.ROOT), handler);
    }

    @Override
    public boolean open(String stationType, Player player, Block block) {
        BiConsumer<Player, Block> handler = handlers.get(stationType.toLowerCase(Locale.ROOT));
        if (handler == null) return false;
        handler.accept(player, block);
        return true;
    }
}
