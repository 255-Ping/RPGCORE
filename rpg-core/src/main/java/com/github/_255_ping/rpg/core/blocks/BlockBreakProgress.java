package com.github._255_ping.rpg.core.blocks;

import com.github._255_ping.rpg.api.blocks.Block;
import org.bukkit.Location;

final class BlockBreakProgress {

    final Location location;
    final Block definition;
    /** Drained each server tick. Read by the Netty IO thread via the crack interceptor — must be volatile. */
    volatile double remainingHp;
    /**
     * Set to {@code true} before sending a stage-clear (0f) packet so the Netty crack interceptor
     * passes it through unchanged rather than replacing the stage with our RPG progress.
     * Used in tick-loop bail-outs and {@code completeBreak} where the entry is still in the
     * active map when the clear packet is written.
     */
    volatile boolean clearing = false;

    BlockBreakProgress(Location location, Block definition) {
        this.location = location;
        this.definition = definition;
        this.remainingHp = definition.toughness();
    }
}
