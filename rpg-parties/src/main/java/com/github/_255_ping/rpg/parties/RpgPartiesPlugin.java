package com.github._255_ping.rpg.parties;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.parties.Party;
import com.github._255_ping.rpg.api.skills.SkillXpAwardEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RpgPartiesPlugin extends JavaPlugin implements Listener {

    private PartyManager manager;

    /**
     * Tracks UUIDs currently undergoing XP-sharing distribution to prevent
     * recursive re-entry when {@code awardXp()} fires a new {@link SkillXpAwardEvent}
     * for each party member that receives a shared bonus.
     */
    private final ThreadLocal<Set<UUID>> sharingInProgress =
            ThreadLocal.withInitial(HashSet::new);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new PartyManager(this);
        RpgServices.setParties(manager);

        PartyCommand partyCommand = new PartyCommand(manager, this);
        var partyCmd = Objects.requireNonNull(getCommand("party"), "command 'party' missing");
        partyCmd.setExecutor(partyCommand);
        partyCmd.setTabCompleter(partyCommand);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, manager::cleanExpiredInvites, 200L, 200L);

        if (getConfig().getBoolean("party-hud.enabled", true)) {
            long interval = getConfig().getLong("party-hud.interval-ticks", 20L);
            getServer().getScheduler().runTaskTimer(this, new PartyHudTask(manager), interval, interval);
        }

        getLogger().info("rpg-parties v" + getPluginMeta().getVersion() + " enabled.");
    }

    // ── XP sharing ──────────────────────────────────────────────────────────

    /**
     * When a party member earns skill XP, bonus XP is shared with each
     * in-range party member according to the {@code xp-sharing.split-formula}.
     * The original earner's XP is unmodified; others receive the formula result
     * as a separate award.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSkillXpAward(SkillXpAwardEvent event) {
        if (!getConfig().getBoolean("xp-sharing.enabled", false)) return;

        Player earner = event.getPlayer();

        // Guard against recursive fire when we award XP to party members below.
        Set<UUID> inProgress = sharingInProgress.get();
        if (inProgress.contains(earner.getUniqueId())) return;

        // Scope check.
        if (!inScopeSkill(event.skillId())) return;

        Optional<Party> opt = manager.partyOf(earner);
        if (opt.isEmpty()) return;

        // Collect in-range members (excluding the earner).
        int rangeBlocks = getConfig().getInt("xp-sharing.range-blocks", 64);
        List<Player> recipients = new ArrayList<>();
        for (Player member : opt.get().members()) {
            if (member.getUniqueId().equals(earner.getUniqueId())) continue;
            if (!member.isOnline()) continue;
            if (rangeBlocks > 0) {
                if (!member.getWorld().equals(earner.getWorld())) continue;
                if (member.getLocation().distanceSquared(earner.getLocation())
                        > (double) rangeBlocks * rangeBlocks) continue;
            }
            recipients.add(member);
        }
        if (recipients.isEmpty()) return;

        // Evaluate the split formula.
        int partySize = recipients.size() + 1; // earner + in-range members
        double share = RpgServices.expressions().evaluate(
                getConfig().getString("xp-sharing.split-formula", "amount / party_size"),
                java.util.Map.of(
                        "amount",     (double) event.amount(),
                        "party_size", (double) partySize));
        long shareAmount = Math.max(1L, Math.round(share));

        // Award to each recipient, guarded so their events don't re-enter here.
        Set<UUID> allUuids = new HashSet<>();
        allUuids.add(earner.getUniqueId());
        for (Player r : recipients) allUuids.add(r.getUniqueId());
        inProgress.addAll(allUuids);
        try {
            for (Player recipient : recipients) {
                RpgServices.skills().awardXp(recipient, event.skillId(), shareAmount);
            }
        } finally {
            inProgress.removeAll(allUuids);
        }
    }

    /** Returns true if {@code skillId} falls within the configured sharing scope. */
    private boolean inScopeSkill(String skillId) {
        String scope = getConfig().getString("xp-sharing.scope", "all-skills");
        return switch (scope) {
            case "all-skills"  -> true;
            case "combat-only" -> "combat".equalsIgnoreCase(skillId);
            case "list"        -> getConfig().getStringList("xp-sharing.skills")
                    .stream().anyMatch(s -> s.equalsIgnoreCase(skillId));
            default            -> true;
        };
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Optional<Party> opt = manager.partyOf(event.getPlayer());
        opt.ifPresent(party -> manager.removeMember((CoreParty) party, event.getPlayer()));
    }

    @Override
    public void onDisable() {
        getLogger().info("rpg-parties disabled.");
    }
}
