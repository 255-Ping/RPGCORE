package com.github._255_ping.rpg.enchanting;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the per-level scaling math in {@link EnchantDef#statsAtLevel}. This is the only
 * pure-logic surface inside the enchanting addon; everything else depends on Bukkit / Inventory.
 */
class EnchantDefTest {

    private static final double EPS = 1e-9;

    private EnchantDef sharpness(double scale) {
        return new EnchantDef("sharpness", "Sharpness", List.of(), 5,
                List.of("sword"), Map.of("strength", 5.0), scale,
                0L, 0.0, 1);
    }

    @Test void level0_returnsEmpty() {
        assertTrue(sharpness(1.0).statsAtLevel(0).isEmpty());
    }

    @Test void level1_returnsBase() {
        var stats = sharpness(1.0).statsAtLevel(1);
        assertEquals(5.0, stats.get("strength"), EPS);
    }

    @Test void scale1_isLinearOverLevels() {
        var def = sharpness(1.0);
        // level=1 -> 5, level=2 -> 5 * (1 + 1*1) = 10, level=3 -> 15
        assertEquals(5.0, def.statsAtLevel(1).get("strength"), EPS);
        assertEquals(10.0, def.statsAtLevel(2).get("strength"), EPS);
        assertEquals(15.0, def.statsAtLevel(3).get("strength"), EPS);
    }

    @Test void scaleFractional_isLinear() {
        var def = sharpness(0.5);
        // level=2 -> 5 * (1 + 1*0.5) = 7.5
        assertEquals(7.5, def.statsAtLevel(2).get("strength"), EPS);
    }
}
