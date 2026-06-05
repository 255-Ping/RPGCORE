package com.github._255_ping.rpg.bossbar;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.bossbar.BossBarService;
import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import com.github._255_ping.rpg.api.mobs.BossBarDef;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Boss-bar implementation. One {@link BossBar} instance per tracked entity, shared across all
 * players watching that mob. Uses the mob's {@link BossBarDef} for title/colour/style, with the
 * HP fraction updated whenever our custom damage pipeline fires a {@link PostDamageEvent}.
 *
 * <p>Must be registered as a Bukkit listener in {@code RpgBossBarPlugin.onEnable}.
 */
public final class CoreBossBarService implements BossBarService, Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final Plugin plugin;

    /** entityUUID → active BossBar */
    private final Map<UUID, BossBar> entityBars = new HashMap<>();

    /** entityUUID → BossBarDef for that entity (cached to avoid re-fetching) */
    private final Map<UUID, BossBarDef> entityDefs = new HashMap<>();

    /** playerId → set of entity UUIDs the player is currently watching */
    private final Map<UUID, Set<UUID>> playerEntities = new HashMap<>();

    public CoreBossBarService(Plugin plugin) {
        this.plugin = plugin;
    }

    // ── BossBarService API ────────────────────────────────────────────────────

    @Override
    public void track(Player player, LivingEntity entity) {
        UUID entityId = entity.getUniqueId();
        UUID playerId = player.getUniqueId();

        // Look up the BossBarDef if we don't have a bar yet
        BossBarDef def = entityDefs.computeIfAbsent(entityId, k ->
                RpgServices.mobs().from(entity)
                        .flatMap(RpgMob::bossBar)
                        .orElse(null));
        if (def == null) return; // not a boss-bar mob

        // Get or create the shared BossBar for this entity
        BossBar bar = entityBars.computeIfAbsent(entityId, k -> {
            BossBar b = plugin.getServer().createBossBar(
                    def.name(),
                    def.color() != null ? def.color() : BarColor.RED,
                    def.style() != null ? def.style() : BarStyle.SOLID);
            b.setProgress(hpFraction(entity));
            return b;
        });

        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }

        playerEntities.computeIfAbsent(playerId, k -> new HashSet<>()).add(entityId);
    }

    @Override
    public void untrack(Player player, LivingEntity entity) {
        UUID entityId = entity.getUniqueId();
        UUID playerId = player.getUniqueId();

        BossBar bar = entityBars.get(entityId);
        if (bar != null) {
            bar.removePlayer(player);
            if (bar.getPlayers().isEmpty()) {
                bar.removeAll();
                entityBars.remove(entityId);
                entityDefs.remove(entityId);
            }
        }

        Set<UUID> watching = playerEntities.get(playerId);
        if (watching != null) {
            watching.remove(entityId);
            if (watching.isEmpty()) playerEntities.remove(playerId);
        }
    }

    @Override
    public void clearAll(Player player) {
        UUID playerId = player.getUniqueId();
        Set<UUID> watching = playerEntities.remove(playerId);
        if (watching == null) return;

        for (UUID entityId : watching) {
            BossBar bar = entityBars.get(entityId);
            if (bar == null) continue;
            bar.removePlayer(player);
            if (bar.getPlayers().isEmpty()) {
                bar.removeAll();
                entityBars.remove(entityId);
                entityDefs.remove(entityId);
            }
        }
    }

    // ── Public helper for proximity task ─────────────────────────────────────

    /** Returns the set of entity UUIDs the player is currently watching (read-only view). */
    public Set<UUID> watchedBy(Player player) {
        Set<UUID> s = playerEntities.get(player.getUniqueId());
        return s != null ? Set.copyOf(s) : Set.of();
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    /** Update bar progress whenever RPG damage lands on a tracked entity. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(PostDamageEvent event) {
        UUID id = event.context().victim().getUniqueId();
        BossBar bar = entityBars.get(id);
        if (bar == null) return;
        bar.setProgress(hpFraction(event.context().victim()));
    }

    /** Remove bar when the entity dies. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();
        BossBar bar = entityBars.remove(id);
        if (bar == null) return;
        entityDefs.remove(id);
        bar.removeAll();

        // Clean reverse index
        playerEntities.values().forEach(s -> s.remove(id));
        playerEntities.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    @EventHandler public void onQuit(PlayerQuitEvent e)        { clearAll(e.getPlayer()); }
    @EventHandler public void onDeath(PlayerDeathEvent e)      { clearAll(e.getPlayer()); }
    @EventHandler public void onWorld(PlayerChangedWorldEvent e) { clearAll(e.getPlayer()); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double hpFraction(LivingEntity entity) {
        try {
            double max = RpgServices.health().maxHp(entity);
            if (max <= 0) return 1.0;
            double cur = RpgServices.health().currentHp(entity);
            return Math.max(0.0, Math.min(1.0, cur / max));
        } catch (Exception ex) {
            // Health service not available — fall back to vanilla
            double max = entity.getAttribute(
                    org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                    ? entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
                    : 20.0;
            return Math.max(0.0, Math.min(1.0, entity.getHealth() / max));
        }
    }
}
