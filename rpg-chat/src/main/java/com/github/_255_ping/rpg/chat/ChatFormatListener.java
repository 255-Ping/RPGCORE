package com.github._255_ping.rpg.chat;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.guilds.Guild;
import com.github._255_ping.rpg.api.parties.Party;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

        String channel = chatPlugin.activeChannel(p);
        Set<Audience> recipients = pickRecipients(p, channel);
        if (recipients != null) {
            event.viewers().clear();
            event.viewers().addAll(recipients);
        }

        String template = plugin.getConfig().getString("chat-format-" + channel,
                plugin.getConfig().getString("chat-format",
                        "{prefix}{name}{suffix} &7» &f{message}"));
        String channelPrefix = plugin.getConfig().getString("channel-prefix-" + channel, "");

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        String prefix = safe(() -> RpgServices.nameFormatter().prefix(p));
        String suffix = safe(() -> RpgServices.nameFormatter().suffix(p));

        String rendered = (channelPrefix.isEmpty() ? "" : channelPrefix) + template
                .replace("{prefix}", prefix)
                .replace("{name}", p.getName())
                .replace("{suffix}", suffix)
                .replace("{world}", p.getWorld().getName())
                .replace("{message}", message);

        event.renderer((source, sourceDisplayName, msg, viewer) ->
                LEGACY.deserialize(rendered));
    }

    private Set<Audience> pickRecipients(Player sender, String channel) {
        return switch (channel) {
            case "party" -> {
                Set<Audience> out = new HashSet<>();
                out.add(sender);
                try {
                    Optional<Party> p = RpgServices.parties().partyOf(sender);
                    p.ifPresent(party -> out.addAll(party.members()));
                } catch (IllegalStateException ignored) {}
                yield out;
            }
            case "guild" -> {
                Set<Audience> out = new HashSet<>();
                out.add(sender);
                try {
                    Optional<Guild> g = RpgServices.guilds().guildOf(sender);
                    g.ifPresent(guild -> {
                        for (UUID id : guild.memberIds()) {
                            Player p = Bukkit.getPlayer(id);
                            if (p != null) out.add(p);
                        }
                    });
                } catch (IllegalStateException ignored) {}
                yield out;
            }
            case "staff" -> {
                // Only players who hold the rpg.chat.use.staff permission can see staff messages.
                Set<Audience> out = new HashSet<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("rpg.chat.use.staff")) out.add(p);
                }
                // Always include sender so they see their own message even if perm is lost mid-session.
                out.add(sender);
                yield out;
            }
            default -> null;  // global = use default broadcast
        };
    }

    private static String safe(java.util.function.Supplier<String> sup) {
        try { String s = sup.get(); return s == null ? "" : s; }
        catch (Exception ex) { return ""; }
    }
}
