package com.github._255_ping.rpg.hud;

import com.github._255_ping.rpg.api.skills.SkillXpAwardEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the most recently awarded skill XP per player for the {@code {recent_xp}} placeholder.
 * Gains for the same skill within the fade window are accumulated; a different skill resets the display.
 */
public final class RecentXpTracker implements Listener {

    private record XpGain(String skillId, long amount, long expireAtMs) {}

    private final Map<UUID, XpGain> recent = new ConcurrentHashMap<>();
    private final long fadeMs;

    public RecentXpTracker(long fadeMs) {
        this.fadeMs = fadeMs;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onXpAward(SkillXpAwardEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        long now = System.currentTimeMillis();
        XpGain existing = recent.get(id);
        if (existing != null && existing.skillId().equals(event.skillId()) && now < existing.expireAtMs()) {
            recent.put(id, new XpGain(event.skillId(), existing.amount() + event.amount(), now + fadeMs));
        } else {
            recent.put(id, new XpGain(event.skillId(), event.amount(), now + fadeMs));
        }
    }

    /** Returns a formatted recent-XP string, or {@code ""} if the last gain has faded. */
    public String get(UUID playerId) {
        XpGain gain = recent.get(playerId);
        if (gain == null) return "";
        if (System.currentTimeMillis() > gain.expireAtMs()) {
            recent.remove(playerId);
            return "";
        }
        return "§a+" + gain.amount() + " §7XP §8(" + gain.skillId() + ")";
    }

    public void onQuit(UUID playerId) {
        recent.remove(playerId);
    }
}
