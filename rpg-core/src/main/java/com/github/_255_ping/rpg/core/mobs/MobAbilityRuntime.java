package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityPipeline;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;

/**
 * Shared helper for firing mob ability bindings whose trigger matches a given type.
 * Used by both the timer task and the event listeners so the cast logic stays in one place.
 *
 * <p>Call {@link #setMobLevelKey} once at startup (after SpawnerManager is created) so that
 * ability {@code min-level} gates can be evaluated.
 */
public final class MobAbilityRuntime {

    private static volatile NamespacedKey mobLevelKey;

    private MobAbilityRuntime() {}

    /** Called by RpgCorePlugin after SpawnerManager is initialised. */
    public static void setMobLevelKey(NamespacedKey key) {
        mobLevelKey = key;
    }

    public static void fireTrigger(LivingEntity mob, CoreRpgMob def, Class<? extends MobAbilityTrigger> triggerClass) {
        fireTrigger(mob, def, triggerClass, null);
    }

    public static void fireTrigger(LivingEntity mob, CoreRpgMob def,
                                    Class<? extends MobAbilityTrigger> triggerClass,
                                    LivingEntity hint) {
        for (MobAbilityBinding b : def.abilityBindings()) {
            if (!triggerClass.isInstance(b.trigger())) continue;

            // Respect min-level gate: skip if the mob's PDC level is below the required minimum.
            if (b.minLevel() > 1 && mobLevelKey != null) {
                Integer level = mob.getPersistentDataContainer().get(mobLevelKey, PersistentDataType.INTEGER);
                if (level == null || level < b.minLevel()) continue;
            }

            cast(mob, def, b, hint);
        }
    }

    public static void cast(LivingEntity mob, CoreRpgMob def, MobAbilityBinding binding, LivingEntity hint) {
        AbilityContext ctx = new AbilityContext(mob, def.damage());
        if (hint != null) ctx.setTarget(hint);
        else if (mob instanceof Mob m && m.getTarget() != null) ctx.setTarget(m.getTarget());
        ctx.setPoint(mob.getEyeLocation());

        AbilityPipeline pipeline = new AbilityPipeline(binding.invocations());
        pipeline.cast(ctx, RpgServices.abilities()).exceptionally(err -> {
            java.util.logging.Logger.getLogger("rpg-core").warning(
                    "[MobAbility] cast failed for mob " + mob.getType().name()
                            + ": " + err);
            return ctx;
        });
    }
}
