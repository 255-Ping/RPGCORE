package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.abilities.AbilityInvocation;

import java.util.List;

/**
 * Binds a list of ability invocations to a trigger and an optional minimum mob level.
 * Abilities with {@code minLevel > 1} are silently skipped for mobs below that level.
 *
 * <p>YAML map format (supports min-level):
 * <pre>
 * Abilities:
 *   - invocation: "fireball"
 *     trigger: "OnTimer:30"
 *     min-level: 5
 * </pre>
 * Legacy string format defaults to min-level 1 (always fires):
 * <pre>
 * Abilities:
 *   - "fireball~OnTimer:30"
 * </pre>
 */
public record MobAbilityBinding(List<AbilityInvocation> invocations, MobAbilityTrigger trigger, int minLevel) {

    /** Backward-compat constructor — minLevel defaults to 1 (always fires). */
    public MobAbilityBinding(List<AbilityInvocation> invocations, MobAbilityTrigger trigger) {
        this(invocations, trigger, 1);
    }
}
