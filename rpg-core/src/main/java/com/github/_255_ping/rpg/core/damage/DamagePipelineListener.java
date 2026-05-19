package com.github._255_ping.rpg.core.damage;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.damage.DamageContext;
import com.github._255_ping.rpg.api.damage.PostDamageEvent;
import com.github._255_ping.rpg.api.damage.PreDamageEvent;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.core.health.CoreHealthService;
import org.bukkit.Bukkit;
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
import org.bukkit.plugin.java.JavaPlugin;

public final class DamagePipelineListener implements Listener {

    private final JavaPlugin plugin;
    private final CoreHealthService health;

    public DamagePipelineListener(JavaPlugin plugin, CoreHealthService health) {
        this.plugin = plugin;
        this.health = health;
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

        PostDamageEvent post = new PostDamageEvent(ctx, finalDamage);
        Bukkit.getPluginManager().callEvent(post);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        health.removeEntity(event.getEntity());
    }

    private static String sourceFor(EntityDamageEvent.DamageCause cause) {
        return cause.name().toLowerCase();
    }
}
