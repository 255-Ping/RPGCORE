package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.economy.Economy;
import com.github._255_ping.rpg.api.loot.LootContext;
import com.github._255_ping.rpg.api.loot.LootPool;
import com.github._255_ping.rpg.api.mobs.RpgMob;
import com.github._255_ping.rpg.core.loot.CoreLootPool;
import com.github._255_ping.rpg.core.drops.DropManager;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * On RPG mob death:
 * <ol>
 *   <li>Cancels vanilla drops.</li>
 *   <li>Sets vanilla XP (sum of mob's {@code XP:}, inline table {@code exp:}, and all
 *       referenced pool {@code exp:} values).</li>
 *   <li>Rolls the inline {@code LootTable:} if present.</li>
 *   <li>Resolves each named {@code LootPool:} / {@code LootPools:} reference and rolls them
 *       independently.</li>
 *   <li>Drops all rolled items at the corpse, optionally tagged for the assigned player via
 *       {@link DropManager}.</li>
 *   <li>Deposits currency rolls directly to player balances.</li>
 *   <li>Awards combat-skill XP from inline table and pools to all eligible damagers.</li>
 * </ol>
 */
public final class MobLootListener implements Listener {

    private final DamagerTracker tracker;
    private final DropManager dropManager;

    public MobLootListener(DamagerTracker tracker, DropManager dropManager) {
        this.tracker = tracker;
        this.dropManager = dropManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Optional<RpgMob> opt = RpgServices.mobs().from(victim);
        if (opt.isEmpty()) return;
        if (!(opt.get() instanceof CoreRpgMob def)) return;

        // Cancel vanilla drops — RPG loot is the source of truth.
        event.getDrops().clear();

        Player killer         = tracker.lastHitter(victim.getUniqueId());
        Map<Player, Double> damagers = tracker.takeFor(victim.getUniqueId());
        LootContext ctx       = new LootContext(victim, damagers, killer);

        // ── Vanilla XP ────────────────────────────────────────────────────────
        // Sum: mob's XP field + inline table exp + referenced table exp + all pool exp.
        int totalVanillaXp = (int) def.xp();
        CoreLootTable inlineTable = def.lootTable();
        if (inlineTable != null) totalVanillaXp += inlineTable.vanillaExp();

        // Resolve the external LootTable reference (if any) up-front so its exp contributes.
        CoreLootTable referencedTable = resolveTable(def);
        if (referencedTable != null) totalVanillaXp += referencedTable.vanillaExp();

        List<LootPool> resolvedPools = resolvePools(def);
        for (LootPool pool : resolvedPools) totalVanillaXp += pool.vanillaExp();

        // Suppress orb spawning — split XP directly to each damager proportional to
        // damage dealt so it lands in their level bar immediately instead of dropping.
        event.setDroppedExp(0);
        if (totalVanillaXp > 0) splitVanillaXp(damagers, totalVanillaXp);

        if (victim.getWorld() == null) return;

        // Elite loot multiplier: floor(mult) guaranteed rolls + fractional chance for one extra.
        // Non-elite mobs return 1.0 → exactly one roll as normal.
        double lootMult = EliteService.get() != null ? EliteService.get().lootMultiplier(victim) : 1.0;
        int fullRolls = (int) lootMult;
        double extraChance = lootMult - fullRolls;
        int totalRolls = fullRolls
                + (extraChance > 0 && ThreadLocalRandom.current().nextDouble() < extraChance ? 1 : 0);

        // ── Roll inline LootTable ──────────────────────────────────────────────
        if (inlineTable != null) {
            for (int i = 0; i < totalRolls; i++) {
                dropItems(victim, inlineTable.roll(ctx));
                depositCurrency(inlineTable.rollCurrency(ctx));
            }
        }

        // ── Roll external LootTable reference ──────────────────────────────────
        if (referencedTable != null) {
            for (int i = 0; i < totalRolls; i++) {
                dropItems(victim, referencedTable.roll(ctx));
                depositCurrency(referencedTable.rollCurrency(ctx));
            }
        }

        // ── Roll each named LootPool ───────────────────────────────────────────
        for (LootPool pool : resolvedPools) {
            for (int i = 0; i < totalRolls; i++) {
                dropItems(victim, pool.roll(ctx));
                if (pool instanceof CoreLootPool cp) depositCurrency(cp.rollCurrency(ctx));
            }
        }

        // ── Achievement: mob kills counter ────────────────────────────────────
        // Increment for every player who dealt damage, not just the killer.
        if (!damagers.isEmpty()) {
            try {
                var achievements = RpgServices.achievements();
                for (Player p : damagers.keySet()) {
                    if (p != null && p.isOnline()) {
                        achievements.increment(p, "mob_kills", 1L);
                        // Also track elite kills separately
                        if (EliteService.get() != null && EliteService.get().isElite(victim)) {
                            achievements.grant(p, "elite_slayer");
                        }
                    }
                }
            } catch (IllegalStateException ignored) {
                // Achievement service not loaded — silently skip.
            }
        }

        // ── Combat skill XP ───────────────────────────────────────────────────
        // Award to every player who dealt damage (skill XP is shared, not attributed).
        if (!damagers.isEmpty()) {
            long totalCombatExp = (inlineTable != null ? inlineTable.combatExp() : 0L)
                    + (referencedTable != null ? referencedTable.combatExp() : 0L);
            String combatSkill  = (inlineTable != null && !inlineTable.combatSkillId().isBlank())
                    ? inlineTable.combatSkillId()
                    : (referencedTable != null && !referencedTable.combatSkillId().isBlank())
                            ? referencedTable.combatSkillId() : "combat";
            // If any pool overrides the skill, use the first non-empty one found.
            for (LootPool pool : resolvedPools) {
                totalCombatExp += pool.combatExp();
                if (!pool.combatSkillId().isBlank()) combatSkill = pool.combatSkillId();
            }
            if (totalCombatExp > 0) {
                awardCombatXp(damagers.keySet(), combatSkill, totalCombatExp);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Resolves an external loot table ID reference ({@code LootTable: some_id}) from the
     * {@link com.github._255_ping.rpg.core.loot.CoreLootTableRegistry}. Returns {@code null}
     * when the mob has no external reference, or when the referenced table isn't loaded.
     */
    private CoreLootTable resolveTable(CoreRpgMob def) {
        String tableId = def.lootTableId();
        if (tableId == null || tableId.isBlank()) return null;
        try {
            Optional<com.github._255_ping.rpg.api.loot.LootTable> opt = RpgServices.lootTables().get(tableId);
            if (opt.isPresent() && opt.get() instanceof CoreLootTable clt) return clt;
            org.bukkit.Bukkit.getLogger().warning(
                    "[rpg-core] Mob '" + def.id() + "' references unknown loot table '" + tableId + "'");
        } catch (IllegalStateException ignored) {
            // LootTableRegistry not yet set — skip silently during startup.
        }
        return null;
    }

    /** Resolves the mob's loot-pool ID references from the registry. Warns on unknown IDs. */
    private List<LootPool> resolvePools(CoreRpgMob def) {
        if (def.lootPoolIds().isEmpty()) return List.of();
        List<LootPool> pools = new ArrayList<>();
        for (String poolId : def.lootPoolIds()) {
            try {
                Optional<LootPool> poolOpt = RpgServices.lootPools().get(poolId);
                if (poolOpt.isPresent()) {
                    pools.add(poolOpt.get());
                } else {
                    RpgServices.mobs(); // ensure service is up before logging
                    // Log once — warn in console that pool is missing.
                    org.bukkit.Bukkit.getLogger().warning(
                            "[rpg-core] Mob '" + def.id() + "' references unknown loot pool '" + poolId + "'");
                }
            } catch (IllegalStateException ignored) {
                // LootPoolRegistry not yet set — skip silently during startup.
            }
        }
        return pools;
    }

    private void dropItems(LivingEntity victim, Map<Player, List<ItemStack>> rolled) {
        for (Map.Entry<Player, List<ItemStack>> entry : rolled.entrySet()) {
            Player owner = entry.getKey();
            for (ItemStack s : entry.getValue()) {
                if (s == null) continue;
                Item dropped = victim.getWorld().dropItemNaturally(victim.getLocation(), s);
                if (owner != null) dropManager.register(dropped, owner);
            }
        }
    }

    private void depositCurrency(Map<Player, BigDecimal> currency) {
        if (currency.isEmpty()) return;
        try {
            Economy economy = RpgServices.economy();
            for (Map.Entry<Player, BigDecimal> entry : currency.entrySet()) {
                if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                    economy.deposit(entry.getKey(), entry.getValue(), "mob_drop");
                }
            }
        } catch (IllegalStateException ignored) {
            // rpg-economy not loaded — silently skip.
        }
    }

    private void awardCombatXp(Iterable<Player> players, String skillId, long amount) {
        try {
            var skills = RpgServices.skills();
            for (Player p : players) {
                if (p != null && p.isOnline()) skills.awardXp(p, skillId, amount);
            }
        } catch (IllegalStateException ignored) {
            // rpg-combat (skills service) not loaded — silently skip.
        }
    }

    /**
     * Splits {@code totalXp} vanilla Minecraft XP among damagers proportional to damage dealt,
     * awarding each player's share via {@link Player#giveExp(int)} so it goes straight to their
     * level bar rather than spawning pick-up orbs on the ground.
     * Players who are offline at the time of the kill receive nothing.
     */
    private void splitVanillaXp(Map<Player, Double> damagers, int totalXp) {
        if (damagers.isEmpty()) return;
        double totalDamage = damagers.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalDamage <= 0) return;
        for (Map.Entry<Player, Double> entry : damagers.entrySet()) {
            Player p = entry.getKey();
            if (p == null || !p.isOnline()) continue;
            int share = (int) Math.round(entry.getValue() / totalDamage * totalXp);
            if (share > 0) p.giveExp(share);
        }
    }
}
