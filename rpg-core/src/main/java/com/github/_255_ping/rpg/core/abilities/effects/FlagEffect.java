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
 * Per-entity named boolean flags that persist across separate ability invocations
 * for the lifetime of the entity (until death or disconnect).
 *
 * <p>Four DSL effects built on the same mechanism:
 * <ul>
 *   <li>{@code if_flag{name=X}} — gate: passes if flag X is set on the caster</li>
 *   <li>{@code if_not_flag{name=X}} — gate: passes if flag X is NOT set on the caster</li>
 *   <li>{@code set_flag{name=X}} — sets flag X on the caster (not a gate)</li>
 *   <li>{@code clear_flag{name=X}} — clears flag X from the caster (not a gate)</li>
 * </ul>
 *
 * <p>Flags are stored as Bukkit entity metadata under the key
 * {@code "rpg_flag_<name>"} so they are automatically cleaned up on entity death
 * (Bukkit clears metadata when an entity is removed from the world). No explicit
 * cleanup is needed.
 *
 * <p>Typical boss phase-transition pattern:
 * <pre>
 * # Fires every timer tick. Triggers only once because set_flag{} prevents re-entry.
 * "if_health_below{percent=25} if_not_flag{name=phase2} set_flag{name=phase2}
 *  spawn_mob{id=golem_shard,count=3,radius=5.0} shield{amount=500,target=caster}"
 * </pre>
 *
 * @see ChanceEffect for the blocking mechanism shared by all gate effects
 */
public final class FlagEffect implements AbilityEffect {

    /** PDC key namespace. Flags are stored under "rpg_flag_&lt;name&gt;" in the plugin's namespace. */
    private static final String PDC_NAMESPACE = "rpg_flag_";

    public enum Mode { IF_FLAG, IF_NOT_FLAG, SET_FLAG, CLEAR_FLAG }

    private final Mode mode;
    private final String flagName;

    public FlagEffect(Mode mode, Map<String, String> params) {
        this.mode     = mode;
        this.flagName = params.getOrDefault("name", "");
    }

    @Override
    public String name() {
        return switch (mode) {
            case IF_FLAG     -> "if_flag";
            case IF_NOT_FLAG -> "if_not_flag";
            case SET_FLAG    -> "set_flag";
            case CLEAR_FLAG  -> "clear_flag";
        };
    }

    @Override
    public CompletableFuture<AbilityContext> apply(AbilityContext ctx) {
        // Gate effects must not un-block a chain that is already blocked.
        if (mode == Mode.IF_FLAG || mode == Mode.IF_NOT_FLAG) {
            if (ctx.isBlocked()) return CompletableFuture.completedFuture(ctx);
        }

        LivingEntity caster = ctx.caster();
        if (caster == null || flagName.isBlank()) {
            if (mode == Mode.IF_FLAG || mode == Mode.IF_NOT_FLAG) ctx.setBlocked(true);
            return CompletableFuture.completedFuture(ctx);
        }

        NamespacedKey key = new NamespacedKey(RpgCorePlugin.get(), PDC_NAMESPACE + flagName);

        switch (mode) {
            case IF_FLAG -> {
                boolean set = caster.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
                if (!set) ctx.setBlocked(true);
            }
            case IF_NOT_FLAG -> {
                boolean set = caster.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
                if (set) ctx.setBlocked(true);
            }
            case SET_FLAG ->
                caster.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            case CLEAR_FLAG ->
                caster.getPersistentDataContainer().remove(key);
        }

        return CompletableFuture.completedFuture(ctx);
    }

    // ── Convenience query (used by other effects if needed) ──────────────────

    /** Returns true if the entity has the named flag set. */
    public static boolean hasFlag(LivingEntity entity, String name) {
        NamespacedKey key = new NamespacedKey(RpgCorePlugin.get(), PDC_NAMESPACE + name);
        return entity.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
