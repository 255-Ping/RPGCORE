package com.github._255_ping.rpg.core.mobs;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-tick scan of all tagged RPG mobs in every loaded world. For each mob, walks its
 * {@code OnTimer} bindings and fires the ones whose interval has elapsed since the last
 * fire. Per-mob state is keyed by (mob UUID, binding index).
 */
public final class MobAbilityTimerTask implements Runnable {

    private final CoreMobRegistry mobs;
    private final NamespacedKey mobIdKey;

    // (mob UUID, binding index) -> tick of last fire
    private final ConcurrentHashMap<UUID, Map<Integer, Long>> lastFired = new ConcurrentHashMap<>();
    private long currentTick = 0;

    public MobAbilityTimerTask(CoreMobRegistry mobs, NamespacedKey mobIdKey) {
        this.mobs = mobs;
        this.mobIdKey = mobIdKey;
    }

    @Override
    public void run() {
        currentTick++;

        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getLivingEntities()) {
                if (!(e instanceof LivingEntity le)) continue;
                String id = le.getPersistentDataContainer().get(mobIdKey, PersistentDataType.STRING);
                if (id == null) continue;
                Optional<?> def = mobs.get(id);
                if (def.isEmpty()) continue;
                if (!(def.get() instanceof CoreRpgMob mob)) continue;

                Map<Integer, Long> perBinding = lastFired.computeIfAbsent(le.getUniqueId(), k -> new HashMap<>());
                int idx = 0;
                for (MobAbilityBinding b : mob.abilityBindings()) {
                    if (b.trigger() instanceof MobAbilityTrigger.OnTimer ot) {
                        long last = perBinding.getOrDefault(idx, 0L);
                        if (last == 0L || currentTick - last >= ot.intervalTicks()) {
                            MobAbilityRuntime.cast(le, mob, b, null);
                            perBinding.put(idx, currentTick);
                        }
                    }
                    idx++;
                }
            }
        }

        // Periodic GC of dead entries.
        if (currentTick % 200 == 0) {
            Iterator<Map.Entry<UUID, Map<Integer, Long>>> it = lastFired.entrySet().iterator();
            while (it.hasNext()) {
                UUID id = it.next().getKey();
                Entity e = Bukkit.getEntity(id);
                if (e == null || e.isDead()) it.remove();
            }
        }
    }

}
