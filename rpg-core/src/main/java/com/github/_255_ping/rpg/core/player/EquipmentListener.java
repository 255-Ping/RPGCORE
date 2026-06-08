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
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
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
        // Defer one tick — Paper fires this event before the armor slot is updated in some versions,
        // so reading the inventory immediately would see stale contents. Matches ArmorSetListener.
        plugin.getServer().getScheduler().runTask(plugin, () -> recalc(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        // Armor is restored on respawn; recalc one tick after so the inventory is populated.
        plugin.getServer().getScheduler().runTask(plugin, () -> recalc(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        // Catches armour dragged directly into equipment slots (not covered by InventoryClickEvent).
        if (!(e.getWhoClicked() instanceof Player p)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> recalc(p));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        // Auto-equip on item pickup can put armour into equipment slots without a click event.
        if (!(e.getEntity() instanceof Player p)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> recalc(p));
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

    /**
     * Starts a repeating task that resyncs every online player's stats every
     * {@code intervalTicks} ticks. Acts as a safety net for any missed equipment-change
     * events (inventory edge cases, plugin interactions, etc.).
     */
    public void startResyncTask(JavaPlugin plugin, long intervalTicks) {
        plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> plugin.getServer().getOnlinePlayers().forEach(this::recalc),
                intervalTicks, intervalTicks);
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
     *
     * <p>Modifiers are scrubbed before setting the base value. Paper 1.21.4 dynamically
     * re-applies the vanilla material's default ATTACK_SPEED modifier (e.g. -2.4 for swords)
     * on each equip/unequip cycle even when the modifier was removed from the item meta via
     * {@code removeAttributeModifier()}. Without the scrub, these modifiers accumulate on
     * every hotbar switch, driving the effective value to ≤ 0 and permanently freezing the
     * sword animation (charge never rises). No vanilla potion effects add modifiers to this
     * attribute, so unconditional clearing is safe.
     */
    private static void applyAttackSpeed(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attr == null) return;

        // Purge accumulated modifiers before writing our value.
        for (AttributeModifier mod : java.util.List.copyOf(attr.getModifiers())) {
            attr.removeModifier(mod);
        }

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
