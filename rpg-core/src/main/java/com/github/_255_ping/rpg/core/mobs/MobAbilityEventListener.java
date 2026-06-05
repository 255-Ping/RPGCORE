package com.github._255_ping.rpg.core.mobs;

import com.destroystokyo.paper.event.entity.EntityJumpEvent;
import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fires event-driven mob ability triggers:
 * <ul>
 *   <li>{@link MobAbilityTrigger.OnHit}    — mob dealt RPG damage (PostDamageEvent MONITOR)</li>
 *   <li>{@link MobAbilityTrigger.OnHurt}   — mob received RPG damage (PostDamageEvent MONITOR)</li>
 *   <li>{@link MobAbilityTrigger.OnDeath}  — mob died (EntityDeathEvent MONITOR)</li>
 *   <li>{@link MobAbilityTrigger.OnAttack} — mob initiated a melee attack (EntityDamageByEntityEvent LOWEST)</li>
 *   <li>{@link MobAbilityTrigger.OnKill}   — mob landed the killing blow on any entity (EntityDeathEvent MONITOR)</li>
 *   <li>{@link MobAbilityTrigger.OnJump}   — mob jumped (EntityJumpEvent MONITOR)</li>
 * </ul>
 * OnTimer is handled by {@link MobAbilityTimerTask}; OnSpawn is fired by
 * {@link CoreRpgMob#spawn} directly.
 */
public final class MobAbilityEventListener implements Listener {

    /**
     * Tracks the last non-player entity that dealt RPG damage to each victim.
     * Used to attribute OnKill when the victim dies. Cleared on each death.
     * Value can safely outlive a short-lived entity because we check {@code isValid()} before use.
     */
    private final ConcurrentHashMap<UUID, LivingEntity> mobKillerCache = new ConcurrentHashMap<>();

    // ── PostDamageEvent — OnHit / OnHurt + kill-attribution cache ─────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPostDamage(PostDamageEvent event) {
        LivingEntity attacker = event.context().attacker();
        LivingEntity victim = event.context().victim();

        // Cache the last mob attacker for OnKill attribution.
        if (attacker != null && !(attacker instanceof Player) && victim != null) {
            mobKillerCache.put(victim.getUniqueId(), attacker);
        }

        if (attacker != null) fire(attacker, victim, MobAbilityTrigger.OnHit.class);
        if (victim != null) {
            fire(victim, attacker, MobAbilityTrigger.OnHurt.class);
            // Defensive AI: re-target the attacker.
            if (victim instanceof org.bukkit.entity.Mob mob && attacker != null) {
                Optional<RpgMob> opt = RpgServices.mobs().from(victim);
                if (opt.isPresent() && opt.get() instanceof CoreRpgMob coreMob
                        && coreMob.aiProfile().kind() == MobAiProfile.Kind.DEFENSIVE) {
                    mob.setTarget(attacker);
                }
            }
        }
    }

    // ── EntityDamageByEntityEvent LOWEST — OnAttack ────────────────────────────

    /**
     * Fires {@link MobAbilityTrigger.OnAttack} when a mob initiates an attack, before the
     * damage pipeline cancels the vanilla event. This is the "swing attempt" trigger —
     * it fires even if the hit is later cancelled or dodged.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onMobAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;
        if (attacker instanceof Player) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        fire(attacker, victim, MobAbilityTrigger.OnAttack.class);
    }

    // ── EntityDeathEvent — OnDeath + OnKill ────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();

        // OnDeath for the dying mob.
        fire(dead, null, MobAbilityTrigger.OnDeath.class);

        // OnKill for the mob that landed the killing blow (if any).
        LivingEntity killer = mobKillerCache.remove(dead.getUniqueId());
        if (killer != null && killer.isValid() && !killer.isDead()) {
            fire(killer, dead, MobAbilityTrigger.OnKill.class);
        }
    }

    // ── EntityJumpEvent — OnJump ───────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobJump(EntityJumpEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob)) return;
        if (mob instanceof Player) return;
        fire(mob, null, MobAbilityTrigger.OnJump.class);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static void fire(LivingEntity mob, LivingEntity hint, Class<? extends MobAbilityTrigger> triggerClass) {
        Optional<RpgMob> opt = RpgServices.mobs().from(mob);
        if (opt.isEmpty()) return;
        if (!(opt.get() instanceof CoreRpgMob def)) return;
        MobAbilityRuntime.fireTrigger(mob, def, triggerClass, hint);
    }
}
