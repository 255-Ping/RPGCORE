package com.github._255_ping.rpg.core.status;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import com.github._255_ping.rpg.api.status.ActiveStatusEffect;
import com.github._255_ping.rpg.api.status.StackingStrategy;
import com.github._255_ping.rpg.api.status.StatusEffect;
import com.github._255_ping.rpg.api.status.StatusEffectService;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CoreStatusEffectService implements StatusEffectService {

    private final CoreStatusEffectRegistry registry;
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<ActiveEffectInstance>> active = new ConcurrentHashMap<>();

    public CoreStatusEffectService(CoreStatusEffectRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void apply(LivingEntity target, String effectId, int level, int durationTicks, String sourceId) {
        Optional<StatusEffect> def = registry.get(effectId);
        if (def.isEmpty()) return;

        long now = System.currentTimeMillis();
        long expiry = now + durationTicks * 50L;

        CopyOnWriteArrayList<ActiveEffectInstance> list = active.computeIfAbsent(target.getUniqueId(),
                k -> new CopyOnWriteArrayList<>());

        StackingStrategy strategy = def.get().stacking();
        switch (strategy) {
            case INDEPENDENT -> list.add(new ActiveEffectInstance(effectId, level, expiry, sourceId));
            case REFRESH -> applyRefresh(list, effectId, level, expiry, sourceId);
            case TAKE_MAX -> applyTakeMax(list, effectId, level, expiry, sourceId);
            case STACK_POWER -> applyStackPower(list, effectId, level, expiry, sourceId, durationTicks);
        }

        triggerRecalc(target);
        if (def.get() instanceof CoreStatusEffect cse && cse.onApply() != null) {
            executeHook(target, cse.onApply());
        }
    }

    private void applyRefresh(CopyOnWriteArrayList<ActiveEffectInstance> list, String id, int level, long expiry, String src) {
        for (ActiveEffectInstance ae : list) {
            if (ae.effectId.equals(id)) {
                if (level >= ae.level) {
                    ae.level = level;
                    ae.expiryMs = expiry;
                }
                return;
            }
        }
        list.add(new ActiveEffectInstance(id, level, expiry, src));
    }

    private void applyTakeMax(CopyOnWriteArrayList<ActiveEffectInstance> list, String id, int level, long expiry, String src) {
        for (ActiveEffectInstance ae : list) {
            if (ae.effectId.equals(id)) {
                if (level > ae.level) ae.level = level;
                if (expiry > ae.expiryMs) ae.expiryMs = expiry;
                return;
            }
        }
        list.add(new ActiveEffectInstance(id, level, expiry, src));
    }

    private void applyStackPower(CopyOnWriteArrayList<ActiveEffectInstance> list, String id, int level, long expiry, String src, int durationTicks) {
        for (ActiveEffectInstance ae : list) {
            if (ae.effectId.equals(id)) {
                ae.level += level;
                ae.expiryMs = Math.max(ae.expiryMs, System.currentTimeMillis()) + durationTicks * 50L;
                return;
            }
        }
        list.add(new ActiveEffectInstance(id, level, expiry, src));
    }

    @Override
    public void clear(LivingEntity target, String effectId) {
        CopyOnWriteArrayList<ActiveEffectInstance> list = active.get(target.getUniqueId());
        if (list == null) return;
        list.removeIf(ae -> ae.effectId.equals(effectId));
        triggerRecalc(target);
    }

    @Override
    public void clearAll(LivingEntity target) {
        active.remove(target.getUniqueId());
        triggerRecalc(target);
    }

    /** UUID-keyed variant for callers that don't hold a live LivingEntity reference (e.g., dead/unloaded entity). */
    public void clearAll(UUID entityId) {
        active.remove(entityId);
    }

    @Override
    public Collection<ActiveStatusEffect> active(LivingEntity target) {
        CopyOnWriteArrayList<ActiveEffectInstance> list = active.get(target.getUniqueId());
        if (list == null) return Collections.emptyList();
        long now = System.currentTimeMillis();
        List<ActiveStatusEffect> out = new ArrayList<>(list.size());
        for (ActiveEffectInstance ae : list) {
            int remainingTicks = (int) Math.max(0, (ae.expiryMs - now) / 50L);
            out.add(new ActiveStatusEffect(ae.effectId, ae.level, remainingTicks, ae.sourceId));
        }
        return out;
    }

    /** Internal access for the tick task — returns the mutable list so it can prune expired effects. */
    CopyOnWriteArrayList<ActiveEffectInstance> activeMutable(UUID entityId) {
        return active.get(entityId);
    }

    /** Internal access used by both the tick task and CoreRpgPlayer. */
    public List<StatModifier> modifiersFor(UUID entityId) {
        CopyOnWriteArrayList<ActiveEffectInstance> list = active.get(entityId);
        if (list == null || list.isEmpty()) return List.of();
        long now = System.currentTimeMillis();
        List<StatModifier> out = new ArrayList<>();
        Iterator<ActiveEffectInstance> it = list.iterator();
        while (it.hasNext()) {
            ActiveEffectInstance ae = it.next();
            if (ae.expiryMs <= now) continue;
            Optional<StatusEffect> def = registry.get(ae.effectId);
            if (def.isEmpty()) continue;
            if (!(def.get() instanceof CoreStatusEffect cse)) continue;
            for (StatModifier base : cse.statModifiers()) {
                out.add(new StatModifier(base.stat(), base.kind(), base.value() * ae.level));
            }
        }
        return out;
    }

    public void purgeExpired(LivingEntity entity) {
        CopyOnWriteArrayList<ActiveEffectInstance> list = active.get(entity.getUniqueId());
        if (list == null) return;
        long now = System.currentTimeMillis();
        boolean removed = list.removeIf(ae -> ae.expiryMs <= now);
        if (removed) triggerRecalc(entity);
    }

    public Collection<UUID> trackedEntities() {
        return active.keySet();
    }

    public CoreStatusEffectRegistry registry() {
        return registry;
    }

    static void executeHook(LivingEntity entity, CoreStatusEffect.HookSpec hook) {
        if (hook.sound() != null && !hook.sound().isBlank()) {
            entity.getWorld().playSound(entity.getLocation(), hook.sound(), hook.volume(), hook.pitch());
        }
        if (hook.particle() != null && !hook.particle().isBlank()) {
            try {
                Particle particle = Particle.valueOf(hook.particle().toUpperCase(java.util.Locale.ROOT));
                entity.getWorld().spawnParticle(particle, entity.getLocation().add(0, 1, 0),
                        hook.particleCount(), 0.3, 0.3, 0.3, 0);
            } catch (IllegalArgumentException ignored) {
                // unknown particle name — silently skip
            }
        }
    }

    private void triggerRecalc(LivingEntity target) {
        if (target instanceof Player p) {
            RpgPlayer rp = RpgServices.player(p);
            rp.recalculateStats();
        }
    }
}
