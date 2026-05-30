package com.github._255_ping.rpg.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One TextDisplay per online player, anchored above the head via the player as a passenger.
 * The display follows automatically (no per-tick teleport needed) and shows the configured
 * nametag format with prefix/suffix.
 *
 * <p>Handles player death/respawn: the display is removed on death and re-attached one tick
 * after respawn so the entity reference is always valid.
 */
public final class NametagManager implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final Map<UUID, UUID> displayOf = new HashMap<>();

    public NametagManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void onJoin(Player p) {
        if (!plugin.getConfig().getBoolean("nametag.enabled", true)) return;
        // Defer one tick so the player's entity is fully initialized before we attach a passenger.
        plugin.getServer().getScheduler().runTask(plugin, () -> spawn(p));
    }

    public void onQuit(Player p) {
        removeDisplay(p.getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfig().getBoolean("nametag.enabled", true)) return;
        Player p = event.getPlayer();
        // Remove old display first (it was attached to the pre-death entity).
        removeDisplay(p.getUniqueId());
        // Re-attach one tick after respawn so the player's entity is ready.
        plugin.getServer().getScheduler().runTask(plugin, () -> spawn(p));
    }

    public void tick(Player p) {
        UUID displayId = displayOf.get(p.getUniqueId());
        if (displayId == null) return;
        if (!(org.bukkit.Bukkit.getEntity(displayId) instanceof TextDisplay td)) {
            // Display was lost (e.g., chunk unload edge case) — respawn it.
            displayOf.remove(p.getUniqueId());
            spawn(p);
            return;
        }
        boolean hideOnSneak = plugin.getConfig().getBoolean("nametag.hide-when-sneaking", true);
        if (hideOnSneak && p.isSneaking()) {
            td.setVisibleByDefault(false);
            return;
        } else {
            td.setVisibleByDefault(true);
        }
        String fmt = plugin.getConfig().getString("nametag.format", "{prefix} {name} {suffix}");
        Component text = LEGACY.deserialize(PlaceholderResolver.resolve(p, fmt));
        td.text(text);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void removeDisplay(UUID playerId) {
        UUID id = displayOf.remove(playerId);
        if (id == null) return;
        org.bukkit.entity.Entity ent = org.bukkit.Bukkit.getEntity(id);
        if (ent != null) ent.remove();
    }

    private void spawn(Player p) {
        if (!p.isOnline()) return;
        float yOffset = (float) plugin.getConfig().getDouble("nametag.y-offset", 0.5);
        Location loc = p.getLocation();
        if (loc.getWorld() == null) return;
        TextDisplay td = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        td.setBillboard(Display.Billboard.CENTER);
        td.setPersistent(false);
        td.setTransformation(new Transformation(
                new Vector3f(0f, yOffset, 0f),
                new Quaternionf(),
                new Vector3f(1f, 1f, 1f),
                new Quaternionf()));
        String fmt = plugin.getConfig().getString("nametag.format", "{prefix} {name} {suffix}");
        td.text(LEGACY.deserialize(PlaceholderResolver.resolve(p, fmt)));
        p.addPassenger(td);
        displayOf.put(p.getUniqueId(), td.getUniqueId());
    }
}
