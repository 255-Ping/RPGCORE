package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import com.github._255_ping.rpg.api.damage.DamageContext;
import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Bounces damage from the current target (or {@code ctx.point}) to the N nearest surrounding
 * living entities within {@code range}. Each chained entity receives {@code carriedDamage *
 * damage_multiplier} damage. Enchant particles arc between each hop to sell the lightning feel.
 *
 * <p>Chain does NOT change {@code ctx.target} — the primary target is unaffected by this
 * effect. Chain applies its own independent damage to each bounce target.
 *
 * <p>Best used after a {@code beam{}} that sets {@code ctx.target}: the chain bounces off the
 * primary beam target outward to nearby mobs. Can also be used standalone — the origin point
 * is the caster's location when no target or point is set.
 *
 * <p>Parameters:
 * <ul>
 *   <li>{@code count=3} — maximum bounce targets</li>
 *   <li>{@code range=8.0} — search radius in blocks</li>
 *   <li>{@code damage_multiplier=0.7} — fraction of {@code carriedDamage} dealt per bounce</li>
 * </ul>
 */
public final class ChainEffect implements AbilityEffect {

    private static final ThreadLocal<Boolean> FIRING_POST = ThreadLocal.withInitial(() -> false);

    private final int count;
    private final double range;
    private final double damageMultiplier;

    public ChainEffect(Map<String, String> params) {
        this.count            = AbilityDsl.intParam(params, "count", 3);
        this.range            = AbilityDsl.doubleParam(params, "range", 8.0);
        this.damageMultiplier = AbilityDsl.doubleParam(params, "damage_multiplier", 0.7);
    }

    @Override public String name() { return "chain"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (ctx.caster() == null) return CompletableFuture.completedFuture(ctx);

        double damage = ctx.carriedDamage() * damageMultiplier;
        if (damage <= 0) return CompletableFuture.completedFuture(ctx);

        // Origin: primary target → ctx.point → caster location (priority order)
        Location origin = ctx.target() != null ? ctx.target().getLocation()
                : ctx.point() != null          ? ctx.point()
                : ctx.caster().getLocation();

        World world = origin.getWorld();
        if (world == null) return CompletableFuture.completedFuture(ctx);

        List<LivingEntity> bounceTargets = world
                .getNearbyEntities(origin, range, range, range, e ->
                        e instanceof LivingEntity
                        && !e.equals(ctx.caster())
                        && !e.equals(ctx.target()))
                .stream()
                .map(e -> (LivingEntity) e)
                .sorted(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(origin)))
                .limit(count)
                .toList();

        if (bounceTargets.isEmpty()) return CompletableFuture.completedFuture(ctx);

        Location prev = ctx.target() != null
                ? ctx.target().getLocation().add(0, 1, 0)
                : origin.clone().add(0, 1, 0);

        for (LivingEntity t : bounceTargets) {
            if (t instanceof Player p && p.getGameMode() == GameMode.CREATIVE) continue;

            RpgServices.health().damage(t, damage, "ability_chain");

            if (!FIRING_POST.get()) {
                FIRING_POST.set(true);
                try {
                    DamageContext dCtx = new DamageContext(ctx.caster(), t, damage, "ability_chain");
                    Bukkit.getPluginManager().callEvent(new PostDamageEvent(dCtx, damage));
                } finally {
                    FIRING_POST.set(false);
                }
            }

            // Particle arc from previous entity to this one
            Location next = t.getLocation().add(0, 1, 0);
            drawParticleLine(world, prev, next, Particle.ENCHANT, 30);
            prev = next;
        }

        return CompletableFuture.completedFuture(ctx);
    }

    private static void drawParticleLine(World world, Location from, Location to,
                                         Particle particle, int steps) {
        if (!from.getWorld().equals(to.getWorld())) return;
        double dx = (to.getX() - from.getX()) / steps;
        double dy = (to.getY() - from.getY()) / steps;
        double dz = (to.getZ() - from.getZ()) / steps;
        double cx = from.getX(), cy = from.getY(), cz = from.getZ();
        for (int i = 0; i <= steps; i++) {
            world.spawnParticle(particle, cx, cy, cz, 1, 0, 0, 0, 0);
            cx += dx; cy += dy; cz += dz;
        }
    }
}
