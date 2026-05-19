package com.github._255_ping.rpg.core.player;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.persistence.DataStore;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.api.stats.Stat;
import com.github._255_ping.rpg.core.health.CoreHealthService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads RpgPlayer state from DataStore on join and saves it on quit. Initializes the
 * heart-as-percent display via CoreHealthService.
 *
 * <p>Player file schema (under {@code data/players/<uuid>.yml}):
 * <pre>
 * schema-version: 1
 * hp: 87.5
 * mana: 95.0
 * </pre>
 * Base stats come from {@code starting-state.base-stats} in core config, applied each
 * session — they're not stored per-player.
 */
public final class PlayerLifecycleListener implements Listener {

    private static final int CURRENT_SCHEMA = 1;
    private static final String REPO_NAME = "players";

    private final JavaPlugin plugin;
    private final CorePlayerLookup lookup;
    private final CoreHealthService health;

    public PlayerLifecycleListener(JavaPlugin plugin, CorePlayerLookup lookup, CoreHealthService health) {
        this.plugin = plugin;
        this.lookup = lookup;
        this.health = health;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        CoreRpgPlayer rp = lookup.getOrCreate(player);
        applyBaseStats(rp);
        rp.recalculateStats();

        DataStore.Repository repo = RpgServices.dataStore().repository(REPO_NAME);
        Optional<Map<String, Object>> existing = repo.get(player.getUniqueId().toString());

        double maxHp = rp.get(BuiltinStat.MAX_HEALTH);
        double maxMana = rp.get(BuiltinStat.MAX_MANA);
        double hp = maxHp;
        double mana = maxMana;

        if (existing.isPresent()) {
            Map<String, Object> data = existing.get();
            hp = numberOr(data.get("hp"), maxHp);
            mana = numberOr(data.get("mana"), maxMana);
        }

        rp.setMana(mana);
        health.initPlayer(player, maxHp, hp);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        RpgPlayer rp = RpgServices.player(player);

        Map<String, Object> data = new HashMap<>();
        data.put("schema-version", CURRENT_SCHEMA);
        data.put("hp", health.currentHp(player));
        data.put("mana", rp.mana());
        RpgServices.dataStore().repository(REPO_NAME).save(player.getUniqueId().toString(), data);

        lookup.remove(player);
        health.removeEntity(player);
    }

    private void applyBaseStats(CoreRpgPlayer rp) {
        rp.clearBaseStats();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("starting-state.base-stats");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            Optional<Stat> stat = RpgServices.stats().get(id);
            if (stat.isEmpty()) {
                plugin.getLogger().warning("starting-state.base-stats references unknown stat: " + id);
                continue;
            }
            rp.setBaseStat(stat.get(), sec.getDouble(id));
        }
    }

    private static double numberOr(Object o, double fallback) {
        if (o instanceof Number n) return n.doubleValue();
        return fallback;
    }
}
