package com.github._255_ping.rpg.core.spawners;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import com.github._255_ping.rpg.api.persistence.DataStore;
import com.github._255_ping.rpg.core.health.CoreHealthService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Persistent admin-placed mob spawners. Spawners tick periodically: if alive count is
 * below max and cooldown elapsed, spawn a new mob from the configured mob id at a random
 * point within the spawn radius.
 *
 * <p>Spawned mobs are PDC-tagged with the spawner's id so we can track them for the
 * alive-count cap.
 */
public final class SpawnerManager {

    private static final String REPO = "spawners";

    private final JavaPlugin plugin;
    private final CoreHealthService healthService;
    private final NamespacedKey spawnerKey;
    private final NamespacedKey mobLevelKey;
    private final ConcurrentMap<String, SpawnerDef> byId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> lastSpawnTick = new ConcurrentHashMap<>();
    private long currentTick = 0;

    public SpawnerManager(JavaPlugin plugin, CoreHealthService healthService) {
        this.plugin = plugin;
        this.healthService = healthService;
        this.spawnerKey = new NamespacedKey(plugin, "spawner_id");
        this.mobLevelKey = new NamespacedKey(plugin, "mob_level");
    }

    public NamespacedKey spawnerKey() { return spawnerKey; }
    public NamespacedKey mobLevelKey() { return mobLevelKey; }

    public void loadAll() {
        byId.clear();
        DataStore.Repository repo = RpgServices.dataStore().repository(REPO);
        for (String id : repo.keys()) {
            repo.get(id).ifPresent(data -> {
                try {
                    SpawnerDef def = new SpawnerDef(
                            id,
                            str(data, "mob"),
                            str(data, "world"),
                            num(data, "x"), num(data, "y"), num(data, "z"),
                            num(data, "spawn-radius"),
                            num(data, "cooldown-ticks"),
                            num(data, "max-alive"),
                            data.get("continuous") instanceof Boolean b ? b : true,
                            num(data, "min-level") > 0 ? num(data, "min-level") : 1,
                            num(data, "max-level") > 0 ? num(data, "max-level") : 1
                    );
                    byId.put(id, def);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to load spawner " + id + ": " + ex.getMessage());
                }
            });
        }
    }

    public void put(SpawnerDef def) {
        byId.put(def.id(), def);
        saveOne(def.id());
    }

    public boolean remove(String id) {
        if (byId.remove(id) == null) return false;
        RpgServices.dataStore().repository(REPO).delete(id);
        lastSpawnTick.remove(id);
        return true;
    }

    public Optional<SpawnerDef> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<SpawnerDef> all() {
        return byId.values();
    }

    public void saveOne(String id) {
        SpawnerDef def = byId.get(id);
        if (def == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("schema-version", 1);
        data.put("mob", def.mobId());
        data.put("world", def.worldName());
        data.put("x", def.x());
        data.put("y", def.y());
        data.put("z", def.z());
        data.put("spawn-radius", def.spawnRadius());
        data.put("cooldown-ticks", def.cooldownTicks());
        data.put("max-alive", def.maxAlive());
        data.put("continuous", def.continuous());
        data.put("min-level", def.minLevel());
        data.put("max-level", def.maxLevel());
        RpgServices.dataStore().repository(REPO).save(id, data);
    }

    public void saveAll() {
        for (String id : byId.keySet()) saveOne(id);
    }

    public void tick() {
        currentTick++;
        for (SpawnerDef def : byId.values()) {
            if (!def.continuous()) continue;
            long last = lastSpawnTick.getOrDefault(def.id(), 0L);
            if (last != 0 && currentTick - last < def.cooldownTicks()) continue;
            World world = Bukkit.getWorld(def.worldName());
            if (world == null) continue;

            // Count alive mobs tagged to this spawner.
            int alive = 0;
            for (Entity e : world.getEntities()) {
                String tag = e.getPersistentDataContainer().get(spawnerKey, PersistentDataType.STRING);
                if (def.id().equals(tag) && !e.isDead()) alive++;
            }
            if (alive >= def.maxAlive()) continue;

            Optional<RpgMob> mob = RpgServices.mobs().get(def.mobId());
            if (mob.isEmpty()) continue;

            double dx = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * def.spawnRadius();
            double dz = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * def.spawnRadius();
            Location spawnLoc = new Location(world, def.x() + dx + 0.5, def.y(), def.z() + dz + 0.5);
            LivingEntity spawned = mob.get().spawn(spawnLoc);
            if (spawned != null) {
                spawned.getPersistentDataContainer().set(spawnerKey, PersistentDataType.STRING, def.id());
                int level = def.minLevel() == def.maxLevel()
                        ? def.minLevel()
                        : def.minLevel() + ThreadLocalRandom.current().nextInt(def.maxLevel() - def.minLevel() + 1);
                spawned.getPersistentDataContainer().set(mobLevelKey, PersistentDataType.INTEGER, level);

                // Apply HP scaling for leveled mobs (level 1 is base — no change).
                if (level > 1 && plugin.getConfig().getBoolean("mob-level-scaling.enabled", true)) {
                    double hpPerLevel = plugin.getConfig().getDouble("mob-level-scaling.per-level-gains.health", 5.0);
                    double scaledHp = mob.get().maxHealth() + (level - 1) * hpPerLevel;
                    // Update Bukkit MAX_HEALTH so the RPG health sync doesn't clamp below scaled HP.
                    AttributeInstance attr = spawned.getAttribute(Attribute.MAX_HEALTH);
                    if (attr != null) attr.setBaseValue(Math.min(scaledHp, 2048.0));
                    healthService.setMaxHp(spawned, scaledHp);
                    healthService.setCurrentHp(spawned, scaledHp);
                }

                // Prefix the display name with [Lv. N] so players see the level.
                if (level > 1 || def.minLevel() > 1) {
                    var existing = spawned.customName();
                    net.kyori.adventure.text.Component levelPrefix =
                            net.kyori.adventure.text.Component.text("[Lv. " + level + "] ",
                                    net.kyori.adventure.text.format.NamedTextColor.GRAY);
                    spawned.customName(existing != null ? levelPrefix.append(existing) : levelPrefix);
                    spawned.setCustomNameVisible(true);
                }
            }
            lastSpawnTick.put(def.id(), currentTick);
        }
    }

    private static String str(Map<String, Object> data, String key) {
        Object v = data.get(key);
        return v == null ? "" : v.toString();
    }

    private static int num(Map<String, Object> data, String key) {
        return data.get(key) instanceof Number n ? n.intValue() : 0;
    }
}
