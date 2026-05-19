package com.github._255_ping.rpg.accessories;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.accessories.AccessoryService;
import com.github._255_ping.rpg.api.items.BuiltinItemType;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.persistence.DataStore;
import com.github._255_ping.rpg.api.stats.Stat;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player accessory bag inventory. ACCESSORY items inside contribute their stats via
 * {@link #aggregateStats}. Persistence is via DataStore — the inventory is serialized to
 * a base64 ItemStack blob on close / quit and restored on first open / join.
 */
public final class AccessoryBagService implements AccessoryService {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final String REPO = "accessory-bags";

    private final RpgAccessoriesPlugin plugin;
    private final Map<UUID, Inventory> bags = new ConcurrentHashMap<>();

    public AccessoryBagService(RpgAccessoriesPlugin plugin) {
        this.plugin = plugin;
    }

    public Inventory openBag(Player player) {
        Inventory bag = bags.computeIfAbsent(player.getUniqueId(), k -> {
            int rows = Math.max(1, Math.min(6, plugin.getConfig().getInt("bag.rows", 3)));
            int size = rows * 9;
            String title = plugin.getConfig().getString("bag.title", "&5&lAccessory Bag");
            Inventory inv = Bukkit.createInventory(player, size, LEGACY.deserialize(title));
            loadInto(player.getUniqueId(), inv);
            return inv;
        });
        return bag;
    }

    public boolean isBagInventory(Inventory inv) {
        return bags.containsValue(inv);
    }

    public void save(UUID id) {
        Inventory bag = bags.get(id);
        if (bag == null) return;
        try {
            String encoded = serialize(bag.getContents());
            Map<String, Object> data = new HashMap<>();
            data.put("schema-version", 1);
            data.put("contents", encoded);
            RpgServices.dataStore().repository(REPO).save(id.toString(), data);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to save accessory bag for " + id + ": " + ex.getMessage());
        }
    }

    public void release(UUID id) {
        save(id);
        bags.remove(id);
    }

    public void saveAll() {
        for (UUID id : bags.keySet()) save(id);
    }

    @Override
    public Map<Stat, Double> aggregateStats(Player player) {
        Inventory bag = bags.get(player.getUniqueId());
        if (bag == null) return Map.of();
        Map<Stat, Double> out = new HashMap<>();
        for (ItemStack stack : bag.getContents()) {
            if (stack == null) continue;
            Optional<RpgItem> opt = RpgServices.items().from(stack);
            if (opt.isEmpty()) continue;
            RpgItem item = opt.get();
            if (item.type() != BuiltinItemType.ACCESSORY) continue;
            for (Map.Entry<Stat, Double> e : item.stats().entrySet()) {
                out.merge(e.getKey(), e.getValue(), Double::sum);
            }
        }
        return out;
    }

    private void loadInto(UUID id, Inventory inv) {
        Optional<Map<String, Object>> opt = RpgServices.dataStore().repository(REPO).get(id.toString());
        if (opt.isEmpty()) return;
        Object raw = opt.get().get("contents");
        if (!(raw instanceof String encoded) || encoded.isEmpty()) return;
        try {
            ItemStack[] contents = deserialize(encoded);
            int limit = Math.min(contents.length, inv.getSize());
            for (int i = 0; i < limit; i++) inv.setItem(i, contents[i]);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to load accessory bag for " + id + ": " + ex.getMessage());
        }
    }

    private static String serialize(ItemStack[] items) throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(bos)) {
            out.writeInt(items.length);
            for (ItemStack item : items) out.writeObject(item);
            out.flush();
            return java.util.Base64.getEncoder().encodeToString(bos.toByteArray());
        }
    }

    private static ItemStack[] deserialize(String encoded) throws Exception {
        byte[] data = java.util.Base64.getDecoder().decode(encoded);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             BukkitObjectInputStream in = new BukkitObjectInputStream(bis)) {
            int len = in.readInt();
            ItemStack[] items = new ItemStack[len];
            for (int i = 0; i < len; i++) items[i] = (ItemStack) in.readObject();
            return items;
        }
    }

    @SuppressWarnings("unused")
    private static void touchDataStore(DataStore ignored) {}
}
