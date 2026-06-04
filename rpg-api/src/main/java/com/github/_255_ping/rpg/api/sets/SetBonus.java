package com.github._255_ping.rpg.api.sets;

import com.github._255_ping.rpg.api.abilities.ItemAbilityBinding;
import com.github._255_ping.rpg.api.stats.Stat;

import java.util.List;
import java.util.Map;

/**
 * The reward granted when a player activates a tier of an {@link ArmorSetDef}.
 *
 * <p>Stats are added as a flat bonus on top of equipment stats (Layer 2.5 in the stat pipeline,
 * after equipment but before accessories and status modifiers).
 *
 * <p>Ability bindings behave identically to item-worn passive bindings — they are proc'd by the
 * same listeners ({@code ON_HIT}, {@code ON_HURT}, {@code ON_JUMP}, {@code PASSIVE}).  Active
 * click triggers ({@code RIGHT_CLICK}, etc.) on set bonuses are silently ignored — use item
 * abilities for player-activated effects.
 *
 * <p>Numeric params in abilities are already scaled at load time if the YAML declared
 * {@code Scale: <factor>}.
 */
public record SetBonus(Map<Stat, Double> stats, List<ItemAbilityBinding> abilities) {

    public SetBonus {
        stats = Map.copyOf(stats);
        abilities = List.copyOf(abilities);
    }

    /** Returns true if this bonus grants at least one stat or ability. */
    public boolean isEmpty() {
        return stats.isEmpty() && abilities.isEmpty();
    }
}
