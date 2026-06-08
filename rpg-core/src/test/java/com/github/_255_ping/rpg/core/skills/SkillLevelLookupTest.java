package com.github._255_ping.rpg.core.skills;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the pure binary-search level lookup in {@link CoreSkillsService#levelForThresholds}.
 *
 * <p>The method is the hot path for every skill level display and XP-to-level conversion.
 * Tests use hand-crafted threshold arrays so no Bukkit / Plugin instance is needed.
 *
 * <p>Threshold array contract: {@code thresholds[i]} = total XP required to REACH level {@code i}.
 * Level 0 and 1 both start at 0 XP (players begin at level 1 with no XP), so typically:
 * <pre>  thresholds = [0, 0, 100, 300, 600, ...]</pre>
 */
class SkillLevelLookupTest {

    /**
     * Builds a simple threshold array: levels 0–maxLevel.
     * {@code thresholds[i] = (i-1) * step} for i≥2; levels 0 and 1 are both 0.
     */
    private static long[] linearThresholds(int maxLevel, long step) {
        long[] t = new long[maxLevel + 1];
        t[0] = 0;
        t[1] = 0;
        for (int i = 2; i <= maxLevel; i++) {
            t[i] = (long) (i - 1) * step;
        }
        return t;
    }

    @Test void zeroXp_returnsLevelOne() {
        long[] t = linearThresholds(10, 100);
        assertEquals(1, CoreSkillsService.levelForThresholds(t, 0));
    }

    @Test void exactBoundary_advancesToNextLevel() {
        // thresholds[2] = 100 → exactly 100 XP should resolve to level 2
        long[] t = linearThresholds(10, 100);
        assertEquals(2, CoreSkillsService.levelForThresholds(t, 100));
    }

    @Test void justBelowBoundary_staysAtCurrentLevel() {
        // 99 XP → still level 1 (threshold for level 2 is 100)
        long[] t = linearThresholds(10, 100);
        assertEquals(1, CoreSkillsService.levelForThresholds(t, 99));
    }

    @Test void midRange_returnsCorrectLevel() {
        // With step=100: level 5 requires 400 XP (t[5]=400), level 6 requires 500.
        // 450 XP → level 5.
        long[] t = linearThresholds(10, 100);
        assertEquals(5, CoreSkillsService.levelForThresholds(t, 450));
    }

    @Test void maxLevelXp_returnsCap() {
        // XP equal to the last threshold → capped at maxLevel.
        int max = 10;
        long[] t = linearThresholds(max, 100);
        // t[10] = 9 * 100 = 900
        assertEquals(max, CoreSkillsService.levelForThresholds(t, 900));
    }

    @Test void beyondMaxXp_stillReturnsCap() {
        // Any XP above the highest threshold should resolve to maxLevel (no overflow).
        int max = 10;
        long[] t = linearThresholds(max, 100);
        assertEquals(max, CoreSkillsService.levelForThresholds(t, Long.MAX_VALUE / 2));
    }

    @Test void nonLinearThresholds_correctLevel() {
        // Hand-crafted quadratic: t = [0, 0, 100, 300, 600, 1000]
        // Level 3 = 300 XP, level 4 = 600 XP.
        long[] t = { 0, 0, 100, 300, 600, 1000 };
        assertEquals(2, CoreSkillsService.levelForThresholds(t, 100));
        assertEquals(3, CoreSkillsService.levelForThresholds(t, 300));
        assertEquals(3, CoreSkillsService.levelForThresholds(t, 599));
        assertEquals(4, CoreSkillsService.levelForThresholds(t, 600));
        assertEquals(5, CoreSkillsService.levelForThresholds(t, 1000));
    }

    @Test void singleLevelArray_alwaysReturnsOne() {
        // Edge case: maxLevel=1 → thresholds = [0, 0]
        long[] t = { 0, 0 };
        assertEquals(1, CoreSkillsService.levelForThresholds(t, 0));
        assertEquals(1, CoreSkillsService.levelForThresholds(t, 999_999));
    }
}
