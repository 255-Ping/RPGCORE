package com.github._255_ping.rpg.core.abilities;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.AbilityContext;
import com.github._255_ping.rpg.api.abilities.AbilityPipeline;
import com.github._255_ping.rpg.api.items.BuiltinItemType;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.core.RpgCorePlugin;
import com.github._255_ping.rpg.core.abilities.effects.ManaCostEffect;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.concurrent.CompletionException;

/**
 * Right-click with a held RPG item that declares Abilities -> cast them.
 *
 * Air-rightclick on SWORD/WAND/BOW always fires; block-rightclick cancels the placement
 * only for those weapon types (so MATERIAL/QUEST items still place normally).
 */
public final class ItemAbilityListener implements Listener {

    private final RpgCorePlugin plugin;
    private final CoreAbilityRegistry registry;

    public ItemAbilityListener(RpgCorePlugin plugin, CoreAbilityRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack stack = event.getItem();
        if (stack == null || stack.getType().isAir()) return;

        Optional<RpgItem> opt = RpgServices.items().from(stack);
        if (opt.isEmpty()) return;
        RpgItem item = opt.get();
        if (item.abilities().isEmpty()) return;

        // Cancel block-place for weapon-type items so the right-click cast doesn't also place a block.
        if (action == Action.RIGHT_CLICK_BLOCK && isWeaponType(item)) {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        }

        Player player = event.getPlayer();
        double baseDamage = RpgServices.player(player).get(BuiltinStat.DAMAGE);
        AbilityContext ctx = new AbilityContext(player, baseDamage);
        ctx.setPoint(player.getEyeLocation());

        AbilityPipeline pipeline = new AbilityPipeline(item.abilities());
        pipeline.cast(ctx, registry).exceptionally(err -> {
            Throwable root = err instanceof CompletionException ce && ce.getCause() != null ? ce.getCause() : err;
            if (root instanceof ManaCostEffect.InsufficientMana) {
                player.sendActionBar(plugin.messages().component("ability.no-mana"));
            } else {
                plugin.getLogger().warning("Ability chain failed: " + root.getMessage());
            }
            return ctx;
        });
    }

    private static boolean isWeaponType(RpgItem item) {
        if (!(item.type() instanceof BuiltinItemType bt)) return false;
        return switch (bt) {
            case SWORD, BOW, WAND, CONSUMABLE, ACCESSORY, UPGRADE, QUEST -> true;
            case ARMOR, MATERIAL -> false;
        };
    }

    /** Convenience to ensure all built-ins are registered. Called once on plugin enable. */
    public static void registerBuiltins(CoreAbilityRegistry registry) {
        registry.register("damage", com.github._255_ping.rpg.core.abilities.effects.DamageEffect::new);
        registry.register("heal", com.github._255_ping.rpg.core.abilities.effects.HealEffect::new);
        registry.register("beam", com.github._255_ping.rpg.core.abilities.effects.BeamEffect::new);
        registry.register("explode", com.github._255_ping.rpg.core.abilities.effects.ExplodeEffect::new);
        registry.register("particles", com.github._255_ping.rpg.core.abilities.effects.ParticlesEffect::new);
        registry.register("sound", com.github._255_ping.rpg.core.abilities.effects.SoundEffect::new);
        registry.register("delay", com.github._255_ping.rpg.core.abilities.effects.DelayEffect::new);
        registry.register("apply_status", com.github._255_ping.rpg.core.abilities.effects.ApplyStatusEffect::new);
        registry.register("mana_cost", com.github._255_ping.rpg.core.abilities.effects.ManaCostEffect::new);
        registry.register("cooldown", com.github._255_ping.rpg.core.abilities.effects.CooldownEffect::new);
    }

}
