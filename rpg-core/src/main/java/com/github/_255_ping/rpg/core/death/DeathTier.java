package com.github._255_ping.rpg.core.death;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class DeathTier {

    public final String permission;
    public final int dropItemsPercent;
    public final String dropCoinsAmount;        // "0" | "all" | "<n>" | "<n>percent"
    public final boolean keepXp;

    public DeathTier(String permission, int dropItemsPercent, String dropCoinsAmount, boolean keepXp) {
        this.permission = permission;
        this.dropItemsPercent = dropItemsPercent;
        this.dropCoinsAmount = dropCoinsAmount;
        this.keepXp = keepXp;
    }

    public static DeathTier from(ConfigurationSection s) {
        String perm = s.getString("permission", "");
        int drop = s.getInt("drop-items-percent", 100);
        String coins = s.getString("drop-coins-amount", "0");
        boolean keepXp = s.getBoolean("keep-xp", false);
        return new DeathTier(perm, drop, coins, keepXp);
    }

    public boolean applies(Player p) {
        return permission.isEmpty() || p.hasPermission(permission);
    }
}
