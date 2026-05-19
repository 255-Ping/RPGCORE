package com.github._255_ping.rpg.api.skills;

import org.bukkit.entity.Player;

public interface SkillsService {
    int level(Player player, String skillId);
    long totalXp(Player player, String skillId);
    long xpToNext(Player player, String skillId);
    void awardXp(Player player, String skillId, long amount);
    void setLevel(Player player, String skillId, int level);
    void setTotalXp(Player player, String skillId, long totalXp);
    int maxLevel(String skillId);
    String pinnedSkill(Player player);
    void pinSkill(Player player, String skillId);
}
