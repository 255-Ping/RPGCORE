package com.github._255_ping.rpg.api.parties;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

public interface PartyService {

    /** The party this player is in, if any. */
    Optional<Party> partyOf(Player player);

    Collection<Party> all();

    int maxSize();
}
