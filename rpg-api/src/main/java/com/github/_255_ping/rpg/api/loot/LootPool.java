package com.github._255_ping.rpg.api.loot;

/**
 * A named, reusable loot table that can be referenced by ID from mob YAML, dungeon
 * definitions, and other content.  Extends {@link LootTable} — everywhere a
 * {@code LootTable} is accepted, a {@code LootPool} works too.
 *
 * <p>In addition to item rolls, pools carry two reward scalars that fire when the pool
 * is resolved on a kill:
 * <ul>
 *   <li>{@link #vanillaExp()} — vanilla XP orbs spawned at the mob's corpse.</li>
 *   <li>{@link #combatExp()} — skill XP awarded to each eligible player via
 *       {@code RpgServices.skills().awardXp(player, combatSkillId(), amount)}.</li>
 * </ul>
 *
 * <p>Pools are defined in {@code plugins/rpg-core/loot-pools/*.yml}:
 * <pre>
 * goblin_drops:
 *   attribution: last-hit
 *   roll-mode: per-player
 *   exp: 15
 *   combat-exp: 50
 *   rolls:
 *     - { item: goblin_fang, chance: 60.0, min: 1, max: 2 }
 *   guaranteed:
 *     - { item: bone, min: 1, max: 1 }
 *   currency-rolls:
 *     - { chance: 100.0, min: 20, max: 50 }
 * </pre>
 *
 * <p>Mobs reference pools via {@code LootPool: &lt;id&gt;} (single) or
 * {@code LootPools: [id1, id2]} (multiple; all roll independently).
 */
public interface LootPool extends LootTable {

    /**
     * Vanilla XP orbs to spawn at the corpse when this pool fires.
     * 0 = no XP orbs from this pool.
     */
    int vanillaExp();

    /**
     * Skill XP to award to each eligible player when this pool fires.
     * 0 = no skill XP from this pool.
     */
    long combatExp();

    /**
     * ID of the skill that receives {@link #combatExp()}.  Defaults to {@code "combat"}.
     */
    String combatSkillId();
}
