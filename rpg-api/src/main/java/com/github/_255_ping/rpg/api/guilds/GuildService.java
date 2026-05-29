package com.github._255_ping.rpg.api.guilds;

import com.github._255_ping.rpg.api.stats.Stat;
import org.bukkit.OfflinePlayer;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface GuildService {

    Optional<Guild> guildOf(OfflinePlayer player);

    Optional<Guild> getByName(String name);

    Collection<Guild> all();

    /** Computed guild level based on total XP and the configured level curve. */
    default int guildLevel(Guild guild) { return 0; }

    /**
     * Stat bonuses the player receives from their guild's perks at the current guild level.
     * Returns an empty map if the player is not in a guild or the guild has no perks configured.
     */
    default Map<Stat, Double> perkStatsFor(OfflinePlayer player) { return Map.of(); }
}
