package com.github._255_ping.rpg.kits;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.persistence.DataStore;

import java.util.*;

/**
 * Persists per-player kit claim records.
 *
 * <p>Stored in DataStore repository {@code "kit_claims"}, keyed by player UUID string.
 * Shape: {@code { "claimed": ["starter", ...], "cooldowns": { "daily": <epoch-ms> } }}
 *
 * <ul>
 *   <li>{@code claimed} — one-time kits the player has already received; never receive again.</li>
 *   <li>{@code cooldowns} — cooldown-based kits: maps kit ID → next-available epoch-ms.</li>
 * </ul>
 */
public final class KitClaimStore {

    private static final String REPO = "kit_claims";

    // -------------------------------------------------------------------------
    // One-time claims
    // -------------------------------------------------------------------------

    public boolean hasClaimed(UUID uuid, String kitId) {
        return getClaimed(uuid).contains(kitId.toLowerCase(Locale.ROOT));
    }

    public void markClaimed(UUID uuid, String kitId) {
        Map<String, Object> data = loadRaw(uuid);
        @SuppressWarnings("unchecked")
        List<String> claimed = (List<String>) data.computeIfAbsent("claimed", k -> new ArrayList<>());
        String key = kitId.toLowerCase(Locale.ROOT);
        if (!claimed.contains(key)) claimed.add(key);
        save(uuid, data);
    }

    public void resetClaim(UUID uuid, String kitId) {
        Map<String, Object> data = loadRaw(uuid);
        @SuppressWarnings("unchecked")
        List<String> claimed = (List<String>) data.getOrDefault("claimed", new ArrayList<>());
        claimed.remove(kitId.toLowerCase(Locale.ROOT));
        data.put("claimed", claimed);
        save(uuid, data);
    }

    /** Clears ALL claim state for a player. */
    public void resetAll(UUID uuid) {
        repo().delete(uuid.toString());
    }

    // -------------------------------------------------------------------------
    // Cooldown claims
    // -------------------------------------------------------------------------

    /** Milliseconds until the cooldown expires; 0 if the kit is ready to claim. */
    public long cooldownRemainingMs(UUID uuid, String kitId) {
        long next = getNextAvailable(uuid, kitId);
        return Math.max(0L, next - System.currentTimeMillis());
    }

    public void setCooldown(UUID uuid, String kitId, int cooldownSeconds) {
        Map<String, Object> data = loadRaw(uuid);
        @SuppressWarnings("unchecked")
        Map<String, Object> cooldowns = (Map<String, Object>) data.computeIfAbsent("cooldowns", k -> new HashMap<>());
        cooldowns.put(kitId.toLowerCase(Locale.ROOT),
                System.currentTimeMillis() + cooldownSeconds * 1000L);
        save(uuid, data);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private List<String> getClaimed(UUID uuid) {
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) loadRaw(uuid).getOrDefault("claimed", List.of());
        return list;
    }

    private long getNextAvailable(UUID uuid, String kitId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> cooldowns = (Map<String, Object>) loadRaw(uuid).getOrDefault("cooldowns", Map.of());
        Object val = cooldowns.get(kitId.toLowerCase(Locale.ROOT));
        return val instanceof Number n ? n.longValue() : 0L;
    }

    private Map<String, Object> loadRaw(UUID uuid) {
        return repo().get(uuid.toString()).map(HashMap::new).orElseGet(HashMap::new);
    }

    private void save(UUID uuid, Map<String, Object> data) {
        repo().save(uuid.toString(), data);
    }

    private DataStore.Repository repo() {
        return RpgServices.dataStore().repository(REPO);
    }
}
