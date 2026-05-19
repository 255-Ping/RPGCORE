package com.github._255_ping.rpg.api.accessories;

import com.github._255_ping.rpg.api.stats.Stat;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Aggregates the stats of ACCESSORY items in the player's accessory bag. CoreRpgPlayer
 * queries this service during stat recalculation and adds the result as another stat
 * source (between equipment and status effects).
 *
 * <p>If rpg-accessories isn't loaded, RpgServices.accessories() throws; callers should
 * gracefully fall back to an empty map.
 */
public interface AccessoryService {

    /** Map of stat → total contribution from the player's accessory bag right now. */
    Map<Stat, Double> aggregateStats(Player player);
}
