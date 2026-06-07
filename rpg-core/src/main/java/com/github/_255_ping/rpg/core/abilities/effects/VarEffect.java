package com.github._255_ping.rpg.core.abilities.effects;

import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityEffect;
import com.github._255_ping.rpg.core.RpgCorePlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Per-entity named numeric variables (doubles) that persist across separate ability
 * invocations for the lifetime of the entity (until death or disconnect).
 *
 * <p>Seven DSL effects built on the same mechanism:
 * <ul>
 *   <li>{@code set_var{name=X,value=N}}   — set variable X to N</li>
 *   <li>{@code increment{name=X,amount=1,max=}} — add amount to X, optional upper clamp</li>
 *   <li>{@code decrement{name=X,amount=1,min=0}} — subtract amount from X, optional lower clamp</li>
 *   <li>{@code reset{name=X}}             — set X to 0</li>
 *   <li>{@code if_var_gte{name=X,value=N}} — gate: pass if X ≥ N</li>
 *   <li>{@code if_var_lte{name=X,value=N}} — gate: pass if X ≤ N</li>
 *   <li>{@code if_var_eq{name=X,value=N}}  — gate: pass if X == N (within 1e-9)</li>
 * </ul>
 *
 * <p>Variables are stored as {@link PersistentDataType#DOUBLE} in entity PDC under the key
 * {@code "rpg_var_<name>"}, so they are automatically cleaned up when the entity is
 * removed from the world (Bukkit clears PDC on entity removal). No explicit cleanup needed.
 *
 * <p>Typical combo-counter pattern:
 * <pre>
 * # Every melee hit: count up. On the 3rd hit, trigger a power strike and reset.
 * - "increment{name=combo} ~onHit"
 * - "if_var_gte{name=combo,value=3} reset{name=combo} aoe{radius=4.0,damage=40.0,particle=CRIT} ~onHit"
 * </pre>
 *
 * <p>Typical enrage-stack pattern:
 * <pre>
 * # Boss below 50% HP gains a stack (capped at 5). Each onHit checks the stack level.
 * - "if_health_below{percent=50} increment{name=enrage,max=5} ~onTimer:100"
 * - "if_var_gte{name=enrage,value=1} damage{} ~onHit"
 * </pre>
 */
public final class VarEffect implements AbilityEffect {

    private static final String PDC_NAMESPACE = "rpg_var_";
    private static final double EPSILON = 1e-9;

    public enum Mode {
        SET_VAR,
        INCREMENT,
        DECREMENT,
        RESET,
        IF_VAR_GTE,
        IF_VAR_LTE,
        IF_VAR_EQ
    }

    private final Mode   mode;
    private final String varName;
    private final double value;    // set_var target / comparison threshold
    private final double amount;   // increment / decrement step
    private final double clampMin; // decrement lower bound
    private final double clampMax; // increment upper bound (Double.MAX_VALUE = no cap)

    public VarEffect(Mode mode, Map<String, String> params) {
        this.mode     = mode;
        this.varName  = params.getOrDefault("name", "");
        this.value    = dbl(params.get("value"), 0.0);
        this.amount   = dbl(params.get("amount"), 1.0);
        this.clampMin = dbl(params.get("min"), 0.0);
        this.clampMax = dbl(params.get("max"), Double.MAX_VALUE);
    }

    @Override
    public String name() {
        return switch (mode) {
            case SET_VAR    -> "set_var";
            case INCREMENT  -> "increment";
            case DECREMENT  -> "decrement";
            case RESET      -> "reset";
            case IF_VAR_GTE -> "if_var_gte";
            case IF_VAR_LTE -> "if_var_lte";
            case IF_VAR_EQ  -> "if_var_eq";
        };
    }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        // Gate effects must not un-block a chain that is already blocked.
        boolean isGate = (mode == Mode.IF_VAR_GTE || mode == Mode.IF_VAR_LTE || mode == Mode.IF_VAR_EQ);
        if (isGate && ctx.isBlocked()) return CompletableFuture.completedFuture(ctx);

        LivingEntity caster = ctx.caster();
        if (caster == null || varName.isBlank()) {
            if (isGate) ctx.setBlocked(true);
            return CompletableFuture.completedFuture(ctx);
        }

        NamespacedKey key = new NamespacedKey(RpgCorePlugin.get(), PDC_NAMESPACE + varName);

        switch (mode) {
            case SET_VAR -> set(caster, key, value);
            case INCREMENT -> {
                double cur = get(caster, key);
                set(caster, key, Math.min(clampMax, cur + amount));
            }
            case DECREMENT -> {
                double cur = get(caster, key);
                set(caster, key, Math.max(clampMin, cur - amount));
            }
            case RESET -> set(caster, key, 0.0);
            case IF_VAR_GTE -> {
                if (get(caster, key) < value) ctx.setBlocked(true);
            }
            case IF_VAR_LTE -> {
                if (get(caster, key) > value) ctx.setBlocked(true);
            }
            case IF_VAR_EQ -> {
                if (Math.abs(get(caster, key) - value) > EPSILON) ctx.setBlocked(true);
            }
        }

        return CompletableFuture.completedFuture(ctx);
    }

    // ── PDC helpers ───────────────────────────────────────────────────────────

    private static double get(LivingEntity e, NamespacedKey key) {
        Double v = e.getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);
        return v == null ? 0.0 : v;
    }

    private static void set(LivingEntity e, NamespacedKey key, double v) {
        e.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, v);
    }

    /** Read a named variable on an entity (used by external effects if needed). */
    public static double read(LivingEntity entity, String name) {
        NamespacedKey key = new NamespacedKey(RpgCorePlugin.get(), PDC_NAMESPACE + name);
        return get(entity, key);
    }

    private static double dbl(String s, double def) {
        if (s == null || s.isBlank()) return def;
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
