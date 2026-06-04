package com.github._255_ping.rpg.core.abilities;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityPipeline;
import com.github._255_ping.rpg.api.abilities.ItemAbilityBinding;
import com.github._255_ping.rpg.api.abilities.PlayerAbilityTrigger;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Shared helper — collects and fires passive/proc ability bindings for a player.
 *
 * <p>Sources checked in order:
 * <ol>
 *   <li>All equipped armor slots (helmet → boots)</li>
 *   <li>Main-hand item (for ON_HIT / PASSIVE only)</li>
 *   <li>Active armor-set bonuses (from {@link com.github._255_ping.rpg.core.sets.ArmorSetListener})</li>
 * </ol>
 *
 * <p>Set-bonus abilities fire exactly once even if multiple pieces of the same set are worn.
 */
public final class PassiveAbilityFirer {

    private final CoreAbilityRegistry registry;
    private final Logger logger;
    // Lazily resolved to avoid circular dependency at construction time.
    private com.github._255_ping.rpg.core.sets.ArmorSetListener armorSetListener;

    public PassiveAbilityFirer(CoreAbilityRegistry registry, Logger logger) {
        this.registry = registry;
        this.logger = logger;
    }

    /** Inject the ArmorSetListener after it is created (avoids circular constructor dependency). */
    public void setArmorSetListener(com.github._255_ping.rpg.core.sets.ArmorSetListener asl) {
        this.armorSetListener = asl;
    }

    /**
     * Fires all bindings matching {@code trigger} from the player's equipment and active set bonuses.
     *
     * @param player  the player
     * @param trigger which trigger to fire
     * @param target  the combat target (may be null for non-combat triggers)
     */
    public void fire(Player player, PlayerAbilityTrigger trigger, org.bukkit.entity.LivingEntity target) {
        List<ItemAbilityBinding> bindings = collectBindings(player, trigger);
        if (bindings.isEmpty()) return;

        double baseDamage = RpgServices.player(player).get(BuiltinStat.DAMAGE);
        for (ItemAbilityBinding binding : bindings) {
            AbilityContext ctx = new AbilityContext(player, baseDamage);
            ctx.setPoint(player.getEyeLocation());
            if (target != null) ctx.setTarget(target);
            AbilityPipeline pipeline = new AbilityPipeline(binding.invocations());
            pipeline.cast(ctx, registry).exceptionally(err -> {
                logger.fine("Passive ability error for " + player.getName()
                        + " trigger=" + trigger + ": " + err.getMessage());
                return ctx;
            });
        }
    }

    private List<ItemAbilityBinding> collectBindings(Player player, PlayerAbilityTrigger trigger) {
        List<ItemAbilityBinding> result = new ArrayList<>();

        // Armor slots
        ItemStack[] armor = {
            player.getInventory().getHelmet(),
            player.getInventory().getChestplate(),
            player.getInventory().getLeggings(),
            player.getInventory().getBoots()
        };
        for (ItemStack stack : armor) {
            addBindingsFrom(stack, trigger, result);
        }

        // Main hand (relevant for all triggers — e.g. a sword with ~on_hit)
        addBindingsFrom(player.getInventory().getItemInMainHand(), trigger, result);

        // Active set bonuses (fire once per set, not once per piece)
        if (armorSetListener != null) {
            result.addAll(armorSetListener.getPassivesForTrigger(player.getUniqueId(), trigger));
        }

        return result;
    }

    private static void addBindingsFrom(ItemStack stack, PlayerAbilityTrigger trigger,
                                        List<ItemAbilityBinding> out) {
        if (stack == null || stack.getType().isAir()) return;
        Optional<RpgItem> opt;
        try {
            opt = RpgServices.items().from(stack);
        } catch (IllegalStateException ex) {
            return;
        }
        if (opt.isEmpty()) return;
        for (ItemAbilityBinding b : opt.get().triggeredAbilities()) {
            if (b.trigger() == trigger) out.add(b);
        }
    }
}
