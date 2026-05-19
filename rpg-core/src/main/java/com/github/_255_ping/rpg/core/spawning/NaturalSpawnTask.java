package com.github._255_ping.rpg.core.spawning;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Walks all online players each tick; for each player, rolls each rule's
 * {@code per-player-per-tick} chance and (on hit) tries to spawn a mob at a random
 * location around them. Conditions are evaluated at the candidate location.
 *
 * <p>Tick task runs every 20 ticks (1s) by default to keep CPU low; configurable in
 * {@code natural-spawning.interval-ticks}.
 */
public final class NaturalSpawnTask implements Runnable {

    private final JavaPlugin plugin;
    private final NaturalSpawnLoader loader;
    private final int ticksPerRoll;

    public NaturalSpawnTask(JavaPlugin plugin, NaturalSpawnLoader loader, int ticksPerRoll) {
        this.plugin = plugin;
        this.loader = loader;
        this.ticksPerRoll = Math.max(1, ticksPerRoll);
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("natural-spawning.enabled", false)) return;

        List<NaturalSpawnRule> rules = loader.rules();
        if (rules.isEmpty()) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            for (NaturalSpawnRule rule : rules) {
                if (!rule.enabled()) continue;
                // Scale the per-tick chance by the tick cadence so the configured value remains "per-tick".
                double effectiveChance = rule.perPlayerPerTick() * ticksPerRoll;
                if (ThreadLocalRandom.current().nextDouble() >= effectiveChance) continue;

                Location candidate = pickCandidate(p, rule);
                if (candidate == null) continue;
                if (!conditionsPass(p, candidate, rule)) continue;

                String mobId = pickWeighted(rule.mobs());
                if (mobId == null) continue;
                Optional<RpgMob> mob = RpgServices.mobs().get(mobId);
                if (mob.isEmpty()) continue;

                mob.get().spawn(candidate);
            }
        }
    }

    private Location pickCandidate(Player p, NaturalSpawnRule rule) {
        int range = rule.maxDistanceFromPlayer() - rule.minDistanceFromPlayer();
        if (range < 0) range = 0;
        double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
        double dist = rule.minDistanceFromPlayer()
                + ThreadLocalRandom.current().nextDouble() * range;
        double dx = Math.cos(angle) * dist;
        double dz = Math.sin(angle) * dist;
        Location base = p.getLocation().clone().add(dx, 0, dz);
        // Snap to top non-air block at that XZ for surface spawns.
        int x = base.getBlockX();
        int z = base.getBlockZ();
        int y = base.getWorld().getHighestBlockYAt(x, z);
        if (rule.yMin() != null && y < rule.yMin()) return null;
        if (rule.yMax() != null && y > rule.yMax()) return null;
        Location candidate = new Location(base.getWorld(), x + 0.5, y + 1, z + 0.5);
        if (candidate.getBlock().getType() != Material.AIR) return null;
        return candidate;
    }

    private boolean conditionsPass(Player p, Location loc, NaturalSpawnRule rule) {
        if (!rule.timeOfDay().contains("any")) {
            long time = loc.getWorld().getTime();   // 0..23999
            boolean day = time < 13000;
            boolean isNight = !day;
            if (rule.timeOfDay().contains("night") && !isNight) return false;
            if (rule.timeOfDay().contains("day") && !day) return false;
        }
        if (!rule.weather().contains("any")) {
            boolean storming = loc.getWorld().hasStorm();
            boolean thundering = loc.getWorld().isThundering();
            String wKey = thundering ? "storm" : storming ? "rain" : "clear";
            if (!rule.weather().contains(wKey)) return false;
        }
        if (!rule.biomes().contains("any")) {
            String biome = loc.getBlock().getBiome().getKey().getKey().toLowerCase(Locale.ROOT);
            if (!rule.biomes().contains(biome)) return false;
        }
        if (rule.lightMin() != null && loc.getBlock().getLightLevel() < rule.lightMin()) return false;
        if (rule.lightMax() != null && loc.getBlock().getLightLevel() > rule.lightMax()) return false;
        return true;
    }

    private static String pickWeighted(List<NaturalSpawnRule.WeightedMob> mobs) {
        int total = 0;
        for (NaturalSpawnRule.WeightedMob m : mobs) total += Math.max(0, m.weight());
        if (total <= 0) return null;
        int roll = ThreadLocalRandom.current().nextInt(total);
        int acc = 0;
        for (NaturalSpawnRule.WeightedMob m : mobs) {
            acc += Math.max(0, m.weight());
            if (roll < acc) return m.mobId();
        }
        return null;
    }
}
