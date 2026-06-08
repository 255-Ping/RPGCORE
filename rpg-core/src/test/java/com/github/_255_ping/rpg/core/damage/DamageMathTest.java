package com.github._255_ping.rpg.core.damage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-math tests for {@link DamageMath#computePure}. The full {@code compute(DamageContext)}
 * entry point isn't covered here because it reads through RpgServices, which depends on a live
 * Bukkit server — integration testing for that is deferred.
 */
class DamageMathTest {

    private static final double EPS = 1e-6;

    @Test void noStats_returnsBase() {
        assertEquals(100.0, DamageMath.computePure(100, 0, 0, 0), EPS);
    }

    @Test void strengthScalesLinearly() {
        // 50% Strength → 1.5x base
        assertEquals(150.0, DamageMath.computePure(100, 50, 0, 0), EPS);
    }

    @Test void critDamageStacksOnStrength() {
        // 100 base × (1 + 0.5 STR) × (1 + 1.0 CRIT) = 100 × 1.5 × 2.0 = 300
        assertEquals(300.0, DamageMath.computePure(100, 50, 0, 100), EPS);
    }

    @Test void defenseAppliesDiminishingReturns() {
        // defense=100 → factor = 1 - 100/(100+100) = 0.5
        assertEquals(50.0, DamageMath.computePure(100, 0, 100, 0), EPS);
        // defense=300 → factor = 1 - 300/400 = 0.25
        assertEquals(25.0, DamageMath.computePure(100, 0, 300, 0), EPS);
    }

    @Test void defenseNeverNegative_clamped() {
        // High defense should asymptote toward 0, never below.
        double r = DamageMath.computePure(100, 0, 1_000_000, 0);
        assertTrue(r >= 0.0);
        assertTrue(r < 1.0, "expected near-zero, got " + r);
    }

    @Test void zeroBase_returnsZero() {
        assertEquals(0.0, DamageMath.computePure(0, 100, 0, 100), EPS);
    }

    @Test void fullStack() {
        // base=100, STR=100% → 200; CRIT=50% → 300; DEF=100 → halve → 150.
        assertEquals(150.0, DamageMath.computePure(100, 100, 100, 50), EPS);
    }

    // ── Additional edge cases ─────────────────────────────────────────────────

    @Test void zeroDefense_fullDamagePassesThrough() {
        // defense=0 → factor = 1 - 0/(0+100) = 1.0 → no mitigation at all.
        assertEquals(100.0, DamageMath.computePure(100, 0, 0, 0), EPS);
    }

    @Test void negativeBase_clampedToZero() {
        // Negative base can come from unusual ability chains; must never deal "healing" damage.
        assertTrue(DamageMath.computePure(-50, 0, 0, 0) >= 0.0);
    }

    @Test void zeroCritDamage_isNonCrit() {
        // critDamage=0 must not apply the crit multiplier (1 + 0/100 = 1, but guard is critDamage > 0).
        assertEquals(DamageMath.computePure(100, 50, 0, 0),
                     DamageMath.computePure(100, 50, 0, 0), EPS);
        // 100 * 1.5 * 1.0 (no crit) = 150
        assertEquals(150.0, DamageMath.computePure(100, 50, 0, 0), EPS);
    }

    @Test void highCritDamage_scalesCorrectly() {
        // base=100, no STR, no DEF, critDamage=400% → after_strength=100 → ×5 = 500
        assertEquals(500.0, DamageMath.computePure(100, 0, 0, 400), EPS);
    }

    @Test void defenseAtExactHundred_halvesUnbuffedDamage() {
        // Deterministic peg: DEF=100 always gives exactly 0.5× mitigation regardless of STR/crit.
        assertEquals(50.0, DamageMath.computePure(100, 0, 100, 0), EPS);
        assertEquals(75.0, DamageMath.computePure(100, 50, 100, 0), EPS); // ×1.5 then halved
    }

    @Test void largeDamageValues_noOverflow() {
        // Sanity-check that double arithmetic stays sane at extreme inputs.
        double result = DamageMath.computePure(1_000_000, 500, 50, 200);
        assertTrue(result > 0, "expected positive damage, got " + result);
        assertTrue(Double.isFinite(result), "result must be finite");
    }
}
