package com.github._255_ping.rpg.api.achievement;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

/**
 * API for the achievement system. Accessible via {@link com.github._255_ping.rpg.api.RpgServices#achievements()}.
 *
 * <p>Achievements are per-player and persisted across sessions. Each achievement can only
 * be granted once; subsequent calls to {@link #grant} or {@link #increment} that would
 * re-unlock a completed achievement are silently ignored.
 */
public interface AchievementService {

    /** Returns all loaded achievement definitions. */
    Collection<AchievementDef> all();

    /** Looks up a definition by ID. */
    Optional<AchievementDef> get(String id);

    /**
     * Returns true if the player has already unlocked the given achievement.
     * Returns false for unknown IDs.
     */
    boolean isUnlocked(Player player, String id);

    /**
     * Returns the current value of a named counter for the player.
     * Returns 0 for unknown counters.
     */
    long getCounter(Player player, String counterKey);

    /**
     * Manually grants an achievement to the player (MANUAL trigger type).
     * Fires the unlock reward, sends the player a notification, and persists the state.
     * Safe to call even if already unlocked (no-op in that case).
     */
    void grant(Player player, String id);

    /**
     * Increments a named counter for the player by {@code delta} and automatically
     * unlocks any COUNTER achievements whose target is now met.
     *
     * @param player     the player whose counter should be incremented
     * @param counterKey the counter name (defined by {@link AchievementDef#counterKey()})
     * @param delta      amount to add (should be positive)
     */
    void increment(Player player, String counterKey, long delta);
}
