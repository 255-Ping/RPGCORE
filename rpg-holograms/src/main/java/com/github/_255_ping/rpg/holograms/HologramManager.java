package com.github._255_ping.rpg.holograms;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent admin-defined holograms.
 *
 * <p><b>Static holograms</b> ({@code Animated: false}, the default) spawn one {@link TextDisplay}
 * entity per line, stacked vertically with the configured {@code hologram.line-spacing}.
 *
 * <p><b>Animated holograms</b> ({@code Animated: true}) spawn a single {@link TextDisplay} entity
 * and cycle through all {@code Lines} as animation frames at the configured {@code FrameInterval}
 * ticks. A single server-tick repeating task drives all animated holograms efficiently.
 *
 * <p>Both modes support legacy ampersand colour codes.
 */
public final class HologramManager {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final File file;
    private final NamespacedKey idKey;
    private final NamespacedKey lineKey;
    private final Map<String, HologramDef> byId = new ConcurrentHashMap<>();
    private final Map<String, List<UUID>>  displayEntities = new ConcurrentHashMap<>();

    // Animation state — keyed by hologram id, populated only for animated holograms
    private final Map<String, Integer> frameIndices = new ConcurrentHashMap<>();
    private final Map<String, Integer> frameTicks   = new ConcurrentHashMap<>();
    private BukkitTask animationTask;

    public HologramManager(JavaPlugin plugin) {
        this.plugin  = plugin;
        this.file    = new File(plugin.getDataFolder(), "holograms.yml");
        this.idKey   = new NamespacedKey(plugin, "rpg_holo_id");
        this.lineKey = new NamespacedKey(plugin, "rpg_holo_line");
    }

    public void loadAll() {
        despawnAll(); // also cancels animation task
        byId.clear();
        frameIndices.clear();
        frameTicks.clear();

        if (file.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            int defaultInterval = plugin.getConfig().getInt("hologram.animation-interval", 20);
            for (String key : y.getKeys(false)) {
                ConfigurationSection s = y.getConfigurationSection(key);
                if (s == null) continue;
                try {
                    String world       = s.getString("World", "world");
                    double x           = s.getDouble("X"), yv = s.getDouble("Y"), z = s.getDouble("Z");
                    List<String> lines = s.getStringList("Lines");
                    boolean animated   = s.getBoolean("Animated", false);
                    int frameInterval  = s.getInt("FrameInterval", defaultInterval);
                    byId.put(key.toLowerCase(Locale.ROOT),
                            new HologramDef(key.toLowerCase(Locale.ROOT), world, x, yv, z,
                                    lines, animated, frameInterval));
                } catch (Exception ex) {
                    plugin.getLogger().warning("Skipping hologram '" + key + "': " + ex.getMessage());
                }
            }
        }

        spawnAll();
        startAnimationTask();
    }

    public void saveAll() {
        YamlConfiguration y = new YamlConfiguration();
        for (HologramDef def : byId.values()) y.createSection(def.id(), def.toMap());
        try { y.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("Failed to save holograms: " + ex.getMessage()); }
    }

    public Collection<HologramDef> all() { return byId.values(); }

    public Optional<HologramDef> get(String id) {
        return Optional.ofNullable(byId.get(id.toLowerCase(Locale.ROOT)));
    }

    public HologramDef create(String id, Location loc, List<String> lines) {
        String key = id.toLowerCase(Locale.ROOT);
        HologramDef def = new HologramDef(key, loc, lines);
        byId.put(key, def);
        spawn(def);
        saveAll();
        return def;
    }

    public boolean delete(String id) {
        String key = id.toLowerCase(Locale.ROOT);
        HologramDef def = byId.remove(key);
        if (def == null) return false;
        despawn(def);
        frameIndices.remove(key);
        frameTicks.remove(key);
        saveAll();
        return true;
    }

    public void move(HologramDef def, Location loc) {
        despawn(def);
        def.moveTo(loc);
        spawn(def);
        saveAll();
    }

    /** Despawns and respawns the hologram, applying structural changes (animated flag, line edits, etc.). */
    public void rebuild(HologramDef def) {
        despawn(def);
        // Reset animation to frame 0 so the entity starts fresh
        frameIndices.put(def.id(), 0);
        frameTicks.put(def.id(), 0);
        spawn(def);
        saveAll();
    }

    public void spawnAll() {
        for (HologramDef def : byId.values()) spawn(def);
    }

    public void despawnAll() {
        if (animationTask != null) { animationTask.cancel(); animationTask = null; }
        for (String id : new ArrayList<>(displayEntities.keySet())) {
            despawn(byId.get(id));
        }
        displayEntities.clear();
        // Sweep any lingering tagged entities across all loaded worlds
        for (var w : Bukkit.getWorlds()) {
            for (Entity ent : w.getEntities()) {
                if (ent.getPersistentDataContainer().has(idKey, PersistentDataType.STRING)) ent.remove();
            }
        }
    }

    public void despawn(HologramDef def) {
        if (def == null) return;
        List<UUID> entities = displayEntities.remove(def.id());
        if (entities != null) {
            for (UUID uuid : entities) {
                Entity e = Bukkit.getEntity(uuid);
                if (e != null) e.remove();
            }
        }
        // Sweep by tag in case the world was reloaded
        Location loc = def.location();
        if (loc == null || loc.getWorld() == null) return;
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, 4, 4, 4)) {
            String id = ent.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
            if (def.id().equals(id)) ent.remove();
        }
    }

    /**
     * Spawns entities for {@code def}.
     * <ul>
     *   <li>Static — one entity per line, stacked downward by {@code hologram.line-spacing}.</li>
     *   <li>Animated — one entity showing the current frame; the animation task updates it each interval.</li>
     * </ul>
     */
    public void spawn(HologramDef def) {
        Location loc = def.location();
        if (loc == null || loc.getWorld() == null) {
            plugin.getLogger().warning("Hologram '" + def.id() + "' world not loaded; skip spawn.");
            return;
        }

        List<UUID> entities = new ArrayList<>();

        if (def.animated() && !def.lines().isEmpty()) {
            // Single entity — starts at the current stored frame index
            int frame = frameIndices.getOrDefault(def.id(), 0) % def.lines().size();
            TextDisplay td = makeDisplay(loc, LEGACY.deserialize(def.lines().get(frame)), def.id(), 0);
            entities.add(td.getUniqueId());
        } else {
            double lineSpacing = plugin.getConfig().getDouble("hologram.line-spacing", 0.3);
            for (int i = 0; i < def.lines().size(); i++) {
                Location lineLoc = loc.clone().add(0, -i * lineSpacing, 0);
                TextDisplay td = makeDisplay(lineLoc, LEGACY.deserialize(def.lines().get(i)), def.id(), i);
                entities.add(td.getUniqueId());
            }
        }

        displayEntities.put(def.id(), entities);
    }

    // ── Animation task ──────────────────────────────────────────────────────────

    private void startAnimationTask() {
        if (animationTask != null) animationTask.cancel();
        animationTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::tickAnimation, 1L, 1L);
    }

    private void tickAnimation() {
        for (Map.Entry<String, HologramDef> entry : byId.entrySet()) {
            HologramDef def = entry.getValue();
            if (!def.animated() || def.lines().size() <= 1) continue;

            String id    = def.id();
            int ticks    = frameTicks.merge(id, 1, Integer::sum);
            if (ticks < def.frameInterval()) continue;

            frameTicks.put(id, 0);
            int next = (frameIndices.getOrDefault(id, 0) + 1) % def.lines().size();
            frameIndices.put(id, next);

            List<UUID> uuids = displayEntities.get(id);
            if (uuids == null || uuids.isEmpty()) continue;
            Entity ent = Bukkit.getEntity(uuids.get(0));
            if (ent instanceof TextDisplay td) {
                td.text(LEGACY.deserialize(def.lines().get(next)));
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private TextDisplay makeDisplay(Location loc, Component text, String holoId, int lineIndex) {
        TextDisplay td = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        td.text(text);
        td.setBillboard(Display.Billboard.CENTER);
        td.setPersistent(true);
        td.getPersistentDataContainer().set(idKey,   PersistentDataType.STRING,  holoId);
        td.getPersistentDataContainer().set(lineKey, PersistentDataType.INTEGER, lineIndex);
        return td;
    }

    @SuppressWarnings("unused")
    Map<String, HologramDef> snapshot() { return new HashMap<>(byId); }
}
