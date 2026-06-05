package com.github._255_ping.rpg.core.abilities;

import com.github._255_ping.rpg.api.abilities.PlayerAbilityTrigger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Fires {@link PlayerAbilityTrigger#ON_BLOCK} bindings when a player is hit while holding a
 * shield in the blocking stance ({@code player.isBlocking()}).
 *
 * <p>Fires at {@link EventPriority#LOWEST} — before the custom damage pipeline cancels the
 * vanilla event — so the player's blocking state is still accurately readable. In the RPG
 * system vanilla shield damage reduction is bypassed (the pipeline handles defense), but the
 * trigger still fires meaningfully when the player has their shield raised on an incoming hit.
 *
 * <p>The attacker is passed as {@code ctx.target} so that retaliatory effects like
 * {@code knockback{target=target}} or {@code apply_status{id=stun,target=target}} can
 * push back or debuff the attacker.
 */
public final class PlayerBlockAbilityListener implements Listener {

    private final PassiveAbilityFirer firer;

    public PlayerBlockAbilityListener(PassiveAbilityFirer firer) {
        this.firer = firer;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlock(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.isBlocking()) return;

        LivingEntity attacker = resolveAttacker(event.getDamager());
        firer.fire(player, PlayerAbilityTrigger.ON_BLOCK, attacker);
    }

    private static LivingEntity resolveAttacker(Entity damager) {
        if (damager instanceof LivingEntity le) return le;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof LivingEntity le) return le;
        return null;
    }
}
