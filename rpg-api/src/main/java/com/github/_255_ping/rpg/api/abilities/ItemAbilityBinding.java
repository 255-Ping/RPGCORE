package com.github._255_ping.rpg.api.abilities;

import java.util.List;

/**
 * Pairs a {@link PlayerAbilityTrigger} with a sequence of {@link AbilityInvocation}s.
 * One item can have many bindings, each on a different trigger.
 */
public record ItemAbilityBinding(PlayerAbilityTrigger trigger, List<AbilityInvocation> invocations) {

    public ItemAbilityBinding {
        invocations = List.copyOf(invocations);
    }
}
