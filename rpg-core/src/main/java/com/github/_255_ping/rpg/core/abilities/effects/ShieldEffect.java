package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import com.github._255_ping.rpg.core.RpgCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Grants the caster (or target) a damage-absorbing shield.
 *
 * <p>The shield intercepts <em>all</em> damage at the {@link com.github._255_ping.rpg.core.health.CoreHealthService}
 * level — melee hits, ability damage, zone pulses, everything — via the static
 * {@link #absorb(UUID, double)} method called from {@code CoreHealthService.damage()}.
 * This makes shields consistent regardless of damage source.
 *
 * <p>Multiple {@code shield{}} casts stack additively. Each cast schedules its own expiry
 * task, so they expire independently.
 *
 * <p>Parameters:
 * <ul>
 *   <li>{@code amount=50} — HP the shield can absorb</li>
 *   <li>{@code duration=100} — ticks before the shield expires even if not fully depleted</li>
 *   <li>{@code target=caster} — {@code "caster"} or {@code "target"}</li>
 * </ul>
 */
public final class ShieldEffect implements AbilityEffect {

    // ── Global shield state ───────────────────────────────────────────────────
    private static final ConcurrentHashMap<UUID, Double> SHIELDS = new ConcurrentHashMap<>();

    /**
     * Called from {@code CoreHealthService.damage()} before any HP reduction.
     *
     * @param entityId entity receiving damage
     * @param incoming raw damage amount
     * @return residual damage after shield absorption (0 = fully absorbed)
     */
    public static double absorb(UUID entityId, double incoming) {
        if (incoming <= 0) return incoming;
        Double current = SHIELDS.get(entityId);
        if (current == null || current <= 0) return incoming;

        if (current >= incoming) {
            double remaining = current - incoming;
            if (remaining <= 0) {
                SHIELDS.remove(entityId);
            } else {
                SHIELDS.put(entityId, remaining);
            }
            return 0.0; // fully absorbed
        } else {
            // Partial absorption: shield breaks, residual damage passes through
            SHIELDS.remove(entityId);
            return incoming - current;
        }
    }

    /** Returns the remaining shield HP for the given entity (0 if no shield). */
    public static double getShield(UUID entityId) {
        return SHIELDS.getOrDefault(entityId, 0.0);
    }

    /** Removes a shield immediately (e.g. on player quit). */
    public static void clearShield(UUID entityId) {
        SHIELDS.remove(entityId);
    }

    // ── Instance ──────────────────────────────────────────────────────────────
    private final double amount;
    private final int duration;
    private final String targetParam;

    public ShieldEffect(Map<String, String> params) {
        this.amount      = AbilityDsl.doubleParam(params, "amount", 50.0);
        this.duration    = AbilityDsl.intParam(params, "duration", 100);
        this.targetParam = params.getOrDefault("target", "caster");
    }

    @Override public String name() { return "shield"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        if (amount <= 0) return CompletableFuture.completedFuture(ctx);
        LivingEntity entity = "target".equals(targetParam) ? ctx.target() : ctx.caster();
        if (entity == null) return CompletableFuture.completedFuture(ctx);

        UUID id = entity.getUniqueId();
        double capturedAmount = amount; // capture for lambda

        // Stack on top of any existing shield
        SHIELDS.merge(id, capturedAmount, Double::sum);

        // Visual: enchantment particles burst around the shielded entity
        entity.getWorld().spawnParticle(
                Particle.ENCHANT,
                entity.getLocation().add(0, 1, 0),
                30, 0.5, 0.8, 0.5, 0.1);

        // Schedule independent expiry: subtract only this cast's contribution
        Bukkit.getScheduler().runTaskLater(RpgCorePlugin.get(), () ->
                SHIELDS.compute(id, (k, v) -> {
                    if (v == null) return null;
                    double after = v - capturedAmount;
                    return after <= 0 ? null : after;
                }), duration);

        return CompletableFuture.completedFuture(ctx);
    }

    // ── Player-quit cleanup ───────────────────────────────────────────────────
    public static final class ShieldCleanupListener implements Listener {
        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            clearShield(event.getPlayer().getUniqueId());
        }
    }
}
