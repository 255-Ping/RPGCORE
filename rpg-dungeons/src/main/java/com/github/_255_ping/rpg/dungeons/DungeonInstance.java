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
    public final long createdEpochMillis = System.currentTimeMillis();

    public DungeonInstance(String dungeonId, Vector origin) {
        this.dungeonId = dungeonId;
        this.originInInstanceWorld = origin;
    }
}
