package com.github._255_ping.rpg.core.abilities;

import com.github._255_ping.rpg.api.abilities.PlayerAbilityTrigger;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Fires {@link PlayerAbilityTrigger#ON_HIT} bindings when a player deals damage,
 * either via melee or an owned projectile.
 */
public final class PlayerHitAbilityListener implements Listener {

    private final PassiveAbilityFirer firer;

    public PlayerHitAbilityListener(PassiveAbilityFirer firer) {
        this.firer = firer;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        Player attacker = resolvePlayerSource(event.getDamager());
        if (attacker == null) return;

        firer.fire(attacker, PlayerAbilityTrigger.ON_HIT, target);
    }

    /** Returns the player responsible for the damage, whether direct or via owned projectile. */
    private static Player resolvePlayerSource(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }
}
