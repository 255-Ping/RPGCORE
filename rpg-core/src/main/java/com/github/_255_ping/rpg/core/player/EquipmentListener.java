package com.github._255_ping.rpg.core.player;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.player.RpgPlayer;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import com.github._255_ping.rpg.core.health.CoreHealthService;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Triggers stat recalculation when a player's equipped gear changes. After recalc,
 * resyncs HealthService max HP so equipping +max_health gear actually raises the cap.
 *
 * Also applies the {@code AttackCooldown} field from the held item to the
 * {@code generic.attack_speed} Bukkit attribute, which controls how fast Minecraft's
 * attack charge bar fills. This enables both the visual charge indicator and the
 * damage scaling in {@link com.github._255_ping.rpg.core.damage.DamagePipelineListener}.
 */
public final class EquipmentListener implements Listener {

    private static final double DEFAULT_ATTACK_SPEED = 4.0;           // Minecraft player default
    private static final double DEFAULT_MOVEMENT_SPEED = 0.1;         // Minecraft player default (blocks/tick)
    private static final double DEFAULT_ENTITY_INTERACTION_RANGE = 3.0; // Minecraft player default (blocks)

    private final JavaPlugin plugin;
    private final CoreHealthService health;

    public EquipmentListener(JavaPlugin plugin, CoreHealthService health) {
        this.plugin = plugin;
        this.health = health;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        // Defer one tick so inventory is fully loaded before applying attributes.
        plugin.getServer().getScheduler().runTask(plugin, () -> recalc(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent e) {
        recalc(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHotbarSwitch(PlayerItemHeldEvent e) {
        // Defer one tick so the held-slot index has updated.
        plugin.getServer().getScheduler().runTask(plugin, () -> recalc(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        plugin.getServer().getScheduler().runTask(plugin, () -> recalc(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) {
            recalc(p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> recalc(p));
    }

    /**
     * Public entry-point for external callers (e.g. the admin {@code /rpg fix} command) to
     * force a complete stat + attribute resync without going through an equipment-change event.
     */
    public void resync(Player player) {
        recalc(player);
    }

    private void recalc(Player player) {
        if (player == null || !player.isOnline()) return;
        RpgPlayer rp = RpgServices.player(player);
        rp.recalculateStats();
        // Sync max HP so equip/unequip of max_health gear updates the bar cap.
        double newMax = rp.get(BuiltinStat.MAX_HEALTH);
        if (newMax > 0) {
            health.setMaxHp(player, newMax);
        }
        applyAttackSpeed(player);
        applyMovementSpeed(player);
        applySwingRange(player);
    }

    /**
     * Set the {@code generic.attack_speed} attribute to match the held item's
     * {@code AttackCooldown} field. This controls how fast the charge bar fills.
     * {@code AttackCooldown: 20} = 1 attack/sec, {@code AttackCooldown: 10} = 2/sec, etc.
     */
    private static void applyAttackSpeed(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attr == null) return;

        int cooldownTicks = 0;
        try {
            var opt = RpgServices.items().from(player.getInventory().getItemInMainHand());
            if (opt.isPresent()) cooldownTicks = opt.get().attackCooldownTicks();
        } catch (Exception ignored) {}

        double speed = cooldownTicks > 0 ? (20.0 / cooldownTicks) : DEFAULT_ATTACK_SPEED;
        attr.setBaseValue(speed);
    }

    /**
     * Apply the {@code speed} stat to {@code generic.movement_speed}.
     * Each speed point adds {@code stats.speed-per-point} percent over the vanilla base (0.1 b/tick).
     * Formula: {@code 0.1 * (1 + speed * pctPerPoint / 100)}.
     */
    private void applyMovementSpeed(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;

        // Scrub orphaned MOVEMENT_SPEED modifiers. Vanilla Speed/Slowness potion effects add
        // MULTIPLY_TOTAL modifiers that stack on top of the base value — they survive a server
        // restart even if the potion effect itself expired during downtime, leaving the player
        // permanently slowed with no visible potion icon. We only do this cleanup when the
        // player has neither effect active, so we never disturb legitimate vanilla modifiers.
        if (!player.hasPotionEffect(PotionEffectType.SLOWNESS)
                && !player.hasPotionEffect(PotionEffectType.SPEED)) {
            for (AttributeModifier mod : java.util.List.copyOf(attr.getModifiers())) {
                attr.removeModifier(mod);
            }
        }

        double speed = RpgServices.player(player).get(BuiltinStat.SPEED);
        double pctPerPoint = plugin.getConfig().getDouble("stats.speed-per-point", 1.0);
        attr.setBaseValue(DEFAULT_MOVEMENT_SPEED * (1.0 + speed * pctPerPoint / 100.0));
    }

    /**
     * Apply the {@code swing_range} stat to {@code entity_interaction_range} (melee reach).
     * Each swing_range point adds {@code stats.swing-range-per-point} blocks to the vanilla
     * default of 3.0 blocks.
     * Formula: {@code 3.0 + swingRange * blocksPerPoint}.
     */
    private void applySwingRange(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        if (attr == null) return;
        double swingRange = RpgServices.player(player).get(BuiltinStat.SWING_RANGE);
        double blocksPerPoint = plugin.getConfig().getDouble("stats.swing-range-per-point", 1.0);
        attr.setBaseValue(DEFAULT_ENTITY_INTERACTION_RANGE + swingRange * blocksPerPoint);
    }
}
