package com.github._255_ping.rpg.core.skills;

import com.github._255_ping.rpg.api.skills.BuiltinSkill;
import com.github._255_ping.rpg.api.skills.Skill;
import com.github._255_ping.rpg.api.skills.SkillRegistry;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CoreSkillRegistry implements SkillRegistry {

    private final ConcurrentMap<String, Skill> byId = new ConcurrentHashMap<>();

    public CoreSkillRegistry() {
        for (BuiltinSkill s : BuiltinSkill.values()) {
            byId.put(s.id(), s);
        }
    }

    @Override
    public void register(Skill skill) {
        byId.put(skill.id(), skill);
    }

    @Override
    public Optional<Skill> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Collection<Skill> all() {
        return byId.values();
    }
}
