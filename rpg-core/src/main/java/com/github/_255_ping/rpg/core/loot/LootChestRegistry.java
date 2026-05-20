package com.github._255_ping.rpg.core.loot;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.loot.LootContext;
import com.github._255_ping.rpg.api.loot.LootTable;
import com.github._255_ping.rpg.api.persistence.DataStore;
import com.github._255_ping.rpg.core.blocks.BlockKey;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks chest block locations that should fill from a {@link LootTable} when a player opens
 * them. Persists chest→table mapping via DataStore; once a player has looted a chest it stays
 * empty (per-chest cooldown TBD).
 *
 * <p>The location is stored as {@code <world>:<x>,<y>,<z>}; LootTableRegistry resolution
 * happens on open. Players who have already looted a chest are tracked per location to prevent
 * double-rolls within the same instance.
 */
public final class LootChestRegistry implements Listener {

    private static final String REPO = "loot-chests";

    private final JavaPlugin plugin;
    private final Map<BlockKey, String> chestToTable = new ConcurrentHashMap<>();
    private final Map<BlockKey, Set<UUID>> looted = new ConcurrentHashMap<>();

    public LootChestRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        try {
            DataStore.Repository repo = RpgServices.dataStore().repository(REPO);
            for (String key : repo.keys()) {
                java.util.Optional<Map<String, Object>> opt = repo.get(key);
                if (opt.isEmpty()) continue;
                Object table = opt.get().get("table");
                if (!(table instanceof String tableId)) continue;
                BlockKey bk = parseKey(key);
                if (bk != null) chestToTable.put(bk, tableId);
            }
        } catch (IllegalStateException ex) {
            // datastore not bootstrapped yet — chest map starts empty
        }
    }

    public void bind(Location loc, String tableId) {
        BlockKey k = BlockKey.of(loc);
        chestToTable.put(k, tableId);
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("table", tableId);
            RpgServices.dataStore().repository(REPO).save(serializeKey(k), data);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to persist loot chest at " + loc + ": " + ex.getMessage());
        }
    }

    public boolean unbind(Location loc) {
        BlockKey k = BlockKey.of(loc);
        boolean removed = chestToTable.remove(k) != null;
        if (removed) {
            try { RpgServices.dataStore().repository(REPO).delete(serializeKey(k)); }
            catch (Exception ignored) {}
        }
        return removed;
    }

    public int count() { return chestToTable.size(); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = e.getClickedBlock();
        BlockKey key = BlockKey.of(block.getLocation());
        String tableId = chestToTable.get(key);
        if (tableId == null) return;

        Set<UUID> lootedHere = looted.computeIfAbsent(key, k -> new HashSet<>());
        if (lootedHere.contains(e.getPlayer().getUniqueId())) return;
        if (!(block.getState() instanceof Container container)) return;

        LootTable table = RpgServices.lootTables().get(tableId).orElse(null);
        if (table == null) return;

        Player p = e.getPlayer();
        Inventory inv = container.getInventory();
        // LootContext was designed for mob drops (victim + damager attribution). For chests we
        // synthesize a single-damager context: the looter is the only attributed player.
        Map<Player, Double> damagers = new HashMap<>();
        damagers.put(p, 1.0);
        List<ItemStack> rolls = table.roll(new LootContext(null, damagers, p)).getOrDefault(p, List.of());
        for (ItemStack stack : rolls) {
            if (stack == null) continue;
            inv.addItem(stack);
        }
        lootedHere.add(p.getUniqueId());
    }

    private static String serializeKey(BlockKey k) {
        return k.world() + ":" + k.x() + "," + k.y() + "," + k.z();
    }

    private static BlockKey parseKey(String s) {
        try {
            int colon = s.indexOf(':');
            String world = s.substring(0, colon);
            String[] xyz = s.substring(colon + 1).split(",");
            return new BlockKey(world, Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
        } catch (Exception ex) {
            return null;
        }
    }
}
