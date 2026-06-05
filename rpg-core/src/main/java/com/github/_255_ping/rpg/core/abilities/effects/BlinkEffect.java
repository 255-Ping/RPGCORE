package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Teleports the caster forward (in their look direction) up to {@code range} blocks,
 * stopping just before the first solid block they would enter.
 *
 * <p>When {@code safe=true} (default), the landing spot is snapped to the nearest safe
 * two-block-tall air pocket above solid ground, searching ±5 blocks vertically from
 * the endpoint. Set {@code safe=false} to teleport to the raw endpoint (allows wall-phasing
 * if no block is directly in the path).
 *
 * <p>Sets {@code ctx.point} to the landing location so subsequent effects (e.g. explode)
 * act at the blink destination.
 *
 * <p>Parameters:
 * <ul>
 *   <li>{@code range=12.0} — max distance in blocks</li>
 *   <li>{@code safe=true} — snap to safe ground near the endpoint</li>
 *   <li>{@code particles=true} — spawn PORTAL particle trail along the blink path</li>
 * </ul>
 */
public final class BlinkEffect implements AbilityEffect {

    private final double range;
    private final boolean safe;
    private final boolean spawnParticles;

    public BlinkEffect(Map<String, String> params) {
        this.range          = AbilityDsl.doubleParam(params, "range", 12.0);
        this.safe           = AbilityDsl.boolParam(params, "safe", true);
        this.spawnParticles = AbilityDsl.boolParam(params, "particles", true);
    }

    @Override public String name() { return "blink"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        LivingEntity caster = ctx.caster();
        if (caster == null) return CompletableFuture.completedFuture(ctx);
        World world = caster.getWorld();
        if (world == null) return CompletableFuture.completedFuture(ctx);

        Location eye = caster.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        // Raycast for the first solid block in the look direction
        RayTraceResult blockHit = world.rayTraceBlocks(
                eye, dir, range, FluidCollisionMode.NEVER, true);

        Location end;
        if (blockHit != null) {
            // Step back 0.8 blocks from the wall so the player lands in front of it
            end = blockHit.getHitPosition().toLocation(world)
                    .subtract(dir.clone().multiply(0.8));
        } else {
            end = eye.clone().add(dir.clone().multiply(range));
        }

        if (safe) {
            end = findSafeGround(world, end);
        }

        // Preserve look direction after teleport
        end.setYaw(eye.getYaw());
        end.setPitch(eye.getPitch());

        if (spawnParticles) {
            double dist = Math.max(0.1, eye.distance(end));
            int steps = Math.max(1, (int) (dist * 2.5));
            Vector step = dir.clone().multiply(dist / steps);
            Location cursor = eye.clone();
            for (int i = 0; i < steps; i++) {
                cursor.add(step);
                world.spawnParticle(Particle.PORTAL, cursor, 4, 0.1, 0.1, 0.1, 0.0);
            }
        }

        caster.teleport(end);
        ctx.setPoint(end);
        return CompletableFuture.completedFuture(ctx);
    }

    /**
     * Searches downward (up to 5 blocks) then upward (up to 5 blocks) for a safe two-block-tall
     * air pocket above a solid floor. Returns the original location if nothing safe is found.
     */
    private static Location findSafeGround(World world, Location loc) {
        int bx = loc.getBlockX();
        int bz = loc.getBlockZ();
        int byStart = (int) Math.floor(loc.getY());

        // Search downward first (most common — target is in the air above terrain)
        for (int yOff = 0; yOff >= -5; yOff--) {
            int y = byStart + yOff;
            if (!world.getBlockAt(bx, y, bz).getType().isSolid()
                    && !world.getBlockAt(bx, y + 1, bz).getType().isSolid()
                    && world.getBlockAt(bx, y - 1, bz).getType().isSolid()) {
                return new Location(world, bx + 0.5, y, bz + 0.5);
            }
        }
        // Try searching upward (target ended up inside a block)
        for (int yOff = 1; yOff <= 5; yOff++) {
            int y = byStart + yOff;
            if (!world.getBlockAt(bx, y, bz).getType().isSolid()
                    && !world.getBlockAt(bx, y + 1, bz).getType().isSolid()
                    && world.getBlockAt(bx, y - 1, bz).getType().isSolid()) {
                return new Location(world, bx + 0.5, y, bz + 0.5);
            }
        }
        // Fallback: just return the raw endpoint
        return loc;
    }
}
