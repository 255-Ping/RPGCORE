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
 * Fires {@link PlayerAbilityTrigger#ON_ATTACK} bindings when a player initiates a melee or
 * projectile attack — before the damage pipeline processes or cancels the event.
 *
 * <p>This fires at {@link EventPriority#LOWEST} (even on cancelled events) so it acts as a
 * "swing attempt" trigger, not a "confirmed hit" trigger. Add {@code mana_cost{}} explicitly
 * if you want to gate the proc behind mana.
 */
public final class PlayerAttackAbilityListener implements Listener {

    private final PassiveAbilityFirer firer;

    public PlayerAttackAbilityListener(PassiveAbilityFirer firer) {
        this.firer = firer;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null) return;

        firer.fire(attacker, PlayerAbilityTrigger.ON_ATTACK, target);
    }

    private static Player resolvePlayer(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }
}
