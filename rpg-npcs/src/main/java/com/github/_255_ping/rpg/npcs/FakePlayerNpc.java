package com.github._255_ping.rpg.npcs;

import com.google.common.collect.HashMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Packet-based fake player NPC.
 *
 * The ServerPlayer is NEVER added to the world — doing so causes crashes because Paper's entity
 * tracker calls player.connection.send() and connection is null.
 *
 * Instead we:
 *  1. Allocate a fake entity ID via Entity.nextEntityId().
 *  2. Send ClientboundPlayerInfoUpdatePacket (ADD_PLAYER with skin, listed=false) to load skin.
 *  3. Send ClientboundAddEntityPacket(PLAYER) to spawn the client-side model.
 *  4. Schedule ClientboundPlayerInfoRemovePacket after 2 ticks (tab list gone, skin cached).
 *  5. Spawn an invisible Interaction entity at the same spot for server-side click detection.
 *
 * On despawn: ClientboundRemoveEntitiesPacket + ClientboundPlayerInfoRemovePacket to all players.
 * On player join: re-send steps 1-4 so they see the NPC.
 */
public final class FakePlayerNpc {

    private FakePlayerNpc() {}

    /** State per active fake-player NPC. */
    public record State(int entityId, UUID uuid, org.bukkit.entity.Entity interactionEntity) {}

    /**
     * Spawn the fake player NPC. Returns the `State` containing the entity ID and interaction entity.
     * The interaction entity is tagged with the NPC id key so NpcInteractListener can find it.
     */
    public static State spawn(JavaPlugin plugin, NpcDef def, NamespacedKey npcIdKey) {
        Location loc = def.location();
        if (loc == null || loc.getWorld() == null) return null;

        UUID uuid = deterministicUuid(def.id());
        int entityId = Entity.nextEntityId();
        GameProfile profile = buildProfile(uuid, def.id(), def.skin());

        // Send skin/tab-list entry + entity spawn to all online players
        sendToAll(plugin, uuid, entityId, profile, def);

        // Spawn invisible Interaction entity for server-side click detection
        Interaction interaction = loc.getWorld().spawn(loc, Interaction.class, e -> {
            e.getPersistentDataContainer().set(npcIdKey, PersistentDataType.STRING, def.id());
            e.setPersistent(true);
            e.setInteractionWidth(0.6f);
            e.setInteractionHeight(1.8f);
            e.setResponsive(true);
        });

        return new State(entityId, uuid, interaction);
    }

    /** Send the NPC to a player who just joined. */
    public static void sendToPlayer(JavaPlugin plugin, Player joiner, NpcDef def, State state) {
        sendNpcToPlayer(plugin, joiner, state.uuid(), state.entityId(),
            buildProfile(state.uuid(), def.id(), def.skin()), def);
    }

    /** Remove the fake player entity from all clients and remove the interaction entity. */
    public static void despawn(JavaPlugin plugin, State state) {
        var removeEntities = new ClientboundRemoveEntitiesPacket(new int[]{state.entityId()});
        var removeInfo = new ClientboundPlayerInfoRemovePacket(List.of(state.uuid()));
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendPacket(p, removeEntities);
            sendPacket(p, removeInfo);
        }
        if (state.interactionEntity() != null && state.interactionEntity().isValid()) {
            state.interactionEntity().remove();
        }
    }

    // ---- look-at rotation ----

    /**
     * Send a body + head rotation update for the fake player to the given viewers.
     * Called every 2 ticks by NpcManager's look-at task for PLAYER-style NPCs.
     *
     * ClientboundRotateHeadPacket only has a public constructor that takes a real Entity,
     * so we access the private (int, byte) constructor via reflection — it is always present
     * because the STREAM_CODEC references it with a method handle.
     */
    public static void rotateHead(State state, float yaw, float pitch, List<Player> viewers) {
        if (viewers.isEmpty()) return;
        byte yRot = encodeAngle(yaw);
        byte xRot = encodeAngle(pitch);
        var bodyPacket = new ClientboundMoveEntityPacket.Rot(state.entityId(), yRot, xRot, false);
        ClientboundRotateHeadPacket headPacket = buildRotateHeadPacket(state.entityId(), yRot);
        for (Player p : viewers) {
            sendPacket(p, bodyPacket);
            if (headPacket != null) sendPacket(p, headPacket);
        }
    }

    private static byte encodeAngle(float angleDeg) {
        return (byte) Math.floor(angleDeg * 256.0f / 360.0f);
    }

    private static ClientboundRotateHeadPacket buildRotateHeadPacket(int entityId, byte yHeadRot) {
        try {
            var ctor = ClientboundRotateHeadPacket.class.getDeclaredConstructor(int.class, byte.class);
            ctor.setAccessible(true);
            return (ClientboundRotateHeadPacket) ctor.newInstance(entityId, yHeadRot);
        } catch (Exception ex) {
            return null;
        }
    }

    // ---- private helpers ----

    private static void sendToAll(JavaPlugin plugin, UUID uuid, int entityId, GameProfile profile, NpcDef def) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendNpcToPlayer(plugin, p, uuid, entityId, profile, def);
        }
    }

    private static void sendNpcToPlayer(JavaPlugin plugin, Player target, UUID uuid, int entityId,
                                        GameProfile profile, NpcDef def) {
        // 1. Add to tab list (unlisted=false keeps them off the visible list, but skin loads)
        var infoPacket = buildInfoPacket(uuid, profile);
        sendPacket(target, infoPacket);

        // 2. Spawn the player entity client-side
        var spawnPacket = buildSpawnPacket(entityId, uuid, def);
        sendPacket(target, spawnPacket);

        // 3. Remove from tab list after 2 ticks so they don't appear in the player list
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (target.isOnline()) {
                sendPacket(target, new ClientboundPlayerInfoRemovePacket(List.of(uuid)));
            }
        }, 2L);
    }

    private static ClientboundPlayerInfoUpdatePacket buildInfoPacket(UUID uuid, GameProfile profile) {
        // Use the Entry record constructor directly — avoids reading player.connection.latency()
        var entry = new ClientboundPlayerInfoUpdatePacket.Entry(
            uuid,
            profile,
            false,          // listed = false: skin loads but player is not shown in tab list
            0,              // latency
            GameType.SURVIVAL,
            null,           // displayName
            true,           // showHat
            0,              // listOrder
            null            // chatSession
        );
        return new ClientboundPlayerInfoUpdatePacket(
            EnumSet.of(
                ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT
            ),
            List.of(entry)
        );
    }

    private static ClientboundAddEntityPacket buildSpawnPacket(int entityId, UUID uuid, NpcDef def) {
        return new ClientboundAddEntityPacket(
            entityId,
            uuid,
            def.x(), def.y(), def.z(),
            def.pitch(), def.yaw(),
            EntityType.PLAYER,
            0,
            Vec3.ZERO,
            def.yaw()
        );
    }

    private static GameProfile buildProfile(UUID uuid, String npcId, NpcDef.SkinDef skin) {
        PropertyMap properties = new PropertyMap(HashMultimap.create());
        if (skin != null && skin.value() != null && skin.signature() != null) {
            properties.put("textures", new Property("textures", skin.value(), skin.signature()));
        }
        return new GameProfile(uuid, truncateName(npcId), properties);
    }

    private static void sendPacket(Player player, net.minecraft.network.protocol.Packet<?> packet) {
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    public static UUID deterministicUuid(String npcId) {
        return UUID.nameUUIDFromBytes(("rpg_npc:" + npcId).getBytes(StandardCharsets.UTF_8));
    }

    private static String truncateName(String id) {
        return id.length() > 16 ? id.substring(0, 16) : id;
    }
}
