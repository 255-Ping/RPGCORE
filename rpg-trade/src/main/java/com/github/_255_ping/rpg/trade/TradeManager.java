package com.github._255_ping.rpg.trade;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks pending trade invites and active trade sessions.
 *
 * <p>Pending invites expire automatically via a periodic cleanup task
 * (see {@link #cleanExpiredInvites(int)}).
 */
public final class TradeManager {

    /** Pending invite: invitee UUID → inviter UUID */
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    /** Timestamp (System.currentTimeMillis) when each invite was sent: invitee UUID → millis */
    private final Map<UUID, Long> inviteTimes = new HashMap<>();
    /** Active sessions: player UUID → session (both A and B map to the same session). */
    private final Map<UUID, TradeSession> sessions = new HashMap<>();
    /** Players currently in coin-amount chat-entry mode. */
    private final Map<UUID, Boolean> coinMode = new HashMap<>();

    // ── Invites ─────────────────────────────────────────────────────────────

    public void sendInvite(Player from, Player to) {
        pendingInvites.put(to.getUniqueId(), from.getUniqueId());
        inviteTimes.put(to.getUniqueId(), System.currentTimeMillis());
    }

    /** Returns true if {@code player} has a pending (non-expired) invite. */
    public boolean hasPendingInvite(UUID player) {
        return pendingInvites.containsKey(player);
    }

    /** Returns the UUID of the player who sent an invite to {@code invitee}, or null. */
    public UUID getInviter(UUID invitee) {
        return pendingInvites.get(invitee);
    }

    public void clearInvite(UUID invitee) {
        pendingInvites.remove(invitee);
        inviteTimes.remove(invitee);
    }

    /** Remove expired invites; {@code expirySeconds} from config. */
    public Map<UUID, UUID> drainExpiredInvites(int expirySeconds) {
        long now = System.currentTimeMillis();
        Map<UUID, UUID> expired = new HashMap<>();
        inviteTimes.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > (long) expirySeconds * 1000L) {
                UUID invitee = entry.getKey();
                UUID inviter = pendingInvites.remove(invitee);
                if (inviter != null) expired.put(invitee, inviter);
                return true;
            }
            return false;
        });
        return expired;
    }

    // ── Sessions ─────────────────────────────────────────────────────────────

    public TradeSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    public boolean inSession(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public TradeSession createSession(Player a, Player b) {
        TradeSession session = new TradeSession(
                a.getUniqueId(), a.getName(),
                b.getUniqueId(), b.getName());
        sessions.put(a.getUniqueId(), session);
        sessions.put(b.getUniqueId(), session);
        return session;
    }

    public void removeSession(UUID uuid) {
        sessions.remove(uuid);
    }

    // ── Coin input mode ──────────────────────────────────────────────────────

    public void enterCoinMode(UUID uuid) {
        coinMode.put(uuid, true);
    }

    public void exitCoinMode(UUID uuid) {
        coinMode.remove(uuid);
    }

    public boolean inCoinMode(UUID uuid) {
        return coinMode.containsKey(uuid);
    }
}
