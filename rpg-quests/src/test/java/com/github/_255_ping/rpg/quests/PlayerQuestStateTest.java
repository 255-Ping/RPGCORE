package com.github._255_ping.rpg.quests;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the serialisation / deserialisation round-trip for {@link PlayerQuestState}.
 *
 * <p>{@code toMap()} feeds the persistence layer (DataStore) and {@code fromMap()} restores it
 * on next login. A silent regression here corrupts player quest progress on every restart.
 * No Bukkit dependency — all types are pure Java.
 */
class PlayerQuestStateTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test void emptyState_roundTrips() {
        PlayerQuestState original = new PlayerQuestState(PLAYER);
        PlayerQuestState restored = PlayerQuestState.fromMap(PLAYER, original.toMap());

        assertEquals(PLAYER, restored.playerId);
        assertTrue(restored.active.isEmpty());
        assertTrue(restored.completed.isEmpty());
        assertTrue(restored.lastCompletionEpochSeconds.isEmpty());
    }

    @Test void activeQuest_progressPreserved() {
        PlayerQuestState s = new PlayerQuestState(PLAYER);
        s.active.add(new PlayerQuestState.Active("goblin_hunt", new int[]{3, 0}));

        PlayerQuestState r = PlayerQuestState.fromMap(PLAYER, s.toMap());

        assertEquals(1, r.active.size());
        assertEquals("goblin_hunt", r.active.get(0).questId);
        assertArrayEquals(new int[]{3, 0}, r.active.get(0).progress);
    }

    @Test void multipleActiveQuests_allPreserved() {
        PlayerQuestState s = new PlayerQuestState(PLAYER);
        s.active.add(new PlayerQuestState.Active("quest_a", new int[]{5}));
        s.active.add(new PlayerQuestState.Active("quest_b", new int[]{0, 2, 10}));

        PlayerQuestState r = PlayerQuestState.fromMap(PLAYER, s.toMap());

        assertEquals(2, r.active.size());
        assertEquals("quest_a", r.active.get(0).questId);
        assertArrayEquals(new int[]{5}, r.active.get(0).progress);
        assertEquals("quest_b", r.active.get(1).questId);
        assertArrayEquals(new int[]{0, 2, 10}, r.active.get(1).progress);
    }

    @Test void completedList_preservedInOrder() {
        PlayerQuestState s = new PlayerQuestState(PLAYER);
        s.completed.addAll(List.of("forest_intro", "goblin_menace", "goblin_hunt_ii"));

        PlayerQuestState r = PlayerQuestState.fromMap(PLAYER, s.toMap());

        assertEquals(List.of("forest_intro", "goblin_menace", "goblin_hunt_ii"), r.completed);
    }

    @Test void lastCompletionTimestamps_preserved() {
        PlayerQuestState s = new PlayerQuestState(PLAYER);
        long ts = 1_700_000_000L;
        s.lastCompletionEpochSeconds.put("daily_ore_run", ts);

        PlayerQuestState r = PlayerQuestState.fromMap(PLAYER, s.toMap());

        assertTrue(r.lastCompletionEpochSeconds.containsKey("daily_ore_run"));
        assertEquals(ts, r.lastCompletionEpochSeconds.get("daily_ore_run"));
    }

    @Test void fullState_roundTrips() {
        PlayerQuestState s = new PlayerQuestState(PLAYER);
        s.active.add(new PlayerQuestState.Active("active_quest", new int[]{7, 2}));
        s.completed.add("done_quest");
        s.lastCompletionEpochSeconds.put("done_quest", 1_000_000L);
        s.lastCompletionEpochSeconds.put("daily_ore_run", 2_000_000L);

        PlayerQuestState r = PlayerQuestState.fromMap(PLAYER, s.toMap());

        assertEquals(1, r.active.size());
        assertArrayEquals(new int[]{7, 2}, r.active.get(0).progress);
        assertEquals(List.of("done_quest"), r.completed);
        assertEquals(1_000_000L, r.lastCompletionEpochSeconds.get("done_quest"));
        assertEquals(2_000_000L, r.lastCompletionEpochSeconds.get("daily_ore_run"));
    }

    @Test void activeMapRoundTrip_viaActiveToFromMap() {
        // Tests Active.toMap() / Active.fromMap() in isolation.
        var original = new PlayerQuestState.Active("test_quest", new int[]{1, 2, 3});
        Map<String, Object> map = original.toMap();
        var restored = PlayerQuestState.Active.fromMap(map);

        assertEquals("test_quest", restored.questId);
        assertArrayEquals(new int[]{1, 2, 3}, restored.progress);
    }

    @Test void emptyProgress_roundTrips() {
        // Objective-less quest (e.g., TALK_NPC-only with count=0 progress array).
        var original = new PlayerQuestState.Active("talk_only", new int[0]);
        var restored = PlayerQuestState.Active.fromMap(original.toMap());
        assertEquals("talk_only", restored.questId);
        assertEquals(0, restored.progress.length);
    }
}
