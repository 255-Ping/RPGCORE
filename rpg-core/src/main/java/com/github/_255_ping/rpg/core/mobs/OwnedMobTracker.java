package com.github._255_ping.rpg.core.mobs;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks mobs spawned by {@code spawn_mob{owned=true}} and associates them with their caster.
 *
 * <ul>
 *   <li>Owned mobs are tagged with the caster's UUID in PDC under key {@code rpg_mob_owner}.
 *   <li>On caster disconnect, all their owned mobs are removed from the world.
 *   <li>On mob death, it is automatically de-registered.
 * </ul>
 *
 * <p>Register as a Bukkit listener in {@code RpgCorePlugin.onEnable}.
 * Access the singleton via {@link #get()}.
 */
public final class OwnedMobTracker implements Listener {

    /** PDC key written to each owned mob's entity. Value = caster UUID string. */
    public static final String OWNER_KEY_NAME = "rpg_mob_owner";

    private static OwnedMobTracker instance;

    private final NamespacedKey ownerKey;
    /** caster UUID → set of owned entity UUIDs */
    private final Map<UUID, Set<UUID>> casterToMobs = new HashMap<>();
    /** entity UUID → caster UUID (reverse index for fast death cleanup) */
    private final Map<UUID, UUID> mobToCaster = new HashMap<>();

    public OwnedMobTracker(Plugin plugin) {
        this.ownerKey = new NamespacedKey(plugin, OWNER_KEY_NAME);
        instance = this;
    }

    public static OwnedMobTracker get() { return instance; }

    public NamespacedKey ownerKey() { return ownerKey; }

    // ── API ───────────────────────────────────────────────────────────────────

    /**
     * Returns how many owned mobs the caster currently has alive.
     * Used by {@code SpawnMobEffect} to enforce the per-caster cap.
     */
    public int countOwned(UUID casterId) {
        Set<UUID> set = casterToMobs.get(casterId);
        return set == null ? 0 : set.size();
    }

    /**
     * Registers a newly spawned owned mob.
     *
     * @param casterId   the caster's UUID
     * @param entityUUID the spawned entity's UUID
     */
    public void track(UUID casterId, UUID entityUUID) {
        casterToMobs.computeIfAbsent(casterId, k -> new HashSet<>()).add(entityUUID);
        mobToCaster.put(entityUUID, casterId);
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        despawnAll(event.getPlayer().getUniqueId());
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void remove(UUID entityUUID) {
        UUID caster = mobToCaster.remove(entityUUID);
        if (caster == null) return;
        Set<UUID> set = casterToMobs.get(caster);
        if (set != null) {
            set.remove(entityUUID);
            if (set.isEmpty()) casterToMobs.remove(caster);
        }
    }

    /** Removes all alive owned mobs from the world when the caster logs out. */
    private void despawnAll(UUID casterId) {
        Set<UUID> mobs = casterToMobs.remove(casterId);
        if (mobs == null) return;
        for (UUID id : mobs) {
            mobToCaster.remove(id);
            Entity e = Bukkit.getEntity(id);
            if (e != null && !e.isDead()) e.remove();
        }
    }
}
