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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Creates a persistent damage zone at the caster's feet (or at a preceding beam's hit point
 * when {@code use_point=true}). Every {@code interval} ticks, all living entities inside the
 * radius take damage and/or receive a status effect. The zone expires after {@code duration}
 * ticks.
 *
 * <p>A server-wide {@code abilities.zone.max-active} config cap prevents lag from zone spam
 * (default 50). Zones belonging to a player who disconnects are immediately removed.
 *
 * <p>Parameters:
 * <ul>
 *   <li>{@code radius=4.0} — zone radius in blocks</li>
 *   <li>{@code duration=100} — total lifetime in ticks</li>
 *   <li>{@code interval=20} — ticks between damage/effect pulses</li>
 *   <li>{@code damage=5.0} — RPG HP dealt per pulse (0 = no damage)</li>
 *   <li>{@code status=} — status effect ID applied to entities per pulse (empty = none)</li>
 *   <li>{@code status_level=1} — level of the applied status effect</li>
 *   <li>{@code status_duration=60} — ticks the applied status lasts</li>
 *   <li>{@code particle=FLAME} — Particle enum name for the zone outline and pulse burst</li>
 *   <li>{@code use_point=false} — if true, places the zone at {@code ctx.point} (e.g. beam
 *       endpoint) rather than the caster's feet</li>
 * </ul>
 */
public final class ZoneEffect implements AbilityEffect {

    // ── Global zone registry ──────────────────────────────────────────────────
    static final CopyOnWriteArrayList<ActiveZone> ACTIVE = new CopyOnWriteArrayList<>();
    private static volatile int maxActive = 50;

    /** Called once on plugin enable to read the config cap. */
    public static void init(int configuredMax) {
        maxActive = configuredMax;
    }

    /** Called every tick by RpgCorePlugin. Ticks all active zones; removes expired ones. */
    public static void tickAll() {
        if (ACTIVE.isEmpty()) return;
        ACTIVE.removeIf(ActiveZone::tick);
    }

    /** Removes all zones owned by the given player (called on PlayerQuitEvent). */
    public static void clearForPlayer(UUID uuid) {
        ACTIVE.removeIf(z -> uuid.equals(z.ownerUuid));
    }

    // ── Instance (ability invocation params) ─────────────────────────────────
    private final double radius;
    private final int duration;
    private final int interval;
    private final double damage;
    private final String statusId;
    private final int statusLevel;
    private final int statusDurationTicks;
    private final Particle particle;
    private final boolean usePoint;

    public ZoneEffect(Map<String, String> params) {
        this.radius              = AbilityDsl.doubleParam(params, "radius", 4.0);
        this.duration            = AbilityDsl.intParam(params, "duration", 100);
        this.interval            = AbilityDsl.intParam(params, "interval", 20);
        this.damage              = AbilityDsl.doubleParam(params, "damage", 5.0);
        this.statusId            = params.getOrDefault("status", "");
        this.statusLevel         = AbilityDsl.intParam(params, "status_level", 1);
        this.statusDurationTicks = AbilityDsl.intParam(params, "status_duration", 60);
        this.particle            = parseParticle(params.getOrDefault("particle", "FLAME"));
        this.usePoint            = AbilityDsl.boolParam(params, "use_point", false);
    }

    @Override public String name() { return "zone"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        LivingEntity caster = ctx.caster();
        if (caster == null || caster.getWorld() == null) {
            return CompletableFuture.completedFuture(ctx);
        }
        if (ACTIVE.size() >= maxActive) return CompletableFuture.completedFuture(ctx);

        // Default: caster's feet (ground-level). use_point=true lets admins chain with beam{}.
        Location center = (usePoint && ctx.point() != null)
                ? ctx.point().clone()
                : caster.getLocation().clone();

        ACTIVE.add(new ActiveZone(caster.getUniqueId(), center, radius, duration, interval,
                damage, statusId, statusLevel, statusDurationTicks, particle, caster));
        return CompletableFuture.completedFuture(ctx);
    }

    private static Particle parseParticle(String name) {
        try { return Particle.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return Particle.FLAME; }
    }

    // ── Player-quit cleanup ───────────────────────────────────────────────────
    public static final class ZoneCleanupListener implements Listener {
        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            clearForPlayer(event.getPlayer().getUniqueId());
        }
    }

    // ── Per-zone mutable state ────────────────────────────────────────────────
    static final class ActiveZone {
        private static final ThreadLocal<Boolean> FIRING_POST = ThreadLocal.withInitial(() -> false);

        final UUID ownerUuid;
        final Location center;
        final double radius;
        final double damage;
        final String statusId;
        final int statusLevel;
        final int statusDurationTicks;
        final int intervalTicks;
        final Particle particle;
        final LivingEntity owner;

        int ticksRemaining;
        int nextFireIn;
        int nextParticleIn;

        ActiveZone(UUID ownerUuid, Location center, double radius, int duration, int interval,
                   double damage, String statusId, int statusLevel, int statusDurationTicks,
                   Particle particle, LivingEntity owner) {
            this.ownerUuid           = ownerUuid;
            this.center              = center;
            this.radius              = radius;
            this.damage              = damage;
            this.statusId            = statusId;
            this.statusLevel         = statusLevel;
            this.statusDurationTicks = statusDurationTicks;
            this.intervalTicks       = interval;
            this.particle            = particle;
            this.owner               = owner;
            this.ticksRemaining      = duration;
            this.nextFireIn          = Math.max(1, interval);
            this.nextParticleIn      = 5; // first ring draws quickly
        }

        /** Returns {@code true} when the zone has expired and should be removed. */
        boolean tick() {
            ticksRemaining--;
            nextFireIn--;
            nextParticleIn--;

            if (nextParticleIn <= 0) {
                drawOutlineRing();
                nextParticleIn = 20; // refresh outline every second
            }
            if (nextFireIn <= 0) {
                firePulse();
                nextFireIn = intervalTicks;
            }

            return ticksRemaining <= 0;
        }

        /** Draws a circle of particles at ground level around the zone perimeter. */
        private void drawOutlineRing() {
            World world = center.getWorld();
            if (world == null) return;
            int points = Math.max(12, (int) (radius * 8));
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);
                world.spawnParticle(particle, x, center.getY() + 0.1, z, 1, 0, 0, 0, 0);
            }
        }

        /** Damages and/or applies status to all entities in the zone. */
        private void firePulse() {
            World world = center.getWorld();
            if (world == null) return;

            // Burst particles at the center to signal a pulse
            world.spawnParticle(particle, center.clone().add(0, 0.3, 0),
                    15, radius * 0.3, 0.3, radius * 0.3, 0.02);

            for (Entity e : world.getNearbyEntities(center, radius, radius * 0.75 + 1, radius)) {
                if (!(e instanceof LivingEntity le)) continue;
                if (le.equals(owner)) continue;
                if (le instanceof Player p && p.getGameMode() == GameMode.CREATIVE) continue;

                if (damage > 0) {
                    RpgServices.health().damage(le, damage, "ability_zone");
                    if (!FIRING_POST.get()) {
                        FIRING_POST.set(true);
                        try {
                            // Attacker is null: zone pulses are environmental AoE, not a direct
                            // hit from the owner mob/player. A null attacker prevents mob ~onHit
                            // abilities from re-triggering on every zone pulse (e.g. freeze
                            // refreshing every second from a frost golem's zone). The victim's
                            // ~on_hurt item procs still fire correctly via the victim field.
                            DamageContext dCtx = new DamageContext(null, le, damage, "ability_zone");
                            Bukkit.getPluginManager().callEvent(new PostDamageEvent(dCtx, damage));
                        } finally {
                            FIRING_POST.set(false);
                        }
                    }
                }

                if (!statusId.isBlank()) {
                    RpgServices.statusEffects().apply(
                            le, statusId, statusLevel, statusDurationTicks, ownerUuid.toString());
                }
            }
        }
    }
}
