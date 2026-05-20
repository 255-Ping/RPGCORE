package com.github._255_ping.rpg.core.stats;

import com.github._255_ping.rpg.api.stats.BuiltinStat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the API contract on the mutable stat holder used by recalc. The {@code add} /
 * {@code multiply} methods are the extension point addons (rpg-enchanting) use to inject stats
 * during a recalc — regressions here would silently break stat layering.
 */
class MutableStatHolderTest {

    private static final double EPS = 1e-9;

    @Test void defaultGetReturnsZero() {
        var h = new MutableStatHolder();
        assertEquals(0.0, h.get(BuiltinStat.STRENGTH), EPS);
    }

    @Test void setOverwrites() {
        var h = new MutableStatHolder();
        h.set(BuiltinStat.STRENGTH, 10);
        h.set(BuiltinStat.STRENGTH, 20);
        assertEquals(20.0, h.get(BuiltinStat.STRENGTH), EPS);
    }

    @Test void addAccumulates() {
        var h = new MutableStatHolder();
        h.set(BuiltinStat.STRENGTH, 10);
        h.add(BuiltinStat.STRENGTH, 5);
        h.add(BuiltinStat.STRENGTH, 3);
        assertEquals(18.0, h.get(BuiltinStat.STRENGTH), EPS);
    }

    @Test void addFromZeroWorks() {
        var h = new MutableStatHolder();
        h.add(BuiltinStat.CRIT_CHANCE, 25);
        assertEquals(25.0, h.get(BuiltinStat.CRIT_CHANCE), EPS);
    }

    @Test void multiplyAppliesPercent() {
        var h = new MutableStatHolder();
        h.set(BuiltinStat.STRENGTH, 100);
        h.multiply(BuiltinStat.STRENGTH, 50); // +50%
        assertEquals(150.0, h.get(BuiltinStat.STRENGTH), EPS);
    }

    @Test void clearResetsEverything() {
        var h = new MutableStatHolder();
        h.set(BuiltinStat.STRENGTH, 10);
        h.set(BuiltinStat.MAX_HEALTH, 200);
        h.clear();
        assertEquals(0.0, h.get(BuiltinStat.STRENGTH), EPS);
        assertEquals(0.0, h.get(BuiltinStat.MAX_HEALTH), EPS);
    }

    @Test void snapshotIsACopy() {
        var h = new MutableStatHolder();
        h.set(BuiltinStat.STRENGTH, 10);
        var snap = h.snapshot();
        h.set(BuiltinStat.STRENGTH, 99);
        // Snapshot should still reflect the old value.
        assertTrue(snap.get(BuiltinStat.STRENGTH).equals(10.0));
    }
}
