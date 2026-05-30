package com.github._255_ping.rpg.core.items;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.items.BuiltinItemType;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles custom BOW and CROSSBOW items. Cancels vanilla projectile spawning and
 * replaces it with our own:
 * <ul>
 *   <li>Ammo check — if the item requires ammo (AmmoType), the player must have
 *       at least one in their inventory.</li>
 *   <li>Ammo consumption — reduced by AMMO_USAGE_REDUCTION stat (% chance to save).</li>
 *   <li>Projectile type — spawns the entity type set by ProjectileType on the item.</li>
 *   <li>Arrow damage — stored in PDC on the spawned projectile so DamagePipelineListener
 *       uses the bow's damage instead of vanilla arrow damage.</li>
 *   <li>Force scaling — damage and speed are scaled by draw force (0.0–1.0).</li>
 * </ul>
 */
public final class BowListener implements Listener {

    /** PDC key written onto custom projectiles to carry bow damage through the pipeline. */
    public static final NamespacedKey ARROW_DAMAGE_KEY = new NamespacedKey("rpg-core", "arrow_damage");

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;

    public BowListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bowStack = event.getBow();
        if (bowStack == null || bowStack.getType().isAir()) return;

        Optional<RpgItem> opt = RpgServices.items().from(bowStack);
        if (opt.isEmpty()) return;
        RpgItem item = opt.get();

        // Only handle BOW and CROSSBOW types.
        if (!(item.type() instanceof BuiltinItemType bt)) return;
        if (bt != BuiltinItemType.BOW && bt != BuiltinItemType.CROSSBOW) return;

        float force = event.getForce();
        if (force < 0.05f) return;

        // Cancel vanilla projectile and arrow consumption.
        event.setCancelled(true);
        event.setConsumeArrow(false);

        // Ammo check.
        if (!item.infiniteAmmo()) {
            String ammoId = item.ammoType();
            if (ammoId != null && !ammoId.isEmpty()) {
                double usageReduction = RpgServices.player(player).get(BuiltinStat.AMMO_USAGE_REDUCTION);
                boolean saveAmmo = usageReduction > 0
                        && ThreadLocalRandom.current().nextDouble(100.0) < usageReduction;
                if (!saveAmmo && !consumeAmmo(player, ammoId)) {
                    sendNoAmmo(player, ammoId);
                    return;
                }
            }
        }

        // Compute arrow damage: bow item's DAMAGE stat (or player DAMAGE as fallback).
        double bowDamage = item.stats().containsKey(BuiltinStat.DAMAGE)
                ? item.stats().get(BuiltinStat.DAMAGE)
                : RpgServices.player(player).get(BuiltinStat.DAMAGE);
        // Scale by force (full-draw = 1.0, half-draw = 0.5^2 = 0.25).
        double arrowDamage = bowDamage * force * force;

        // Projectile speed from stat (default 1.0).
        double speedMult = RpgServices.player(player).get(BuiltinStat.PROJECTILE_SPEED);
        if (speedMult <= 0) speedMult = 1.0;
        double speed = force * speedMult * 3.0; // 3.0 ≈ vanilla full-draw arrow speed

        spawnProjectile(player, item.projectileType(), speed, arrowDamage);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean consumeAmmo(Player player, String ammoId) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack is = player.getInventory().getItem(i);
            if (is == null || is.getType().isAir()) continue;
            Optional<RpgItem> ammoOpt = RpgServices.items().from(is);
            if (ammoOpt.isEmpty() || !ammoOpt.get().id().equals(ammoId)) continue;
            if (is.getAmount() > 1) {
                is.setAmount(is.getAmount() - 1);
                player.getInventory().setItem(i, is);
            } else {
                player.getInventory().setItem(i, null);
            }
            return true;
        }
        return false;
    }

    private void sendNoAmmo(Player player, String ammoId) {
        try {
            RpgServices.actionBar().send(player,
                    Component.text("§cNo ammo: §e" + ammoId), 20);
        } catch (IllegalStateException ignored) {
            player.sendActionBar(Component.text("§cNo ammo: §e" + ammoId));
        }
    }

    private void spawnProjectile(Player player, String projType, double speed, double damage) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();

        Entity proj = switch (projType.toUpperCase(java.util.Locale.ROOT)) {
            case "SPECTRAL_ARROW" -> {
                SpectralArrow a = player.getWorld().spawn(eye, SpectralArrow.class);
                a.setShooter(player);
                a.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
                yield a;
            }
            case "SNOWBALL" -> {
                Snowball s = player.getWorld().spawn(eye, Snowball.class);
                s.setShooter(player);
                yield s;
            }
            case "EGG" -> {
                Egg e = player.getWorld().spawn(eye, Egg.class);
                e.setShooter(player);
                yield e;
            }
            case "SMALL_FIREBALL", "FIREBALL" -> {
                SmallFireball f = player.getWorld().spawn(eye, SmallFireball.class);
                f.setShooter(player);
                yield f;
            }
            default -> {  // ARROW
                Arrow a = player.getWorld().spawn(eye, Arrow.class);
                a.setShooter(player);
                a.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
                yield a;
            }
        };

        proj.setVelocity(dir.multiply(speed));

        // Tag with our damage so DamagePipelineListener uses it instead of vanilla value.
        if (proj instanceof Projectile p) {
            try {
                p.getPersistentDataContainer().set(ARROW_DAMAGE_KEY, PersistentDataType.DOUBLE, damage);
            } catch (Exception ignored) {}
        }
    }
}
