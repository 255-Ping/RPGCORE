package com.github._255_ping.rpg.core.damage;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.damage.DamageContext;
import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import com.github._255_ping.rpg.api.damage.PreDamageEvent;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.core.RpgCorePlugin;
import com.github._255_ping.rpg.core.health.CoreHealthService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public final class DamagePipelineListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final RpgCorePlugin plugin;
    private final CoreHealthService health;
    private NamespacedKey mobIdKey;
    private NamespacedKey mobLevelKey;

    public DamagePipelineListener(RpgCorePlugin plugin, CoreHealthService health) {
        this.plugin = plugin;
        this.health = health;
    }

    /** Called after SpawnerManager is created so we can read keys without circular deps. */
    public void setMobKeys(NamespacedKey mobIdKey, NamespacedKey mobLevelKey) {
        this.mobIdKey = mobIdKey;
        this.mobLevelKey = mobLevelKey;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.getConfig().getBoolean("vanilla-suppression.damage", true)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        event.setCancelled(true);

        LivingEntity attacker = null;
        String source = sourceFor(event.getCause());

        if (event instanceof EntityDamageByEntityEvent edbe) {
            if (edbe.getDamager() instanceof LivingEntity le) {
                attacker = le;
                source = "melee";
            } else if (edbe.getDamager() instanceof Projectile proj) {
                ProjectileSource shooter = proj.getShooter();
                if (shooter instanceof LivingEntity le) {
                    attacker = le;
                    source = "projectile";
                }
            }
        }

        DamageContext ctx = new DamageContext(attacker, victim, event.getFinalDamage(), source);

        PreDamageEvent pre = new PreDamageEvent(ctx);
        Bukkit.getPluginManager().callEvent(pre);
        if (pre.isCancelled()) return;

        double finalDamage = DamageMath.compute(ctx);
        if (finalDamage <= 0) return;

        // Mob level stat bonuses — leveled mobs deal more damage and absorb more.
        finalDamage = applyMobLevelStatScaling(attacker, victim, finalDamage);

        // Level scaling: reduce damage dealt to/by over-leveled mobs.
        finalDamage = applyLevelScaling(attacker, victim, finalDamage);

        health.damage(victim, finalDamage, source);

        if (attacker instanceof Player ap) health.markInCombat(ap);
        if (victim instanceof Player vp) health.markInCombat(vp);

        if (attacker instanceof Player ap) {
            RpgPlayer rp = RpgServices.player(ap);
            double lifesteal = rp.get(BuiltinStat.LIFESTEAL);
            if (lifesteal > 0) {
                health.heal(ap, finalDamage * lifesteal / 100.0);
            }
        }

        // Update mob health bar in display name.
        if (!(victim instanceof Player) && mobIdKey != null) {
            updateMobHealthBar(victim);
        }

        PostDamageEvent post = new PostDamageEvent(ctx, finalDamage);
        Bukkit.getPluginManager().callEvent(post);
    }

    /**
     * Applies per-level stat bonuses to mob attackers and mob victims.
     * <ul>
     *   <li>Mob attacker: each level above 1 multiplies outgoing damage by
     *       {@code 1 + (level-1) * damage-percent / 100}</li>
     *   <li>Mob victim: each level above 1 reduces incoming damage using the standard
     *       defense formula {@code defense / (defense + 100)} where
     *       {@code defense = (level-1) * per-level-defense}</li>
     * </ul>
     * Both effects are controlled by {@code mob-level-scaling.enabled} and their respective
     * {@code per-level-gains} keys in config.yml.
     */
    private double applyMobLevelStatScaling(LivingEntity attacker, LivingEntity victim, double damage) {
        if (!plugin.getConfig().getBoolean("mob-level-scaling.enabled", true)) return damage;
        if (mobLevelKey == null) return damage;

        // Leveled mob attacker → bonus damage output
        if (attacker != null && !(attacker instanceof Player)) {
            Integer mobLvl = attacker.getPersistentDataContainer().get(mobLevelKey, PersistentDataType.INTEGER);
            if (mobLvl != null && mobLvl > 1) {
                double dmgPct = plugin.getConfig().getDouble("mob-level-scaling.per-level-gains.damage-percent", 5.0);
                damage *= 1.0 + (mobLvl - 1) * dmgPct / 100.0;
            }
        }

        // Leveled mob victim → bonus damage reduction (defense scaling)
        if (!(victim instanceof Player) && damage > 0) {
            Integer mobLvl = victim.getPersistentDataContainer().get(mobLevelKey, PersistentDataType.INTEGER);
            if (mobLvl != null && mobLvl > 1) {
                double defPerLvl = plugin.getConfig().getDouble("mob-level-scaling.per-level-gains.defense", 2.0);
                double bonusDef = (mobLvl - 1) * defPerLvl;
                damage *= 1.0 - bonusDef / (bonusDef + 100.0);
            }
        }

        return damage;
    }

    private double applyLevelScaling(LivingEntity attacker, LivingEntity victim, double damage) {
        if (!plugin.getConfig().getBoolean("level-scaling.enabled", true)) return damage;
        int threshold = plugin.getConfig().getInt("level-scaling.threshold", 5);
        double reductionPerLevel = plugin.getConfig().getDouble("level-scaling.reduction-per-level", 0.08);
        double minFactor = plugin.getConfig().getDouble("level-scaling.min-damage-factor", 0.10);

        Integer mobLevel = null;
        LivingEntity mob = null;
        Player player = null;

        if (attacker instanceof Player p && !(victim instanceof Player)) {
            player = p; mob = victim;
        } else if (victim instanceof Player p && !(attacker instanceof Player)) {
            player = p; mob = attacker;
        }
        if (player == null || mob == null || mobLevelKey == null) return damage;

        Integer stored = mob.getPersistentDataContainer().get(mobLevelKey, PersistentDataType.INTEGER);
        if (stored == null) return damage;
        mobLevel = stored;

        int playerLevel;
        try {
            playerLevel = RpgServices.skills().level(player, "combat");
            if (playerLevel <= 0) playerLevel = 1;
        } catch (Exception ex) {
            return damage;
        }

        int diff = playerLevel - mobLevel;
        if (diff <= threshold) return damage;

        double factor = Math.max(minFactor, 1.0 - (diff - threshold) * reductionPerLevel);
        return damage * factor;
    }

    private void updateMobHealthBar(LivingEntity mob) {
        if (mobIdKey == null) return;
        String mobId = mob.getPersistentDataContainer().get(mobIdKey, PersistentDataType.STRING);
        if (mobId == null) return;
        if (!plugin.getConfig().getBoolean("mob-health-bar.enabled", true)) return;

        Optional<RpgMob> def = RpgServices.mobs().get(mobId);
        if (def.isEmpty()) return;

        double cur = health.currentHp(mob);
        double max = health.maxHp(mob);
        int barLen = plugin.getConfig().getInt("mob-health-bar.bar-length", 10);
        int filled = (int) Math.round((cur / max) * barLen);
        filled = Math.max(0, Math.min(barLen, filled));

        String bar = "§c" + "█".repeat(filled) + "§8" + "░".repeat(barLen - filled);

        Integer level = mobLevelKey != null
                ? mob.getPersistentDataContainer().get(mobLevelKey, PersistentDataType.INTEGER)
                : null;
        String levelPrefix = (level != null && level > 1) ? "§7[Lv. " + level + "] " : "";

        String rawName = def.get().displayName() != null ? def.get().displayName() : mobId;
        String display = levelPrefix + rawName + " " + bar + " §7" + (int) Math.ceil(cur) + "§8/§7" + (int) max;
        mob.customName(LEGACY.deserialize(display));
        mob.setCustomNameVisible(true);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        health.removeEntity(event.getEntity());
    }

    private static String sourceFor(EntityDamageEvent.DamageCause cause) {
        return cause.name().toLowerCase();
    }
}
