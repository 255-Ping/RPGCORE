package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.mobs.MobRegistry;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CoreMobRegistry implements MobRegistry {

    private final ConcurrentMap<String, RpgMob> byId = new ConcurrentHashMap<>();
    private final NamespacedKey mobIdKey;

    public CoreMobRegistry(NamespacedKey mobIdKey) {
        this.mobIdKey = mobIdKey;
    }

    @Override
    public void register(RpgMob mob) {
        byId.put(mob.id(), mob);
    }

    @Override
    public Optional<RpgMob> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<RpgMob> from(LivingEntity entity) {
        if (entity == null) return Optional.empty();
        String id = entity.getPersistentDataContainer().get(mobIdKey, PersistentDataType.STRING);
        if (id == null) return Optional.empty();
        return get(id);
    }

    @Override
    public Collection<RpgMob> all() {
        return byId.values();
    }

    public NamespacedKey idKey() {
        return mobIdKey;
    }

    public void clear() {
        byId.clear();
    }
}
