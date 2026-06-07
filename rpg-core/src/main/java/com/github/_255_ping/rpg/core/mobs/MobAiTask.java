package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import com.github._255_ping.rpg.core.health.CoreHealthService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scans tagged RPG mobs and updates their combat target each AI tick.
 *
 * <p>Mobs with a non-empty {@code AiGoals:} list are evaluated through the goal system;
 * goals run top-to-bottom and the first one that can act wins. Mobs without a goal list
 * fall back to the legacy {@link MobAiProfile.Kind} switch.
 *
 * <p>Runs at a low cadence (default 10 ticks); vanilla pathfinding handles movement once
 * {@code Mob.setTarget()} is called.
 */
public final class MobAiTask implements Runnable {

    private final CoreMobRegistry mobs;
    private final NamespacedKey mobIdKey;
    private final FactionAlertMap factionAlerts;
    private final CoreHealthService health;

    /**
     * Spawn locations remembered per mob UUID. Populated on the first AI tick the mob is seen.
     * Used by {@link AiGoalDef.GuardRadius}. Entries are pruned lazily each run.
     */
    private final ConcurrentHashMap<UUID, Location> spawnLocations = new ConcurrentHashMap<>();

    public MobAiTask(CoreMobRegistry mobs, NamespacedKey mobIdKey,
                     FactionAlertMap factionAlerts, CoreHealthService health) {
        this.mobs = mobs;
        this.mobIdKey = mobIdKey;
        this.factionAlerts = factionAlerts;
        this.health = health;
    }

    @Override
    public void run() {
        for (World w : Bukkit.getWorlds()) {
            for (LivingEntity le : w.getLivingEntities()) {
                if (!(le instanceof Mob mob)) continue;
                String id = le.getPersistentDataContainer().get(mobIdKey, PersistentDataType.STRING);
                if (id == null) continue;
                Optional<?> def = mobs.get(id);
                if (def.isEmpty() || !(def.get() instanceof CoreRpgMob coreMob)) continue;

                if (coreMob.hasGoals()) {
                    evaluateGoals(mob, coreMob);
                } else {
                    applyProfile(mob, coreMob.aiProfile());
                }
            }
        }
        // Lazily prune dead spawn-location entries so the map stays bounded.
        spawnLocations.keySet().removeIf(uuid -> Bukkit.getEntity(uuid) == null);
    }

    // ── Legacy profile dispatch ───────────────────────────────────────────────

    private void applyProfile(Mob mob, MobAiProfile profile) {
        switch (profile.kind()) {
            case AGGRESSIVE, BOSS, SWARMING, PACK_HUNTER, FLYING, RANGED_KITER -> profileAggro(mob, profile);
            case PASSIVE, STATIONARY -> {
                if (mob.getTarget() != null) mob.setTarget(null);
            }
            case DEFENSIVE -> { /* OnHurt event listener handles immediate re-targeting */ }
        }
    }

    private void profileAggro(Mob mob, MobAiProfile profile) {
        if (mob.getTarget() instanceof Player cur && cur.isOnline()
                && cur.getLocation().distance(mob.getLocation()) <= profile.aggressionRange() * 1.5) {
            return; // already chasing a valid target
        }
        Player closest = null;
        double bestSq = profile.aggressionRange() * profile.aggressionRange();
        for (Player p : mob.getWorld().getPlayers()) {
            if (p.getGameMode() == org.bukkit.GameMode.CREATIVE
                    || p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            double dSq = p.getLocation().distanceSquared(mob.getLocation());
            if (dSq <= bestSq) { bestSq = dSq; closest = p; }
        }
        mob.setTarget(closest);
    }

    // ── Goal-list evaluation ──────────────────────────────────────────────────

    private void evaluateGoals(Mob mob, CoreRpgMob def) {
        double aggroRange = def.aiProfile().aggressionRange();
        for (AiGoalDef goal : def.aiGoals()) {
            if (applyGoal(mob, goal, aggroRange)) return;
        }
        mob.setTarget(null); // nothing matched — idle
    }

    /**
     * Try one goal. Returns {@code true} if the goal acted (target set / flee triggered)
     * so the caller can stop evaluating. Returns {@code false} to fall through to the next.
     */
    private boolean applyGoal(Mob mob, AiGoalDef goal, double aggroRange) {
        return switch (goal) {
            case AiGoalDef.AttackPlayer  g -> tryAttackPlayer(mob, aggroRange);
            case AiGoalDef.AttackFaction g -> tryAttackFaction(mob, g.faction(),
                                                    g.range() > 0 ? g.range() : aggroRange);
            case AiGoalDef.DefendFaction g -> tryDefendFaction(mob, g.faction(),
                                                    g.radius() > 0 ? g.radius() : aggroRange);
            case AiGoalDef.AssistFaction g -> tryAssistFaction(mob, g.faction(),
                                                    g.radius() > 0 ? g.radius() : aggroRange);
            case AiGoalDef.FleeFrom      g -> tryFleeFrom(mob, g.faction(),
                                                    g.range() > 0 ? g.range() : aggroRange,
                                                    g.healthThreshold());
            case AiGoalDef.CallForHelp   g -> false; // event-driven; not tick-driven
            case AiGoalDef.GuardRadius   g -> tryGuardRadius(mob, g.radius());
            case AiGoalDef.Idle          g -> { mob.setTarget(null); yield true; }
        };
    }

    // ── Individual goal implementations ──────────────────────────────────────

    private boolean tryAttackPlayer(Mob mob, double range) {
        // Keep valid player already within hysteresis zone.
        if (mob.getTarget() instanceof Player cur && cur.isOnline()
                && cur.getLocation().distanceSquared(mob.getLocation()) <= range * range * 2.25) {
            return true;
        }
        Player best = null;
        double bestSq = range * range;
        for (Player p : mob.getWorld().getPlayers()) {
            if (p.getGameMode() == org.bukkit.GameMode.CREATIVE
                    || p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            double dSq = p.getLocation().distanceSquared(mob.getLocation());
            if (dSq <= bestSq) { bestSq = dSq; best = p; }
        }
        if (best != null) { mob.setTarget(best); return true; }
        return false;
    }

    private boolean tryAttackFaction(Mob mob, String faction, double range) {
        if (faction.isBlank()) return false;
        // Keep valid same-faction target in hysteresis zone.
        LivingEntity cur = mob.getTarget();
        if (cur != null && cur.isValid() && !cur.isDead()
                && faction.equals(getFaction(cur))
                && cur.getLocation().distanceSquared(mob.getLocation()) <= range * range * 2.25) {
            return true;
        }
        LivingEntity best = null;
        double bestSq = range * range;
        for (LivingEntity nearby : mob.getWorld().getNearbyLivingEntities(mob.getLocation(), range)) {
            if (nearby == mob || nearby.isDead()) continue;
            if (!faction.equals(getFaction(nearby))) continue;
            double dSq = nearby.getLocation().distanceSquared(mob.getLocation());
            if (dSq < bestSq) { bestSq = dSq; best = nearby; }
        }
        if (best != null) { mob.setTarget(best); return true; }
        return false;
    }

    private boolean tryDefendFaction(Mob mob, String faction, double radius) {
        if (faction.isBlank()) return false;
        double radiusSq = radius * radius;
        for (var entry : factionAlerts.snapshot()) {
            UUID victimId  = entry.getKey();
            LivingEntity attacker = entry.getValue();
            // Locate the victim entity in this world.
            Entity victimEnt = mob.getWorld().getEntity(victimId);
            if (!(victimEnt instanceof LivingEntity victim) || !victim.isValid() || victim.isDead()) continue;
            if (!faction.equals(getFaction(victim))) continue;
            if (victim.getLocation().distanceSquared(mob.getLocation()) > radiusSq) continue;
            if (attacker.isValid() && !attacker.isDead()) {
                mob.setTarget(attacker);
                return true;
            }
        }
        return false;
    }

    private boolean tryAssistFaction(Mob mob, String faction, double radius) {
        if (faction.isBlank()) return false;
        for (LivingEntity nearby : mob.getWorld().getNearbyLivingEntities(mob.getLocation(), radius)) {
            if (nearby == mob || !(nearby instanceof Mob nearbyMob)) continue;
            if (!faction.equals(getFaction(nearby))) continue;
            LivingEntity target = nearbyMob.getTarget();
            if (target != null && target.isValid() && !target.isDead()) {
                mob.setTarget(target);
                return true;
            }
        }
        return false;
    }

    private boolean tryFleeFrom(Mob mob, String faction, double range, double healthThreshold) {
        if (faction.isBlank()) return false;
        // Threshold check: flee only when HP% ≤ healthThreshold (100 = always flee).
        double maxHp = health.maxHp(mob);
        if (maxHp > 0 && health.currentHp(mob) / maxHp * 100.0 > healthThreshold) return false;
        // Find nearest threat of the given faction.
        LivingEntity nearest = null;
        double bestSq = range * range;
        for (LivingEntity nearby : mob.getWorld().getNearbyLivingEntities(mob.getLocation(), range)) {
            if (nearby == mob || nearby.isDead()) continue;
            if (!faction.equals(getFaction(nearby))) continue;
            double dSq = nearby.getLocation().distanceSquared(mob.getLocation());
            if (dSq < bestSq) { bestSq = dSq; nearest = nearby; }
        }
        if (nearest == null) return false;
        // Flee: clear combat target + apply a small velocity push away each AI tick.
        mob.setTarget(null);
        Vector away = mob.getLocation().toVector().subtract(nearest.getLocation().toVector());
        if (away.lengthSquared() > 1e-6) {
            away.normalize().multiply(0.35).setY(0.05);
            mob.setVelocity(away);
        }
        return true;
    }

    /**
     * If the mob is outside its spawn-radius, disengage. Returns {@code true} only when
     * the mob is OUTSIDE radius (goal triggered), so lower-priority goals don't fire.
     * Returns {@code false} when inside radius (mob may still pursue other goals).
     */
    private boolean tryGuardRadius(Mob mob, double radius) {
        Location spawn = spawnLocations.computeIfAbsent(mob.getUniqueId(),
                k -> mob.getLocation().clone());
        if (mob.getLocation().distanceSquared(spawn) <= radius * radius) return false;
        mob.setTarget(null); // outside leash — disengage; vanilla idle will drift it back
        return true;
    }

    // ── Faction lookup helper ─────────────────────────────────────────────────

    /**
     * Returns the faction of a living entity: {@code "player"} for players,
     * the mob's {@code Faction:} tag for RPG mobs, or {@code null} for vanilla entities.
     */
    private String getFaction(LivingEntity entity) {
        if (entity instanceof Player) return "player";
        Optional<RpgMob> opt = RpgServices.mobs().from(entity);
        if (opt.isEmpty() || !(opt.get() instanceof CoreRpgMob def)) return null;
        return def.faction();
    }
}
