package com.github._255_ping.rpg.core.health;

import com.github._255_ping.rpg.api.health.HealthService;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CoreHealthService implements HealthService {

    private final JavaPlugin plugin;
    private final ConcurrentHashMap<UUID, HealthState> states = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> combatExpiry = new ConcurrentHashMap<>();

    public CoreHealthService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initPlayer(Player player, double maxHp, double currentHp) {
        states.put(player.getUniqueId(), new HealthState(currentHp, maxHp));
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        double bukkitMax = plugin.getConfig().getDouble("health.bukkit-max-health", 40.0);
        if (attr != null) attr.setBaseValue(bukkitMax);
        syncDisplay(player);
    }

    public void initMob(LivingEntity mob, double maxHp) {
        states.put(mob.getUniqueId(), new HealthState(maxHp, maxHp));
    }

    public void removeEntity(LivingEntity entity) {
        states.remove(entity.getUniqueId());
        combatExpiry.remove(entity.getUniqueId());
    }

    @Override
    public double currentHp(LivingEntity entity) {
        return getOrInit(entity).currentHp;
    }

    @Override
    public double maxHp(LivingEntity entity) {
        return getOrInit(entity).maxHp;
    }

    @Override
    public void setCurrentHp(LivingEntity entity, double hp) {
        HealthState s = getOrInit(entity);
        s.currentHp = Math.max(0, Math.min(hp, s.maxHp));
        if (entity instanceof Player p) {
            syncDisplay(p);
        } else {
            double bukkitMax = entity.getAttribute(Attribute.MAX_HEALTH) != null
                    ? entity.getAttribute(Attribute.MAX_HEALTH).getValue()
                    : s.maxHp;
            entity.setHealth(Math.max(0, Math.min(s.currentHp, bukkitMax)));
        }
        if (s.currentHp <= 0 && entity.getHealth() > 0) {
            entity.setHealth(0);
        }
    }

    @Override
    public void heal(LivingEntity entity, double amount) {
        setCurrentHp(entity, currentHp(entity) + amount);
    }

    @Override
    public void damage(LivingEntity entity, double amount, String source) {
        setCurrentHp(entity, currentHp(entity) - amount);
        // Hurt animation — we bypass EntityDamageEvent so vanilla never sends the
        // red-flash status packet. Play it explicitly so the hit is always visible.
        entity.playHurtAnimation(0f);
    }

    @Override
    public void syncDisplay(Player player) {
        HealthState s = states.get(player.getUniqueId());
        if (s == null) return;
        double bukkitMax = plugin.getConfig().getDouble("health.bukkit-max-health", 40.0);
        double ratio = s.maxHp > 0 ? s.currentHp / s.maxHp : 0;
        double bukkitHp = Math.max(0, Math.min(ratio * bukkitMax, bukkitMax));
        player.setHealth(bukkitHp);
    }

    @Override
    public boolean inCombat(Player player) {
        Long until = combatExpiry.get(player.getUniqueId());
        return until != null && System.currentTimeMillis() < until;
    }

    @Override
    public void markInCombat(Player player) {
        long durationSec = plugin.getConfig().getLong("combat-tag.duration-seconds", 8);
        // VITALITY reduces combat-tag duration: 1%/point capped at the config cap (default 50%).
        try {
            double vitality = com.github._255_ping.rpg.api.RpgServices.player(player)
                    .get(com.github._255_ping.rpg.api.stats.BuiltinStat.VITALITY);
            double perPoint = plugin.getConfig().getDouble("combat-tag.vitality-reduction-percent-per-point", 1.0);
            double cap = plugin.getConfig().getDouble("combat-tag.vitality-reduction-cap-percent", 50.0);
            double reductionPct = Math.min(cap, Math.max(0, vitality * perPoint));
            durationSec = Math.round(durationSec * (1.0 - reductionPct / 100.0));
            durationSec = Math.max(0, durationSec);
        } catch (IllegalStateException ignored) {
            // playerLookup not yet bootstrapped — just use base duration.
        }
        if (durationSec <= 0) return;
        long now = System.currentTimeMillis();
        long newExpiry = now + durationSec * 1000L;
        Long previous = combatExpiry.put(player.getUniqueId(), newExpiry);
        // Fire CombatTagEvent only when transitioning from out-of-combat -> in-combat.
        if (previous == null || previous < now) {
            org.bukkit.Bukkit.getPluginManager().callEvent(
                    new com.github._255_ping.rpg.api.combat.CombatTagEvent(player, durationSec));
        }
    }

    public void setMaxHp(LivingEntity entity, double max) {
        HealthState s = getOrInit(entity);
        s.maxHp = max;
        if (s.currentHp > max) s.currentHp = max;
        if (entity instanceof Player p) syncDisplay(p);
    }

    private HealthState getOrInit(LivingEntity e) {
        return states.computeIfAbsent(e.getUniqueId(), k -> {
            AttributeInstance attr = e.getAttribute(Attribute.MAX_HEALTH);
            double max = attr != null ? attr.getValue() : e.getHealth();
            return new HealthState(e.getHealth(), max);
        });
    }
}
