package com.github._255_ping.rpg.core.hud;

import com.github._255_ping.rpg.api.hud.ActionBarService;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory action bar priority queue. Messages stored here override the HUD's idle
 * stats format for their duration. Entries expire automatically when {@link #peek} is called.
 */
public final class CoreActionBarService implements ActionBarService {

    private record Pending(Component text, long expiryMs) {}

    private final ConcurrentHashMap<UUID, Pending> pending = new ConcurrentHashMap<>();

    @Override
    public void send(Player player, Component message, int durationTicks) {
        pending.put(player.getUniqueId(),
                new Pending(message, System.currentTimeMillis() + durationTicks * 50L));
    }

    @Override
    public Component peek(Player player) {
        Pending p = pending.get(player.getUniqueId());
        if (p == null) return null;
        if (System.currentTimeMillis() > p.expiryMs()) {
            pending.remove(player.getUniqueId());
            return null;
        }
        return p.text();
    }
}
