package com.github._255_ping.rpg.chat;

import com.github._255_ping.rpg.api.RpgServices;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChatFormatListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final RpgChatPlugin chatPlugin;

    public ChatFormatListener(RpgChatPlugin chatPlugin) {
        this.plugin = chatPlugin;
        this.chatPlugin = chatPlugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player p = event.getPlayer();

        if (chatPlugin.isMuted() && !p.hasPermission("rpg.chat.mute.bypass")) {
            event.setCancelled(true);
            p.sendMessage(LEGACY.deserialize("&cChat is currently muted."));
            return;
        }

        String template = plugin.getConfig().getString("chat-format",
                "{prefix}{name}{suffix} &7» &f{message}");

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        String prefix = safe(() -> RpgServices.nameFormatter().prefix(p));
        String suffix = safe(() -> RpgServices.nameFormatter().suffix(p));

        String rendered = template
                .replace("{prefix}", prefix)
                .replace("{name}", p.getName())
                .replace("{suffix}", suffix)
                .replace("{world}", p.getWorld().getName())
                .replace("{message}", message);

        event.renderer((source, sourceDisplayName, msg, viewer) ->
                LEGACY.deserialize(rendered));
    }

    private static String safe(java.util.function.Supplier<String> sup) {
        try { String s = sup.get(); return s == null ? "" : s; }
        catch (Exception ex) { return ""; }
    }

}
