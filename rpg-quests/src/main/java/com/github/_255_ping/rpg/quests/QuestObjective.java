package com.github._255_ping.rpg.quests;

import java.util.Locale;

public record QuestObjective(Type type, String target, int count) {

    public enum Type {
        KILL_MOB, MINE_BLOCK, COLLECT_ITEM, TALK_NPC;

        public static Type fromString(String raw) {
            return Type.valueOf(raw.replace('-', '_').toUpperCase(Locale.ROOT));
        }
    }

    public String describe() {
        return switch (type) {
            case KILL_MOB -> "Kill " + count + " " + target;
            case MINE_BLOCK -> "Mine " + count + " " + target;
            case COLLECT_ITEM -> "Collect " + count + " " + target;
            case TALK_NPC -> "Speak with " + target;
        };
    }
}
