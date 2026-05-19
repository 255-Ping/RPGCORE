package com.github._255_ping.rpg.api.skills;

public sealed interface Skill permits BuiltinSkill, CustomSkill {
    String id();
    String displayName();
}
