package com.github._255_ping.rpg.core.loot;

import com.github._255_ping.rpg.api.loot.Attribution;
import com.github._255_ping.rpg.api.loot.LootPool;
import com.github._255_ping.rpg.api.loot.RollMode;
import com.github._255_ping.rpg.core.mobs.CoreLootTable;

import java.util.List;

/**
 * Named, reusable implementation of {@link LootPool} backed by {@link CoreLootTable}'s rolling
 * logic.  Pools are loaded from {@code plugins/rpg-core/loot-pools/*.yml} and registered in
 * {@link CoreLootPoolRegistry}.
 *
 * <p>All item-roll, currency-roll, magic-find, and attribution logic is inherited from
 * {@link CoreLootTable}.  This class adds the {@code LootPool} identity (registered by name)
 * and exposes the {@code vanillaExp} / {@code combatExp} / {@code combatSkillId} rewards that
 * inline loot tables also carry (but are unused there by convention).
 */
public final class CoreLootPool extends CoreLootTable implements LootPool {

    public CoreLootPool(String id,
                        Attribution attribution,
                        RollMode rollMode,
                        List<Roll> rolls,
                        List<Guaranteed> guaranteed,
                        List<CurrencyRoll> currencyRolls,
                        int vanillaExp,
                        long combatExp,
                        String combatSkillId) {
        super(id, attribution, rollMode, rolls, guaranteed, currencyRolls,
              vanillaExp, combatExp, combatSkillId);
    }

    // vanillaExp(), combatExp(), combatSkillId() are all inherited from CoreLootTable
    // and satisfy the LootPool interface automatically.
}
