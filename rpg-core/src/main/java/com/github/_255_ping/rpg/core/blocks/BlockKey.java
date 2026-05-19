package com.github._255_ping.rpg.core.blocks;

import org.bukkit.Location;
import org.bukkit.block.Block;

public record BlockKey(String world, int x, int y, int z) {

    public static BlockKey of(Location loc) {
        return new BlockKey(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public static BlockKey of(Block block) {
        return new BlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }
}
