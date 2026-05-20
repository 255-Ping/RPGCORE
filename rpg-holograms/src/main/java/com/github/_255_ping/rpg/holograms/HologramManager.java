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
 * Persistent admin-defined holograms — one TextDisplay entity per hologram line, vertically
 * stacked at the saved location. Lines are configurable color/format via legacy ampersand codes.
 */
public final class HologramManager {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final File file;
    private final NamespacedKey idKey;
    private final NamespacedKey lineKey;
    private final Map<String, HologramDef> byId = new ConcurrentHashMap<>();
    private final Map<String, List<UUID>> displayEntities = new ConcurrentHashMap<>();

    public HologramManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "holograms.yml");
        this.idKey = new NamespacedKey(plugin, "rpg_holo_id");
        this.lineKey = new NamespacedKey(plugin, "rpg_holo_line");
    }

    public void loadAll() {
        despawnAll();
        byId.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String key : y.getKeys(false)) {
            ConfigurationSection s = y.getConfigurationSection(key);
            if (s == null) continue;
            try {
                String world = s.getString("World", "world");
                double x = s.getDouble("X"), yv = s.getDouble("Y"), z = s.getDouble("Z");
                List<String> lines = s.getStringList("Lines");
                byId.put(key.toLowerCase(Locale.ROOT),
                        new HologramDef(key.toLowerCase(Locale.ROOT), world, x, yv, z, lines));
            } catch (Exception ex) {
                plugin.getLogger().warning("Skipping hologram '" + key + "': " + ex.getMessage());
            }
        }
        spawnAll();
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
        HologramDef def = byId.remove(id.toLowerCase(Locale.ROOT));
        if (def == null) return false;
        despawn(def);
        saveAll();
        return true;
    }

    public void move(HologramDef def, Location loc) {
        despawn(def);
        def.moveTo(loc);
        spawn(def);
        saveAll();
    }

    public void rebuild(HologramDef def) {
        despawn(def);
        spawn(def);
        saveAll();
    }

    public void spawnAll() {
        for (HologramDef def : byId.values()) spawn(def);
    }

    public void despawnAll() {
        for (String id : new ArrayList<>(displayEntities.keySet())) {
            despawn(byId.get(id));
        }
        displayEntities.clear();
        // Sweep any lingering entities tagged by us across loaded worlds.
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
        // Sweep by tag too in case the world was reloaded.
        Location loc = def.location();
        if (loc == null || loc.getWorld() == null) return;
        for (Entity ent : loc.getWorld().getNearbyEntities(loc, 4, 4, 4)) {
            String id = ent.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
            if (def.id().equals(id)) ent.remove();
        }
    }

    public void spawn(HologramDef def) {
        Location loc = def.location();
        if (loc == null || loc.getWorld() == null) {
            plugin.getLogger().warning("Hologram '" + def.id() + "' world not loaded; skip spawn.");
            return;
        }
        double lineSpacing = plugin.getConfig().getDouble("hologram.line-spacing", 0.3);
        List<UUID> entities = new ArrayList<>();
        for (int i = 0; i < def.lines().size(); i++) {
            Location lineLoc = loc.clone().add(0, -i * lineSpacing, 0);
            TextDisplay td = (TextDisplay) loc.getWorld().spawnEntity(lineLoc, EntityType.TEXT_DISPLAY);
            td.text(LEGACY.deserialize(def.lines().get(i)));
            td.setBillboard(Display.Billboard.CENTER);
            td.setPersistent(true);
            td.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, def.id());
            td.getPersistentDataContainer().set(lineKey, PersistentDataType.INTEGER, i);
            entities.add(td.getUniqueId());
        }
        displayEntities.put(def.id(), entities);
    }

    @SuppressWarnings("unused")
    Map<String, HologramDef> snapshot() { return new HashMap<>(byId); }
}
