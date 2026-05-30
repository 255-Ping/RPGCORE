package com.github._255_ping.rpg.npcs;

import com.google.common.collect.HashMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

/**
 * Spawns and manages NMS-level fake player entities.
 * The entity appears as a player model with a skin but is NOT in the server's PlayerList
 * and NOT shown in the tab list after initial skin-load tick.
 */
public final class FakePlayerNpc {

    private FakePlayerNpc() {}

    /**
     * Spawn a fake player NPC into the world. Tags it with the NPC id key so NpcManager can
     * find it via PDC. Returns the Bukkit entity, or null if the world is unavailable.
     */
    public static Entity spawn(JavaPlugin plugin, NpcDef def, NamespacedKey npcIdKey) {
        var loc = def.location();
        if (loc == null || loc.getWorld() == null) return null;

        MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel nmsLevel = ((CraftWorld) loc.getWorld()).getHandle();

        UUID uuid = UUID.nameUUIDFromBytes(("rpg_npc:" + def.id()).getBytes(java.nio.charset.StandardCharsets.UTF_8));

        PropertyMap properties = new PropertyMap(HashMultimap.create());
        NpcDef.SkinDef skin = def.skin();
        if (skin != null && skin.value() != null && skin.signature() != null) {
            properties.put("textures", new Property("textures", skin.value(), skin.signature()));
        }
        GameProfile profile = new GameProfile(uuid, truncateName(def.id()), properties);

        ServerPlayer fakePlayer = new ServerPlayer(nmsServer, nmsLevel, profile, ClientInformation.createDefault());
        fakePlayer.setPos(def.x(), def.y(), def.z());
        fakePlayer.setYRot(def.yaw());
        fakePlayer.setXRot(def.pitch());
        fakePlayer.setInvulnerable(true);
        fakePlayer.noPhysics = true;
        fakePlayer.setNoGravity(true);
        fakePlayer.setSilent(true);

        // Tag with NPC id before adding to world so interact listener can resolve it immediately
        Entity bukkit = fakePlayer.getBukkitEntity();
        bukkit.getPersistentDataContainer().set(npcIdKey, PersistentDataType.STRING, def.id());
        bukkit.setPersistent(true);

        nmsLevel.addFreshEntity(fakePlayer);

        // Brief tab-list add so clients load the skin texture, then remove after 2 ticks
        sendTabListAdd(fakePlayer);
        Bukkit.getScheduler().runTaskLater(plugin, () -> sendTabListRemove(uuid), 2L);

        return bukkit;
    }

    /** Send to a single newly-joined player so they see the skin; tab-list entry removed after 2 ticks. */
    public static void sendSkinToJoiningPlayer(JavaPlugin plugin, ServerPlayer fakePlayer, Player joiningPlayer) {
        sendTabListAddTo(fakePlayer, joiningPlayer);
        Bukkit.getScheduler().runTaskLater(plugin,
            () -> sendTabListRemoveTo(fakePlayer.getUUID(), joiningPlayer), 2L);
    }

    private static void sendTabListAdd(ServerPlayer fakePlayer) {
        var packet = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(fakePlayer));
        for (Player p : Bukkit.getOnlinePlayers()) {
            ((CraftPlayer) p).getHandle().connection.send(packet);
        }
    }

    private static void sendTabListAddTo(ServerPlayer fakePlayer, Player target) {
        var packet = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(fakePlayer));
        ((CraftPlayer) target).getHandle().connection.send(packet);
    }

    private static void sendTabListRemove(UUID uuid) {
        var packet = new ClientboundPlayerInfoRemovePacket(List.of(uuid));
        for (Player p : Bukkit.getOnlinePlayers()) {
            ((CraftPlayer) p).getHandle().connection.send(packet);
        }
    }

    private static void sendTabListRemoveTo(UUID uuid, Player target) {
        if (!target.isOnline()) return;
        var packet = new ClientboundPlayerInfoRemovePacket(List.of(uuid));
        ((CraftPlayer) target).getHandle().connection.send(packet);
    }

    /** GameProfile names are capped at 16 chars. */
    private static String truncateName(String id) {
        return id.length() > 16 ? id.substring(0, 16) : id;
    }
}
