package com.github._255_ping.rpg.core.skills;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlayerSkillState {

    private final ConcurrentMap<String, Long> totalXp = new ConcurrentHashMap<>();

    public long totalXp(String skillId) {
        return totalXp.getOrDefault(skillId, 0L);
    }

    public void setTotalXp(String skillId, long xp) {
        totalXp.put(skillId, Math.max(0, xp));
    }

    public void addTotalXp(String skillId, long delta) {
        totalXp.merge(skillId, Math.max(0, delta), Long::sum);
    }

    public ConcurrentMap<String, Long> raw() {
        return totalXp;
    }
}
