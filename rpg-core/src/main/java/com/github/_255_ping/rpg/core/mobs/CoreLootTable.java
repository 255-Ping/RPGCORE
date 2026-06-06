package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.items.RpgItem;
import com.github._255_ping.rpg.api.loot.Attribution;
import com.github._255_ping.rpg.api.loot.LootContext;
import com.github._255_ping.rpg.api.loot.LootTable;
import com.github._255_ping.rpg.api.loot.RollMode;
import com.github._255_ping.rpg.api.stats.BuiltinStat;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * YAML-driven loot table impl. Supports all four attribution modes and per-player vs
 * shared roll modes. Magic-find scales {@code magic-find-affected} chances by
 * {@code 1 + magic_find / 100}.
 */
public class CoreLootTable implements LootTable {

    public record Roll(String itemId, double chancePercent, int min, int max, boolean magicFindAffected) {}
    public record Guaranteed(String itemId, int min, int max) {}
    /** A currency drop: rolls a random amount in [min, max] with the given chance. Uses the primary currency. */
    public record CurrencyRoll(double chancePercent, long min, long max) {}

    /** Maximum multiplier applied to magic-find-affected drop chances. Configurable via rpg-core config. */
    private static double magicFindMultiplierCap = 3.0;

    public static void setMagicFindMultiplierCap(double cap) {
        magicFindMultiplierCap = Math.max(1.0, cap);
    }

    private final String id;
    private final Attribution attribution;
    private final RollMode rollMode;
    private final List<Roll> rolls;
    private final List<Guaranteed> guaranteed;
    private final List<CurrencyRoll> currencyRolls;
    /** Vanilla XP orbs to spawn at the corpse when this table fires. */
    private final int vanillaExp;
    /** Skill XP awarded to each eligible player when this table fires. */
    private final long combatExp;
    /** Skill ID that receives {@link #combatExp}. Defaults to {@code "combat"}. */
    private final String combatSkillId;

    public CoreLootTable(String id, Attribution attribution, RollMode rollMode,
                          List<Roll> rolls, List<Guaranteed> guaranteed) {
        this(id, attribution, rollMode, rolls, guaranteed, List.of(), 0, 0L, "combat");
    }

    public CoreLootTable(String id, Attribution attribution, RollMode rollMode,
                          List<Roll> rolls, List<Guaranteed> guaranteed, List<CurrencyRoll> currencyRolls) {
        this(id, attribution, rollMode, rolls, guaranteed, currencyRolls, 0, 0L, "combat");
    }

    public CoreLootTable(String id, Attribution attribution, RollMode rollMode,
                          List<Roll> rolls, List<Guaranteed> guaranteed, List<CurrencyRoll> currencyRolls,
                          int vanillaExp, long combatExp, String combatSkillId) {
        this.id = id;
        this.attribution = attribution;
        this.rollMode = rollMode;
        this.rolls = List.copyOf(rolls);
        this.guaranteed = List.copyOf(guaranteed);
        this.currencyRolls = List.copyOf(currencyRolls);
        this.vanillaExp = Math.max(0, vanillaExp);
        this.combatExp = Math.max(0L, combatExp);
        this.combatSkillId = (combatSkillId != null && !combatSkillId.isBlank()) ? combatSkillId : "combat";
    }

    public int vanillaExp()      { return vanillaExp; }
    public long combatExp()      { return combatExp; }
    public String combatSkillId(){ return combatSkillId; }

    @Override public String id() { return id; }
    @Override public Attribution attribution() { return attribution; }
    @Override public RollMode rollMode() { return rollMode; }

    @Override
    public Map<Player, List<ItemStack>> roll(LootContext context) {
        List<Player> eligible = pickEligible(context);
        Map<Player, List<ItemStack>> out = new HashMap<>();
        if (eligible.isEmpty()) return out;

        // Guaranteed drops: every eligible player gets one of each.
        for (Player p : eligible) {
            List<ItemStack> stacks = out.computeIfAbsent(p, k -> new ArrayList<>());
            for (Guaranteed g : guaranteed) {
                ItemStack stack = resolve(g.itemId(), rolled(g.min(), g.max()));
                if (stack != null) stacks.add(stack);
            }
        }

        if (rollMode == RollMode.SHARED) {
            // Roll the table once, distribute among eligible (round-robin for simplicity).
            int who = 0;
            for (Roll r : rolls) {
                double effective = r.magicFindAffected() && context.magicFindLoadout() != null
                        ? r.chancePercent() * Math.min(magicFindMultiplierCap, 1 + magicFind(context.magicFindLoadout()) / 100.0)
                        : r.chancePercent();
                if (ThreadLocalRandom.current().nextDouble(100.0) < effective) {
                    Player target = eligible.get(who % eligible.size());
                    who++;
                    ItemStack stack = resolve(r.itemId(), rolled(r.min(), r.max()));
                    if (stack != null) out.computeIfAbsent(target, k -> new ArrayList<>()).add(stack);
                }
            }
        } else {
            // PER_PLAYER — each eligible player rolls the whole table independently.
            for (Player p : eligible) {
                double mf = magicFind(p);
                double mfMult = Math.min(magicFindMultiplierCap, 1 + mf / 100.0);
                for (Roll r : rolls) {
                    double effective = r.magicFindAffected() ? r.chancePercent() * mfMult : r.chancePercent();
                    if (ThreadLocalRandom.current().nextDouble(100.0) < effective) {
                        ItemStack stack = resolve(r.itemId(), rolled(r.min(), r.max()));
                        if (stack != null) out.computeIfAbsent(p, k -> new ArrayList<>()).add(stack);
                    }
                }
            }
        }
        return out;
    }

    /**
     * Rolls the currency-rolls table and returns the amount each player should receive.
     * Callers are responsible for depositing via {@code RpgServices.economy()}.
     */
    public Map<Player, BigDecimal> rollCurrency(LootContext context) {
        if (currencyRolls.isEmpty()) return Collections.emptyMap();
        List<Player> eligible = pickEligible(context);
        Map<Player, BigDecimal> out = new HashMap<>();
        if (eligible.isEmpty()) return out;

        for (CurrencyRoll r : currencyRolls) {
            if (ThreadLocalRandom.current().nextDouble(100.0) >= r.chancePercent()) continue;
            long amount = r.min() >= r.max() ? r.min()
                    : ThreadLocalRandom.current().nextLong(r.min(), r.max() + 1);
            if (amount <= 0) continue;
            BigDecimal bd = BigDecimal.valueOf(amount);
            // All eligible players each receive the full rolled amount.
            for (Player p : eligible) {
                out.merge(p, bd, BigDecimal::add);
            }
        }
        return out;
    }

    private List<Player> pickEligible(LootContext ctx) {
        Map<Player, Double> damagers = ctx.damagers();
        if (damagers == null || damagers.isEmpty()) return Collections.emptyList();
        return switch (attribution) {
            case LAST_HIT -> ctx.magicFindLoadout() != null && damagers.containsKey(ctx.magicFindLoadout())
                    ? List.of(ctx.magicFindLoadout())
                    : List.of(damagers.keySet().iterator().next());
            case TOP_DAMAGER -> {
                Player top = null;
                double best = -1;
                for (Map.Entry<Player, Double> e : damagers.entrySet()) {
                    if (e.getValue() > best) { best = e.getValue(); top = e.getKey(); }
                }
                yield top == null ? Collections.emptyList() : List.of(top);
            }
            case SPLIT_EQUAL, WEIGHTED_BY_DAMAGE -> new ArrayList<>(damagers.keySet());
        };
    }

    private static double magicFind(Player p) {
        try {
            return RpgServices.player(p).get(BuiltinStat.MAGIC_FIND);
        } catch (Exception ex) {
            return 0;
        }
    }

    private static int rolled(int min, int max) {
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static ItemStack resolve(String itemId, int amount) {
        if (amount <= 0) return null;
        String cleanId = itemId;
        if (cleanId.startsWith("vanilla:")) cleanId = cleanId.substring(8);
        else if (cleanId.startsWith("minecraft:")) cleanId = cleanId.substring(10);

        Optional<RpgItem> custom = RpgServices.items().get(cleanId);
        if (custom.isPresent()) {
            ItemStack stack = custom.get().toItemStack();
            stack.setAmount(amount);
            return stack;
        }
        Material mat = Material.matchMaterial(cleanId);
        if (mat == null) return null;
        return new ItemStack(mat, amount);
    }
}
