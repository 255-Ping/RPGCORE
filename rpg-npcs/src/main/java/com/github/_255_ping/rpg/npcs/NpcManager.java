package com.github._255_ping.rpg.npcs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Loads, saves, and spawns NPCs. NPC ↔ entity correlation lives in PDC on the entity. */
public final class NpcManager {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final File npcsDir;
    private final NamespacedKey npcIdKey;
    private final SkinFetcher skinFetcher;
    private final Map<String, NpcDef> byId = new ConcurrentHashMap<>();
    private final Map<UUID, String> entityToId = new ConcurrentHashMap<>();
    /** Tracks live State for PLAYER-style NPCs (npcId → State). */
    private final Map<String, FakePlayerNpc.State> fakePlayerStates = new ConcurrentHashMap<>();
    private BukkitTask lookAtTask;

    public NpcManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.npcsDir = new File(plugin.getDataFolder(), "npcs");
        this.npcIdKey = new NamespacedKey(plugin, "rpg_npc_id");
        this.skinFetcher = new SkinFetcher(plugin);
    }

    public NamespacedKey idKey() { return npcIdKey; }
    public Collection<NpcDef> all() { return byId.values(); }
    public Optional<NpcDef> get(String id) { return Optional.ofNullable(byId.get(id.toLowerCase(Locale.ROOT))); }

    public Optional<NpcDef> fromEntity(Entity ent) {
        String id = ent.getPersistentDataContainer().get(npcIdKey, PersistentDataType.STRING);
        if (id == null) return Optional.empty();
        return get(id);
    }

    public void loadAll() {
        // Sweep persistent entities tagged by this plugin from all loaded worlds.
        // This handles the case where the server was restarted or the plugin was
        // reloaded: setPersistent(true) entities survive chunk unload/reload, so
        // despawnAll() (which only tracks UUIDs from the current session) misses them.
        // Without this sweep, each reload stacks a new copy on top of the old one.
        sweepOrphanedEntities();
        despawnAll();
        byId.clear();
        if (!npcsDir.isDirectory()) {
            if (!npcsDir.mkdirs()) {
                plugin.getLogger().warning("Could not create npcs/ directory.");
                return;
            }
        }
        File[] files = npcsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
                for (String id : y.getKeys(false)) {
                    ConfigurationSection s = y.getConfigurationSection(id);
                    if (s == null) continue;
                    try {
                        byId.put(id.toLowerCase(Locale.ROOT), parse(id.toLowerCase(Locale.ROOT), s));
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Skipping npc '" + id + "' in " + f.getName() + ": " + ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to parse npcs file " + f.getName() + ": " + ex.getMessage());
            }
        }
        spawnAll();
        startLookAtTask();
    }

    public void saveAll() {
        if (!npcsDir.isDirectory()) npcsDir.mkdirs();
        YamlConfiguration out = new YamlConfiguration();
        for (NpcDef def : byId.values()) {
            out.createSection(def.id(), def.toMap());
        }
        try {
            out.save(new File(npcsDir, "all.yml"));
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save npcs: " + ex.getMessage());
        }
    }

    public NpcDef create(String id, Location loc, String displayName) {
        String key = id.toLowerCase(Locale.ROOT);
        NpcDef def = new NpcDef(key, displayName, loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(),
                NpcDef.EntityStyle.ENTITY, null,
                NpcDef.BehaviorType.DIALOGUE, new ArrayList<>(),
                new ArrayList<>(List.of("&7Hello!")), null, null);
        byId.put(key, def);
        spawn(def);
        autosave();
        return def;
    }

    public boolean delete(String id) {
        NpcDef def = byId.remove(id.toLowerCase(Locale.ROOT));
        if (def == null) return false;
        despawn(def);
        autosave();
        return true;
    }

    public void move(NpcDef def, Location loc) {
        despawn(def);
        def.moveTo(loc);
        spawn(def);
        autosave();
    }

    public void rebuild(NpcDef def) {
        despawn(def);
        spawn(def);
        autosave();
    }

    private void autosave() {
        if (plugin.getConfig().getBoolean("autosave", true)) saveAll();
    }

    /** Save without despawning/respawning entities (for data-only changes like dialogue/shop edits). */
    public void saveOnly() { autosave(); }

    // ---- look-at-player task ----

    private void startLookAtTask() {
        if (lookAtTask != null) { lookAtTask.cancel(); lookAtTask = null; }
        if (!plugin.getConfig().getBoolean("npc.look-at-players.enabled", true)) return;
        long interval = plugin.getConfig().getLong("npc.look-at-players.interval-ticks", 2L);
        lookAtTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickLookAt, interval, interval);
    }

    private void tickLookAt() {
        for (NpcDef def : byId.values()) {
            if (!def.lookAtPlayers()) continue;
            Location loc = def.location();
            if (loc == null || loc.getWorld() == null) continue;

            // Find nearest player within radius.
            Player nearest = null;
            double nearestDist2 = def.lookRadius() * def.lookRadius();
            for (Player p : loc.getWorld().getPlayers()) {
                double d2 = p.getLocation().distanceSquared(loc);
                if (d2 < nearestDist2) { nearestDist2 = d2; nearest = p; }
            }

            if (def.entityStyle() == NpcDef.EntityStyle.ENTITY) {
                tickLookAtEntity(def, nearest);
            } else {
                tickLookAtFakePlayer(def, loc, nearest);
            }
        }
    }

    private void tickLookAtEntity(NpcDef def, Player target) {
        for (Map.Entry<UUID, String> entry : entityToId.entrySet()) {
            if (!def.id().equals(entry.getValue())) continue;
            Entity ent = Bukkit.getEntity(entry.getKey());
            if (ent == null || ent instanceof TextDisplay || ent instanceof Interaction) continue;

            float yaw, pitch;
            if (target != null) {
                var dir = target.getEyeLocation().toVector()
                        .subtract(ent.getLocation().add(0, ent.getHeight() / 2.0, 0).toVector());
                if (dir.lengthSquared() < 1e-6) continue;
                var look = ent.getLocation().clone().setDirection(dir);
                yaw = look.getYaw();
                pitch = look.getPitch();
            } else {
                yaw = def.yaw();
                pitch = def.pitch();
            }
            ent.setRotation(yaw, pitch);
            break; // one body entity per NPC
        }
    }

    private void tickLookAtFakePlayer(NpcDef def, Location loc, Player target) {
        FakePlayerNpc.State state = fakePlayerStates.get(def.id());
        if (state == null) return;

        float yaw, pitch;
        if (target != null) {
            var dir = target.getEyeLocation().toVector()
                    .subtract(loc.clone().add(0, 1.0, 0).toVector());
            if (dir.lengthSquared() < 1e-6) return;
            var look = loc.clone().setDirection(dir);
            yaw = look.getYaw();
            pitch = look.getPitch();
        } else {
            yaw = def.yaw();
            pitch = def.pitch();
        }

        // Only broadcast to players within a reasonable render distance.
        var nearby = loc.getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distanceSquared(loc) <= 64.0 * 64.0)
                .toList();
        if (nearby.isEmpty()) return;
        FakePlayerNpc.rotateHead(state, yaw, pitch, nearby);
    }

    public void despawnAll() {
        if (lookAtTask != null) { lookAtTask.cancel(); lookAtTask = null; }
        for (UUID uuid : new ArrayList<>(entityToId.keySet())) {
            Entity ent = Bukkit.getEntity(uuid);
            if (ent != null) ent.remove();
        }
        entityToId.clear();
        for (FakePlayerNpc.State state : fakePlayerStates.values()) {
            FakePlayerNpc.despawn(plugin, state);
        }
        fakePlayerStates.clear();
    }

    public void despawn(NpcDef def) {
        // Despawn fake player state if any
        FakePlayerNpc.State state = fakePlayerStates.remove(def.id());
        if (state != null) {
            FakePlayerNpc.despawn(plugin, state);
        }
        // Despawn entity-style body + name tag
        Location loc = def.location();
        if (loc == null || loc.getWorld() == null) return;
        for (Entity ent : loc.getWorld().getEntities()) {
            String id = ent.getPersistentDataContainer().get(npcIdKey, PersistentDataType.STRING);
            if (def.id().equals(id)) {
                entityToId.remove(ent.getUniqueId());
                ent.remove();
            }
        }
    }

    public void spawnAll() {
        for (NpcDef def : byId.values()) spawn(def);
    }

    public void spawn(NpcDef def) {
        Location loc = def.location();
        if (loc == null || loc.getWorld() == null) {
            plugin.getLogger().warning("NPC '" + def.id() + "' world not loaded; skip spawn.");
            return;
        }

        if (def.entityStyle() == NpcDef.EntityStyle.PLAYER) {
            spawnPlayerNpc(def, loc);
        } else {
            spawnEntityNpc(def, loc);
        }
    }

    private void spawnEntityNpc(NpcDef def, Location loc) {
        // Per-NPC entity type takes priority over the global config default.
        String entTypeStr = def.entityType() != null
                ? def.entityType()
                : plugin.getConfig().getString("display.body-entity", "ZOMBIE");
        EntityType type;
        try {
            type = EntityType.valueOf(entTypeStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            type = EntityType.ZOMBIE;
        }
        Entity ent = loc.getWorld().spawnEntity(loc, type);
        ent.setPersistent(true);
        ent.getPersistentDataContainer().set(npcIdKey, PersistentDataType.STRING, def.id());
        boolean useTextDisplay = plugin.getConfig().getBoolean("display.use-text-display-for-name", true);
        if (ent instanceof LivingEntity le) {
            le.setAI(false);
            le.setCollidable(false);
            le.setInvulnerable(true);
            le.setSilent(true);
            le.setRemoveWhenFarAway(false);
            // Only set the entity's built-in name if we're NOT using a TextDisplay — otherwise
            // both the floating TextDisplay and the hover tooltip would show simultaneously.
            if (useTextDisplay) {
                le.customName(null);
                le.setCustomNameVisible(false);
            } else {
                le.customName(LEGACY.deserialize(def.displayName()));
                le.setCustomNameVisible(true);
            }
        }
        entityToId.put(ent.getUniqueId(), def.id());
        spawnNameTag(def, loc, useTextDisplay);
    }

    private void spawnPlayerNpc(NpcDef def, Location loc) {
        NpcDef.SkinDef skin = def.skin();

        // If skin has a player name but no cached texture yet, fetch then spawn
        if (skin != null && skin.playerName() != null && skin.value() == null) {
            skinFetcher.fetchByName(skin.playerName(), fetched -> {
                if (fetched != null) {
                    def.setSkin(fetched);
                    autosave();
                }
                doSpawnPlayerNpc(def, loc);
            });
        } else {
            doSpawnPlayerNpc(def, loc);
        }
    }

    private void doSpawnPlayerNpc(NpcDef def, Location loc) {
        FakePlayerNpc.State state = FakePlayerNpc.spawn(plugin, def, npcIdKey);
        if (state == null) {
            plugin.getLogger().warning("Failed to spawn fake player NPC '" + def.id() + "'.");
            return;
        }
        fakePlayerStates.put(def.id(), state);
        // Track the interaction entity so fromEntity() resolves it
        entityToId.put(state.interactionEntity().getUniqueId(), def.id());
        boolean useTextDisplay = plugin.getConfig().getBoolean("display.use-text-display-for-name", true);
        spawnNameTag(def, loc, useTextDisplay);
    }

    /** Called by NpcInteractListener on PlayerJoinEvent to resend fake player packets. */
    public Map<String, FakePlayerNpc.State> fakePlayerStates() { return fakePlayerStates; }

    private void spawnNameTag(NpcDef def, Location loc, boolean useTextDisplay) {
        if (!useTextDisplay) return;
        Location above = loc.clone().add(0, 2.0, 0);
        TextDisplay text = (TextDisplay) loc.getWorld().spawnEntity(above, EntityType.TEXT_DISPLAY);
        text.text(Component.text("").append(LEGACY.deserialize(def.displayName())));
        text.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        text.getPersistentDataContainer().set(npcIdKey, PersistentDataType.STRING, def.id());
        text.setPersistent(true);
        entityToId.put(text.getUniqueId(), def.id());
    }

    private NpcDef parse(String id, ConfigurationSection s) {
        String name = s.getString("DisplayName", id);
        String world = s.getString("World", "world");
        double x = s.getDouble("X"), y = s.getDouble("Y"), z = s.getDouble("Z");
        float yaw = (float) s.getDouble("Yaw", 0), pitch = (float) s.getDouble("Pitch", 0);

        NpcDef.EntityStyle style = NpcDef.EntityStyle.ENTITY;
        try {
            style = NpcDef.EntityStyle.valueOf(s.getString("EntityStyle", "entity").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {}

        NpcDef.SkinDef skin = null;
        ConfigurationSection skinSec = s.getConfigurationSection("Skin");
        if (skinSec != null) {
            skin = new NpcDef.SkinDef(
                skinSec.getString("Name"),
                skinSec.getString("Value"),
                skinSec.getString("Signature"));
        }

        ConfigurationSection beh = s.getConfigurationSection("Behavior");
        NpcDef.BehaviorType type = NpcDef.BehaviorType.DIALOGUE;
        List<NpcDef.ShopEntry> shop = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        String quest = null;
        NpcDef.BankerData bankerData = null;

        if (beh != null) {
            type = parseBehavior(beh.getString("Type", "dialogue"));
            for (Object o : beh.getList("Items", List.of())) {
                if (o instanceof Map<?, ?> m) {
                    Object buy = m.get("Buy"), sell = m.get("Sell");
                    shop.add(new NpcDef.ShopEntry(
                        String.valueOf(m.get("Item")),
                        buy instanceof Number n ? n.doubleValue() : 0.0,
                        sell instanceof Number n ? n.doubleValue() : 0.0));
                }
            }
            lines.addAll(beh.getStringList("Lines"));
            quest = beh.getString("Quest");
            if (type == NpcDef.BehaviorType.BANKER) {
                String bankName = beh.getString("BankName", name);
                double interest = beh.getDouble("DailyInterestPercent",
                    plugin.getConfig().getDouble("banker.default-daily-interest-percent", 0.5));
                bankerData = new NpcDef.BankerData(bankName, interest);
            }
        }
        NpcDef def = new NpcDef(id, name, world, x, y, z, yaw, pitch, style, skin, type, shop, lines, quest, bankerData);
        def.setEntityType(s.getString("EntityType", null));
        def.setLookAtPlayers(s.getBoolean("LookAtPlayers", false));
        def.setLookRadius(s.getDouble("LookRadius",
                plugin.getConfig().getDouble("npc.look-at-players.default-radius", 8.0)));
        return def;
    }

    private static NpcDef.BehaviorType parseBehavior(String s) {
        try {
            return NpcDef.BehaviorType.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return NpcDef.BehaviorType.DIALOGUE;
        }
    }

    /**
     * Removes all entities in every loaded world that carry the rpg_npc_id PDC key.
     * Called at the start of loadAll() so persistent entities from a previous session
     * don't accumulate on top of freshly-spawned ones.
     */
    private void sweepOrphanedEntities() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity ent : world.getEntities()) {
                if (ent.getPersistentDataContainer().has(npcIdKey, PersistentDataType.STRING)) {
                    ent.remove();
                }
            }
        }
    }

    @SuppressWarnings("unused")
    Map<UUID, String> entityIndex() { return new HashMap<>(entityToId); }
}
