package com.github._255_ping.rpg.api.parties;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * A session-only group of players. No persistence — parties dissolve when the owner
 * disconnects (configurable) or on server shutdown.
 */
public interface Party {

    UUID id();

    Player owner();

    Collection<Player> members();

    Collection<Player> moderators();

    boolean isMember(Player player);

    boolean isOwner(Player player);

    boolean isModerator(Player player);
}
