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
}
