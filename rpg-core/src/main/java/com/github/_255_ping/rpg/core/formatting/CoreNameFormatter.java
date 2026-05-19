package com.github._255_ping.rpg.core.formatting;

import com.github._255_ping.rpg.api.formatting.NameFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public final class CoreNameFormatter implements NameFormatter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final LuckPermsBridge bridge;

    public CoreNameFormatter() {
        boolean lpLoaded = Bukkit.getPluginManager().isPluginEnabled("LuckPerms");
        this.bridge = lpLoaded ? safeBridge() : null;
    }

    private static LuckPermsBridge safeBridge() {
        try {
            return new LuckPermsBridge();
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public String format(OfflinePlayer player) {
        String name = player.getName() == null ? player.getUniqueId().toString() : player.getName();
        return prefix(player) + name + suffix(player);
    }

    @Override
    public Component formatComponent(OfflinePlayer player) {
        return LEGACY.deserialize(format(player));
    }

    @Override
    public String prefix(OfflinePlayer player) {
        return bridge == null ? "" : bridge.prefix(player);
    }

    @Override
    public String suffix(OfflinePlayer player) {
        return bridge == null ? "" : bridge.suffix(player);
    }
}
