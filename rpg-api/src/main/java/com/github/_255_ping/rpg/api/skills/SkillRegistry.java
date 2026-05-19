package com.github._255_ping.rpg.api.skills;

import java.util.Collection;
import java.util.Optional;

public interface SkillRegistry {
    void register(Skill skill);
    Optional<Skill> get(String id);
    Collection<Skill> all();
}
