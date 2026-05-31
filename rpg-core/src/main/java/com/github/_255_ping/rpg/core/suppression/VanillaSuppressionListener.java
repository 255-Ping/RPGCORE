package com.github._255_ping.rpg.core.suppression;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Cancels the slice of vanilla events that already have agreed-on replacements.
 * Each cancellation is gated by its config flag under {@code vanilla-suppression}.
 *
 * <p>Wired flags: mob-spawning, hunger, player-regen, xp-orbs, raids, enchanting-table,
 * anvil, brewing-stand, crafting, fishing, mob-loot, mob-ai (player-targeting),
 * smelting, villager-trading, beacons (potion-effect side), death-drops,
 * block-explosion-damage, pillager-patrols. The {@code damage} and {@code block-break-tagged}
 * flags are handled by their respective listeners (DamagePipelineListener, BlockBreakHandler).
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEnchantTablePrep(PrepareItemEnchantEvent e) {
        if (!flag("enchanting-table")) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAnvilPrep(PrepareAnvilEvent e) {
        if (!flag("anvil")) return;
        e.setResult(null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSmithing(PrepareSmithingEvent e) {
        if (!flag("anvil")) return;
        e.setResult(null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBrew(BrewEvent e) {
        if (!flag("brewing-stand")) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCraft(PrepareItemCraftEvent e) {
        if (!flag("crafting")) return;
        // Allow our own recipes through — only vanilla recipes get cleared.
        if (e.getRecipe() instanceof org.bukkit.Keyed keyed) {
            if (!"minecraft".equals(keyed.getKey().getNamespace())) return;
        }
        e.getInventory().setResult(null);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        if (!flag("fishing")) return;
        if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH) e.setCancelled(true);
    }

    // ----- mob-loot: cancel vanilla drops on tagged mobs (and globally if flag is on) -----
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMobLoot(EntityDeathEvent e) {
        if (!flag("mob-loot")) return;
        if (e.getEntity() instanceof org.bukkit.entity.Player) return;
        e.getDrops().clear();
        e.setDroppedExp(0);
    }

    // ----- mob-ai: cancel vanilla retargeting of players (our AI profiles drive this) -----
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTarget(EntityTargetEvent e) {
        if (!flag("mob-ai")) return;
        if (e.getTarget() == null) return;
        // Only suppress when the target is a player and the entity is a custom mob; vanilla
        // entities still have vanilla AI unless the admin tags them.
        if (e.getReason() == EntityTargetEvent.TargetReason.CLOSEST_PLAYER
                || e.getReason() == EntityTargetEvent.TargetReason.RANDOM_TARGET) {
            // Leave RpgMob-tagged entities to MobAiTask; cancel vanilla retargeting on others.
            // Cheap heuristic: if the entity has any PDC keys we care about, defer to AI task.
            if (e.getEntity().getPersistentDataContainer().getKeys().stream()
                    .anyMatch(k -> k.getNamespace().equals("rpg-core") && k.getKey().equals("mob_id"))) {
                return;
            }
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSmelt(FurnaceSmeltEvent e) {
        if (!flag("smelting")) return;
        // FurnaceSmeltEvent doesn't expose the recipe key directly; consult the iterator and
        // match the source/result. If the running recipe is in our namespace, let it through.
        java.util.Iterator<org.bukkit.inventory.Recipe> it = org.bukkit.Bukkit.recipeIterator();
        while (it.hasNext()) {
            org.bukkit.inventory.Recipe r = it.next();
            if (!(r instanceof org.bukkit.Keyed keyed)) continue;
            if (!plugin.getName().toLowerCase().equals(keyed.getKey().getNamespace())) continue;
            if (!(r instanceof org.bukkit.inventory.CookingRecipe<?> cr)) continue;
            if (cr.getResult().isSimilar(e.getResult())
                    && cr.getInputChoice().test(e.getSource())) {
                return; // ours — allow
            }
        }
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVillagerTrade(PlayerInteractEntityEvent e) {
        if (!flag("villager-trading")) return;
        EntityType t = e.getRightClicked().getType();
        if (t == EntityType.VILLAGER || t == EntityType.WANDERING_TRADER) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPotionEffectApply(EntityPotionEffectEvent e) {
        // Default false: vanilla potions work normally until a proper RPG potion→status-effect
        // converter is built. Set vanilla-suppression.potions: true in config to re-enable suppression.
        if (!plugin.getConfig().getBoolean("vanilla-suppression.potions", false)) return;
        // PLUGIN means an addon (status-effect framework) wrote it -> always let through.
        if (e.getCause() == EntityPotionEffectEvent.Cause.PLUGIN) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!flag("death-drops")) return;
        e.getDrops().clear();
        e.setDroppedExp(0);
        e.setKeepInventory(true);
        e.setKeepLevel(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (!flag("block-explosion-damage")) return;
        e.blockList().clear();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (!flag("block-explosion-damage")) return;
        e.blockList().clear();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent e) {
        // Side-effect of pillager-patrols suppression: nothing useful here yet, kept hookable.
        if (!flag("pillager-patrols")) return;
        // Patrol spawns flow through CreatureSpawnEvent.SpawnReason.PATROL — already covered above.
    }
}
