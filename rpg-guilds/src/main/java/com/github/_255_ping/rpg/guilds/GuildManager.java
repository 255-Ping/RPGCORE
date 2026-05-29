package com.github._255_ping.rpg.guilds;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.guilds.Guild;
import com.github._255_ping.rpg.api.guilds.GuildService;
import com.github._255_ping.rpg.api.persistence.DataStore;
import com.github._255_ping.rpg.api.stats.Stat;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class GuildManager implements GuildService {

    private static final String REPO = "guilds";

    private final JavaPlugin plugin;
    private final ConcurrentMap<UUID, CoreGuild> byId = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, UUID> memberIndex = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, PendingInvite> invites = new ConcurrentHashMap<>();

    public GuildManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        DataStore.Repository repo = RpgServices.dataStore().repository(REPO);
        for (String key : repo.keys()) {
            try {
                UUID id = UUID.fromString(key);
                repo.get(key).ifPresent(data -> tryLoad(id, data));
            } catch (IllegalArgumentException ignored) {
                // not a UUID
            }
        }
    }

    private void tryLoad(UUID id, Map<String, Object> data) {
        try {
            String name = String.valueOf(data.get("name"));
            UUID owner = UUID.fromString(String.valueOf(data.get("owner")));
            CoreGuild guild = new CoreGuild(id, name, owner);
            if (data.get("members") instanceof Map<?, ?> rawMap) {
                for (Map.Entry<?, ?> e : rawMap.entrySet()) {
                    try {
                        UUID memberId = UUID.fromString(String.valueOf(e.getKey()));
                        guild.rawRanks().put(memberId, String.valueOf(e.getValue()));
                        memberIndex.put(memberId, id);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            if (!guild.rawRanks().containsKey(owner)) {
                guild.rawRanks().put(owner, CoreGuild.RANK_OWNER);
                memberIndex.put(owner, id);
            }
            if (data.get("bank") instanceof Number n) guild.setBankBalance(BigDecimal.valueOf(n.doubleValue()));
            else if (data.get("bank") instanceof String s) guild.setBankBalance(new BigDecimal(s));
            if (data.get("total-xp") instanceof Number x) guild.setTotalXp(x.longValue());
            byId.put(id, guild);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to load guild " + id + ": " + ex.getMessage());
        }
    }

    public void save(UUID id) {
        CoreGuild guild = byId.get(id);
        if (guild == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("schema-version", 1);
        data.put("name", guild.name());
        data.put("owner", guild.ownerId().toString());
        Map<String, String> members = new LinkedHashMap<>();
        for (Map.Entry<UUID, String> e : guild.rawRanks().entrySet()) {
            members.put(e.getKey().toString(), e.getValue());
        }
        data.put("members", members);
        data.put("bank", guild.bankBalance().toPlainString());
        data.put("total-xp", guild.totalXp());
        RpgServices.dataStore().repository(REPO).save(id.toString(), data);
    }

    public void saveAll() {
        for (UUID id : byId.keySet()) save(id);
    }

    @Override
    public Optional<Guild> guildOf(OfflinePlayer player) {
        UUID guildId = memberIndex.get(player.getUniqueId());
        if (guildId == null) return Optional.empty();
        return Optional.ofNullable(byId.get(guildId));
    }

    @Override
    public Optional<Guild> getByName(String name) {
        for (CoreGuild g : byId.values()) {
            if (g.name().equalsIgnoreCase(name)) return Optional.of(g);
        }
        return Optional.empty();
    }

    @Override
    public Collection<Guild> all() {
        return java.util.Collections.unmodifiableCollection(byId.values());
    }

    public CoreGuild create(Player owner, String name) {
        if (memberIndex.containsKey(owner.getUniqueId())) return null;
        if (getByName(name).isPresent()) return null;
        CoreGuild guild = new CoreGuild(UUID.randomUUID(), name, owner.getUniqueId());
        byId.put(guild.id(), guild);
        memberIndex.put(owner.getUniqueId(), guild.id());
        save(guild.id());
        return guild;
    }

    public boolean invite(CoreGuild guild, Player invitee) {
        if (guild.rawRanks().containsKey(invitee.getUniqueId())) return false;
        if (memberIndex.containsKey(invitee.getUniqueId())) return false;
        if (guild.rawRanks().size() >= maxMembers()) return false;
        long timeoutSec = plugin.getConfig().getLong("invite-timeout-seconds", 60);
        invites.put(invitee.getUniqueId(),
                new PendingInvite(guild.id(), System.currentTimeMillis() + timeoutSec * 1000L));
        return true;
    }

    public Optional<CoreGuild> acceptInvite(Player invitee) {
        PendingInvite pending = invites.remove(invitee.getUniqueId());
        if (pending == null || System.currentTimeMillis() > pending.expiryMs) return Optional.empty();
        CoreGuild guild = byId.get(pending.guildId);
        if (guild == null) return Optional.empty();
        if (guild.rawRanks().size() >= maxMembers()) return Optional.empty();
        if (memberIndex.containsKey(invitee.getUniqueId())) return Optional.empty();
        guild.rawRanks().put(invitee.getUniqueId(), CoreGuild.RANK_MEMBER);
        memberIndex.put(invitee.getUniqueId(), guild.id());
        save(guild.id());
        return Optional.of(guild);
    }

    public boolean removeMember(CoreGuild guild, UUID memberId) {
        if (guild.rawRanks().remove(memberId) == null) return false;
        memberIndex.remove(memberId);
        if (guild.ownerId().equals(memberId)) {
            // Owner left — promote next remaining member, or disband if empty.
            if (guild.rawRanks().isEmpty()) {
                disband(guild);
                return true;
            }
            UUID newOwner = guild.rawRanks().keySet().iterator().next();
            guild.setOwnerId(newOwner);
        }
        save(guild.id());
        return true;
    }

    public void disband(CoreGuild guild) {
        for (UUID memberId : guild.rawRanks().keySet()) memberIndex.remove(memberId);
        byId.remove(guild.id());
        RpgServices.dataStore().repository(REPO).delete(guild.id().toString());
    }

    public boolean promote(CoreGuild guild, UUID memberId) {
        if (!guild.rawRanks().containsKey(memberId)) return false;
        if (memberId.equals(guild.ownerId())) return false;
        String cur = guild.rawRanks().get(memberId);
        if (!CoreGuild.RANK_MEMBER.equals(cur)) return false;
        guild.rawRanks().put(memberId, CoreGuild.RANK_OFFICER);
        save(guild.id());
        return true;
    }

    public boolean demote(CoreGuild guild, UUID memberId) {
        String cur = guild.rawRanks().get(memberId);
        if (!CoreGuild.RANK_OFFICER.equals(cur)) return false;
        guild.rawRanks().put(memberId, CoreGuild.RANK_MEMBER);
        save(guild.id());
        return true;
    }

    public void deposit(CoreGuild guild, BigDecimal amount) {
        guild.setBankBalance(guild.bankBalance().add(amount));
        save(guild.id());
    }

    public boolean withdraw(CoreGuild guild, BigDecimal amount) {
        if (guild.bankBalance().compareTo(amount) < 0) return false;
        guild.setBankBalance(guild.bankBalance().subtract(amount));
        save(guild.id());
        return true;
    }

    public void addXp(CoreGuild guild, long amount) {
        guild.setTotalXp(guild.totalXp() + amount);
        save(guild.id());
    }

    /** Routes a fraction of a member's skill XP gain to the guild bank of XP. */
    public void addXpFromSkill(Player member, long skillXpAmount) {
        if (!plugin.getConfig().getBoolean("xp-sharing.enabled", true)) return;
        double ratePercent = plugin.getConfig().getDouble("xp-sharing.rate-percent", 10.0);
        long contribution = Math.max(1L, (long) (skillXpAmount * ratePercent / 100.0));
        guildOf(member).ifPresent(g -> addXp((CoreGuild) g, contribution));
    }

    @Override
    public int guildLevel(Guild guild) {
        if (guild == null) return 0;
        String curve = plugin.getConfig().getString("guild-curve", "level * level * 1000");
        int maxLevel = plugin.getConfig().getInt("guild-max-level", 25);
        long remaining = guild.totalXp();
        try {
            for (int lv = 1; lv <= maxLevel; lv++) {
                long cost = (long) RpgServices.expressions().evaluate(curve, Map.of("level", (double) lv));
                if (remaining < cost) return lv;
                remaining -= cost;
            }
        } catch (Exception ignored) {}
        return maxLevel;
    }

    @Override
    public Map<Stat, Double> perkStatsFor(OfflinePlayer player) {
        Optional<Guild> opt = guildOf(player);
        if (opt.isEmpty()) return Map.of();
        int level = guildLevel(opt.get());
        if (level <= 0) return Map.of();

        ConfigurationSection perks = plugin.getConfig().getConfigurationSection("guild-perks");
        if (perks == null) return Map.of();

        Map<Stat, Double> result = new HashMap<>();
        Map<String, Double> vars = Map.of("level", (double) level);
        for (String statId : perks.getKeys(false)) {
            String formula = perks.getString(statId);
            if (formula == null || formula.isBlank()) continue;
            RpgServices.stats().get(statId).ifPresent(stat -> {
                try {
                    double value = RpgServices.expressions().evaluate(formula, vars);
                    if (value != 0) result.put(stat, value);
                } catch (Exception ignored) {}
            });
        }
        return result;
    }

    public int maxMembers() {
        return plugin.getConfig().getInt("max-members", 50);
    }

    public void cleanExpiredInvites() {
        long now = System.currentTimeMillis();
        invites.values().removeIf(p -> p.expiryMs < now);
    }

    private record PendingInvite(UUID guildId, long expiryMs) {}
}
