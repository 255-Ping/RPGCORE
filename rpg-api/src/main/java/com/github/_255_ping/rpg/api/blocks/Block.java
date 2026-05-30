package com.github._255_ping.rpg.api.blocks;

import org.bukkit.Material;

import java.util.List;

/**
 * A YAML-defined custom block. Custom blocks are tagged onto vanilla bases via PDC at the
 * placed location; the impl is responsible for maintaining the location-to-block mapping
 * and intercepting break events.
 */
public interface Block {

    String id();

    /** Base vanilla material placed in the world to represent this block. */
    Material material();

    /** Block "HP" — {@code MINING_SPEED} drains this at HP/sec while held click. */
    double toughness();

    /** {@code BREAKING_POWER} stat must be {@code >=} this for the player to break the block. */
    int requiredPower();

    /** Tool category required (e.g., pickaxe). {@link RequiredToolType#NONE} = bare hands OK. */
    RequiredToolType requiredToolType();

    /** Ticks until the block respawns after being broken. {@code 0} = no respawn. */
    int respawnTicks();

    /** Placeholder material shown at the location while the block is regenerating. */
    Material respawnPlaceholder();

    /** Whether right-click invokes an action (station GUI, ability). */
    boolean interactable();

    /** Station type ({@code crafting}, {@code cooking}, {@code brewing}, {@code enchanting}, {@code anvil}), or empty. */
    String stationType();

    /** Drop entries in the form {@code [file] <itemId> <min>[-<max>]}. */
    List<String> dropSpecs();

    /**
     * Skill XP awarded when this block is broken. {@code 0} means use the skill addon's
     * configured {@code default-xp} value instead. Set {@code XP: N} in the block YAML.
     */
    default long xp() { return 0; }
}
