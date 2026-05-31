package com.github._255_ping.rpg.core.drops;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player item drop system.
 *
 * <p>When {@link #register(Item, Player)} is called, the dropped item is tagged with the
 * owner's UUID and a TextDisplay hologram is spawned above it. Only the owner (or anyone
 * after the release window expires) can pick the item up.
 *
 * <p>If the owner has {@code AUTO_LOOT > 0}, the item is immediately given to them
 * (teleported to their feet with zero pickup delay).
 *
 * <p>Config keys (in {@code rpg-core/config.yml} under {@code per-player-drops}):
 * <ul>
 *   <li>{@code enabled} — master toggle (default true)</li>
 *   <li>{@code release-seconds} — seconds before drop becomes public (default 30)</li>
 * </ul>
 */
public final class DropManager implements Listener {

    /** PDC key on Item entities — value is owner player UUID as String. */
    public static final NamespacedKey OWNER_KEY = new NamespacedKey("rpg-core", "drop_owner");
    /** PDC key on Item entities — expiry as epoch-ms Long. */
    public static final NamespacedKey EXPIRY_KEY = new NamespacedKey("rpg-core", "drop_expiry");

    private record DropEntry(UUID ownerUuid, String ownerName, long expiryMs, UUID hologramUuid) {}

    private final JavaPlugin plugin;
    private final ConcurrentHashMap<UUID, DropEntry> entries = new ConcurrentHashMap<>();

    public DropManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Tag a dropped item as owned by {@code owner}. Spawns a hologram and handles auto-loot.
     * No-op if per-player-drops is disabled in config.
     */
    public void register(Item item, Player owner) {
        if (!plugin.getConfig().getBoolean("per-player-drops.enabled", true)) return;
        int releaseSeconds = plugin.getConfig().getInt("per-player-drops.release-seconds", 30);
        long expiryMs = System.currentTimeMillis() + releaseSeconds * 1000L;

        // Tag item entity with PDC.
        item.getPersistentDataContainer().set(OWNER_KEY, PersistentDataType.STRING, owner.getUniqueId().toString());
        item.getPersistentDataContainer().set(EXPIRY_KEY, PersistentDataType.LONG, expiryMs);
        item.setPickupDelay(20); // prevent immediate pickup for 1 tick

        // Spawn hologram.
        UUID holoId = spawnHologram(item, owner);
        entries.put(item.getUniqueId(), new DropEntry(owner.getUniqueId(), owner.getName(), expiryMs, holoId));

        // Auto-loot: if owner has AUTO_LOOT stat, give to them directly.
        try {
            double autoLoot = RpgServices.player(owner).get(BuiltinStat.AUTO_LOOT);
            if (autoLoot > 0) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!item.isValid()) return;
                    HashMap<Integer, ItemStack> overflow = owner.getInventory().addItem(item.getItemStack().clone());
                    if (overflow.isEmpty()) {
                        removeHologram(entries.get(item.getUniqueId()));
                        entries.remove(item.getUniqueId());
                        item.remove();
                    } else {
                        // Inventory full — keep the item on the ground (stays locked to owner).
                        owner.sendMessage(Component.text("§eInventory full! Drop waiting on the ground."));
                    }
                });
            }
        } catch (IllegalStateException ignored) {}
    }

    // ── Events ────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPickupAttempt(PlayerAttemptPickupItemEvent event) {
        Item item = event.getItem();
        DropEntry entry = entries.get(item.getUniqueId());
        if (entry == null) {
            // Check PDC for persistent ownership (survives plugin reload).
            String ownerStr = item.getPersistentDataContainer().get(OWNER_KEY, PersistentDataType.STRING);
            if (ownerStr == null) return;
            Long expiry = item.getPersistentDataContainer().get(EXPIRY_KEY, PersistentDataType.LONG);
            if (expiry != null && System.currentTimeMillis() > expiry) return; // expired, anyone can pick up
            UUID ownerUuid;
            try { ownerUuid = UUID.fromString(ownerStr); } catch (IllegalArgumentException ex) { return; }
            if (!event.getPlayer().getUniqueId().equals(ownerUuid)) {
                event.setCancelled(true);
            }
            return;
        }
        // Check expiry.
        if (System.currentTimeMillis() > entry.expiryMs()) {
            // Released — remove from tracking and allow pickup.
            removeHologram(entry);
            entries.remove(item.getUniqueId());
            return;
        }
        // Still owned: only owner can pick up.
        if (!event.getPlayer().getUniqueId().equals(entry.ownerUuid())) {
            event.setCancelled(true);
            return;
        }
        // Owner picking up — clean up hologram at MONITOR.
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickupMonitor(PlayerAttemptPickupItemEvent event) {
        DropEntry entry = entries.remove(event.getItem().getUniqueId());
        if (entry != null) removeHologram(entry);
    }

    @EventHandler
    public void onDespawn(ItemDespawnEvent event) {
        DropEntry entry = entries.remove(event.getEntity().getUniqueId());
        if (entry != null) removeHologram(entry);
    }

    // ── Hologram ──────────────────────────────────────────────────────────

    private UUID spawnHologram(Item item, Player owner) {
        if (item.getWorld() == null) return null;
        TextDisplay td = (TextDisplay) item.getWorld().spawnEntity(item.getLocation(), EntityType.TEXT_DISPLAY);
        td.setBillboard(Display.Billboard.CENTER);
        td.setPersistent(false);
        td.setViewRange(0.5f); // only visible up close — ~8 blocks
        td.setSeeThrough(false);
        // Float the label above the item via translation offset; making it a passenger
        // of the item entity so it tracks wherever the item entity moves.
        td.setTransformation(new Transformation(
                new Vector3f(0f, 0.5f, 0f),
                new Quaternionf(),
                new Vector3f(1f, 1f, 1f),
                new Quaternionf()));

        String itemName = getItemName(item.getItemStack());
        Component text = Component.text("§e" + itemName + "\n§7→ §b" + owner.getName())
                .decoration(TextDecoration.ITALIC, false);
        td.text(text);
        item.addPassenger(td);
        return td.getUniqueId();
    }

    private void removeHologram(DropEntry entry) {
        if (entry == null || entry.hologramUuid() == null) return;
        var ent = plugin.getServer().getEntity(entry.hologramUuid());
        if (ent != null) ent.remove();
    }

    private static String getItemName(ItemStack stack) {
        try {
            Optional<RpgItem> opt = RpgServices.items().from(stack);
            if (opt.isPresent() && opt.get().displayName() != null) {
                // Strip color codes for plain text.
                return opt.get().displayName().replaceAll("&[0-9a-fA-Fk-orK-OR]", "");
            }
        } catch (IllegalStateException ignored) {}
        // Fallback: format the material name.
        String raw = stack.getType().name().toLowerCase().replace("_", " ");
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
