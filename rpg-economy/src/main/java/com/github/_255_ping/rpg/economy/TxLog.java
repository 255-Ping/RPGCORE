package com.github._255_ping.rpg.economy;

import com.github._255_ping.rpg.api.RpgServices;
import com.github._255_ping.rpg.api.persistence.DataStore;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player economy transaction log. In-memory cache per session, backed by DataStore.
 * Entries are kept newest-first and capped at {@code maxEntries}.
 */
public final class TxLog {

    public enum Kind { DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT, SET }

    public record Entry(Kind kind, BigDecimal amount, String reason, long timestampMs) {}

    static final String REPO = "txlog";

    private final int maxEntries;
    private final ConcurrentHashMap<UUID, Deque<Entry>> logs = new ConcurrentHashMap<>();

    public TxLog(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
    }

    public void append(UUID player, Kind kind, BigDecimal amount, String reason) {
        if (amount == null || amount.signum() == 0) return;
        Deque<Entry> deque = logs.computeIfAbsent(player, k -> new ArrayDeque<>());
        Entry entry = new Entry(kind, amount, reason == null ? "" : reason, System.currentTimeMillis());
        synchronized (deque) {
            deque.addFirst(entry);
            while (deque.size() > maxEntries) deque.removeLast();
        }
    }

    /** Returns an immutable snapshot of log entries (newest first) for the given player. */
    public List<Entry> get(UUID player) {
        Deque<Entry> deque = logs.get(player);
        if (deque == null) return List.of();
        synchronized (deque) {
            return List.copyOf(deque);
        }
    }

    public void load(UUID player) {
        if (logs.containsKey(player)) return;
        try {
            DataStore.Repository repo = RpgServices.dataStore().repository(REPO);
            repo.get(player.toString()).ifPresent(data -> {
                Object raw = data.get("entries");
                if (!(raw instanceof List<?> rawList)) return;
                Deque<Entry> deque = new ArrayDeque<>();
                for (Object item : rawList) {
                    if (!(item instanceof Map<?, ?> map)) continue;
                    try {
                        Kind kind = Kind.valueOf(String.valueOf(map.get("kind")));
                        BigDecimal amount = new BigDecimal(String.valueOf(map.get("amount")));
                        String reason = String.valueOf(map.getOrDefault("reason", ""));
                        long ts = Long.parseLong(String.valueOf(map.get("ts")));
                        deque.addLast(new Entry(kind, amount, reason, ts));
                    } catch (Exception ignored) {}
                }
                logs.put(player, deque);
            });
        } catch (IllegalStateException ignored) {}
    }

    public void save(UUID player) {
        Deque<Entry> deque = logs.get(player);
        if (deque == null) return;
        List<Map<String, Object>> serialized;
        synchronized (deque) {
            serialized = new ArrayList<>(deque.size());
            for (Entry e : deque) {
                serialized.add(Map.of(
                        "kind",   e.kind().name(),
                        "amount", e.amount().toPlainString(),
                        "reason", e.reason(),
                        "ts",     e.timestampMs()
                ));
            }
        }
        try {
            RpgServices.dataStore().repository(REPO)
                    .save(player.toString(), Map.of("entries", serialized));
        } catch (IllegalStateException ignored) {}
    }

    public void evict(UUID player) {
        logs.remove(player);
    }
}
