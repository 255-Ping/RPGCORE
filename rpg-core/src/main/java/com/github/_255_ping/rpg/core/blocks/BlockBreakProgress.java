package com.github._255_ping.rpg.core.blocks;

import com.github._255_ping.rpg.api.blocks.Block;
import org.bukkit.Location;

final class BlockBreakProgress {

    final Location location;
    final Block definition;
    double remainingHp;
    int lastStage;
    /** System.currentTimeMillis() of the last BlockDamageEvent for this block.
     *  Used to detect when the player releases the mouse button. */
    long lastClickMs;

    BlockBreakProgress(Location location, Block definition) {
        this.location = location;
        this.definition = definition;
        this.remainingHp = definition.toughness();
        this.lastStage = -1;
        this.lastClickMs = System.currentTimeMillis();
    }
}
