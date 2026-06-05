package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import com.github._255_ping.rpg.core.RpgCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tags the target with a mark that causes the next {@code damage{}} hit against it to deal
 * bonus damage (and consume the mark), or all hits within a time window when
 * {@code consume=false}.
 *
 * <p>The multiplier is applied in {@link DamageEffect} via {@link #consumeMark(UUID)}.
 * Only {@code damage{}} checks marks; other damage effects ({@code drain}, {@code chain},
 * zones) do not detonate marks.
 *
 * <p>Parameters:
 * <ul>
 *   <li>{@code bonus=2.0} — damage multiplier on the detonating hit (2.0 = double damage)</li>
 *   <li>{@code duration=200} — ticks before the mark expires if not consumed</li>
 *   <li>{@code consume=true} — true = one-shot mark (consumed on first {@code damage{}} hit);
 *       false = mark persists for full duration, bonus applies to every {@code damage{}} hit</li>
 *   <li>{@code target=target} — {@code "target"} or {@code "caster"}</li>
 * </ul>
 */
public final class MarkEffect implements AbilityEffect {

    // ── Global mark state ─────────────────────────────────────────────────────
    static final ConcurrentHashMap<UUID, MarkState> MARKS = new ConcurrentHashMap<>();

    /**
     * Called from {@link DamageEffect} before damage is applied.
     * If the entity is marked and {@code consume=true}, removes the mark and returns its bonus.
     * If {@code consume=false}, returns the bonus without removing the mark.
     * Returns {@code 1.0} if no mark.
     */
    public static double consumeMark(UUID targetId) {
        MarkState mark = MARKS.get(targetId);
        if (mark == null) return 1.0;
        if (mark.consume()) {
            MARKS.remove(targetId);
        }
        return mark.bonus();
    }

    public static boolean isMarked(UUID entityId) {
        return MARKS.containsKey(entityId);
    }

    public static void clearMark(UUID entityId) {
        MARKS.remove(entityId);
    }

    // ── Instance ──────────────────────────────────────────────────────────────
    private final double bonus;
    private final int duration;
    private final boolean consume;
    private final String targetParam;

    public MarkEffect(Map<String, String> params) {
        this.bonus       = AbilityDsl.doubleParam(params, "bonus", 2.0);
        this.duration    = AbilityDsl.intParam(params, "duration", 200);
        this.consume     = AbilityDsl.boolParam(params, "consume", true);
        this.targetParam = params.getOrDefault("target", "target");
    }

    @Override public String name() { return "mark"; }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        LivingEntity entity = "caster".equals(targetParam) ? ctx.caster() : ctx.target();
        if (entity == null) return CompletableFuture.completedFuture(ctx);

        UUID id = entity.getUniqueId();
        boolean capturedConsume = consume;
        double capturedBonus = bonus;

        MARKS.put(id, new MarkState(capturedBonus, capturedConsume));

        // Visual: CRIT sparkles above the marked entity
        entity.getWorld().spawnParticle(
                Particle.CRIT,
                entity.getLocation().add(0, 1.5, 0),
                15, 0.3, 0.5, 0.3, 0.1);

        // Auto-expire after duration (no-op if mark was already consumed)
        Bukkit.getScheduler().runTaskLater(RpgCorePlugin.get(),
                () -> MARKS.remove(id), duration);

        return CompletableFuture.completedFuture(ctx);
    }

    /** Immutable per-target mark data. */
    public record MarkState(double bonus, boolean consume) {}
}
