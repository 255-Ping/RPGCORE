package com.github._255_ping.rpg.core.player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds per-player preferences that are persisted to the player data file.
 *
 * <p>All fields default to {@code true} (features enabled) on first load. The service is
 * populated by {@link PlayerLifecycleListener} on join and flushed back to the DataStore on quit.
 *
 * <p>Current preferences:
 * <ul>
 *   <li><b>partyHudEnabled</b> — show/hide the action-bar party HP display (rpg-parties)</li>
 *   <li><b>soundEffectsEnabled</b> — mute/unmute RPG ability and UI sounds</li>
 *   <li><b>damageNumbersEnabled</b> — show/hide floating damage indicator holograms</li>
 *   <li><b>showOnLeaderboard</b> — opt in/out of public leaderboards</li>
 * </ul>
 */
public final class PlayerPreferencesService {

    private final ConcurrentHashMap<UUID, PlayerPreferences> prefs = new ConcurrentHashMap<>();

    /** Returns preferences for {@code uuid}, creating defaults if this is the first call. */
    public PlayerPreferences get(UUID uuid) {
        return prefs.computeIfAbsent(uuid, k -> new PlayerPreferences());
    }

    /** Overwrites stored preferences for {@code uuid}. Used by load. */
    public void put(UUID uuid, PlayerPreferences p) {
        prefs.put(uuid, p);
    }

    /** Removes cached preferences on player quit. */
    public void remove(UUID uuid) {
        prefs.remove(uuid);
    }

    // ── DTO ────────────────────────────────────────────────────────────────────

    /** Simple mutable preferences bag. All flags default to {@code true}. */
    public static final class PlayerPreferences {
        public boolean partyHudEnabled      = true;
        public boolean soundEffectsEnabled  = true;
        public boolean damageNumbersEnabled = true;
        public boolean showOnLeaderboard    = true;

        /** Serialises to a map suitable for YAML storage. */
        public Map<String, Object> toMap() {
            return Map.of(
                    "party-hud",           partyHudEnabled,
                    "sound-effects",       soundEffectsEnabled,
                    "damage-numbers",      damageNumbersEnabled,
                    "show-on-leaderboard", showOnLeaderboard
            );
        }

        /** Populates from a YAML-deserialized map; missing keys keep defaults. */
        public static PlayerPreferences fromMap(Map<?, ?> map) {
            PlayerPreferences p = new PlayerPreferences();
            if (map.get("party-hud")           instanceof Boolean b) p.partyHudEnabled      = b;
            if (map.get("sound-effects")        instanceof Boolean b) p.soundEffectsEnabled  = b;
            if (map.get("damage-numbers")       instanceof Boolean b) p.damageNumbersEnabled = b;
            if (map.get("show-on-leaderboard")  instanceof Boolean b) p.showOnLeaderboard    = b;
            return p;
        }
    }
}
