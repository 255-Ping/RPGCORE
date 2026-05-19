package com.github._255_ping.rpg.guilds;

import com.github._255_ping.rpg.api.guilds.Guild;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CoreGuild implements Guild {

    public static final String RANK_OWNER = "owner";
    public static final String RANK_OFFICER = "officer";
    public static final String RANK_MEMBER = "member";

    private final UUID id;
    private String name;
    private UUID ownerId;
    /** Insertion order = join order; first entry is always the current owner. */
    private final LinkedHashMap<UUID, String> ranks = new LinkedHashMap<>();
    private BigDecimal bankBalance = BigDecimal.ZERO;
    private long totalXp = 0L;

    public CoreGuild(UUID id, String name, UUID ownerId) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.ranks.put(ownerId, RANK_OWNER);
    }

    @Override public UUID id() { return id; }
    @Override public String name() { return name; }
    @Override public UUID ownerId() { return ownerId; }
    @Override public Collection<UUID> memberIds() { return ranks.keySet(); }

    @Override
    public String rankOf(UUID memberId) {
        return ranks.getOrDefault(memberId, "");
    }

    @Override public BigDecimal bankBalance() { return bankBalance; }
    @Override public long totalXp() { return totalXp; }

    @Override
    public boolean isMember(OfflinePlayer player) {
        return ranks.containsKey(player.getUniqueId());
    }

    @Override
    public boolean isOwner(OfflinePlayer player) {
        return ownerId.equals(player.getUniqueId());
    }

    // Mutators (package-private)
    void setName(String name) { this.name = name; }
    void setOwnerId(UUID newOwner) {
        this.ownerId = newOwner;
        this.ranks.put(newOwner, RANK_OWNER);
    }
    Map<UUID, String> rawRanks() { return ranks; }
    void setBankBalance(BigDecimal v) { this.bankBalance = v; }
    void setTotalXp(long v) { this.totalXp = v; }
}
