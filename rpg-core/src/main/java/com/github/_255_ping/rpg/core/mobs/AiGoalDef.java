package com.github._255_ping.rpg.core.mobs;

/**
 * One entry in a mob's {@code AiGoals:} list. Goals are evaluated top-to-bottom each AI
 * tick; the first one that can act wins.
 *
 * <p>Any mob with a non-empty goal list ignores its legacy {@code profile:} kind entirely.
 * Mobs without {@code AiGoals:} continue using the profile kind as before.
 *
 * <p>Goal DSL (one per line in YAML):
 * <pre>
 * attack_player
 * attack_faction{faction=undead}
 * attack_faction{faction=undead,range=16}
 * defend_faction{faction=guards,radius=20}
 * assist_faction{faction=guards,radius=20}
 * flee_from{faction=player,health_threshold=30}
 * call_for_help{faction=guards,radius=20}
 * guard_radius{radius=32}
 * idle
 * </pre>
 */
public sealed interface AiGoalDef permits
        AiGoalDef.AttackPlayer,
        AiGoalDef.AttackFaction,
        AiGoalDef.DefendFaction,
        AiGoalDef.AssistFaction,
        AiGoalDef.FleeFrom,
        AiGoalDef.CallForHelp,
        AiGoalDef.GuardRadius,
        AiGoalDef.Idle {

    /** Target the nearest non-creative player within the mob's configured aggression range. */
    record AttackPlayer() implements AiGoalDef {}

    /**
     * Target the nearest mob of {@code faction} within {@code range} blocks.
     * If {@code range} ≤ 0, the mob's aggression-range from the AI block is used.
     */
    record AttackFaction(String faction, double range) implements AiGoalDef {}

    /**
     * When any mob of {@code faction} within {@code radius} is attacked, target its attacker.
     * Alert data is populated by {@link MobAbilityEventListener} via {@link FactionAlertMap}
     * and checked on every AI tick.
     */
    record DefendFaction(String faction, double radius) implements AiGoalDef {}

    /**
     * If a mob of {@code faction} within {@code radius} already has a combat target,
     * join the fight by targeting the same entity.
     */
    record AssistFaction(String faction, double radius) implements AiGoalDef {}

    /**
     * Flee from the nearest entity of {@code faction} within {@code range}.
     * Activates only when this mob's HP% is at or below {@code healthThreshold}
     * (100 = always flee regardless of HP).
     */
    record FleeFrom(String faction, double range, double healthThreshold) implements AiGoalDef {}

    /**
     * When this mob is hurt, immediately alert all mobs of {@code faction} within
     * {@code radius} to target the attacker. Fires at hurt-time in
     * {@link MobAbilityEventListener} — positional in the goal list for ordering only.
     */
    record CallForHelp(String faction, double radius) implements AiGoalDef {}

    /**
     * Leash: if this mob has wandered more than {@code radius} blocks from its spawn point,
     * disengage (clear target) so vanilla idle pathfinding can drift it back.
     * Place this goal above combat goals to act as an outer boundary.
     */
    record GuardRadius(double radius) implements AiGoalDef {}

    /** Fallback — wander or stand idle; clears any combat target. */
    record Idle() implements AiGoalDef {}
}
