package com.github._255_ping.rpg.core.blocks;

import com.github._255_ping.rpg.api.blocks.Block;
import com.github._255_ping.rpg.api.blocks.BlockRegistry;
import org.bukkit.Location;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CoreBlockRegistry implements BlockRegistry {

    private final ConcurrentMap<String, Block> byId = new ConcurrentHashMap<>();
    private final ConcurrentMap<BlockKey, String> byLocation = new ConcurrentHashMap<>();

    @Override
    public void register(Block block) {
        byId.put(block.id(), block);
    }

    @Override
    public Optional<Block> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Block> at(Location location) {
        String id = byLocation.get(BlockKey.of(location));
        if (id == null) return Optional.empty();
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Collection<Block> all() {
        return byId.values();
    }

    public void tagLocation(Location loc, String blockId) {
        byLocation.put(BlockKey.of(loc), blockId);
    }

    public void untagLocation(Location loc) {
        byLocation.remove(BlockKey.of(loc));
    }

    public boolean hasTag(Location loc) {
        return byLocation.containsKey(BlockKey.of(loc));
    }

    public void clearDefinitions() {
        byId.clear();
    }

    public int trackedCount() {
        return byLocation.size();
    }
}
