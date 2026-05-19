package com.github._255_ping.rpg.api.blocks;

import org.bukkit.Location;

import java.util.Collection;
import java.util.Optional;

public interface BlockRegistry {

    void register(Block block);

    Optional<Block> get(String id);

    /** Returns the custom block registered at this location, if any. */
    Optional<Block> at(Location location);

    Collection<Block> all();
}
