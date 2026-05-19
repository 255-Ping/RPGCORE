package com.github._255_ping.rpg.parties;

import com.github._255_ping.rpg.api.parties.Party;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CoreParty implements Party {

    private final UUID id;
    private UUID ownerId;
    private final Set<UUID> memberIds = new LinkedHashSet<>();
    private final Set<UUID> moderatorIds = new LinkedHashSet<>();

    CoreParty(Player owner) {
        this.id = UUID.randomUUID();
        this.ownerId = owner.getUniqueId();
        this.memberIds.add(owner.getUniqueId());
    }

    @Override public UUID id() { return id; }

    @Override
    public Player owner() {
        return Bukkit.getPlayer(ownerId);
    }

    @Override
    public Collection<Player> members() {
        List<Player> out = new ArrayList<>();
        for (UUID id : memberIds) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) out.add(p);
        }
        return out;
    }

    @Override
    public Collection<Player> moderators() {
        List<Player> out = new ArrayList<>();
        for (UUID id : moderatorIds) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) out.add(p);
        }
        return out;
    }

    @Override
    public boolean isMember(Player player) {
        return memberIds.contains(player.getUniqueId());
    }

    @Override
    public boolean isOwner(Player player) {
        return ownerId.equals(player.getUniqueId());
    }

    @Override
    public boolean isModerator(Player player) {
        return moderatorIds.contains(player.getUniqueId());
    }

    // Internal mutators
    Set<UUID> memberIds() { return memberIds; }
    Set<UUID> moderatorIds() { return moderatorIds; }
    UUID ownerIdRaw() { return ownerId; }
    void setOwnerIdRaw(UUID id) { this.ownerId = id; }
}
