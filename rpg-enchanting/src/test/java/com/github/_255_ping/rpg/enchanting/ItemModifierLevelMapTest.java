package com.github._255_ping.rpg.enchanting;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip tests for the PDC level-map format used to persist enchant/upgrade tiers on items.
 * Bad parsing or non-round-trip encoding would corrupt every enchanted item in the world, so this
 * is one of the higher-value test surfaces.
 */
class ItemModifierLevelMapTest {

    @Test void parseEmpty_returnsEmpty() {
        assertTrue(ItemModifier.parseLevelMap(null).isEmpty());
        assertTrue(ItemModifier.parseLevelMap("").isEmpty());
    }

    @Test void parseSinglePair() {
        var map = ItemModifier.parseLevelMap("sharpness:3");
        assertEquals(1, map.size());
        assertEquals(3, map.get("sharpness"));
    }

    @Test void parseMultiplePairs() {
        var map = ItemModifier.parseLevelMap("sharpness:3,protection:2,smite:1");
        assertEquals(3, map.size());
        assertEquals(3, map.get("sharpness"));
        assertEquals(2, map.get("protection"));
        assertEquals(1, map.get("smite"));
    }

    @Test void encodeEmpty_returnsEmpty() {
        assertEquals("", ItemModifier.encodeLevelMap(Map.of()));
    }

    @Test void encodeMaintainsInsertionOrder() {
        Map<String, Integer> in = new LinkedHashMap<>();
        in.put("first", 1);
        in.put("second", 2);
        assertEquals("first:1,second:2", ItemModifier.encodeLevelMap(in));
    }

    @Test void roundTripPreservesData() {
        Map<String, Integer> in = new LinkedHashMap<>();
        in.put("sharpness", 5);
        in.put("smite", 3);
        in.put("unbreaking", 1);
        String encoded = ItemModifier.encodeLevelMap(in);
        var back = ItemModifier.parseLevelMap(encoded);
        assertEquals(in, back);
    }

    @Test void parseGarbageEntriesAreSkipped() {
        var map = ItemModifier.parseLevelMap("sharpness:3,garbage,protection:2,malformed:notanumber");
        assertEquals(2, map.size());
        assertEquals(3, map.get("sharpness"));
        assertEquals(2, map.get("protection"));
    }
}
