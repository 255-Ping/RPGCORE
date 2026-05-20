package com.github._255_ping.rpg.api.skills;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a skill XP award causes a level transition. {@code newLevel} is the level reached;
 * {@code previousLevel} is what the player was at before the award. Both are inclusive: a single
 * award may span multiple levels, but this event fires once with the final {@code newLevel}.
 */
public class SkillLevelUpEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final String skillId;
    private final int previousLevel;
    private final int newLevel;

    public SkillLevelUpEvent(Player player, String skillId, int previousLevel, int newLevel) {
        super(player);
        this.skillId = skillId;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
    }

    public String skillId() { return skillId; }
    public int previousLevel() { return previousLevel; }
    public int newLevel() { return newLevel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
