package com.github._255_ping.rpg.core.blocks;

import com.github._255_ping.rpg.api.blocks.Block;
import org.bukkit.Location;

final class BlockBreakProgress {

    final Location location;
    final Block definition;
    double remainingHp;
    int lastStage;

    BlockBreakProgress(Location location, Block definition) {
        this.location = location;
        this.definition = definition;
        this.remainingHp = definition.toughness();
        this.lastStage = -1;
    }
}
