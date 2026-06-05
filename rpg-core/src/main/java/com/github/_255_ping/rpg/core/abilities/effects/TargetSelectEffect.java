package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Acquires a target and writes it to {@link AbilityContext#setTarget(LivingEntity)}.
 * Every effect downstream in the same chain sees the updated target.
 *
 * <p>Five modes, all registered as separate DSL names:
 * <ul>
 *   <li>{@code nearest_enemy{range=12.0}} — nearest hostile entity</li>
 *   <li>{@code farthest_enemy{range=12.0}} — farthest hostile entity</li>
 *   <li>{@code nearest_ally{range=12.0, priority=nearest|lowest_health|lowest_mana}} — nearest/best friendly</li>
 *   <li>{@code random_enemy{range=12.0}} — random hostile entity</li>
 *   <li>{@code self{}} — sets target to the caster itself</li>
 * </ul>
 *
 * <p><b>Hostile / ally definitions:</b>
 * <ul>
 *   <li>Player caster: hostiles = non-player {@link LivingEntity} mobs;
 *       allies = other {@link Player} instances (or same party if rpg-parties is present).</li>
 *   <li>Non-player (mob) caster: hostiles = {@link Player} entities;
 *       allies = non-player {@link LivingEntity} entities.</li>
 *   <li>Add {@code allow_pvp=true} to include other players as valid hostile targets for a
 *       player caster (PvP opt-in).</li>
 * </ul>
 *
 * <p>If no valid target is found the context target remains unchanged — downstream
 * effects that require a non-null target already handle null gracefully.
 */
public final class TargetSelectEffect implements AbilityEffect {

    public enum Mode { NEAREST_ENEMY, FARTHEST_ENEMY, NEAREST_ALLY, RANDOM_ENEMY, SELF }

    private final Mode mode;
    private final double range;
    private final boolean allowPvp;
    private final Priority priority;

    public enum Priority { NEAREST, LOWEST_HEALTH, LOWEST_MANA }

    public TargetSelectEffect(Mode mode, Map<String, String> params) {
        this.mode     = mode;
        this.range    = AbilityDsl.doubleParam(params, "range", 12.0);
        this.allowPvp = AbilityDsl.boolParam(params, "allow_pvp", false);
        String pStr   = params.getOrDefault("priority", "nearest");
        this.priority = switch (pStr) {
            case "lowest_health" -> Priority.LOWEST_HEALTH;
            case "lowest_mana"   -> Priority.LOWEST_MANA;
            default              -> Priority.NEAREST;
        };
    }

    @Override public String name() { return mode.name().toLowerCase(); }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        LivingEntity caster = ctx.caster();
        if (caster == null) return CompletableFuture.completedFuture(ctx);

        if (mode == Mode.SELF) {
            ctx.setTarget(caster);
            return CompletableFuture.completedFuture(ctx);
        }

        List<LivingEntity> nearby = new ArrayList<>(
                caster.getWorld().getNearbyLivingEntities(caster.getLocation(), range));
        nearby.removeIf(e -> e.equals(caster) || e.isDead());

        boolean casterIsPlayer = caster instanceof Player;
        List<LivingEntity> candidates = nearby.stream()
                .filter(e -> isValidCandidate(e, casterIsPlayer))
                .toList();

        if (candidates.isEmpty()) return CompletableFuture.completedFuture(ctx);

        LivingEntity chosen = switch (mode) {
            case NEAREST_ENEMY -> nearest(candidates, caster);
            case FARTHEST_ENEMY -> farthest(candidates, caster);
            case RANDOM_ENEMY -> random(candidates);
            case NEAREST_ALLY -> pickAlly(candidates, caster);
            default -> null; // SELF already handled above
        };

        if (chosen != null) ctx.setTarget(chosen);
        return CompletableFuture.completedFuture(ctx);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** True if this entity belongs on the hostile or ally list for this mode. */
    private boolean isValidCandidate(LivingEntity e, boolean casterIsPlayer) {
        return switch (mode) {
            case NEAREST_ENEMY, FARTHEST_ENEMY, RANDOM_ENEMY -> isHostile(e, casterIsPlayer);
            case NEAREST_ALLY -> isAlly(e, casterIsPlayer);
            default -> false;
        };
    }

    /** Hostile = mobs when player casts; players when mob casts; +PvP when flag set. */
    private boolean isHostile(LivingEntity e, boolean casterIsPlayer) {
        if (casterIsPlayer) {
            if (e instanceof Player) return allowPvp;
            return true; // any mob
        } else {
            return e instanceof Player; // mob targets players
        }
    }

    /** Ally = other players when player casts; other mobs when mob casts. */
    private boolean isAlly(LivingEntity e, boolean casterIsPlayer) {
        if (casterIsPlayer) return e instanceof Player;
        return !(e instanceof Player);
    }

    private static LivingEntity nearest(List<LivingEntity> list, LivingEntity origin) {
        return list.stream()
                .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(origin.getLocation())))
                .orElse(null);
    }

    private static LivingEntity farthest(List<LivingEntity> list, LivingEntity origin) {
        return list.stream()
                .max(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(origin.getLocation())))
                .orElse(null);
    }

    private static LivingEntity random(List<LivingEntity> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    private LivingEntity pickAlly(List<LivingEntity> list, LivingEntity origin) {
        return switch (priority) {
            case LOWEST_HEALTH -> list.stream()
                    .min(Comparator.comparingDouble(e ->
                            e.getHealth() / Math.max(e.getAttribute(
                                    org.bukkit.attribute.Attribute.MAX_HEALTH).getValue(), 1.0)))
                    .orElse(null);
            case LOWEST_MANA -> list.stream()
                    .filter(e -> e instanceof Player)
                    .min(Comparator.comparingDouble(e -> {
                        Player p = (Player) e;
                        double max = com.github._255_ping.rpg.api.RpgServices.player(p).maxMana();
                        if (max <= 0) return 1.0;
                        return com.github._255_ping.rpg.api.RpgServices.mana().get(p) / max;
                    }))
                    .orElseGet(() -> nearest(list, origin)); // fallback: nearest if no player allies
            default -> nearest(list, origin);
        };
    }
}
