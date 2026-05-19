package com.github._255_ping.rpg.api.guilds;

import org.bukkit.OfflinePlayer;

import java.util.Collection;
import java.util.Optional;

public interface GuildService {

    Optional<Guild> guildOf(OfflinePlayer player);

    Optional<Guild> getByName(String name);

    Collection<Guild> all();
}
