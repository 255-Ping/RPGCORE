package com.github._255_ping.rpg.alchemy;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/** Right-click with a custom-potion item to drink it: applies its status effects. */
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
    public void onUse(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack hand = e.getItem();
        if (hand == null || hand.getType() != Material.POTION) return;
        Optional<String> idOpt = potionItems.idOf(hand);
        if (idOpt.isEmpty()) return;
        Optional<PotionDef> defOpt = registry.potion(idOpt.get());
        if (defOpt.isEmpty()) return;
        Player p = e.getPlayer();
        if (!p.hasPermission("rpg.alchemy.use.drink")) return;
        if (!plugin.getConfig().getBoolean("features.drinking", true)) return;

        e.setCancelled(true); // suppress vanilla drink animation; we drive the effect
        PotionDef def = defOpt.get();
        try {
            for (PotionDef.EffectSpec eff : def.effects()) {
                RpgServices.statusEffects().apply(p, eff.id(), eff.level(), eff.durationTicks(), "potion:" + def.id());
            }
        } catch (IllegalStateException ignored) {
            // status effect service not loaded — no-op
        }
        if (def.consumeOnDrink()) {
            hand.setAmount(hand.getAmount() - 1);
        }
        long xp = plugin.getConfig().getLong("xp.per-drink", 0);
        if (xp > 0) RpgServices.skills().awardXp(p, BuiltinSkill.ALCHEMY.id(), xp);
    }
}
