package com.github._255_ping.rpg.core.blocks;

import com.github._255_ping.rpg.api.blocks.Block;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages TextDisplay holograms shown above tagged custom block locations.
 *
 * <p>Each block definition can specify {@code Hologram: "&6Text"} in its YAML. When a block
 * is tagged at a location this service spawns a TextDisplay entity above it (centered over
 * the block, {@code HologramYOffset} blocks above the top surface). When the block is
 * removed — broken, admin-wiped, or replaced — the entity is despawned.
 *
 * <p><b>Lifecycle:</b> entities are persistent (survive chunk saves) but {@link #initAll}
 * sweeps all entities carrying our PDC tag before re-spawning fresh ones. This means a
 * server crash leaves at most one set of stale entities, cleaned on the next enable.
 *
 * <p><b>Concurrency note:</b> all methods must be called on the main thread.
 */
public final class BlockHologramService {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    /** PDC key stamped on every TextDisplay this service owns. Value is always {@code "1"}. */
    private final NamespacedKey holoKey;

    /** Maps block location → live TextDisplay entity UUID. */
    private final Map<BlockKey, UUID> active = new HashMap<>();

    public BlockHologramService(JavaPlugin plugin) {
        this.holoKey = new NamespacedKey(plugin, "rpg_block_holo");
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Called once on enable, after {@code blockPersistence.load()} has restored the location map.
     * Sweeps stale entities from the previous session, then spawns holograms for every
     * currently-tagged location whose block definition has {@code hologramText} set.
     * Also called from {@link com.github._255_ping.rpg.core.RpgCorePlugin#reloadAll()} after
     * block definitions are reloaded.
     */
    public void initAll(CoreBlockRegistry registry) {
        sweepStale();
        registry.snapshotLocations().forEach((key, blockId) -> {
            Block block = registry.get(blockId).orElse(null);
            if (block == null || block.hologramText().isBlank()) return;
            org.bukkit.World world = Bukkit.getWorld(key.world());
            if (world == null) return;
            spawnAt(new Location(world, key.x(), key.y(), key.z()), block);
        });
    }

    /**
     * Removes all tracked hologram entities. Called on plugin disable.
     * Entities are also persistent so they will be swept by the next {@link #initAll} call
     * even if disable doesn't complete cleanly.
     */
    public void despawnAll() {
        for (UUID uuid : new ArrayList<>(active.values())) {
            Entity e = Bukkit.getEntity(uuid);
            if (e != null) e.remove();
        }
        active.clear();
    }

    // ── Per-block operations ───────────────────────────────────────────────────

    /**
     * Spawns a hologram above {@code loc} if {@code block.hologramText()} is non-blank.
     * Safe to call unconditionally — early-returns if the block has no hologram text.
     * If a hologram already exists at this location it is removed first.
     */
    public void spawnAt(Location loc, Block block) {
        if (block == null || block.hologramText() == null || block.hologramText().isBlank()) return;
        despawnAt(loc);   // remove any existing entity at this location first
        if (loc.getWorld() == null) return;
        // Hologram floats above the block's top surface (block occupies Y..Y+1; offset above Y+1).
        Location holoLoc = new Location(
                loc.getWorld(),
                loc.getBlockX() + 0.5,
                loc.getBlockY() + 1.0 + block.hologramYOffset(),
                loc.getBlockZ() + 0.5);
        TextDisplay td = (TextDisplay) holoLoc.getWorld().spawnEntity(holoLoc, EntityType.TEXT_DISPLAY);
        td.text(LEGACY.deserialize(block.hologramText()));
        td.setBillboard(Display.Billboard.CENTER);
        td.setPersistent(true);
        td.getPersistentDataContainer().set(holoKey, PersistentDataType.STRING, "1");
        active.put(BlockKey.of(loc), td.getUniqueId());
    }

    /**
     * Removes the hologram entity at {@code loc} if one exists.
     * Called when a block is broken, admin-removed, or a piston destroys the block.
     */
    public void despawnAt(Location loc) {
        BlockKey key = BlockKey.of(loc);
        UUID uuid = active.remove(key);
        if (uuid != null) {
            Entity e = Bukkit.getEntity(uuid);
            if (e != null) e.remove();
        }
        // Proximity sweep as a safety net (handles cases where the in-memory map is stale,
        // e.g., after a reload or if the entity was moved by another plugin).
        if (loc.getWorld() != null) {
            Location sweepCenter = new Location(loc.getWorld(),
                    loc.getBlockX() + 0.5, loc.getBlockY() + 2.5, loc.getBlockZ() + 0.5);
            loc.getWorld().getNearbyEntities(sweepCenter, 1.0, 2.0, 1.0).forEach(ent -> {
                if (ent.getPersistentDataContainer().has(holoKey, PersistentDataType.STRING)) {
                    ent.remove();
                }
            });
        }
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    /** Removes every entity across all loaded worlds that carries our PDC tag. */
    private void sweepStale() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity ent : world.getEntities()) {
                if (ent.getPersistentDataContainer().has(holoKey, PersistentDataType.STRING)) {
                    ent.remove();
                }
            }
        }
        active.clear();
    }
}
