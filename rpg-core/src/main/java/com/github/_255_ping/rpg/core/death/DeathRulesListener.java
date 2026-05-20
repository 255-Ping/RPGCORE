package com.github._255_ping.rpg.core.death;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.economy.Economy;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Resolves the first death-rule tier the player has permission for and applies it: clears the
 * vanilla drop list, randomly drops items from inventory at the configured percent, and spawns
 * a "coin pile" pickup item carrying the dropped currency amount.
 *
 * <p>The vanilla-suppression listener already clears drops + keep-inventory; this listener
 * runs at a higher priority and writes our own drops over the cleared list.
 */
public final class DeathRulesListener implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey coinPileKey;
    private final Random random = new Random();

    public DeathRulesListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.coinPileKey = new NamespacedKey(plugin, "coin_pile_amount");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        if (!plugin.getConfig().getBoolean("death-rules.enabled", true)) return;
        Player p = e.getEntity();
        DeathTier tier = resolveTier(p);
        if (tier == null) return;

        List<ItemStack> dropped = new ArrayList<>();
        if (tier.dropItemsPercent > 0) {
            for (ItemStack stack : p.getInventory().getContents()) {
                if (stack == null) continue;
                if (random.nextInt(100) < tier.dropItemsPercent) {
                    dropped.add(stack.clone());
                    stack.setAmount(0);
                }
            }
        }
        e.getDrops().clear();
        e.getDrops().addAll(dropped);
        e.setKeepInventory(false);

        // Coin pile
        BigDecimal coinDrop = resolveCoinDrop(tier.dropCoinsAmount, p);
        if (coinDrop != null && coinDrop.signum() > 0) {
            try {
                Economy economy = RpgServices.economy();
                if (economy.withdraw(p, coinDrop)) {
                    Item coinPile = p.getWorld().dropItem(p.getLocation(), buildCoinPile(coinDrop));
                    coinPile.setUnlimitedLifetime(false);
                    coinPile.setTicksLived(1);
                }
            } catch (IllegalStateException ignored) { /* economy not present */ }
        }
        if (!tier.keepXp) e.setKeepLevel(false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        ItemMeta meta = e.getItem().getItemStack().getItemMeta();
        if (meta == null) return;
        Double amount = meta.getPersistentDataContainer().get(coinPileKey, PersistentDataType.DOUBLE);
        if (amount == null) return;
        e.setCancelled(true);
        e.getItem().remove();
        try {
            RpgServices.economy().deposit(p, BigDecimal.valueOf(amount));
            p.sendActionBar(net.kyori.adventure.text.Component.text("+" + amount.longValue() + " coins"));
        } catch (IllegalStateException ignored) {}
    }

    private DeathTier resolveTier(Player p) {
        ConfigurationSection rules = plugin.getConfig().getConfigurationSection("death-rules");
        if (rules == null) return null;
        List<?> raw = rules.getList("tiers");
        if (raw == null) return null;
        for (Object o : raw) {
            if (!(o instanceof java.util.Map<?, ?> m)) continue;
            Object permObj = m.get("permission");
            String perm = permObj == null ? "" : String.valueOf(permObj);
            if (!perm.isEmpty() && !p.hasPermission(perm)) continue;
            Object dropI = m.get("drop-items-percent");
            int dropPct = dropI instanceof Number n ? n.intValue() : 100;
            Object dropC = m.get("drop-coins-amount");
            String coins = dropC == null ? "0" : String.valueOf(dropC);
            boolean keepXp = m.get("keep-xp") instanceof Boolean b && b;
            return new DeathTier(perm, dropPct, coins, keepXp);
        }
        return null;
    }

    private BigDecimal resolveCoinDrop(String spec, Player p) {
        if (spec == null || spec.isEmpty() || spec.equals("0")) return null;
        try {
            BigDecimal balance = RpgServices.economy().balance(p);
            String lower = spec.toLowerCase(Locale.ROOT);
            if (lower.equals("all")) return balance;
            if (lower.endsWith("percent")) {
                double pct = Double.parseDouble(lower.substring(0, lower.length() - "percent".length()));
                return balance.multiply(BigDecimal.valueOf(pct / 100.0));
            }
            return new BigDecimal(spec);
        } catch (IllegalStateException ex) {
            return null;
        } catch (NumberFormatException ex) {
            plugin.getLogger().warning("Bad drop-coins-amount: " + spec);
            return null;
        }
    }

    private ItemStack buildCoinPile(BigDecimal amount) {
        ItemStack stack = new ItemStack(org.bukkit.Material.SUNFLOWER); // generic gold-ish look
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("Coin Pile: " + amount.longValue())
                    .color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
            meta.getPersistentDataContainer().set(coinPileKey, PersistentDataType.DOUBLE, amount.doubleValue());
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
