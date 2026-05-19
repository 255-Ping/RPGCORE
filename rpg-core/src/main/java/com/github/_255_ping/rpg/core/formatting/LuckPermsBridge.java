package com.github._255_ping.rpg.core.formatting;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.OfflinePlayer;

/**
 * Wraps LuckPerms lookups in a separate class so the JVM doesn't try to load LP types
 * until this class is instantiated — which {@link CoreNameFormatter} only does after
 * checking that LuckPerms is enabled.
 */
final class LuckPermsBridge {

    private final LuckPerms api;

    LuckPermsBridge() {
        this.api = LuckPermsProvider.get();
    }

    String prefix(OfflinePlayer player) {
        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";
        CachedMetaData meta = user.getCachedData().getMetaData();
        String prefix = meta.getPrefix();
        return prefix == null ? "" : prefix;
    }

    String suffix(OfflinePlayer player) {
        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";
        CachedMetaData meta = user.getCachedData().getMetaData();
        String suffix = meta.getSuffix();
        return suffix == null ? "" : suffix;
    }
}
