package com.github._255_ping.rpg.quests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestObjectiveTest {

    @Test void fromString_acceptsUnderscoresAndDashes() {
        assertEquals(QuestObjective.Type.KILL_MOB, QuestObjective.Type.fromString("kill_mob"));
        assertEquals(QuestObjective.Type.KILL_MOB, QuestObjective.Type.fromString("kill-mob"));
        assertEquals(QuestObjective.Type.MINE_BLOCK, QuestObjective.Type.fromString("MINE_BLOCK"));
        assertEquals(QuestObjective.Type.COLLECT_ITEM, QuestObjective.Type.fromString("collect-item"));
        assertEquals(QuestObjective.Type.TALK_NPC, QuestObjective.Type.fromString("Talk_Npc"));
    }

    @Test void fromString_rejectsUnknown() {
        assertThrows(IllegalArgumentException.class,
                () -> QuestObjective.Type.fromString("not-a-real-objective"));
    }

    @Test void describe_isReadable() {
        var o = new QuestObjective(QuestObjective.Type.KILL_MOB, "goblin", 10);
        assertEquals("Kill 10 goblin", o.describe());

        var t = new QuestObjective(QuestObjective.Type.TALK_NPC, "elder", 1);
        assertEquals("Speak with elder", t.describe());
    }
}
