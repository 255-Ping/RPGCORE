package com.github._255_ping.rpg.api.wand;

import org.bukkit.Location;

/** Two-corner selection set by the admin's selection wand. Both corners are inclusive. */
public record WandSelection(Location corner1, Location corner2) {

    public boolean isComplete() {
        return corner1 != null && corner2 != null
                && corner1.getWorld() != null
                && corner1.getWorld().equals(corner2.getWorld());
    }

    public org.bukkit.util.Vector min() {
        return new org.bukkit.util.Vector(
                Math.min(corner1.getX(), corner2.getX()),
                Math.min(corner1.getY(), corner2.getY()),
                Math.min(corner1.getZ(), corner2.getZ()));
    }

    public org.bukkit.util.Vector max() {
        return new org.bukkit.util.Vector(
                Math.max(corner1.getX(), corner2.getX()),
                Math.max(corner1.getY(), corner2.getY()),
                Math.max(corner1.getZ(), corner2.getZ()));
    }
}
