package com.github._255_ping.rpg.core.mobs;

import com.github._255_ping.rpg.api.abilities.AbilityInvocation;

import java.util.List;

public record MobAbilityBinding(List<AbilityInvocation> invocations, MobAbilityTrigger trigger) {}
