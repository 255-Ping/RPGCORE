package com.github._255_ping.rpg.api.bossbar;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Manages per-player boss health bars for custom RPG mobs.
 *
 * <p>The boss bar for a given entity is shared across all tracking players — one
 * {@link org.bukkit.boss.BossBar} instance per entity, with players added/removed as they
 * enter and leave range.
 *
 * <p>Obtain via {@code RpgServices.bossBar()}.
 *
 * <h3>Typical usage</h3>
 * The {@code rpg-bossbar} plugin's proximity task handles {@link #track} / {@link #untrack}
 * automatically for any mob with a {@link com.github._255_ping.rpg.api.mobs.BossBarDef} set.
 * Other addons can call these directly for custom boss-encounter logic.
 */
public interface BossBarService {

    /**
     * Begin showing this entity's boss bar to the given player. If no bar exists for the entity
     * yet, one is created from the mob's {@link com.github._255_ping.rpg.api.mobs.BossBarDef}.
     * The bar's progress is set to the entity's current HP fraction immediately.
     * No-op if the player is already tracking this entity.
     *
     * @param player the player who should see the bar
     * @param entity the mob whose HP bar to show
     */
    void track(Player player, LivingEntity entity);

    /**
     * Stop showing this entity's boss bar to the given player. The bar is removed from
     * {@code rpg-bossbar}'s internal state if no players remain on it.
     * No-op if the player was not tracking this entity.
     *
     * @param player the player to remove from the bar
     * @param entity the mob whose bar to untrack
     */
    void untrack(Player player, LivingEntity entity);

    /**
     * Remove all active boss bars for this player (use on death, world change, disconnect).
     *
     * @param player the player to clear
     */
    void clearAll(Player player);
}
