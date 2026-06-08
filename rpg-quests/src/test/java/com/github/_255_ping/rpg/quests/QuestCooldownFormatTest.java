package com.github._255_ping.rpg.quests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link QuestManager#formatCooldown(long)} — the human-readable cooldown string shown
 * to players when a repeatable quest is still on cooldown.
 *
 * <p>The method is pure Java (no Bukkit dependency); it was made package-private specifically
 * to enable this test class.
 */
class QuestCooldownFormatTest {

    @Test void zero_returnsZeroSeconds() {
        assertEquals("0s", QuestManager.formatCooldown(0));
    }

    @Test void negative_treatedAsZero() {
        // Negative remaining time shouldn't be reachable in practice, but must not throw.
        assertEquals("0s", QuestManager.formatCooldown(-5));
    }

    @Test void secondsOnly() {
        assertEquals("30s", QuestManager.formatCooldown(30));
        assertEquals("59s", QuestManager.formatCooldown(59));
    }

    @Test void minutesOnly() {
        assertEquals("1m", QuestManager.formatCooldown(60));
        assertEquals("45m", QuestManager.formatCooldown(2700));
        assertEquals("59m", QuestManager.formatCooldown(3540));
    }

    @Test void minutesAndSeconds() {
        // 90s = 1m 30s
        assertEquals("1m 30s", QuestManager.formatCooldown(90));
        // 3599s = 59m 59s
        assertEquals("59m 59s", QuestManager.formatCooldown(3599));
    }

    @Test void hoursOnly() {
        assertEquals("1h", QuestManager.formatCooldown(3600));
        assertEquals("3h", QuestManager.formatCooldown(10800));
    }

    @Test void hoursAndMinutes() {
        // 3h 30m
        assertEquals("3h 30m", QuestManager.formatCooldown(3 * 3600 + 30 * 60));
    }

    @Test void daysOnly() {
        assertEquals("1d", QuestManager.formatCooldown(86400));
        assertEquals("7d", QuestManager.formatCooldown(7 * 86400));
    }

    @Test void daysAndHours() {
        // 1 day + 3 hours
        assertEquals("1d 3h", QuestManager.formatCooldown(86400 + 3 * 3600));
    }

    @Test void fullBreakdown() {
        // 1d 2h 3m 4s
        long seconds = 86400 + 2 * 3600 + 3 * 60 + 4;
        assertEquals("1d 2h 3m 4s", QuestManager.formatCooldown(seconds));
    }

    @Test void dailyCooldown_24Hours() {
        // The standard daily quest uses exactly 86400s — a common real-world input.
        assertEquals("1d", QuestManager.formatCooldown(86400));
    }
}
