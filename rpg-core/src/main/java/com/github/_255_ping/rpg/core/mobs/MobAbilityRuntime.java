package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityPipeline;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

/**
 * Shared helper for firing mob ability bindings whose trigger matches a given type.
 * Used by both the timer task and the event listeners so the cast logic stays in one place.
 */
public final class MobAbilityRuntime {

    private MobAbilityRuntime() {}

    public static void fireTrigger(LivingEntity mob, CoreRpgMob def, Class<? extends MobAbilityTrigger> triggerClass) {
        fireTrigger(mob, def, triggerClass, null);
    }

    public static void fireTrigger(LivingEntity mob, CoreRpgMob def,
                                    Class<? extends MobAbilityTrigger> triggerClass,
                                    LivingEntity hint) {
        for (MobAbilityBinding b : def.abilityBindings()) {
            if (!triggerClass.isInstance(b.trigger())) continue;
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
            // Mob ability chain failed — silently swallow; log if needed for debug.
            return ctx;
        });
    }
}
