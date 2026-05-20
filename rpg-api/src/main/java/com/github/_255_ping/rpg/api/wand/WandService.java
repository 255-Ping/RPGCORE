package com.github._255_ping.rpg.api.wand;

import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Access to the admin selection wand. Other addons read selections from here when they need a
 * bounding box (e.g., {@code /region define <id>}).
 */
public interface WandService {

    /** Current selection for this player, if both corners are set. */
    Optional<WandSelection> selectionOf(Player player);

    /** Current wand mode for this player ({@code region|loot-chest|dungeon|spawner|entrance}). */
    String modeOf(Player player);

    void setMode(Player player, String mode);

    /** Clear the player's selection (e.g., after a successful /region define). */
    void clearSelection(Player player);
}
