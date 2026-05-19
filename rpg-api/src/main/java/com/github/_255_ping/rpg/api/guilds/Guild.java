package com.github._255_ping.rpg.api.guilds;

import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;

/**
 * A persistent player community. Owner-led, ranked, with a shared currency bank.
 */
public interface Guild {

    UUID id();

    String name();

    UUID ownerId();

    /** Members keyed by uuid → rank id. Always non-empty (owner is always a member). */
    Collection<UUID> memberIds();

    /** Rank id ({@code owner}, {@code officer}, {@code member}) for the given uuid, or empty if not a member. */
    String rankOf(UUID memberId);

    BigDecimal bankBalance();

    /** Total XP accumulated from members' skill XP. */
    long totalXp();

    /** Returns true iff the player is in this guild. */
    boolean isMember(OfflinePlayer player);

    boolean isOwner(OfflinePlayer player);
}
