package com.github._255_ping.rpg.core.achievement;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.achievement.AchievementDef;
import com.github._255_ping.rpg.api.achievement.AchievementReward;
import com.github._255_ping.rpg.api.achievement.AchievementService;
import com.github._255_ping.rpg.api.persistence.DataStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DataStore-backed achievement service.
 *
 * <p>Data layout in the {@code "achievements"} repository:
 * <ul>
 *   <li>Key: {@code <playerUUID>} → Map with:
 *     <ul>
 *       <li>{@code "unlocked"}: List of unlocked achievement IDs</li>
 *       <li>{@code "counters.<key>"}: long counter values stored as Long</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>State is loaded on join and saved on quit and on each grant/increment.
 */
public final class CoreAchievementService implements AchievementService, Listener {

    private static final String REPO = "achievements";
    private static final String KEY_UNLOCKED = "unlocked";
    private static final String KEY_COUNTERS = "counters";

    private final JavaPlugin plugin;
    private final Map<String, AchievementDef> byId = new LinkedHashMap<>();

    // Per-player in-memory state
    private final ConcurrentHashMap<UUID, Set<String>> unlockedByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Map<String, Long>> countersByPlayer = new ConcurrentHashMap<>();

    public CoreAchievementService(JavaPlugin plugin, List<AchievementDef> defs) {
        this.plugin = plugin;
        for (AchievementDef def : defs) byId.put(def.id(), def);
    }

    // ── AchievementService ────────────────────────────────────────────────────

    @Override
    public Collection<AchievementDef> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    @Override
    public Optional<AchievementDef> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public boolean isUnlocked(Player player, String id) {
        Set<String> unlocked = unlockedByPlayer.get(player.getUniqueId());
        return unlocked != null && unlocked.contains(id);
    }

    @Override
    public long getCounter(Player player, String counterKey) {
        Map<String, Long> counters = countersByPlayer.get(player.getUniqueId());
        if (counters == null) return 0L;
        return counters.getOrDefault(counterKey, 0L);
    }

    @Override
    public void grant(Player player, String id) {
        AchievementDef def = byId.get(id);
        if (def == null) return;
        UUID uuid = player.getUniqueId();
        Set<String> unlocked = unlockedByPlayer.computeIfAbsent(uuid, k -> new HashSet<>());
        if (!unlocked.add(id)) return; // already unlocked

        applyReward(player, def.reward());
        notifyPlayer(player, def);
        persist(player);
    }

    @Override
    public void increment(Player player, String counterKey, long delta) {
        if (delta <= 0) return;
        UUID uuid = player.getUniqueId();
        Map<String, Long> counters = countersByPlayer.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        long newValue = counters.merge(counterKey, delta, Long::sum);

        // Check all COUNTER achievements listening to this key
        for (AchievementDef def : byId.values()) {
            if (!def.isCounter()) continue;
            if (!counterKey.equals(def.counterKey())) continue;
            if (newValue >= def.target()) {
                grant(player, def.id());
            }
        }
        persist(player);
    }

    // ── Lifecycle events ──────────────────────────────────────────────────────

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        persist(p);
        UUID uuid = p.getUniqueId();
        unlockedByPlayer.remove(uuid);
        countersByPlayer.remove(uuid);
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        DataStore.Repository repo = RpgServices.dataStore().repository(REPO);
        Set<String> unlocked = new HashSet<>();
        Map<String, Long> counters = new ConcurrentHashMap<>();

        repo.get(uuid.toString()).ifPresent(data -> {
            Object rawUnlocked = data.get(KEY_UNLOCKED);
            if (rawUnlocked instanceof List<?> list) {
                for (Object item : list) unlocked.add(String.valueOf(item));
            }
            Object rawCounters = data.get(KEY_COUNTERS);
            if (rawCounters instanceof Map<?, ?> m) {
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    if (e.getValue() instanceof Number n) {
                        counters.put(String.valueOf(e.getKey()), n.longValue());
                    }
                }
            }
        });

        unlockedByPlayer.put(uuid, unlocked);
        countersByPlayer.put(uuid, counters);
    }

    private void persist(Player player) {
        UUID uuid = player.getUniqueId();
        Set<String> unlocked = unlockedByPlayer.getOrDefault(uuid, Collections.emptySet());
        Map<String, Long> counters = countersByPlayer.getOrDefault(uuid, Collections.emptyMap());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put(KEY_UNLOCKED, new ArrayList<>(unlocked));
        data.put(KEY_COUNTERS, new LinkedHashMap<>(counters));
        RpgServices.dataStore().repository(REPO).save(uuid.toString(), data);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyReward(Player player, AchievementReward reward) {
        if (reward.isEmpty()) return;

        // Currency reward
        if (reward.money().compareTo(BigDecimal.ZERO) > 0) {
            try {
                RpgServices.economy().deposit(player, reward.money(), "achievement");
            } catch (IllegalStateException ignored) {
                // rpg-economy not loaded — skip money reward
            }
        }

        // XP reward
        if (reward.xp() > 0) {
            try {
                RpgServices.skills().awardXp(player, "combat", reward.xp());
            } catch (IllegalStateException ignored) {
                // rpg-skills not loaded — skip XP reward
            }
        }
    }

    private void notifyPlayer(Player player, AchievementDef def) {
        Component banner = Component.text("  ★ Achievement Unlocked!  ", NamedTextColor.GOLD, TextDecoration.BOLD);
        Component detail = Component.text("  " + def.title(), NamedTextColor.YELLOW)
                .append(Component.text(" — " + def.description(), NamedTextColor.GRAY));

        player.sendMessage(Component.empty());
        player.sendMessage(banner);
        player.sendMessage(detail);

        AchievementReward reward = def.reward();
        if (!reward.isEmpty()) {
            StringBuilder sb = new StringBuilder("  Rewards:");
            if (reward.money().compareTo(BigDecimal.ZERO) > 0) sb.append(" $").append(reward.money().toPlainString());
            if (reward.xp() > 0) sb.append(" ").append(reward.xp()).append(" XP");
            player.sendMessage(Component.text(sb.toString(), NamedTextColor.GREEN));
        }
        player.sendMessage(Component.empty());

        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }
}
