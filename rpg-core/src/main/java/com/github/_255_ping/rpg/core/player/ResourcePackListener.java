package com.github._255_ping.rpg.core.player;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Sends the server resource pack to players when they join.
 *
 * <p>Configure via {@code resource-pack:} in rpg-core's {@code config.yml}:
 * <pre>
 *   resource-pack:
 *     enabled: true
 *     url: "https://example.com/pack.zip"
 *     sha1: "abc123..."   # hex SHA-1 of the pack; empty string = skip hash check
 *     prompt: "&7This server uses a custom resource pack."
 *     required: false     # true = kick players who decline
 * </pre>
 */
public final class ResourcePackListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;

    public ResourcePackListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation") // setResourcePack(url, hash, required, prompt) is deprecated
                                     // in favour of ResourcePackRequest, but remains reliable
                                     // across minor Paper versions without additional builder imports.
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", false)) return;

        Player p = e.getPlayer();
        String url      = plugin.getConfig().getString("resource-pack.url", "");
        String hash     = plugin.getConfig().getString("resource-pack.sha1", "");
        boolean required = plugin.getConfig().getBoolean("resource-pack.required", false);
        String promptStr = plugin.getConfig().getString("resource-pack.prompt", "");

        if (url.isBlank()) return;

        var prompt = promptStr.isBlank() ? null : LEGACY.deserialize(promptStr);
        p.setResourcePack(url, hash, required, prompt);
    }
}
