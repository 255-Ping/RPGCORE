package com.github._255_ping.rpg.hud;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One TextDisplay per online player, anchored above the head via the player as a passenger. The
 * display follows automatically (no per-tick teleport needed) and shows the configured nametag
 * format with prefix/suffix.
 */
public final class NametagManager {

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
        UUID id = displayOf.remove(p.getUniqueId());
        if (id == null) return;
        org.bukkit.entity.Entity ent = org.bukkit.Bukkit.getEntity(id);
        if (ent != null) ent.remove();
    }

    public void tick(Player p) {
        UUID displayId = displayOf.get(p.getUniqueId());
        if (displayId == null) return;
        if (!(org.bukkit.Bukkit.getEntity(displayId) instanceof TextDisplay td)) {
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

    private void spawn(Player p) {
        if (!p.isOnline()) return;
        double yOffset = plugin.getConfig().getDouble("nametag.y-offset", 0.4);
        Location loc = p.getLocation().clone().add(0, p.getHeight() + yOffset, 0);
        if (loc.getWorld() == null) return;
        TextDisplay td = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        td.setBillboard(Display.Billboard.CENTER);
        td.setPersistent(false);
        String fmt = plugin.getConfig().getString("nametag.format", "{prefix} {name} {suffix}");
        td.text(LEGACY.deserialize(PlaceholderResolver.resolve(p, fmt)));
        p.addPassenger(td);
        displayOf.put(p.getUniqueId(), td.getUniqueId());
    }
}
