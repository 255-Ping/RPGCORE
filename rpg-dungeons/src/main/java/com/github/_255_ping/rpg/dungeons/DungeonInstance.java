package com.github._255_ping.rpg.dungeons;

import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DungeonInstance {
    public final UUID instanceId = UUID.randomUUID();
    public final String dungeonId;
    public final Vector originInInstanceWorld;
    public final Set<UUID> players = new HashSet<>();
    public final Set<UUID> alive = new HashSet<>();
    /** Spawned-mob UUIDs that count toward the KILL_ALL_MOBS win condition. */
    public final Set<UUID> aliveMobs = new HashSet<>();
    /** Chest world-locations bound to loot tables for this instance; unbound on teardown. */
    public final Set<org.bukkit.Location> boundChests = new HashSet<>();
    public final long createdEpochMillis = System.currentTimeMillis();
    public boolean finished;

    public DungeonInstance(String dungeonId, Vector origin) {
        this.dungeonId = dungeonId;
        this.originInInstanceWorld = origin;
    }
}
