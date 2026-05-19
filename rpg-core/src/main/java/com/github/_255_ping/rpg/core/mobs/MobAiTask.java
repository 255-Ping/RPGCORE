package com.github._255_ping.rpg.core.mobs;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * Scans tagged RPG mobs and applies their AI profile to target acquisition. Runs at a low
 * cadence (every 10 ticks) since vanilla pathfinding handles the heavy lifting; we just
 * point mobs at a target and let MC chase.
 */
public final class MobAiTask implements Runnable {

    private final CoreMobRegistry mobs;
    private final NamespacedKey mobIdKey;

    public MobAiTask(CoreMobRegistry mobs, NamespacedKey mobIdKey) {
        this.mobs = mobs;
        this.mobIdKey = mobIdKey;
    }

    @Override
    public void run() {
        for (World w : Bukkit.getWorlds()) {
            for (LivingEntity le : w.getLivingEntities()) {
                if (!(le instanceof Mob mob)) continue;
                String id = le.getPersistentDataContainer().get(mobIdKey, PersistentDataType.STRING);
                if (id == null) continue;
                Optional<?> def = mobs.get(id);
                if (def.isEmpty()) continue;
                if (!(def.get() instanceof CoreRpgMob coreMob)) continue;

                MobAiProfile profile = coreMob.aiProfile();
                switch (profile.kind()) {
                    case AGGRESSIVE, BOSS, SWARMING, PACK_HUNTER, FLYING, RANGED_KITER -> aggro(mob, profile);
                    case PASSIVE, STATIONARY -> {
                        // Re-clear in case vanilla still ticked anything.
                        if (mob.getTarget() != null) mob.setTarget(null);
                    }
                    case DEFENSIVE -> {
                        // Don't acquire targets here; the OnHurt event listener does it.
                    }
                }
            }
        }
    }

    private void aggro(Mob mob, MobAiProfile profile) {
        if (mob.getTarget() instanceof Player current && current.isOnline()
                && current.getLocation().distance(mob.getLocation()) <= profile.aggressionRange() * 1.5) {
            return;     // already chasing a valid target
        }
        Player closest = null;
        double bestSq = profile.aggressionRange() * profile.aggressionRange();
        for (Player p : mob.getWorld().getPlayers()) {
            if (p.getGameMode() == org.bukkit.GameMode.CREATIVE
                    || p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            double dSq = p.getLocation().distanceSquared(mob.getLocation());
            if (dSq <= bestSq) {
                bestSq = dSq;
                closest = p;
            }
        }
        mob.setTarget(closest);
    }
}
