package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Optional;

/**
 * Fires OnHit / OnHurt / OnDeath mob ability triggers. OnTimer is in
 * {@link MobAbilityTimerTask}; OnSpawn is fired by {@link CoreRpgMob#spawn} directly.
 */
public final class MobAbilityEventListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPostDamage(PostDamageEvent event) {
        LivingEntity attacker = event.context().attacker();
        LivingEntity victim = event.context().victim();
        if (attacker != null) fire(attacker, victim, MobAbilityTrigger.OnHit.class);
        if (victim != null) fire(victim, attacker, MobAbilityTrigger.OnHurt.class);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        fire(event.getEntity(), null, MobAbilityTrigger.OnDeath.class);
    }

    private static void fire(LivingEntity mob, LivingEntity hint, Class<? extends MobAbilityTrigger> triggerClass) {
        Optional<RpgMob> opt = RpgServices.mobs().from(mob);
        if (opt.isEmpty()) return;
        if (!(opt.get() instanceof CoreRpgMob def)) return;
        MobAbilityRuntime.fireTrigger(mob, def, triggerClass, hint);
    }
}
