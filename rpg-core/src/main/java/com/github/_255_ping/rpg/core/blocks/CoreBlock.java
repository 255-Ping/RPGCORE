package com.github._255_ping.rpg.core.blocks;

import com.github._255_ping.rpg.api.blocks.Block;
import com.github._255_ping.rpg.api.blocks.RequiredToolType;
import org.bukkit.Material;

import java.util.List;

public record CoreBlock(
        String id,
        Material material,
        double toughness,
        int requiredPower,
        RequiredToolType requiredToolType,
        int respawnTicks,
        Material respawnPlaceholder,
        boolean interactable,
        String stationType,
        List<String> dropSpecs
) implements Block {}
