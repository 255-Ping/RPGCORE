package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.mobs.EliteDef;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Singleton service that promotes mob entities to elite status and provides fast reads
 * for downstream systems (damage pipeline, loot listener).
 *
 * <p>Three PDC entries are stamped on the entity at promotion time:
 * <ul>
 *   <li>{@code rpg_elite} — byte {@code 1} (flag)</li>
 *   <li>{@code rpg_elite_dmg_mult} — double damage multiplier</li>
 *   <li>{@code rpg_elite_loot_mult} — double loot multiplier</li>
 * </ul>
 *
 * <p>Instantiate once in {@code RpgCorePlugin.onEnable}; use {@link #get()} everywhere else.
 */
public final class EliteService {

    private static EliteService instance;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final NamespacedKey eliteKey;
    private final NamespacedKey dmgMultKey;
    private final NamespacedKey lootMultKey;

    public EliteService(JavaPlugin plugin) {
        instance = this;
        this.eliteKey    = new NamespacedKey(plugin, "rpg_elite");
        this.dmgMultKey  = new NamespacedKey(plugin, "rpg_elite_dmg_mult");
        this.lootMultKey = new NamespacedKey(plugin, "rpg_elite_loot_mult");
    }

    public static EliteService get() { return instance; }

    // ── Promotion ─────────────────────────────────────────────────────────────

    /**
     * Promotes {@code entity} to elite status using the given {@link EliteDef}.
     * Applies name prefix, glow, HP multiplier, and stamps PDC tags.
     * Call this immediately after the entity is spawned so the health service
     * already has its base HP set.
     */
    public void promote(LivingEntity entity, EliteDef def) {
        // Prefix the existing custom name (set by CoreRpgMob.spawn)
        Component existing = entity.customName();
        Component prefixComp = LEGACY.deserialize(def.prefix());
        entity.customName(existing != null ? prefixComp.append(existing) : prefixComp);
        entity.setCustomNameVisible(true);

        // Glow (ambient=true, no particles, no icon — purely cosmetic outline)
        if (def.glow()) {
            entity.addPotionEffect(
                    new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, true, false, false));
        }

        // HP multiplier — health service already initialized the mob's base HP.
        // We must also raise Bukkit's MAX_HEALTH attribute so the entity isn't clamped.
        try {
            double base = RpgServices.health().maxHp(entity);
            double eliteHp = base * def.hpMultiplier();
            AttributeInstance attr = entity.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) attr.setBaseValue(Math.min(eliteHp, 2048.0));
            RpgServices.health().setMaxHp(entity, eliteHp);
            RpgServices.health().setCurrentHp(entity, eliteHp);
        } catch (IllegalStateException ignored) {
            // HealthService not available — skip HP boost
        }

        // PDC tags for downstream systems
        var pdc = entity.getPersistentDataContainer();
        pdc.set(eliteKey,    PersistentDataType.BYTE,   (byte) 1);
        pdc.set(dmgMultKey,  PersistentDataType.DOUBLE, def.damageMultiplier());
        pdc.set(lootMultKey, PersistentDataType.DOUBLE, def.lootMultiplier());
    }

    // ── Readers ───────────────────────────────────────────────────────────────

    public boolean isElite(LivingEntity entity) {
        Byte flag = entity.getPersistentDataContainer().get(eliteKey, PersistentDataType.BYTE);
        return flag != null && flag == 1;
    }

    /** Returns the damage multiplier for this entity, or {@code 1.0} if not elite. */
    public double damageMultiplier(LivingEntity entity) {
        Double v = entity.getPersistentDataContainer().get(dmgMultKey, PersistentDataType.DOUBLE);
        return v != null ? v : 1.0;
    }

    /** Returns the loot multiplier for this entity, or {@code 1.0} if not elite. */
    public double lootMultiplier(LivingEntity entity) {
        Double v = entity.getPersistentDataContainer().get(lootMultKey, PersistentDataType.DOUBLE);
        return v != null ? v : 1.0;
    }
}
