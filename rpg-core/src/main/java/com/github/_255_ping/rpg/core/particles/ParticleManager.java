package com.github._255_ping.rpg.core.particles;

import com.github._255_ping.rpg.api.RpgServices;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent world particle effects placed by admins via {@code /rpg particle}.
 *
 * <p>Each effect is stored in the {@code "particles"} DataStore repo and spawned on a
 * repeating task. Effects are visible to all nearby players.
 *
 * <p>Commands: {@code /rpg particle create <id> [type] [count] [spread] [pattern]}<br>
 * {@code /rpg particle delete <id>}<br>
 * {@code /rpg particle list}<br>
 * {@code /rpg particle move <id>} (moves to sender's location)<br>
 * {@code /rpg particle reload}
 */
public final class ParticleManager {

    private static final String REPO = "particles";
    private static final int TICK_INTERVAL = 10; // spawn every 10 ticks

    public enum Pattern { POINT, CIRCLE, SPIRAL }

    public record ParticleEntry(
            String id,
            String worldName,
            double x, double y, double z,
            String particleType,
            int count,
            double spread,
            Pattern pattern
    ) {}

    private final JavaPlugin plugin;
    private final ConcurrentHashMap<String, ParticleEntry> byId = new ConcurrentHashMap<>();
    private BukkitTask task;
    private int tickOffset = 0;

    public ParticleManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        loadAll();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, TICK_INTERVAL, TICK_INTERVAL);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    // ── Commands interface ────────────────────────────────────────────────

    public boolean create(String id, Location loc, String particleType, int count, double spread, Pattern pattern) {
        if (byId.containsKey(id)) return false;
        Particle p = parseParticle(particleType);
        ParticleEntry entry = new ParticleEntry(id, loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                p.name(), count, spread, pattern);
        byId.put(id, entry);
        save(entry);
        return true;
    }

    public boolean delete(String id) {
        if (byId.remove(id) == null) return false;
        RpgServices.dataStore().repository(REPO).delete(id);
        return true;
    }

    public boolean move(String id, Location loc) {
        ParticleEntry old = byId.get(id);
        if (old == null) return false;
        ParticleEntry updated = new ParticleEntry(id, loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                old.particleType(), old.count(), old.spread(), old.pattern());
        byId.put(id, updated);
        save(updated);
        return true;
    }

    public Collection<ParticleEntry> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public Optional<ParticleEntry> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    // ── Tick ─────────────────────────────────────────────────────────────

    private void tick() {
        tickOffset++;
        for (ParticleEntry entry : byId.values()) {
            World world = Bukkit.getWorld(entry.worldName());
            if (world == null) continue;
            Location center = new Location(world, entry.x(), entry.y(), entry.z());
            Particle p = parseParticle(entry.particleType());
            switch (entry.pattern()) {
                case POINT -> world.spawnParticle(p, center, entry.count(),
                        entry.spread(), entry.spread(), entry.spread(), 0);
                case CIRCLE -> spawnCircle(world, p, center, entry.count(), entry.spread());
                case SPIRAL -> spawnSpiral(world, p, center, entry.count(), entry.spread(), tickOffset);
            }
        }
    }

    private static void spawnCircle(World world, Particle p, Location center, int count, double radius) {
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            Location loc = center.clone().add(radius * Math.cos(angle), 0, radius * Math.sin(angle));
            world.spawnParticle(p, loc, 1, 0, 0, 0, 0);
        }
    }

    private static void spawnSpiral(World world, Particle p, Location center, int count, double radius, int offset) {
        double baseAngle = (offset * 0.3) % (2 * Math.PI);
        for (int i = 0; i < count; i++) {
            double angle = baseAngle + 2 * Math.PI * i / count;
            double yOff = Math.sin(baseAngle + i * 0.5) * radius * 0.5;
            Location loc = center.clone().add(radius * Math.cos(angle), yOff, radius * Math.sin(angle));
            world.spawnParticle(p, loc, 1, 0, 0, 0, 0);
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────

    private void save(ParticleEntry entry) {
        Map<String, Object> data = new HashMap<>();
        data.put("world", entry.worldName());
        data.put("x", entry.x()); data.put("y", entry.y()); data.put("z", entry.z());
        data.put("particle", entry.particleType());
        data.put("count", entry.count());
        data.put("spread", entry.spread());
        data.put("pattern", entry.pattern().name());
        RpgServices.dataStore().repository(REPO).save(entry.id(), data);
    }

    private void loadAll() {
        byId.clear();
        var repo = RpgServices.dataStore().repository(REPO);
        for (String key : repo.keys()) {
            repo.get(key).ifPresent(data -> {
                try {
                    String world   = String.valueOf(data.get("world"));
                    double x = d(data.get("x")), y = d(data.get("y")), z = d(data.get("z"));
                    String pType   = String.valueOf(data.getOrDefault("particle", "FLAME"));
                    int count      = data.get("count") instanceof Number n ? n.intValue() : 5;
                    double spread  = d(data.getOrDefault("spread", 0.3));
                    Pattern pat    = parsePattern(String.valueOf(data.getOrDefault("pattern", "POINT")));
                    byId.put(key, new ParticleEntry(key, world, x, y, z, pType, count, spread, pat));
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to load particle " + key + ": " + ex.getMessage());
                }
            });
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    public static Particle parseParticle(String name) {
        try { return Particle.valueOf(name.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { return Particle.FLAME; }
    }

    public static Pattern parsePattern(String name) {
        try { return Pattern.valueOf(name.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { return Pattern.POINT; }
    }

    private static double d(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0.0;
    }
}
