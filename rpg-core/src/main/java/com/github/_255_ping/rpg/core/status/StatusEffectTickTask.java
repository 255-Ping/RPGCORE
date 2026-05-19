package com.github._255_ping.rpg.core.status;

import com.github._255_ping.rpg.api.status.StatusEffect;
import com.github._255_ping.rpg.core.health.CoreHealthService;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class StatusEffectTickTask implements Runnable {

    private final CoreStatusEffectService service;
    private final CoreHealthService health;

    public StatusEffectTickTask(CoreStatusEffectService service, CoreHealthService health) {
        this.service = service;
        this.health = health;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        for (UUID id : service.trackedEntities()) {
            LivingEntity entity = resolve(id);
            if (entity == null || entity.isDead()) {
                service.clearAll(id);
                continue;
            }
            CopyOnWriteArrayList<ActiveEffectInstance> list = service.activeMutable(id);
            if (list == null) continue;

            boolean anyExpired = false;
            for (ActiveEffectInstance ae : list) {
                if (ae.expiryMs <= now) {
                    anyExpired = true;
                    continue;
                }
                Optional<StatusEffect> def = service.registry().get(ae.effectId);
                if (def.isEmpty()) continue;
                if (!(def.get() instanceof CoreStatusEffect cse)) continue;
                CoreStatusEffect.TickSpec spec = cse.tickSpec();
                if (spec == null || spec.action() == null) continue;

                long intervalMs = spec.intervalTicks() * 50L;
                if (ae.lastTickMs == 0) {
                    ae.lastTickMs = now;
                    continue;
                }
                if (now - ae.lastTickMs < intervalMs) continue;

                runTick(entity, ae, spec);
                ae.lastTickMs = now;
            }
            if (anyExpired) {
                service.purgeExpired(entity);
            }
        }
    }

    private void runTick(LivingEntity entity, ActiveEffectInstance ae, CoreStatusEffect.TickSpec spec) {
        double scaled = spec.amount() * ae.level;
        switch (spec.action()) {
            case "damage" -> health.damage(entity, scaled, spec.source() == null ? "status-effect" : spec.source());
            case "heal"   -> health.heal(entity, scaled);
            default -> { /* unknown tick action — silently ignore for now */ }
        }
    }

    private LivingEntity resolve(UUID id) {
        return Bukkit.getEntity(id) instanceof LivingEntity le ? le : null;
    }
}
