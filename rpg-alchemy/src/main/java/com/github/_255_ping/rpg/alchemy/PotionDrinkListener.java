package com.github._255_ping.rpg.alchemy;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * Intercepts vanilla POTION consumption for custom-potion items produced by {@link PotionItemFactory}.
 *
 * <p>We let vanilla drive the 1.6-second drinking animation via the normal item-use path. When the
 * animation completes {@link PlayerItemConsumeEvent} fires; we cancel vanilla consumption (suppresses
 * item removal, glass-bottle replacement, and any vanilla effect) and apply our RPG status effects +
 * manually remove the item instead.
 *
 * <p>Previously this was handled in {@code PlayerInteractEvent}, but in Paper 1.21.4 the
 * {@code ServerboundUseItemPacket} is processed before that event fires, so cancelling there does
 * not reliably suppress the animation or the eventual {@code PlayerItemConsumeEvent}.
 */
public final class PotionDrinkListener implements Listener {

    private final JavaPlugin plugin;
    private final AlchemyRegistry registry;
    private final PotionItemFactory potionItems;

    public PotionDrinkListener(JavaPlugin plugin, AlchemyRegistry registry, PotionItemFactory potionItems) {
        this.plugin = plugin;
        this.registry = registry;
        this.potionItems = potionItems;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.POTION) return;

        Optional<String> idOpt = potionItems.idOf(item);
        if (idOpt.isEmpty()) return;                          // not a custom potion — let vanilla handle it

        Optional<PotionDef> defOpt = registry.potion(idOpt.get());
        if (defOpt.isEmpty()) return;

        Player p = e.getPlayer();
        if (!p.hasPermission("rpg.alchemy.use.drink")) return;
        if (!plugin.getConfig().getBoolean("features.drinking", true)) return;

        // Cancel vanilla: this suppresses item removal, glass-bottle replacement, and vanilla effects.
        // We drive all three ourselves below.
        e.setCancelled(true);

        PotionDef def = defOpt.get();
        try {
            for (PotionDef.EffectSpec eff : def.effects()) {
                RpgServices.statusEffects().apply(p, eff.id(), eff.level(), eff.durationTicks(),
                        "potion:" + def.id());
            }
        } catch (IllegalStateException ignored) {
            // Status effect service not loaded — no-op.
        }

        if (def.consumeOnDrink()) {
            // Reduce the item in whichever hand held the potion.
            EquipmentSlot slot = e.getHand();
            ItemStack held = (slot == EquipmentSlot.HAND)
                    ? p.getInventory().getItemInMainHand()
                    : p.getInventory().getItemInOffHand();
            if (!held.isEmpty()) {
                held.setAmount(held.getAmount() - 1);
            }
        }

        long xp = plugin.getConfig().getLong("xp.per-drink", 0);
        if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.ALCHEMY.id(), xp);
    }
}
