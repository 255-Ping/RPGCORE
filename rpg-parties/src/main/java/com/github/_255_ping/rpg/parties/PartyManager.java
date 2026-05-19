package com.github._255_ping.rpg.parties;

import com.github._255_ping.rpg.api.parties.Party;
import com.github._255_ping.rpg.api.parties.PartyService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PartyManager implements PartyService {

    private final JavaPlugin plugin;
    private final ConcurrentHashMap<UUID, CoreParty> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UUID> playerToParty = new ConcurrentHashMap<>();
    // pending invites: invitee → (party id, expiry ms)
    private final ConcurrentHashMap<UUID, PendingInvite> invites = new ConcurrentHashMap<>();

    public PartyManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<Party> partyOf(Player player) {
        UUID partyId = playerToParty.get(player.getUniqueId());
        if (partyId == null) return Optional.empty();
        return Optional.ofNullable(byId.get(partyId));
    }

    @Override
    public Collection<Party> all() {
        return java.util.Collections.unmodifiableCollection(byId.values());
    }

    @Override
    public int maxSize() {
        return plugin.getConfig().getInt("max-size", 5);
    }

    /** Returns null if the player is already in a party. */
    public CoreParty create(Player owner) {
        if (playerToParty.containsKey(owner.getUniqueId())) return null;
        CoreParty party = new CoreParty(owner);
        byId.put(party.id(), party);
        playerToParty.put(owner.getUniqueId(), party.id());
        return party;
    }

    public boolean invite(CoreParty party, Player invitee) {
        if (party.isMember(invitee)) return false;
        if (party.members().size() >= maxSize()) return false;
        if (playerToParty.containsKey(invitee.getUniqueId())) return false;
        long timeoutSec = plugin.getConfig().getLong("invite-timeout-seconds", 60);
        long expiry = System.currentTimeMillis() + timeoutSec * 1000L;
        invites.put(invitee.getUniqueId(), new PendingInvite(party.id(), expiry));
        return true;
    }

    public Optional<CoreParty> acceptInvite(Player invitee) {
        PendingInvite pending = invites.remove(invitee.getUniqueId());
        if (pending == null) return Optional.empty();
        if (System.currentTimeMillis() > pending.expiryMs) return Optional.empty();
        CoreParty party = byId.get(pending.partyId);
        if (party == null) return Optional.empty();
        if (party.members().size() >= maxSize()) return Optional.empty();
        if (playerToParty.containsKey(invitee.getUniqueId())) return Optional.empty();
        party.memberIds().add(invitee.getUniqueId());
        playerToParty.put(invitee.getUniqueId(), party.id());
        return Optional.of(party);
    }

    public boolean removeMember(CoreParty party, Player member) {
        UUID id = member.getUniqueId();
        if (!party.memberIds().remove(id)) return false;
        party.moderatorIds().remove(id);
        playerToParty.remove(id);
        // If the owner left, transfer or disband per config.
        if (party.ownerIdRaw().equals(id)) {
            boolean autoDisband = plugin.getConfig().getBoolean("disband-on-owner-leave", false);
            if (autoDisband || party.memberIds().isEmpty()) {
                disband(party);
                return true;
            }
            // Transfer to first remaining moderator, else first remaining member.
            UUID next = party.moderatorIds().isEmpty()
                    ? party.memberIds().iterator().next()
                    : party.moderatorIds().iterator().next();
            party.setOwnerIdRaw(next);
            party.moderatorIds().remove(next);
        }
        if (party.memberIds().isEmpty()) disband(party);
        return true;
    }

    public void disband(CoreParty party) {
        for (UUID memberId : party.memberIds()) playerToParty.remove(memberId);
        byId.remove(party.id());
    }

    public boolean promote(CoreParty party, Player target) {
        if (!party.isMember(target)) return false;
        if (party.isOwner(target)) return false;
        return party.moderatorIds().add(target.getUniqueId());
    }

    public boolean demote(CoreParty party, Player target) {
        return party.moderatorIds().remove(target.getUniqueId());
    }

    public void cleanExpiredInvites() {
        long now = System.currentTimeMillis();
        invites.values().removeIf(p -> p.expiryMs < now);
    }

    private record PendingInvite(UUID partyId, long expiryMs) {}
}
