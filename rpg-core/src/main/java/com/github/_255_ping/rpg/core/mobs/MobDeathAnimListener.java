package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

/**
 * Plays a configurable death animation (particle burst + sound) for custom RPG mobs.
 *
 * <p>Fields on the mob YAML (all optional — omit any field to skip that effect):
 * <pre>
 * DeathParticle: FLAME          # org.bukkit.Particle name
 * DeathParticleCount: 30        # default 20
 * DeathParticleSpread: 0.5      # default 0.3
 * DeathSound: ENTITY_WITHER_DEATH  # org.bukkit.Sound name
 * </pre>
 *
 * <p>The handler also zeroes the entity's velocity on death so the corpse doesn't
 * fly sideways from the final hit's knockback.
 */
public final class MobDeathAnimListener implements Listener {

    private final NamespacedKey mobIdKey;

    public MobDeathAnimListener(NamespacedKey mobIdKey) {
        this.mobIdKey = mobIdKey;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) return;

        String mobId = entity.getPersistentDataContainer().get(mobIdKey, PersistentDataType.STRING);
        if (mobId == null) return;

        RpgMob rpgMob = RpgServices.mobs().get(mobId).orElse(null);
        if (!(rpgMob instanceof CoreRpgMob def)) return;
        if (def.deathParticle() == null && def.deathSound() == null) return;

        var loc = entity.getLocation().add(0, 0.5, 0); // aim for body center
        if (loc.getWorld() == null) return;

        // Flatten velocity so the corpse doesn't skid sideways from knockback
        entity.setVelocity(new Vector(0, 0, 0));

        // Particle burst
        if (def.deathParticle() != null) {
            loc.getWorld().spawnParticle(
                    def.deathParticle(), loc,
                    def.deathParticleCount(),
                    def.deathParticleSpread(), def.deathParticleSpread(), def.deathParticleSpread(),
                    0.05);
        }

        // Death sound
        if (def.deathSound() != null) {
            loc.getWorld().playSound(loc, def.deathSound(), 1.0f, 1.0f);
        }
    }
}
