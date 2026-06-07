package com.github._255_ping.rpg.core.mobs;

import com.destroystokyo.paper.event.entity.EntityJumpEvent;
import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;
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
     */
    private final ConcurrentHashMap<UUID, LivingEntity> mobKillerCache = new ConcurrentHashMap<>();

    private final FactionAlertMap factionAlerts;

    public MobAbilityEventListener(FactionAlertMap factionAlerts) {
        this.factionAlerts = factionAlerts;
    }

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
            // Legacy DEFENSIVE profile: immediately re-target the attacker when hurt.
            if (victim instanceof Mob mob && attacker != null) {
                Optional<RpgMob> opt = RpgServices.mobs().from(victim);
                if (opt.isPresent() && opt.get() instanceof CoreRpgMob coreMob) {
                    if (!coreMob.hasGoals()
                            && coreMob.aiProfile().kind() == MobAiProfile.Kind.DEFENSIVE) {
                        mob.setTarget(attacker);
                    }
                    // Faction system: record the attack for defend_faction goals, and
                    // fire any call_for_help goals immediately so allies respond at once.
                    if (attacker != null) {
                        factionAlerts.recordAttack(victim.getUniqueId(), attacker);
                        for (AiGoalDef goal : coreMob.aiGoals()) {
                            if (goal instanceof AiGoalDef.CallForHelp cfh && !cfh.faction().isBlank()) {
                                alertNearbyFaction(victim, attacker, cfh.faction(), cfh.radius());
                            }
                        }
                    }
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

        // Clean up the faction-alert entry so the map doesn't accumulate dead mobs.
        factionAlerts.remove(dead.getUniqueId());
    }

    // ── EntityJumpEvent — OnJump ───────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobJump(EntityJumpEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob)) return;
        if (mob instanceof Player) return;
        fire(mob, null, MobAbilityTrigger.OnJump.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Alert all idle mobs of {@code faction} within {@code radius} of {@code victim}
     * to target {@code attacker}. Only mobs without an existing target are alerted so we
     * don't interrupt mobs already engaged in other fights.
     */
    private static void alertNearbyFaction(LivingEntity victim, LivingEntity attacker,
                                           String faction, double radius) {
        for (LivingEntity nearby : victim.getWorld().getNearbyLivingEntities(victim.getLocation(), radius)) {
            if (nearby == victim || !(nearby instanceof Mob nearbyMob)) continue;
            if (nearbyMob.getTarget() != null) continue; // already fighting — don't interrupt
            Optional<RpgMob> opt = RpgServices.mobs().from(nearby);
            if (opt.isEmpty() || !(opt.get() instanceof CoreRpgMob def)) continue;
            if (!faction.equals(def.faction())) continue;
            nearbyMob.setTarget(attacker);
        }
    }

    private static void fire(LivingEntity mob, LivingEntity hint, Class<? extends MobAbilityTrigger> triggerClass) {
        Optional<RpgMob> opt = RpgServices.mobs().from(mob);
        if (opt.isEmpty()) return;
        if (!(opt.get() instanceof CoreRpgMob def)) return;
        MobAbilityRuntime.fireTrigger(mob, def, triggerClass, hint);
    }
}
