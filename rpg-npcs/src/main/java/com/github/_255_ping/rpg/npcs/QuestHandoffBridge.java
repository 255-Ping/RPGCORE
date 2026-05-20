package com.github._255_ping.rpg.npcs;

import org.bukkit.entity.Player;

/**
 * Bridge interface registered by rpg-quests via Bukkit's ServicesManager. rpg-npcs uses it to
 * hand off a player to a quest when they click a quest NPC. If rpg-quests isn't loaded, the
 * handoff is a no-op.
 */
public interface QuestHandoffBridge {
    void handoff(Player player, String questId);
}
