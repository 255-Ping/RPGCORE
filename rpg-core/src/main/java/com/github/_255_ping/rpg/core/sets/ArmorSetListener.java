package com.github._255_ping.rpg.core.sets;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.abilities.ItemAbilityBinding;
import com.github._255_ping.rpg.api.abilities.PlayerAbilityTrigger;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.sets.ArmorSetDef;
import com.github._255_ping.rpg.api.sets.SetBonus;
import com.github._255_ping.rpg.api.stats.Stat;
import com.github._255_ping.rpg.core.player.CorePlayerLookup;
import com.github._255_ping.rpg.core.player.CoreRpgPlayer;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Tracks which armor-set tiers are active per player.
 *
 * <p>On any armor-change event, this listener:
 * <ol>
 *   <li>Counts how many pieces of each set the player is wearing.</li>
 *   <li>Determines the active tier for each set (highest satisfied threshold).</li>
 *   <li>On tier change: updates the set-bonus stat layer in {@link CoreRpgPlayer}
 *       so that the next {@code recalculateStats()} picks them up.</li>
 *   <li>Caches the active passive/proc {@link ItemAbilityBinding}s for the player
 *       so proc listeners can query them cheaply.</li>
 * </ol>
 *
 * <p>This listener runs at {@link EventPriority#NORMAL} on armor-change events, which is
 * before {@code EquipmentListener}'s {@link EventPriority#MONITOR} recalc — so set-bonus
 * stats are committed to the player before stats are finalised.
 */
public final class ArmorSetListener implements Listener {

    private final JavaPlugin plugin;
    private final CoreArmorSetRegistry registry;
    private final CorePlayerLookup playerLookup;

    /** Maps UUID → (setId → active tier). Used to detect tier changes. */
    private final Map<UUID, Map<String, Integer>> activeTiers = new HashMap<>();

    /** Maps UUID → flat list of passive bindings currently active from set bonuses. */
    private final Map<UUID, List<ItemAbilityBinding>> activePassives = new HashMap<>();

    public ArmorSetListener(JavaPlugin plugin, CoreArmorSetRegistry registry,
                            CorePlayerLookup playerLookup) {
        this.plugin = plugin;
        this.registry = registry;
        this.playerLookup = playerLookup;
    }

    // ── Event hooks ──────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent e) {
        // Defer one tick — Paper fires this event before the slot is swapped in some versions.
        plugin.getServer().getScheduler().runTask(plugin, () -> refresh(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent e) {
        plugin.getServer().getScheduler().runTask(plugin, () -> refresh(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) {
            plugin.getServer().getScheduler().runTask(plugin, () -> refresh(p));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            plugin.getServer().getScheduler().runTask(plugin, () -> refresh(p));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        activeTiers.remove(uuid);
        activePassives.remove(uuid);
    }

    // ── Public query API ─────────────────────────────────────────────────────────

    /**
     * Returns all passive/proc ability bindings from the player's active set bonuses that match
     * {@code trigger}.  Called by {@link com.github._255_ping.rpg.core.abilities.PassiveAbilityFirer}.
     */
    public List<ItemAbilityBinding> getPassivesForTrigger(UUID uuid, PlayerAbilityTrigger trigger) {
        List<ItemAbilityBinding> passives = activePassives.get(uuid);
        if (passives == null || passives.isEmpty()) return List.of();
        return passives.stream().filter(b -> b.trigger() == trigger).toList();
    }

    // ── Core refresh logic ───────────────────────────────────────────────────────

    private void refresh(Player player) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();

        Map<String, Integer> pieceCounts = countPieces(player);
        Map<String, Integer> newTiers    = computeTiers(pieceCounts);
        Map<String, Integer> oldTiers    = activeTiers.getOrDefault(uuid, Map.of());

        if (newTiers.equals(oldTiers)) return; // nothing changed

        activeTiers.put(uuid, newTiers);

        // Recompute combined set-bonus stats and passive ability bindings.
        Map<Stat, Double> combinedStats = new HashMap<>();
        List<ItemAbilityBinding> combinedPassives = new ArrayList<>();

        for (ArmorSetDef def : registry.all()) {
            int tier = newTiers.getOrDefault(def.id(), 0);
            if (tier == 0) continue;
            SetBonus bonus = def.activeBonus(tier);
            if (bonus == null || bonus.isEmpty()) continue;
            bonus.stats().forEach((stat, val) ->
                    combinedStats.merge(stat, val, Double::sum));
            combinedPassives.addAll(bonus.abilities());
        }

        activePassives.put(uuid, Collections.unmodifiableList(combinedPassives));

        // Push the new stat layer into the player object.  EquipmentListener's MONITOR handler
        // will fire recalculateStats() shortly after this NORMAL handler returns, picking these up.
        CoreRpgPlayer corePlayer = playerLookup.getOrCreate(player);
        corePlayer.setSetBonusStats(combinedStats);
    }

    /** Counts how many pieces of each set the player is currently wearing in their armor slots. */
    private static Map<String, Integer> countPieces(Player player) {
        Map<String, Integer> counts = new HashMap<>();
        ItemStack[] armor = {
            player.getInventory().getHelmet(),
            player.getInventory().getChestplate(),
            player.getInventory().getLeggings(),
            player.getInventory().getBoots()
        };
        for (ItemStack stack : armor) {
            if (stack == null || stack.getType().isAir()) continue;
            Optional<RpgItem> opt;
            try {
                opt = RpgServices.items().from(stack);
            } catch (IllegalStateException ex) {
                continue;
            }
            if (opt.isEmpty()) continue;
            String sid = opt.get().setId();
            if (sid != null && !sid.isBlank()) {
                counts.merge(sid, 1, Integer::sum);
            }
        }
        return counts;
    }

    /** For each known set, determine which threshold is currently satisfied. Returns tier 0 = none. */
    private Map<String, Integer> computeTiers(Map<String, Integer> pieceCounts) {
        Map<String, Integer> tiers = new HashMap<>();
        for (ArmorSetDef def : registry.all()) {
            int count = pieceCounts.getOrDefault(def.id(), 0);
            int activeTier = def.bonuses().keySet().stream()
                    .filter(t -> t <= count)
                    .max(Integer::compareTo)
                    .orElse(0);
            tiers.put(def.id(), activeTier);
        }
        return tiers;
    }
}
