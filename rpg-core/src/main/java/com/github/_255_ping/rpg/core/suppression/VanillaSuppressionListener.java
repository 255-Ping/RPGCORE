package com.github._255_ping.rpg.core.suppression;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Cancels the slice of vanilla events that already have agreed-on replacements.
 * Each cancellation is gated by its config flag under {@code vanilla-suppression}.
 *
 * <p>This is the foundation set; flags whose listener wiring hasn't landed yet
 * (damage, mob-loot, mob-ai, enchanting-table, anvil, brewing-stand, crafting,
 * smelting, fishing, villager-trading, beacons, death-drops, block-explosion-damage,
 * block-break-tagged) are no-ops until their slice ships.
 */
public final class VanillaSuppressionListener implements Listener {

    private final JavaPlugin plugin;

    public VanillaSuppressionListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean flag(String key) {
        FileConfiguration cfg = plugin.getConfig();
        return cfg.getBoolean("vanilla-suppression." + key, true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (!flag("mob-spawning")) return;
        CreatureSpawnEvent.SpawnReason r = e.getSpawnReason();
        if (r == CreatureSpawnEvent.SpawnReason.NATURAL
                || r == CreatureSpawnEvent.SpawnReason.JOCKEY
                || r == CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE
                || r == CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION
                || r == CreatureSpawnEvent.SpawnReason.RAID
                || r == CreatureSpawnEvent.SpawnReason.PATROL
                || r == CreatureSpawnEvent.SpawnReason.LIGHTNING
                || r == CreatureSpawnEvent.SpawnReason.SLIME_SPLIT
                || r == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS
                || r == CreatureSpawnEvent.SpawnReason.SILVERFISH_BLOCK
                || r == CreatureSpawnEvent.SpawnReason.TRAP) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent e) {
        if (!flag("hunger")) return;
        e.setCancelled(true);
        e.setFoodLevel(20);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onRegen(EntityRegainHealthEvent e) {
        if (!flag("player-regen")) return;
        if (e.getEntityType() != EntityType.PLAYER) return;
        if (e.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED
                || e.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onXp(PlayerExpChangeEvent e) {
        if (!flag("xp-orbs")) return;
        e.setAmount(0);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onRaid(RaidTriggerEvent e) {
        if (!flag("raids")) return;
        e.setCancelled(true);
    }
}
