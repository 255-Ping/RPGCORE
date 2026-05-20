package com.github._255_ping.rpg.quests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Per-player quest state. Active quests track an int counter per objective by ordinal index. */
public final class PlayerQuestState {

    public static final class Active {
        public final String questId;
        public final int[] progress;     // length == objective count

        public Active(String questId, int[] progress) {
            this.questId = questId;
            this.progress = progress;
        }

        public Map<String, Object> toMap() {
            return Map.of("quest", questId, "progress", intArrayToList(progress));
        }

        static Active fromMap(Map<String, Object> map) {
            String quest = String.valueOf(map.get("quest"));
            int[] progress;
            Object raw = map.get("progress");
            if (raw instanceof List<?> list) {
                progress = new int[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    progress[i] = list.get(i) instanceof Number n ? n.intValue() : 0;
                }
            } else {
                progress = new int[0];
            }
            return new Active(quest, progress);
        }

        private static List<Integer> intArrayToList(int[] a) {
            List<Integer> out = new ArrayList<>(a.length);
            for (int v : a) out.add(v);
            return out;
        }
    }

    public final UUID playerId;
    public final List<Active> active = new ArrayList<>();
    public final List<String> completed = new ArrayList<>();
    public final Map<String, Long> lastCompletionEpochSeconds = new LinkedHashMap<>();

    public PlayerQuestState(UUID playerId) {
        this.playerId = playerId;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> out = new HashMap<>();
        out.put("active", active.stream().map(Active::toMap).toList());
        out.put("completed", completed);
        out.put("lastCompletion", lastCompletionEpochSeconds);
        return out;
    }

    @SuppressWarnings("unchecked")
    public static PlayerQuestState fromMap(UUID playerId, Map<String, Object> map) {
        PlayerQuestState s = new PlayerQuestState(playerId);
        if (map.get("active") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    s.active.add(Active.fromMap((Map<String, Object>) m));
                }
            }
        }
        if (map.get("completed") instanceof List<?> list) {
            for (Object o : list) s.completed.add(String.valueOf(o));
        }
        if (map.get("lastCompletion") instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getValue() instanceof Number n) {
                    s.lastCompletionEpochSeconds.put(String.valueOf(e.getKey()), n.longValue());
                }
            }
        }
        return s;
    }
}
