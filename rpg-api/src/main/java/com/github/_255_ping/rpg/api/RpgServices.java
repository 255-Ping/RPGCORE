package com.github._255_ping.rpg.api;

import com.github._255_ping.rpg.api.abilities.AbilityRegistry;
import com.github._255_ping.rpg.api.items.ItemRegistry;
import com.github._255_ping.rpg.api.mobs.MobRegistry;
import com.github._255_ping.rpg.api.player.ManaService;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import com.github._255_ping.rpg.api.stats.StatRegistry;
import org.bukkit.entity.Player;

public final class RpgServices {

    private static StatRegistry stats;
    private static ItemRegistry items;
    private static MobRegistry mobs;
    private static AbilityRegistry abilities;
    private static ManaService mana;
    private static PlayerLookup players;

    private RpgServices() {}

    public static StatRegistry stats() { return require(stats, "StatRegistry"); }
    public static ItemRegistry items() { return require(items, "ItemRegistry"); }
    public static MobRegistry mobs() { return require(mobs, "MobRegistry"); }
    public static AbilityRegistry abilities() { return require(abilities, "AbilityRegistry"); }
    public static ManaService mana() { return require(mana, "ManaService"); }
    public static RpgPlayer player(Player p) { return require(players, "PlayerLookup").get(p); }

    public static void bootstrap(
            StatRegistry stats,
            ItemRegistry items,
            MobRegistry mobs,
            AbilityRegistry abilities,
            ManaService mana,
            PlayerLookup players
    ) {
        RpgServices.stats = stats;
        RpgServices.items = items;
        RpgServices.mobs = mobs;
        RpgServices.abilities = abilities;
        RpgServices.mana = mana;
        RpgServices.players = players;
    }

    private static <T> T require(T svc, String name) {
        if (svc == null) throw new IllegalStateException(name + " not bootstrapped — is rpg-core loaded?");
        return svc;
    }

    @FunctionalInterface
    public interface PlayerLookup {
        RpgPlayer get(Player player);
    }
}
