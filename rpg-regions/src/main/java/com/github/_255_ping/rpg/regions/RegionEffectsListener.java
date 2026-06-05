package com.github._255_ping.rpg.regions;

import com.github._255_ping.rpg.api.regions.RegionEnterEvent;
import com.github._255_ping.rpg.api.regions.RegionLeaveEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Enforces the region flag set added in v0.6.0:
 * <ul>
 *   <li>{@code enter-message} (String) — title shown on region enter. Supports {@code {player}} and {@code {region}}
 *       placeholders and {@code &} colour codes. Prefix with {@code [actionbar]} to show as action bar instead.</li>
 *   <li>{@code leave-message} (String) — same, on leave.</li>
 *   <li>{@code no-mob-spawn} (boolean) — cancels natural + spawner mob spawning inside the region.</li>
 *   <li>{@code no-damage} (boolean) — players inside take no damage from any source.</li>
 *   <li>{@code fly} (boolean) — grants flight to players while inside; revoked on exit
 *       (unless the player already had flight from their permissions).</li>
 *   <li>{@code no-item-drop} (boolean) — items dropped inside are immediately returned
 *       to the player; they land at the player's feet if the inventory is full.</li>
 *   <li>{@code keep-inventory} (boolean) — overrides death rules: player keeps items on death
 *       inside this region.</li>
 * </ul>
 *
 * <p>All flags above are consulted from the highest-priority region at the relevant location
 * via {@link CoreRegionService}. Global flags apply if no region overrides them.
 */
public final class RegionEffectsListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final Title.Times TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(500), Duration.ofMillis(2500), Duration.ofMillis(500));

    /** Players whose flight was granted by a region (not by their own permission). */
    private final Set<UUID> regionFlight = Collections.synchronizedSet(new HashSet<>());

    private final CoreRegionService regions;

    public RegionEffectsListener(CoreRegionService regions) {
        this.regions = regions;
    }

    // ── Enter / Leave messages + fly ──────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnter(RegionEnterEvent event) {
        Player player = event.getPlayer();
        var region = event.region();

        // ── Enter message ──────────────────────────────────────────────────
        Object msgObj = region.flags().get("enter-message");
        if (msgObj instanceof String raw && !raw.isBlank()) {
            showMessage(player, raw, region.id());
        }

        // ── Fly ────────────────────────────────────────────────────────────
        if (flagBool(region, "fly")) {
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
                regionFlight.add(player.getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeave(RegionLeaveEvent event) {
        Player player = event.getPlayer();
        var region = event.region();

        // ── Leave message ──────────────────────────────────────────────────
        Object msgObj = region.flags().get("leave-message");
        if (msgObj instanceof String raw && !raw.isBlank()) {
            showMessage(player, raw, region.id());
        }

        // ── Fly revoke ─────────────────────────────────────────────────────
        if (flagBool(region, "fly") && regionFlight.contains(player.getUniqueId())) {
            // Only revoke if no other current region grants fly.
            boolean stillFlying = regions.regionsAt(player.getLocation()).stream()
                    .anyMatch(r -> r != region && flagBool(r, "fly"));
            if (!stillFlying) {
                regionFlight.remove(player.getUniqueId());
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
    }

    // ── no-mob-spawn ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        // Allow admin/plugin spawns to bypass; only block natural + spawner sources.
        if (reason == CreatureSpawnEvent.SpawnReason.CUSTOM
                || reason == CreatureSpawnEvent.SpawnReason.COMMAND
                || reason == CreatureSpawnEvent.SpawnReason.BREEDING
                || reason == CreatureSpawnEvent.SpawnReason.EGG
                || reason == CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM
                || reason == CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN
                || reason == CreatureSpawnEvent.SpawnReason.BUILD_WITHER) return;

        Entity entity = event.getEntity();
        if (entity instanceof Player) return;

        if (regions.flag(entity.getLocation(), "no-mob-spawn", false)) {
            event.setCancelled(true);
        }
    }

    // ── no-damage ─────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (regions.flag(p.getLocation(), "no-damage", false)) {
            event.setCancelled(true);
        }
    }

    // ── no-item-drop ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        if (!regions.flag(p.getLocation(), "no-item-drop", false)) return;
        event.setCancelled(true);
        // Item never leaves the player's hand — cancel is sufficient (item stays in inventory).
    }

    // ── keep-inventory ────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        if (regions.flag(p.getLocation(), "keep-inventory", false)) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean flagBool(com.github._255_ping.rpg.api.regions.Region r, String key) {
        Object v = r.flags().get(key);
        return v instanceof Boolean b && b;
    }

    private static void showMessage(Player player, String raw, String regionId) {
        String expanded = raw
                .replace("{player}", player.getName())
                .replace("{region}", regionId);

        if (raw.startsWith("[actionbar]")) {
            String text = expanded.substring("[actionbar]".length()).trim();
            player.sendActionBar(LEGACY.deserialize(text));
        } else {
            // Split first newline into title + subtitle if a newline is present.
            String[] parts = expanded.split("\\\\n", 2);
            Component title    = LEGACY.deserialize(parts[0].trim());
            Component subtitle = parts.length > 1 ? LEGACY.deserialize(parts[1].trim()) : Component.empty();
            player.showTitle(Title.title(title, subtitle, TITLE_TIMES));
        }
    }
}
