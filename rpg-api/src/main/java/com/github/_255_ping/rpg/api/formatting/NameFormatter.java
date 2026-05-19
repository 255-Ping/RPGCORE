package com.github._255_ping.rpg.api.formatting;

import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;

public interface NameFormatter {
    String format(OfflinePlayer player);
    Component formatComponent(OfflinePlayer player);
    String prefix(OfflinePlayer player);
    String suffix(OfflinePlayer player);
}
